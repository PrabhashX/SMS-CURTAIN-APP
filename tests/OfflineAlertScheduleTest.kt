import lk.smscurtains.app.model.OfflineAlertSchedule

fun main() {
    val reminders = OfflineAlertSchedule(5_000L)
    check(!reminders.shouldAlert(isOnline = true, inForeground = true, nowMillis = 0))
    check(reminders.shouldAlert(isOnline = false, inForeground = true, nowMillis = 100))
    check(!reminders.shouldAlert(isOnline = false, inForeground = true, nowMillis = 4_999))
    check(reminders.shouldAlert(isOnline = false, inForeground = true, nowMillis = 5_100))
    check(!reminders.shouldAlert(isOnline = false, inForeground = true, nowMillis = 6_000))
    check(reminders.shouldAlert(isOnline = false, inForeground = true, nowMillis = 10_100))
    check(!reminders.shouldAlert(isOnline = true, inForeground = true, nowMillis = 12_000))
    check(!reminders.shouldAlert(isOnline = true, inForeground = true, nowMillis = 17_000))
    check(reminders.shouldAlert(isOnline = false, inForeground = true, nowMillis = 20_000))
    check(!reminders.shouldAlert(isOnline = false, inForeground = false, nowMillis = 21_000))
    check(reminders.shouldAlert(isOnline = false, inForeground = true, nowMillis = 22_000))
    println("PASS: offline alert immediately, every 5s, resets on reconnect/background")
}
