# SMS Curtains V8 — Continuous offline toast + beep fix

## Fixed behavior (while the app is in the foreground)

- When validated internet goes away: toast and accompanying beep appear immediately (within ~0.5 seconds of detection).
- Both repeat every **5 seconds** for as long as internet remains unavailable.
- While offline, the banner is visible and bills remain in the local queue.
- On reconnection: toast/beep stop, any currently playing alert is cancelled, and sync is triggered. No restored-network toast is used.
- If the app goes into the background, the repeating toast/beep stop because Android limits background toasts. On return to foreground while still offline, alerts restart immediately.
- Uses an independent 500-ms foreground connectivity monitor rather than depending solely on the Android network callback. This specifically fixes the case where the offline status never updated until reconnection.
- Alert audio was rebuilt using the **newly uploaded WhatsApp voice recording**, isolating its dual tones and retaining the previously requested deep 250/375 Hz pitch.
- Android app versionCode incremented to 8. No backend changes or redeployment required.

## Quick manual test

1. Update the app without uninstalling (same signing key to preserve data). Open it and sign in normally.
2. Turn off both Wi-Fi and mobile data (or use Airplane mode). Keep SMS Curtains visible.
3. Listen and watch: alert at ~0 seconds, then ~5, 10, 15 seconds, continuing indefinitely.
4. Turn internet back on. Confirm the beep/toasts stop and queued data syncs.
5. Try again after reconnecting to ensure a second outage triggers a fresh alert.

## Validation

`bash tests/run_offline_alert_tests.sh` tests cadence, repeated offline ticks, reconnect reset and foreground reset. `tests/run_calculation_tests.sh`, `tests/run_revenue_tests.sh` and Node backend tests remain available. Full Android SDK compilation and on-device playback still require Android Studio/phone.
