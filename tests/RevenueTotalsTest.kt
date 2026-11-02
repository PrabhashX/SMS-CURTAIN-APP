import lk.smscurtains.app.model.revenueTotals

fun main() {
    val totals = revenueTotals(listOf(1000.0 to 400.0, 500.0 to 0.0, 700.0 to 700.0))
    check(totals.orders == 3)
    check(totals.billed == 2200.0)
    check(totals.received == 1100.0)
    check(totals.outstanding == 1100.0)
    val empty = revenueTotals(emptyList())
    check(empty.orders == 0 && empty.received == 0.0)
    println("PASS: revenue is received money, not all billed sales; unpaid balances are separate")
}
