package lk.smscurtains.app.model

/** Deterministic, foreground-only reminder schedule, independent of Android callbacks. */
class OfflineAlertSchedule(private val intervalMillis: Long = 5_000L) {
    init { require(intervalMillis > 0L) }
    private var nextDueMillis: Long? = null

    /** Returns true only when a new OFFLINE toast+beep is due. */
    fun shouldAlert(isOnline: Boolean, inForeground: Boolean, nowMillis: Long): Boolean {
        if (isOnline || !inForeground) {
            nextDueMillis = null // Restart immediately after another disconnect/resume.
            return false
        }
        val due = nextDueMillis
        if (due != null && nowMillis < due) return false
        nextDueMillis = nowMillis + intervalMillis
        return true
    }
}
