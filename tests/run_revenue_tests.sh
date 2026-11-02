#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")/.."
out="${TMPDIR:-/tmp}/sms_curtains_revenue_test.jar"
kotlinc app/src/main/java/lk/smscurtains/app/model/RevenueTotals.kt tests/RevenueTotalsTest.kt -include-runtime -d "$out"
java -jar "$out"
