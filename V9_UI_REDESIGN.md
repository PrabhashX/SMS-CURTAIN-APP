# SMS Curtains V9 — phone UI redesign

This is an Android Studio source project, updated from V8. The existing MongoDB/Render API is unchanged.

## Visual changes
- Warm linen (#FBF8F3), forest (#183B36), jade (#087D70), soft jade and brass accents.
- Branded native Android 12+ splash plus a short animated in-app splash.
- Login now offers **Admin** or **Employee** role buttons, show/hide password, big sign-in button, cloud state and clearly visible errors.
- Home now includes a profile avatar, revenue card, two financial summary tiles, strong create-bill button, recent bills with status chips, and an attention card.
- Profile bottom sheet shows signed-in username/role, connectivity, pending sync count and sign-out.
- Bills screen includes customer/phone/bill number search and dropdown filters.
- Measurement entry uses responsive two-column fields; length and units, counts and quantities are easier to enter. Price pairs and options also use responsive layouts.
- Receipt/print buttons, progress status chips and payment dialog are refreshed.
- Bottom navigation reuses the same theme and high-contrast icons/text.
- Android safe-drawing insets prevent content from overlapping status and navigation bars.

## Not changed
- Package name `lk.smscurtains.app`, Room database schema, backend URL, authentication endpoint and sync/event payloads.
- Curtain calculations: 1 measured foot = 1 metre of fabric; six eyelets per cloth metre; etc.
- Original recorded beep, recurring foreground offline toast and sound every 5 seconds until connectivity restores.

## Installation
1. Extract the ZIP; in Android Studio open the `SMSCurtains_V9_Redesign_Work` directory (this directory contains `settings.gradle.kts`).
2. Gradle sync then Build > Build APK(s).
3. Install as an update signed with the **same signing key** to preserve the existing Android app's local billing data. Do not uninstall a working copy with unsynced bills.
4. Verify Admin and Employee login using the production passwords stored securely on Render, create sample bill, check cloud sync and approval.

No new GitHub commit or Render deployment is required for these Android-only UI changes.

## QA status
The pure Kotlin calculation, revenue and alert scheduling tests and Node backend tests can be run using `tests/*.sh` and `npm test` in `backend`. The complete Android APK has not been compiled or device tested here because the Android SDK/Gradle toolchain is not available.
