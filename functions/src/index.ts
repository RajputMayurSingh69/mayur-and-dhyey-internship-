import * as crypto from "crypto";
import * as functions from "firebase-functions";
import { setGlobalOptions } from "firebase-functions/v2";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";

// Initialize Firebase Admin SDK
initializeApp();
const db = getFirestore();

// 1A: Global options: region asia-south1, maxInstances: 10
setGlobalOptions({
  region: "asia-south1",
  maxInstances: 10
});

// 1B: Secret Manager secret for HMAC phone hashing
export const phoneHmacKey = defineSecret("PHONE_HMAC_KEY");

/**
 * Computes HMAC-SHA256 hash of a 10-digit normalized phone number.
 * Ensures the phone number cannot be reversed using offline rainbow tables.
 */
function computePhoneHash(phone: string, secretKey: string): string {
  const clean = phone.replace(/\D/g, "");
  if (clean.length !== 10) {
    throw new HttpsError("invalid-argument", "Phone number must be exactly 10 digits.");
  }
  return crypto.createHmac("sha256", secretKey).update(clean).digest("hex");
}

/**
 * Masks a 10-digit phone number for display (e.g. ******3210).
 */
function maskPhoneNumber(cleanPhone: string): string {
  if (cleanPhone.length !== 10) return "**********";
  return "******" + cleanPhone.slice(-4);
}

/**
 * 1C: Callable function completeSignUp (authenticated).
 * Validates profile fields server-side, verifies phone uniqueness via HMAC in phone_index,
 * and creates the user profile document in ONE Firestore transaction.
 * If phone is taken, safely deletes the newly created Auth user.
 */
export const completeSignUp = onCall(
  { secrets: [phoneHmacKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "User must be authenticated to complete sign-up.");
    }
    const uid = request.auth.uid;
    const data = request.data || {};
    const { name, dateOfBirth, phone, email } = data;

    // Server-side field validation: Name (letters and spaces only, 1..100)
    if (!name || typeof name !== "string" || !/^[a-zA-Z\s]{1,100}$/.test(name.trim())) {
      throw new HttpsError("invalid-argument", "Name must contain letters and spaces only (1-100 characters).");
    }

    // Server-side field validation: DOB (YYYY-MM-DD, valid past date, at least 1 year old)
    if (!dateOfBirth || typeof dateOfBirth !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(dateOfBirth.trim())) {
      throw new HttpsError("invalid-argument", "Date of birth must be in YYYY-MM-DD format.");
    }
    const dobDate = new Date(dateOfBirth.trim());
    if (isNaN(dobDate.getTime())) {
      throw new HttpsError("invalid-argument", "Invalid date of birth.");
    }
    const today = new Date();
    if (dobDate >= today) {
      throw new HttpsError("invalid-argument", "Date of birth cannot be in the future.");
    }
    const ageDiffMs = today.getTime() - dobDate.getTime();
    const ageYears = ageDiffMs / (1000 * 60 * 60 * 24 * 365.25);
    if (ageYears < 1) {
      throw new HttpsError("invalid-argument", "User must be at least 1 year old.");
    }

    // Server-side field validation: Phone (exactly 10 digits)
    const cleanPhone = (typeof phone === "string" ? phone : "").replace(/\D/g, "");
    if (cleanPhone.length !== 10) {
      throw new HttpsError("invalid-argument", "Phone number must be exactly 10 digits.");
    }

    // Server-side field validation: Email (must end with @gmail.com)
    const cleanEmail = (typeof email === "string" ? email.trim() : "");
    if (!/^[a-zA-Z0-9._%+-]+@gmail\.com$/.test(cleanEmail)) {
      throw new HttpsError("invalid-argument", "Email must be a valid @gmail.com address.");
    }

    const phoneHash = computePhoneHash(cleanPhone, phoneHmacKey.value());
    const phoneIndexRef = db.collection("phone_index").doc(phoneHash);
    const userDocRef = db.collection("users").doc(uid);
    const maskedPhone = maskPhoneNumber(cleanPhone);

    const userProfileData = {
      uid,
      fullName: name.trim(),
      email: cleanEmail,
      phone: maskedPhone,
      fullPhone: cleanPhone,
      dob: dateOfBirth.trim(),
      defaultTimes: {
        MORNING: "08:00",
        AFTERNOON: "13:00",
        EVENING: "18:00",
        NIGHT: "21:00"
      },
      pharmacyName: "",
      pharmacyContact: "",
      pharmacyAddress: "",
      doctorName: "",
      doctorContact: "",
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp()
    };

    try {
      await db.runTransaction(async (transaction) => {
        const phoneDoc = await transaction.get(phoneIndexRef);
        if (phoneDoc.exists && phoneDoc.data()?.uid !== uid) {
          throw new Error("PHONE_ALREADY_REGISTERED");
        }
        transaction.set(phoneIndexRef, {
          uid,
          createdAt: FieldValue.serverTimestamp()
        });
        transaction.set(userDocRef, userProfileData);
      });

      return { ok: true };
    } catch (err: any) {
      if (err.message === "PHONE_ALREADY_REGISTERED") {
        try {
          // Cleanup Auth user only when phone collision occurs
          await getAuth().deleteUser(uid);
        } catch (cleanupErr) {
          console.error("Cleanup error deleting user after duplicate phone:", uid);
        }
        throw new HttpsError("already-exists", "phone-already-registered");
      }

      if (err instanceof HttpsError) {
        throw err;
      }

      console.error("completeSignUp failed for uid:", uid);
      throw new HttpsError("internal", "An error occurred while saving user profile.");
    }
  }
);

