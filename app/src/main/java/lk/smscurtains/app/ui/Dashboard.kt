package lk.smscurtains.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import lk.smscurtains.app.data.InvoiceSummary
import lk.smscurtains.app.model.date
import lk.smscurtains.app.model.money
import lk.smscurtains.app.model.billNumber
import lk.smscurtains.app.model.revenueTotals

fun paymentStatus(paid: Double, total: Double): String = when {
    paid >= total - 0.005 -> "Paid in full"
    paid > 0.005 -> "Partially paid"
    else -> "Unpaid"
}
fun workStatus(code: String): String = when (code) {
    "ONGOING" -> "Ongoing"
    "DONE" -> "Work done"
    else -> "To be started"
}

@Composable
fun Dashboard(
    items: List<InvoiceSummary>, role: String, pendingReviews: Int, queued: Int, demo: Boolean,
    onNew: () -> Unit, onOpen: (Long) -> Unit, onApprovals: () -> Unit, onLogout: () -> Unit,
    onAllBills: () -> Unit = {}, onProfile: () -> Unit = {}
) {
    val totals = revenueTotals(items.map { it.invoice.grandTotal to it.paidAmount })
    val latest = items.take(4)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("SMS CURTAINS", style = MaterialTheme.typography.labelMedium, color = ShopColors.Jade)
                    Text("Welcome back!", style = MaterialTheme.typography.headlineSmall, color = ShopColors.Forest)
                    Text("${role.replaceFirstChar { it.uppercase() }} · Mawathagama",
                        style = MaterialTheme.typography.bodyMedium, color = ShopColors.Muted)
                }
                ShopAvatar(role, onClick = onProfile)
            }
        }
        item {
            Card(shape = RoundedCornerShape(25.dp), modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ShopColors.Forest)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AccountBalanceWallet, null, tint = ShopColors.Brass)
                        Spacer(Modifier.width(8.dp))
                        Text("TOTAL CURTAIN REVENUE", style = MaterialTheme.typography.labelLarge,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = .85f))
                    }
                    Text(money(totals.received), style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                    Text("Money received from customers", color = androidx.compose.ui.graphics.Color.White.copy(alpha = .78f),
                        style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider(color = androidx.compose.ui.graphics.Color.White.copy(alpha = .18f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StateChip("${totals.orders} orders", ShopColors.SoftJade, ShopColors.Forest)
                        Spacer(Modifier.width(8.dp))
                        Text("Live shop overview", color = androidx.compose.ui.graphics.Color.White.copy(alpha = .7f),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            TwoFields(
                first = { mod -> InfoTile("Total billed", money(totals.billed), Icons.Outlined.ReceiptLong,
                    ShopColors.Jade, ShopColors.SoftJade, mod) },
                second = { mod -> InfoTile("To collect", money(totals.outstanding), Icons.Outlined.PendingActions,
                    ShopColors.Forest, ShopColors.SoftAmber, mod) }
            )
        }
        item {
            Button(onClick = onNew, modifier = Modifier.fillMaxWidth().height(66.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ShopColors.Jade)) {
                Icon(Icons.Outlined.AddCircleOutline, contentDescription = null, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Text("Create new curtain bill", style = MaterialTheme.typography.titleMedium)
            }
        }
        if (queued > 0 || pendingReviews > 0) item {
            Card(shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ShopColors.SoftAmber)) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.NotificationsActive, null, tint = ShopColors.Forest)
                        Spacer(Modifier.width(8.dp))
                        Text("Needs attention", style = MaterialTheme.typography.titleMedium)
                    }
                    if (pendingReviews > 0) Text("$pendingReviews approval request(s) to check")
                    if (queued > 0) Text("$queued change(s) waiting to sync")
                    OutlinedButton(onClick = onApprovals, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp)) {
                        Text(if (role == "admin") "Review requests" else "View requests")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.ChevronRight, null)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent bills", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f), color = ShopColors.Forest)
                TextButton(onClick = onAllBills) {
                    Text("View all")
                    Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (latest.isEmpty()) item {
            Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.ReceiptLong, null, tint = ShopColors.Jade, modifier = Modifier.size(48.dp))
                    Text("No bills yet", style = MaterialTheme.typography.titleMedium)
                    Text("Tap Create new curtain bill to begin.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else items(latest, key = { it.invoice.id }) { item ->
            BillListCard(item, onClick = { onOpen(item.invoice.id) })
        }
    }
}

@Composable
fun BillsPage(items: List<InvoiceSummary>, onOpen: (Long) -> Unit) {
    var filter by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
    val display = items.filter { item ->
        val match = query.isBlank() || item.invoice.customer.contains(query, ignoreCase = true) ||
            item.invoice.phone.contains(query, ignoreCase = true) ||
            billNumber(item.invoice.uid, item.invoice.id).contains(query, ignoreCase = true)
        match && when (filter) {
            "Money due" -> item.invoice.grandTotal - item.paidAmount > 0.005
            "In progress" -> item.invoice.workStatus == "ONGOING"
            "Finished" -> item.invoice.workStatus == "DONE"
            else -> true
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(6.dp))
        PageHeader("Curtain bills", "Search customers, track work and payments")
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            placeholder = { Text("Search customer, phone or bill") },
            singleLine = true, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear") } }
        )
        Choice("Filter bills", filter, listOf("All", "Money due", "In progress", "Finished")) { filter = it }
        Text("${display.size} bill(s)", color = ShopColors.Muted, style = MaterialTheme.typography.bodySmall)
        if (display.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
                Text("No matching bills. Try another name or filter.", Modifier.padding(24.dp))
            }
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)) {
            items(display, key = { it.invoice.id }) { item ->
                BillListCard(item, onClick = { onOpen(item.invoice.id) })
            }
        }
    }
}

@Composable
private fun BillListCard(item: InvoiceSummary, onClick: () -> Unit) {
    val invoice = item.invoice
    val balance = (invoice.grandTotal - item.paidAmount).coerceAtLeast(0.0)
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(21.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(invoice.customer, style = MaterialTheme.typography.titleMedium,
                        color = ShopColors.Forest, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("#${billNumber(invoice.uid, invoice.id)} · ${date(invoice.createdAt)}",
                        style = MaterialTheme.typography.bodySmall, color = ShopColors.Muted)
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = "View bill", tint = ShopColors.Jade)
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val chips: @Composable () -> Unit = {
                    StateChip(workStatus(invoice.workStatus), ShopColors.SoftBlue, ShopColors.Forest)
                    StateChip(paymentStatus(item.paidAmount, invoice.grandTotal),
                        if (balance < .005) ShopColors.SoftJade else ShopColors.SoftAmber, ShopColors.Forest)
                }
                if (maxWidth < 290.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { chips() }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { chips() }
                }
            }
            HorizontalDivider(color = ShopColors.Sand)
            SummaryPair("Balance due", money(balance), prominent = true)
        }
    }
}
