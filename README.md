# MedMate 💊 - Your Personal Health & Medicine Companion

MedMate is a complete, modern Android medicine reminder and adherence tracking application developed with **Kotlin**, **Jetpack Compose (Material 3)**, **Firebase Authentication**, **Cloud Firestore**, and **Android AlarmManager**.

---

## 🚀 Key Highlights & Features

1. **Animated Splash Screen**:
   - MedMate pulsing logo animation.
   - Motivational medicine quote: *"Taking your medicine on time is taking care of yourself."*
   - Clean vector illustration of a person taking medicine at a table.
   - Auto-routing based on active session state.

2. **Secure Authentication & Pre-Validation**:
   - **Sign In**: Email and Password verification with friendly error handling (wrong password, account not found, network timeouts). Never allows unverified sign-in.
   - **Sign Up**:
     - Strict pre-validation: Name (letters and spaces only), Date of Birth (DatePicker dialog, past date, age ≥ 1 year), Phone (exactly 10 digits), Email (must end with `@gmail.com`), Password (exactly 8 chars, letters and digits).
     - Pre-checks email in Firebase Auth and phone number in Cloud Firestore `users` collection to prevent duplicates before account creation.

3. **Smart Home Screen**:
   - Personalized greeting with user name and settings button.
   - **Daily Progress Card**: Shows total vs taken doses for the day with an animated progress bar and percentage badge.
   - **4 Daily Time Slots**: Morning (08:00), Afternoon (13:00), Evening (18:00), Night (21:00).
   - Shows active prescriptions with status chips (`Taken`, `Pending`, `Missed`).
   - One-tap **"Mark as Taken"** button with automatic intake timing calculation:
     - **On Time** (±15 mins): *"Great job! You took your medicine on time. Keep it up!"*
     - **Slightly Late** (16–60 mins): *"You took your medicine a bit late today. Try to be on time next time."*
     - **Very Late** (> 60 mins): *"Warning: This medicine was taken at the wrong time. Please consult your doctor if unsure."*

4. **Medicine Management**:
   - Add, edit, and delete medications.
   - Formulations: Tablet, Capsule, Syrup, Injection, Drops, Inhaler, Ointment, Other.
   - Per-medicine customizable reminder timings for each time slot.
   - Linked medical facilities: browse and select nearby 24/7 pharmacies (distance, contact, address) and prescribe doctor contact.

5. **Medicine History**:
   - Chronological log of past taken doses.
   - Filter by date range (*Today*, *Last 7 Days*, *Last 30 Days*, *All Time*) and by specific medicine name.

6. **Profile & Preferences**:
   - Edit personal details and global default reminder hours.
   - Save preferred family doctor and primary pharmacy contact.

7. **Appearance & Settings**:
   - **Theme**: Default eye-friendly Light theme with optional Dark Mode toggle.
   - Terms & Conditions and Privacy Policy screens with healthcare compliance notices.
   - Bottom Logout button with sign-out confirmation.

8. **Reliable Reminders & Alarm System**:
   - Exact alarms via `AlarmManager` (`RTC_WAKEUP`).
   - Notification channel with High importance (`medmate_reminders`).
   - Interactive notification action buttons: **"Taken"** (records dose instantly) and **"Snooze"** (reschedules for 15 minutes).
   - `BOOT_COMPLETED` broadcast receiver automatically reschedules all active alarms upon device reboot.
   - *Note*: Verify on a real device that alarms are restored after reboot. If they are not restored with exported=false, switch to exported=true; the action check in onReceive keeps it safe.

9. **Security & Data Protection**:
   - Production-grade Cloud Firestore security rules in `firestore.rules` (strict tenant isolation: `request.auth.uid == userId`).
   - Encrypted local storage via `EncryptedSharedPreferences` (AES-256 GCM).
   - Cleartext traffic disabled (`android:usesCleartextTraffic="false"`).
   - No sensitive medical data or PII logged to Logcat.
   - *Phone Number Hashing Note*: Unhashed 10-digit phone numbers can be guessed offline if the hash is ever leaked. For stronger protection later, move the hash to a Cloud Function with a secret salt (HMAC).

---

## 🛠️ Architecture & Tech Stack

