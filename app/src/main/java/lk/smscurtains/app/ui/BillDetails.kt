package lk.smscurtains.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import lk.smscurtains.app.data.*
import lk.smscurtains.app.model.*
import lk.smscurtains.app.print.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.json.JSONObject

@Composable
fun BillDetailsScreen(
    summary: InvoiceSummary,
    dao: ShopDao,
    role: String,
    onBack: () -> Unit,
    onWorkStatus: (String) -> Unit,
    onRecordPayment: (Payment) -> Unit,
    onModify: (JSONObject, String) -> Unit
) {
    val invoice = summary.invoice
    val payments by dao.watchPayments(invoice.id).collectAsState(initial = emptyList())
    val measurements by dao.watchMeasurements(invoice.id).collectAsState(initial = emptyList())
    val balance = (invoice.grandTotal - summary.paidAmount).coerceAtLeast(0.0)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var paymentDialog by remember { mutableStateOf(false) }
    var modifyDialog by remember { mutableStateOf(false) }
    var receiptDialog by remember { mutableStateOf(false) }
    var printDialog by remember { mutableStateOf(false) }
    var printerList by remember { mutableStateOf<List<PairedPrinter>>(emptyList()) }
    var printerError by remember { mutableStateOf("") }
    var selectedPrinter by remember { mutableStateOf("") }
    var paper by remember { mutableIntStateOf(58) }
    var isPrinting by remember { mutableStateOf(false) }

    fun findPrinters() {
        try {
            printerList = BluetoothReceiptPrinter.paired()
            printerError = if (printerList.isEmpty()) "No paired Bluetooth printers. Pair your printer in Android Bluetooth settings first." else ""
            if (printerList.isNotEmpty() && printerList.none { it.address == selectedPrinter }) selectedPrinter = printerList.first().address
        } catch (e: Exception) {
            printerError = "Cannot access Bluetooth printers: ${e.localizedMessage}"
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) findPrinters()
        else printerError = "Bluetooth access denied. Android Print / Save PDF is still available."
    }
    fun askForPrinters() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        else findPrinters()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
        PageHeader("Bill #${billNumber(invoice.uid, invoice.id)}", invoice.customer, onBack)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Section("Payment status") {
                StateChip(paymentStatus(summary.paidAmount, invoice.grandTotal),
                    if (balance < 0.005) ShopColors.SoftJade else ShopColors.SoftAmber)
                SummaryPair("Grand total", money(invoice.grandTotal))
                SummaryPair("Total received", money(summary.paidAmount))
                HorizontalDivider()
                SummaryPair("BALANCE DUE", money(balance), prominent = true)
                if (invoice.workStatus == "DONE" && balance > 0.005) {
                    Text("Work completed • Payment pending", color = MaterialTheme.colorScheme.error)
                }
                if (balance > 0.005) BigButton(if (role == "admin") "+ Record payment" else "+ Request payment approval", onClick = { paymentDialog = true })
                else Text("✓ Paid in full", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            Section("Work progress") {
                StateChip(workStatus(invoice.workStatus), ShopColors.SoftBlue)
                when (invoice.workStatus) {
                    "TO_START" -> BigButton("▶ Start work", onClick = { onWorkStatus("ONGOING") })
                    "ONGOING" -> BigButton("✓ Mark work as done", onClick = { onWorkStatus("DONE") })
                    "DONE" -> Text("Work finished. Any outstanding balance remains visible until settled.")
                }
                Text("Payments can be received before, during, or after work.", style = MaterialTheme.typography.bodySmall)
            }

            Section("Customer & order") {
                SummaryPair("Created", date(invoice.createdAt))
                if (invoice.phone.isNotBlank()) SummaryPair("Phone", invoice.phone)
                if (invoice.address.isNotBlank()) Text("Address: ${invoice.address}")
                if (invoice.dueDate.isNotBlank()) SummaryPair("Completion date", invoice.dueDate)
                SummaryPair("Payment preference", invoice.paymentPreference)
                OutlinedButton(onClick = { modifyDialog = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(if (role == "admin") "Edit bill" else "Request edit (admin approval)")
                }
            }

            Section("Itemized bill") {
                ItemRow("Curtain fabric • ${invoice.materialName}", "${qty(invoice.fabricMetres)} m", invoice.materialRate, invoice.materialTotal)
                HorizontalDivider()
                ItemRow("Fusion • ${invoice.stripType}", "${qty(invoice.fabricMetres)} m", invoice.stripRate, invoice.stripTotal)
                HorizontalDivider()
                ItemRow("Eyelets • ${invoice.eyeletColor}", "${invoice.eyeletCount} pcs", invoice.eyeletRate, invoice.eyeletTotal)
                HorizontalDivider()
                ItemRow("Wood bar • ${invoice.woodColor}", "${qty(invoice.grandFeet)} ft", invoice.woodRate, invoice.woodTotal)
                HorizontalDivider()
                ItemRow("Brackets", "${invoice.bracketCount} pcs", invoice.bracketRate, invoice.bracketTotal)
                ItemRow("Endcaps", "${invoice.endcapCount} pcs", invoice.endcapRate, invoice.endcapTotal)
                HorizontalDivider()
                ItemRow("Sewing • ${invoice.sewingType}", "${qty(invoice.fabricMetres)} m", invoice.sewingRate, invoice.sewingTotal)
                HorizontalDivider()
                SummaryPair("GRAND TOTAL", money(invoice.grandTotal), prominent = true)
            }

            Section("Measurements") {
                SummaryPair("Window length", "${qty(invoice.windowFeet)} ft")
                SummaryPair("Door length", "${qty(invoice.doorFeet)} ft")
                SummaryPair("Grand length", "${qty(invoice.grandFeet)} ft", prominent = true)
                SummaryPair("Extra fabric", "${qty(invoice.extraMetres)} m")
                SummaryPair("Fabric quantity", "${qty(invoice.fabricMetres)} m")
                measurements.forEachIndexed { index, m ->
                    HorizontalDivider()
                    Text("${index + 1}. ${m.kind.lowercase().replaceFirstChar { it.uppercase() }} • ${m.type}", fontWeight = FontWeight.SemiBold)
                    Text("${qty(m.length)} ${m.unit} × ${m.frames} frames = ${qty(CurtainMath.feet(m.length, m.unit) * m.frames)} ft")
                    Text((if (m.kind == "WINDOW") "Windows/frame: ${m.openings}" else "Door frames: ${m.frames}") + (m.height?.let { "   •   Height: ${qty(it)} ${m.heightUnit} (${qty(CurtainMath.feet(it, m.heightUnit))} ft)" } ?: ""), style = MaterialTheme.typography.bodySmall)
                }
            }

            Section("Payment history") {
                if (payments.isEmpty()) Text("No payments recorded yet")
                else payments.forEach { payment ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${payment.kind} • ${payment.method}", fontWeight = FontWeight.SemiBold)
                            Text(money(payment.amount), fontWeight = FontWeight.Bold)
                        }
                        Text(date(payment.paidAt), style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                }
                SummaryPair("Remaining balance", money(balance), prominent = true)
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { receiptDialog = true }, modifier = Modifier.weight(1f).heightIn(min = 58.dp),
                shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Outlined.ReceiptLong, null)
                Spacer(Modifier.width(5.dp))
                Text("Receipt")
            }
            Button(onClick = { printDialog = true; askForPrinters() }, modifier = Modifier.weight(1f).heightIn(min = 58.dp),
                shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Outlined.Print, null)
                Spacer(Modifier.width(5.dp))
                Text("Print")
            }
        }
    }

    if (paymentDialog) {
        var value by remember { mutableStateOf("") }
        var method by remember { mutableStateOf("CASH") }
        var kind by remember { mutableStateOf("Advance") }
        var paymentDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())) }
        var error by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { paymentDialog = false }, title = { Text(if (role == "admin") "Record payment" else "Submit payment for review") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    SummaryPair("Balance", money(balance), prominent = true)
                    Input("Amount received (Rs.)", value, { value = it }, numeric = true)
                    TwoFields(
                        first = { mod -> Choice("Method", method, listOf("CASH", "CARD"), mod) { method = it } },
                        second = { mod -> Choice("Type", kind, listOf("Advance", "Payment"), mod) { kind = it } }
                    )
                    DateChoice("Date received", paymentDate) { paymentDate = it }
                    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = { Button(onClick = {
                val number = value.toDoubleOrNull()
                if (number == null || !number.isFinite() || number < 0.01 || number > balance + 0.001) {
                    error = "Enter an amount above 0 and no greater than ${money(balance)}."
                } else {
                    val parsed = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(paymentDate) }.getOrNull()
                    if (parsed == null) error = "Choose a valid date."
                    else {
                        val calendar = Calendar.getInstance().apply {
                            time = parsed; set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                        }
                        onRecordPayment(Payment(invoiceId = invoice.id, amount = CurtainMath.round2(number), method = method, kind = kind, paidAt = calendar.timeInMillis))
                        paymentDialog = false
                    }
                }
            }) { Text(if (role == "admin") "Save payment" else "Submit to admin") } },
            dismissButton = { TextButton(onClick = { paymentDialog = false }) { Text("Cancel") } }
        )
    }

    if (modifyDialog) EditBillDialog(invoice, role == "admin", onDismiss = { modifyDialog = false }, onSubmit = onModify)

    if (receiptDialog) {
        AlertDialog(onDismissRequest = { receiptDialog = false }, title = { Text("Receipt preview • 58 mm") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(ReceiptText.lines(invoice, payments, 32).joinToString("\n"), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                }
            }, confirmButton = { Button(onClick = { receiptDialog = false; printDialog = true; askForPrinters() }) { Text("Print receipt") } },
            dismissButton = { TextButton(onClick = { receiptDialog = false }) { Text("Close") } }
        )
    }

    if (printDialog) {
        AlertDialog(onDismissRequest = { if (!isPrinting) printDialog = false }, title = { Text("Print receipt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Paper width")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = paper == 58, onClick = { paper = 58 }, label = { Text("58 mm") })
                        FilterChip(selected = paper == 80, onClick = { paper = 80 }, label = { Text("80 mm") })
                    }
                    Text("Android printing can also save this receipt to a PDF.")
                    OutlinedButton(onClick = {
                        try { SystemReceiptPrinter.print(context, invoice, payments, paper); printDialog = false }
                        catch (e: Exception) { printerError = e.localizedMessage ?: "Printing failed" }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Android Print / Save PDF") }
                    HorizontalDivider()
                    Text("Bluetooth thermal printer", fontWeight = FontWeight.Bold)
                    if (printerList.isNotEmpty()) {
                        val selected = printerList.firstOrNull { it.address == selectedPrinter }
                        Choice("Paired printer", selected?.let { "${it.name} (${it.address})" } ?: "Choose printer",
                            printerList.map { "${it.name} (${it.address})" }) { display ->
                            selectedPrinter = printerList.firstOrNull { "${it.name} (${it.address})" == display }?.address.orEmpty()
                        }
                    }
                    if (printerError.isNotBlank()) Text(printerError, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = { askForPrinters() }, modifier = Modifier.fillMaxWidth()) { Text("Refresh paired printers") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    isPrinting = true
                    scope.launch {
                        try {
                            BluetoothReceiptPrinter.print(selectedPrinter, paper, invoice, payments)
                            Toast.makeText(context, "Receipt sent to printer", Toast.LENGTH_SHORT).show()
                            printDialog = false
                        } catch (e: Exception) {
                            printerError = "Print failed: ${e.localizedMessage}"
                        } finally { isPrinting = false }
                    }
                }, enabled = selectedPrinter.isNotBlank() && !isPrinting) { Text(if (isPrinting) "Printing…" else "Print Bluetooth") }
            },
            dismissButton = { TextButton(onClick = { printDialog = false }, enabled = !isPrinting) { Text("Close") } }
        )
    }
}

@Composable
private fun ItemRow(label: String, count: String, rate: Double, amount: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontWeight = FontWeight.Bold)
        Text("$count × ${money(rate)}", style = MaterialTheme.typography.bodySmall)
        Text(money(amount), modifier = Modifier.align(Alignment.End), fontWeight = FontWeight.SemiBold)
    }
}
