#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")/.."
out="${TMPDIR:-/tmp}/sms_curtains_calc_test.jar"
kotlinc app/src/main/java/lk/smscurtains/app/model/Calculations.kt tests/CalculationSmokeTest.kt -include-runtime -d "$out"
java -jar "$out"