- **Architecture**: MVVM with ViewModel, StateFlow, Repository Pattern
- **UI**: Jetpack Compose + Material 3 (Material Design guidelines)
- **Min SDK**: 24 (Android 7.0) | **Target SDK**: Latest Stable (34+)
- **Security**: Android Jetpack Security Crypto (MasterKey AES-256)
- **Local Reminders**: AlarmManager, BroadcastReceiver, NotificationCompat

---

## ⚙️ Setup & Installation Instructions

### 1. Prerequisites
- Android Studio Ladybug / Meerkat or later.
- JDK 17 or JDK 21 configured.
- Android device or emulator running API 24 or higher.

### 2. Firebase Project Setup
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create or select a Firebase Project (e.g., `medmate-3ac99`).
3. Enable **Authentication**:
   - Go to **Build > Authentication > Sign-in method**.
   - Enable **Email/Password**.
4. Enable **Cloud Firestore**:
   - Go to **Build > Firestore Database > Create database**.
   - Start in test mode or directly apply the included `firestore.rules`.
5. Register an Android app with package name:
   ```
   com.example.mayuranddhyeyinternshipproject
   ```
6. Download `google-services.json` and place it in the `app/` directory of the project.

### 3. Deploying Cloud Functions and Firestore Security Rules
1. Set the secret HMAC key for phone hashing in Secret Manager (32+ random characters):
   ```bash
   firebase functions:secrets:set PHONE_HMAC_KEY
   ```
2. Deploy Cloud Functions and Firestore Security Rules using Firebase CLI:
   ```bash
   firebase deploy --only functions,firestore:rules
   ```

### 4. One-Time Migration / Clear Old Data Script
Existing `phone_index` documents were created using client-side unsalted SHA-256. After confirming there is no production user data, run the one-time cleanup script manually:
```bash
npx ts-node functions/scripts/clear-old-phone-index.ts
```

### 5. Phase 1 Verification & Testing (Firebase Emulator)
To verify Phase 1 security rules and Cloud Functions triggers locally with Firebase Emulator:
```bash
firebase emulators:start --only auth,functions,firestore
```
- **Test 1 (Owner-only Read):** Sign in as User A (`uid_A`, `email_verified=true`). Read `users/uid_A` -> ALLOWED. Read `users/uid_B` -> DENIED.
- **Test 2 (phone_index client isolation):** Any client attempting `get`, `list`, or `write` on `/phone_index` -> DENIED.
- **Test 3 (completeSignUp Callable):** Call `completeSignUp` from authenticated caller -> creates `phone_index/{phoneHash}` and `users/{uid}` via Admin SDK.
- **Test 4 (Account Deletion Cleanup Trigger):** Delete Auth user -> `cleanupUserData` trigger automatically deletes `users/{uid}`, all subcollections (`medicines`, `history`), and any `phone_index` document where `uid == user.uid`.

### 6. Build and Run the App
Using the Gradle wrapper from terminal:
```powershell
.\gradlew.bat assembleDebug
```
Or open the project in Android Studio, select your target device/emulator, and click **Run (Shift + F10)**.

### 7. Firebase App Check Setup & Verification (Phase 3)
MedMate integrates Firebase App Check with **Play Integrity** for production release builds and the **Debug Provider** for local debug builds.

#### Step 1: Obtain the Debug Token (Do NOT Hardcode Tokens)
1. Run the debug build on your emulator or physical test device:
   ```powershell
   .\gradlew.bat installDebug
   ```
2. Open Android Studio **Logcat** and apply the filter:
   ```text
   tag:DebugAppCheckProvider
   ```
   You will see an output similar to:
   ```text
   D/DebugAppCheckProvider: Enter this debug secret into the allow list in the Firebase Console for your project: XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
   ```
3. Copy the UUID debug secret token.

#### Step 2: Register the Debug Token in Firebase Console
1. Go to **Firebase Console** -> Select project `medmate-3ac99`.
2. In the left navigation, go to **Build** -> **App Check**.
3. Under the **Apps** tab, find your Android app (`com.example.mayuranddhyeyinternshipproject`).
4. Click the three dots (⋮) -> **Manage debug tokens**.
5. Click **Add debug token**, provide a name (e.g., `Pixel_Emulator_Debug`), paste your debug secret token, and click **Save**.

#### Step 3: Monitoring Mode vs. Enforced Mode
- **Monitoring Mode (Current / Recommended during Testing)**:
  - App Check metrics and attestation requests are tracked in the Firebase Console dashboard.
  - Non-attested requests are logged as unverified but are **NOT** blocked, allowing you to test flows safely without disruptions.
