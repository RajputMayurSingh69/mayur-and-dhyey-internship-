import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

/**
 * ONE-TIME MIGRATION / CLEANUP SCRIPT:
 * Deletes all legacy documents in the "phone_index" collection that were created
 * using the old unsalted client-side SHA-256 hashing.
 *
 * NOTE: DO NOT RUN THIS AUTOMATICALLY.
 * Run manually only after confirming there is no production user data:
 *
 *   npx ts-node functions/scripts/clear-old-phone-index.ts
 */
async function clearOldPhoneIndex() {
  console.log("Starting one-time phone_index collection cleanup...");
  initializeApp();
  const db = getFirestore();

  const phoneIndexCol = db.collection("phone_index");
  await db.recursiveDelete(phoneIndexCol);
  console.log("Successfully cleared all legacy phone_index documents.");
}

clearOldPhoneIndex().catch((err) => {
  console.error("Error clearing phone_index collection:", err);
  process.exit(1);
});
