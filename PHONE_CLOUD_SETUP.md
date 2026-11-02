# SMS Curtains V7 — Android phone + MongoDB Cloud

## Your live connection

The Android app is preconfigured for:

`https://sms-curtain-app.onrender.com`

API route: `/health` returns `{"status":"ok"` when the Node.js API can ping MongoDB.

**Do not put MongoDB credentials, MONGODB_URI, JWT_SECRET, or user account passwords into the Android project.** The Android app connects only to the Render HTTPS API. The Node.js server has the secrets in Render Environment.

## Open and install

1. Extract the zip and open `SMS_Curtains_Android` with Android Studio.
2. Allow Gradle sync. Build > Build APK(s); install the new `app/build/outputs/apk/debug/app-debug.apk` on your Android phone.
3. Log in with **admin** or **employee** and the passwords you configured in Render Environment (`ADMIN_PASSWORD`, `EMPLOYEE_PASSWORD`). `admin/admin` and `employee/employee` no longer work in this cloud edition.
4. Tap **Create new curtain bill**. On a second phone, log in and verify it appears (app refreshes periodically; allow up to 30 seconds when open).
5. Test employee submitting a payment or bill modification, then admin approving it on another phone. Payment balance should change only after approval and sync.
6. Test airplane mode: records remain in Room and are queued for sync when connected. App shows an offline warning and plays the short supplied notification.
7. Use the phone's Bluetooth thermal printer (58 mm or 80 mm), or Android Print / PDF. PAYable P052 specific functionality has been removed.

## Important limitations

- This archive contains **Android Studio source**, not a prebuilt APK. Android compilation was not available in the environment used to create it.
- The API is already deployed; you do NOT need to redeploy the Node.js `backend` unless you change server code.
- When upgrading from V5, the Android package remains `lk.smscurtains.app` and Room schema is unchanged. Use the same Android debug signing key and update over existing installs if there are saved bills; don't uninstall or clear data.
- If the free Render service sleeps, first login or sync can take longer than usual. Make sure `/health` responds and leave the app online.
- This app implements optimistic/offline synchronization; test with sample bills on both phones before using for real customer payments. Server rejects conflicting payments, but offline queued operations can require administrator review if multiple devices change the same bill.
- When switching from V5 local demo mode, sign in to the cloud account. The app prevents reusing a local-demo session without cloud authentication. Existing local invoices are preserved and the first admin cloud sync attempts to migrate older Room v1 bills.
- `admin` and `employee` user account credentials are defined on the Render backend (not in the Android app); change the passwords only on Render, keep them private.

## Smoke checks

On your computer, you can open `https://sms-curtain-app.onrender.com/health` for database connectivity.

Inside the project:

```bash
bash tests/run_calculation_tests.sh
cd backend && npm test
```
