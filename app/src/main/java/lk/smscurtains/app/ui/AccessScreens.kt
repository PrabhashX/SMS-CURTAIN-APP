package lk.smscurtains.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import lk.smscurtains.app.R
import lk.smscurtains.app.data.ApprovalRequest
import lk.smscurtains.app.data.Invoice
import lk.smscurtains.app.model.CurtainMath
import lk.smscurtains.app.model.date
import lk.smscurtains.app.model.money
import org.json.JSONObject

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, busy: Boolean, error: String, online: Boolean = true) {
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val selectedAdmin = username == "admin"

    Column(Modifier.fillMaxSize().background(ShopColors.Linen).verticalScroll(rememberScrollState())
        .padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(86.dp).background(ShopColors.SoftJade, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painter = painterResource(R.drawable.ic_curtains), contentDescription = "SMS Curtains",
                    tint = Color.Unspecified, modifier = Modifier.size(50.dp))
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("SMS CURTAINS", style = MaterialTheme.typography.headlineLarge, color = ShopColors.Forest)
            Text("Welcome to your shop", style = MaterialTheme.typography.titleMedium,
                color = ShopColors.Jade)
            Text("Simple billing. Better business.", style = MaterialTheme.typography.bodyMedium,
                color = ShopColors.Muted)
        }
        Card(shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Sign in", style = MaterialTheme.typography.headlineSmall, color = ShopColors.Forest)
                    Text("Choose your account to continue", style = MaterialTheme.typography.bodyMedium,
                        color = ShopColors.Muted)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RolePick("Admin", Icons.Outlined.AdminPanelSettings, selectedAdmin,
                        Modifier.weight(1f)) { username = "admin"; password = "" }
                    RolePick("Employee", Icons.Outlined.Badge, !selectedAdmin,
                        Modifier.weight(1f)) { username = "employee"; password = "" }
                }
                OutlinedTextField(value = username, onValueChange = { username = it.trim() },
                    label = { Text("Username") }, singleLine = true, readOnly = true,
                    leadingIcon = { Icon(Icons.Outlined.PersonOutline, null) },
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it },
                    label = { Text("Password") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.LockOutline, null) },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password")
                    } },
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                if (error.isNotBlank()) Card(colors = CardDefaults.cardColors(containerColor = ShopColors.SoftAmber)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Button(onClick = { onLogin(username, password) }, enabled = !busy && username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(62.dp), shape = RoundedCornerShape(18.dp)) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.Login, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(if (busy) "Signing in..." else "Sign in to SMS Curtains",
                        style = MaterialTheme.typography.titleMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (online) Icons.Outlined.CloudDone else Icons.Outlined.WifiOff,
                        contentDescription = null, tint = if (online) ShopColors.Jade else ShopColors.Muted)
                    Text(if (online) "SMS Curtains Cloud ready" else "Offline · cached login only",
                        style = MaterialTheme.typography.bodySmall, color = ShopColors.Muted)
                }
            }
        }
        Text("Your bills are securely saved and synced across staff phones.",
            modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall,
            color = ShopColors.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun RolePick(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(66.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) ShopColors.SoftJade else Color.White,
            contentColor = if (selected) ShopColors.Forest else ShopColors.Muted),
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp,
            if (selected) ShopColors.Jade else ShopColors.Sand),
        contentPadding = PaddingValues(horizontal = 8.dp)) {
        Icon(icon, null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AdminReviewsScreen(items: List<ApprovalRequest>, canDecide: Boolean, onBack: () -> Unit, onDecide: (ApprovalRequest, String) -> Unit) {
    val pending = items.filter { it.status in listOf("PENDING", "PENDING_UPLOAD", "AWAITING_SYNC") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        PageHeader(if (canDecide) "Admin approvals" else "My approval requests", "${pending.size} awaiting review", onBack)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (items.isEmpty()) Text("No employee requests yet.")
            items.forEach { item ->
                Section("${if (item.type == "PAYMENT") "Payment" else "Bill change"} • ${item.createdBy}") {
                    SummaryPair("Submitted", date(item.createdAt))
                    SummaryPair("Bill reference", item.billUid.take(8))
                    if (item.type == "PAYMENT") {
                        val payment = runCatching { JSONObject(item.payload) }.getOrNull()
                        SummaryPair("Amount", money(payment?.optDouble("amount") ?: 0.0))
                        SummaryPair("Method", payment?.optString("method") ?: "")
                    } else {
                        val changes = runCatching { JSONObject(item.payload) }.getOrNull()
                        changes?.keys()?.forEach { key ->
                            SummaryPair(key.replaceFirstChar { it.uppercase() }, changes.optString(key))
                        }
                    }
                    if (item.note.isNotBlank()) Text("Note: ${item.note}")
                    Text("Status: ${item.status}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (canDecide && item.status == "PENDING") {
                        BigButton("Approve request", onClick = { onDecide(item, "APPROVED") })
                        OutlinedButton(onClick = { onDecide(item, "REJECTED") }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text("Reject")
                        }
                    }
                }
            }
        }
    }
}

fun applyBillChanges(invoice: Invoice, changes: JSONObject): Invoice {
    fun txt(name: String, old: String): String = if (changes.has(name)) changes.optString(name) else old
    fun num(name: String, old: Double): Double = if (changes.has(name)) changes.optDouble(name) else old
    val m = num("materialRate", invoice.materialRate)
    val s = num("stripRate", invoice.stripRate)
    val e = num("eyeletRate", invoice.eyeletRate)
    val w = num("woodRate", invoice.woodRate)
    val b = num("bracketRate", invoice.bracketRate)
    val c = num("endcapRate", invoice.endcapRate)
    val sew = num("sewingRate", invoice.sewingRate)
    val a = CurtainMath.round2(invoice.fabricMetres * m)
    val z = CurtainMath.round2(invoice.fabricMetres * s)
    val f = CurtainMath.round2(invoice.eyeletCount * e)
    val g = CurtainMath.round2(invoice.grandFeet * w)
    val h = CurtainMath.round2(invoice.bracketCount * b)
    val k = CurtainMath.round2(invoice.endcapCount * c)
    val l = CurtainMath.round2(invoice.fabricMetres * sew)
    return invoice.copy(
        customer = txt("customer", invoice.customer), phone = txt("phone", invoice.phone),
        address = txt("address", invoice.address), dueDate = txt("dueDate", invoice.dueDate),
        materialRate = m, materialTotal = a, stripRate = s, stripTotal = z,
        eyeletRate = e, eyeletTotal = f, woodRate = w, woodTotal = g,
        bracketRate = b, bracketTotal = h, endcapRate = c, endcapTotal = k,
        sewingRate = sew, sewingTotal = l,
        grandTotal = CurtainMath.round2(a + z + f + g + h + k + l)
    )
}

@Composable
fun EditBillDialog(invoice: Invoice, isAdmin: Boolean, onDismiss: () -> Unit, onSubmit: (JSONObject, String) -> Unit) {
    var customer by remember(invoice.uid) { mutableStateOf(invoice.customer) }
    var phone by remember(invoice.uid) { mutableStateOf(invoice.phone) }
    var address by remember(invoice.uid) { mutableStateOf(invoice.address) }
    var dueDate by remember(invoice.uid) { mutableStateOf(invoice.dueDate) }
    var material by remember(invoice.uid) { mutableStateOf(invoice.materialRate.toString()) }
    var strip by remember(invoice.uid) { mutableStateOf(invoice.stripRate.toString()) }
    var eyelet by remember(invoice.uid) { mutableStateOf(invoice.eyeletRate.toString()) }
    var wood by remember(invoice.uid) { mutableStateOf(invoice.woodRate.toString()) }
    var bracket by remember(invoice.uid) { mutableStateOf(invoice.bracketRate.toString()) }
    var endcap by remember(invoice.uid) { mutableStateOf(invoice.endcapRate.toString()) }
    var sewing by remember(invoice.uid) { mutableStateOf(invoice.sewingRate.toString()) }
    var note by remember(invoice.uid) { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (isAdmin) "Modify bill" else "Request bill modification") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Input("Customer", customer, { customer = it })
                Input("Phone", phone, { phone = it })
                Input("Address", address, { address = it })
                Input("Completion date (YYYY-MM-DD)", dueDate, { dueDate = it })
                HorizontalDivider()
                Text("Prices (LKR)", fontWeight = FontWeight.Bold)
                Input("Fabric / metre", material, { material = it }, numeric = true)
                Input("Fusion / metre", strip, { strip = it }, numeric = true)
                Input("Eyelet / piece", eyelet, { eyelet = it }, numeric = true)
                Input("Wood / foot", wood, { wood = it }, numeric = true)
                Input("Bracket / piece", bracket, { bracket = it }, numeric = true)
                Input("Endcap / piece", endcap, { endcap = it }, numeric = true)
                Input("Sewing / metre", sewing, { sewing = it }, numeric = true)
                if (!isAdmin) Input("Reason for change", note, { note = it }, singleLine = false)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        }, confirmButton = {
            Button(onClick = {
                val values = listOf(material, strip, eyelet, wood, bracket, endcap, sewing).map { it.toDoubleOrNull() }
                if (customer.isBlank() || values.any { it == null || !it.isFinite() || it < 0.0 || it > 1e7 }) {
                    error = "Enter a customer name and valid prices."
                } else {
                    val changes = JSONObject()
                    fun changed(name: String, new: String, old: String) { if (new != old) changes.put(name, new) }
                    fun amount(name: String, new: Double, old: Double) { if (new != old) changes.put(name, new) }
                    changed("customer", customer, invoice.customer)
                    changed("phone", phone, invoice.phone)
                    changed("address", address, invoice.address)
                    changed("dueDate", dueDate, invoice.dueDate)
                    val v = values.map { it!! }
                    amount("materialRate", v[0], invoice.materialRate)
                    amount("stripRate", v[1], invoice.stripRate)
                    amount("eyeletRate", v[2], invoice.eyeletRate)
                    amount("woodRate", v[3], invoice.woodRate)
                    amount("bracketRate", v[4], invoice.bracketRate)
                    amount("endcapRate", v[5], invoice.endcapRate)
                    amount("sewingRate", v[6], invoice.sewingRate)
                    if (changes.length() == 0) error = "Nothing changed."
                    else { onSubmit(changes, note); onDismiss() }
                }
            }) { Text(if (isAdmin) "Save changes" else "Send for approval") }
        }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