/**
 * 1D: Account deletion cleanup trigger (idempotent).
 * Triggered automatically when an Auth user is deleted.
 * Recursively deletes users/{uid} and all subcollections (medicines, history, etc.),
 * and removes all phone_index records for that uid so the phone number can be re-registered.
 * Logs only the uid and count of deleted documents, never any personal data.
 */
export const cleanupUserData = functions
  .region("asia-south1")
  .auth.user()
  .onDelete(async (user) => {
    const uid = user.uid;
    let deletedPhoneIndexCount = 0;

    try {
      // 1. Recursive delete users/{uid} and all subcollections
      const userDocRef = db.collection("users").doc(uid);
      await db.recursiveDelete(userDocRef);

      // 2. Query phone_index where uid == user uid and delete them
      const phoneIndexQuery = await db.collection("phone_index").where("uid", "==", uid).get();
      if (!phoneIndexQuery.empty) {
        const batch = db.batch();
        phoneIndexQuery.docs.forEach((doc) => {
          batch.delete(doc.ref);
          deletedPhoneIndexCount++;
        });
        await batch.commit();
      }

      console.log(`Cleanup completed for uid ${uid}: deleted ${deletedPhoneIndexCount} phone_index document(s).`);
    } catch (err) {
      console.error(`Cleanup failed for uid ${uid}.`);
    }
  });

/**
 * 1E: Callable function updatePhone (authenticated).
 * Updates user phone number in ONE transaction:
 * - Checks that new phoneHash is free or owned by caller.
 * - Deletes the old phone_index record for caller's old phone.
 * - Creates new phone_index record with new phoneHash.
 * - Updates users/{uid} phone fields (masked phone and fullPhone).
 */
export const updatePhone = onCall(
  { secrets: [phoneHmacKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "User must be authenticated.");
    }
    const uid = request.auth.uid;
    const { newPhone } = request.data || {};

    const cleanNewPhone = (typeof newPhone === "string" ? newPhone : "").replace(/\D/g, "");
    if (cleanNewPhone.length !== 10) {
      throw new HttpsError("invalid-argument", "New phone number must be exactly 10 digits.");
    }

    const newPhoneHash = computePhoneHash(cleanNewPhone, phoneHmacKey.value());
    const newPhoneRef = db.collection("phone_index").doc(newPhoneHash);
    const userRef = db.collection("users").doc(uid);

    try {
      await db.runTransaction(async (transaction) => {
        const newPhoneDoc = await transaction.get(newPhoneRef);
        if (newPhoneDoc.exists && newPhoneDoc.data()?.uid !== uid) {
          throw new Error("PHONE_ALREADY_REGISTERED");
        }

        const userDoc = await transaction.get(userRef);
        if (!userDoc.exists) {
          throw new Error("USER_NOT_FOUND");
        }

        const userData = userDoc.data();
        const oldFullPhone = userData?.fullPhone;
        if (oldFullPhone && typeof oldFullPhone === "string") {
          const cleanOld = oldFullPhone.replace(/\D/g, "");
          if (cleanOld.length === 10) {
            const oldPhoneHash = computePhoneHash(cleanOld, phoneHmacKey.value());
            transaction.delete(db.collection("phone_index").doc(oldPhoneHash));
          }
        }

        transaction.set(newPhoneRef, {
          uid,
          createdAt: FieldValue.serverTimestamp()
        });

        const maskedPhone = maskPhoneNumber(cleanNewPhone);
        transaction.update(userRef, {
          phone: maskedPhone,
          fullPhone: cleanNewPhone,
          updatedAt: FieldValue.serverTimestamp()
        });
      });

      return { ok: true };
    } catch (err: any) {
      if (err.message === "PHONE_ALREADY_REGISTERED") {
        throw new HttpsError("already-exists", "phone-already-registered");
      }
      if (err.message === "USER_NOT_FOUND") {
        throw new HttpsError("not-found", "User profile not found.");
      }
      if (err instanceof HttpsError) {
        throw err;
      }
      console.error("updatePhone failed for uid:", uid);
      throw new HttpsError("internal", "An error occurred while updating phone number.");
    }
  }
);
