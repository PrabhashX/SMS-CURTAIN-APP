package lk.smscurtains.app.model

/** Sales value is not the same as collected revenue. Pending approval does not count as paid. */
data class RevenueTotals(val billed: Double, val received: Double, val outstanding: Double, val orders: Int)

fun revenueTotals(bills: List<Pair<Double, Double>>): RevenueTotals {
    val billed = bills.sumOf { it.first.coerceAtLeast(0.0) }
    val received = bills.sumOf { it.second.coerceAtLeast(0.0) }
    val outstanding = bills.sumOf { (it.first - it.second).coerceAtLeast(0.0) }
    return RevenueTotals(billed, received, outstanding, bills.size)
}
