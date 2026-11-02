# SMS Curtains — Original beep matched

The offline alert sound is isolated from the user's supplied WhatsApp PTT audio, with noise outside the beep reduced and the original tone/pitch preserved (primary spectral component near 771 Hz, secondary near 1151 Hz). Unlike earlier synthetic variants, this uses the original recording's tone.

The Android alert is stored at `app/src/main/res/raw/offline_alert.m4a`, with `offline_beep_preview.wav` included for listening. No online services were changed.

The existing V8 foreground offline reminder behavior stays in place: connectivity monitored every 500 ms; on lost validated internet, toast and matching beep immediately and again every 5 seconds as long as still offline; on reconnection, cancel the reminders. This is a foreground-only alert (Android background toast limitations).

Android `versionCode` advanced to 9 so this build can update V8 without uninstalling it, provided the APK uses the same signing key. Keep unsynchronized local data safe; do not uninstall the previous app.

No MongoDB, Render, or GitHub backend redeployment required.

Test on device: disable both Wi-Fi and mobile data with app visible. Ensure toast and beep at 0, 5, 10, 15 seconds; restore connection and confirm alerts cease. Full Android Studio build and on-device listening not verified in this container.
