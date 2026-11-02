#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")/.."
out="${TMPDIR:-/tmp}/sms_curtains_offline_alert_test.jar"
kotlinc app/src/main/java/lk/smscurtains/app/model/OfflineAlertSchedule.kt tests/OfflineAlertScheduleTest.kt -include-runtime -d "$out"
java -jar "$out"