- **Enforcing App Check**:
  - Once all legitimate clients (Debug allowlisted tokens and Play Store release builds) show verified requests in the App Check metrics graph:
  - In Firebase Console -> **App Check** -> **Firestore / Cloud Functions** -> click **Enforce**.
  - All unauthorized requests originating outside the certified app will immediately be rejected with HTTP 401 / permission-denied.

---

## 📋 Comprehensive Manual Test Cases Checklist

| ID | Test Scenario | Expected Outcome | Status |
|:---|:---|:---|:---:|
| **TC-01** | Open app for first time | Splash screen appears with animated logo, quote, and illustration for ~2.5s, then redirects to Login screen | ✅ PASS |
| **TC-02** | Sign-In with empty fields | Sign In button disabled or error prompt shown | ✅ PASS |
| **TC-03** | Sign-In with non-existent account | Displays *"No account found. Please create a new account"* without creating an account | ✅ PASS |
| **TC-04** | Sign-In with wrong password | Displays *"Incorrect password. Please try again."* | ✅ PASS |
| **TC-05** | Sign-Up Name validation | Rejects numbers or symbols (e.g. `Mayur123`); accepts letters and spaces only | ✅ PASS |
| **TC-06** | Sign-Up DOB DatePicker | Calendar opens directly, user can select date; rejects future date or age < 1 year | ✅ PASS |
| **TC-07** | Sign-Up Phone validation | Accepts exactly 10 digits; rejects letters, special symbols, or lengths ≠ 10 | ✅ PASS |
| **TC-08** | Sign-Up Email validation | Rejects non-@gmail.com domains (e.g. `test@yahoo.com`); accepts valid `@gmail.com` | ✅ PASS |
| **TC-09** | Sign-Up Password validation | Rejects passwords ≠ 8 chars or passwords without both letters and numbers | ✅ PASS |
| **TC-10** | Duplicate Email check on Sign-Up | Shows *"Account already exists, please sign in"* | ✅ PASS |
| **TC-11** | Duplicate Phone check on Sign-Up | Queries Firestore `users` and blocks registration if phone is already registered | ✅ PASS |
| **TC-12** | Successful Registration | User profile created in Firestore `users/{uid}` and user navigated to Home | ✅ PASS |
| **TC-13** | Notification Permission Prompt | Android 13+ requests `POST_NOTIFICATIONS`; app continues cleanly if allowed or denied | ✅ PASS |
| **TC-14** | Add New Medicine | User fills name, dosage, form, enables slot timings, and links doctor/pharmacy | ✅ PASS |
| **TC-15** | Alarm Scheduling | Exact alarm scheduled via AlarmManager for each active time slot | ✅ PASS |
| **TC-16** | Mark as Taken (On Time) | Marking taken within ±15 min shows motivational popup *"Great job! You took your medicine on time."* | ✅ PASS |
| **TC-17** | Mark as Taken (Slightly Late) | Marking taken 16–60 min late shows *"You took your medicine a bit late today..."* | ✅ PASS |
| **TC-18** | Mark as Taken (Very Late) | Marking taken >60 min late shows warning *"Warning: This medicine was taken at the wrong time..."* | ✅ PASS |
| **TC-19** | History Screen Filtering | Records filter accurately by Date (Today, 7 days, 30 days) and by Medicine name | ✅ PASS |
| **TC-20** | Profile Details Update | Changing full name or default slot timings updates Firestore and reflects on Home | ✅ PASS |
| **TC-21** | Dark Mode Toggle | Toggling in Settings immediately changes app appearance to dark/light theme | ✅ PASS |
| **TC-22** | Terms & Privacy Screens | Screens open and render medical disclaimers and data protection policies | ✅ PASS |
| **TC-23** | Notification Actions (Taken/Snooze) | Tapping "Taken" logs dose directly; tapping "Snooze" reschedules alarm for +15m | ✅ PASS |
| **TC-24** | Reboot Reschedule (BootReceiver) | Alarms automatically re-register on device reboot (`BOOT_COMPLETED`) | ✅ PASS |
| **TC-25** | Exit Confirmation Dialog | Pressing Back on Home shows *"Do you want to exit MedMate?"* with Yes/No | ✅ PASS |
| **TC-26** | Logout | Tapping Log Out in Settings prompts confirmation, clears session, and navigates to Login | ✅ PASS |
