# SMS Curtains V7 — phone UI and offline alert update

## What changed
- Phone-only UI with high-contrast navy/teal/cream theme and large text/touch targets.
- Home dashboard prominently displays **total curtain revenue = payments received**, plus total billed, unpaid balance, order count, recent bills.
- Fixed bottom navigation: Home, Bills, New bill, Reviews. Hidden when editing a new bill or viewing details to avoid accidental data loss.
- Uses safe drawing insets around status bar, display cutout and Android navigation bar (targetSdk 35 / edge-to-edge).
- Rebuilt single 770 Hz notification chirp with a short rounded envelope for less background noise; WAV preview included.
- While app is visible and validated internet connectivity is absent, offline toast and beep repeat every 5 seconds. On restoration, repetition stops and sync resumes. Persistent offline banner remains visible. Android restricts background toast behavior: alerts deliberately stop when the app is not foregrounded.
- All login, payments, approval logic and MongoDB API URL are unchanged. **Do not redeploy Render or GitHub** for this client-only revision.

## Installation
Extract ZIP > open SMS_Curtains_Android in Android Studio > Build APK(s). Install on phone. Existing `lk.smscurtains.app` bills are retained when updating the *same signing key* and package name. Do **not** uninstall or clear app data before backing up unsynced bills.

## Verification checklist
1. On an Android phone with status bar/cutout, check heading and bottom navigation never overlap system bars.
2. Create invoice; record a cash payment as admin; received revenue increments, outstanding decreases.
3. Log in as employee; bill/payment requests require admin review.
4. Enable airplane mode while viewing app: banner + toast + chirp immediately, again about every 5s; switch online, and alerts stop.
5. Confirm pending sync queue clears on restored connection and bills show on a second phone.
6. If upgrading a currently installed build fails due to different debug signing keys, do not uninstall an app that contains un-synced customer records.

## Limits
No Android SDK present in the build environment; Android compilation/device install have not been verified here. The revenue metric reflects locally saved + synchronized bills, so pending cross-device synchronization may temporarily affect totals. This is not a financial accounting system.
