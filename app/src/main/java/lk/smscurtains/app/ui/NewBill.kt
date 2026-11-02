package lk.smscurtains.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lk.smscurtains.app.data.Invoice
import lk.smscurtains.app.model.*

@Composable
fun NewBillScreen(onBack: () -> Unit, onSave: (Invoice, List<Measurement>, List<Measurement>) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val pageScroll = rememberScrollState()
    LaunchedEffect(step) { pageScroll.scrollTo(0) }
    var measurementTab by remember { mutableIntStateOf(0) }
    val windows = remember { mutableStateListOf<Measurement>() }
    val doors = remember { mutableStateListOf<Measurement>() }
    var error by remember { mutableStateOf("") }

    var materialName by remember { mutableStateOf("") }
    var materialRate by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("0") }
    var eyeletColor by remember { mutableStateOf("Gold") }
    var eyeletRate by remember { mutableStateOf("0") }
    var stripType by remember { mutableStateOf("Normal") }
    var stripRate by remember { mutableStateOf("0") }
    var sewingType by remember { mutableStateOf("In-house") }
    var sewingRate by remember { mutableStateOf("350") }
    var woodColor by remember { mutableStateOf("Teak") }
    var woodRate by remember { mutableStateOf("0") }
    var bracketRate by remember { mutableStateOf("") }
    var endcapRate by remember { mutableStateOf("0") }
    val tripleAtThree = true
    var moreOptions by remember { mutableStateOf(false) }

    var customer by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var payPref by remember { mutableStateOf("CASH") }
    var saving by remember { mutableStateOf(false) }

    val totals = CurtainMath.calculate(
        windows, doors, extra.toDoubleOrNull() ?: 0.0,
        materialRate.toDoubleOrNull() ?: 0.0, eyeletRate.toDoubleOrNull() ?: 0.0,
        stripRate.toDoubleOrNull() ?: 0.0, sewingRate.toDoubleOrNull() ?: 0.0,
        woodRate.toDoubleOrNull() ?: 0.0,
        bracketRate.toDoubleOrNull() ?: woodRate.toDoubleOrNull() ?: 0.0,
        endcapRate.toDoubleOrNull() ?: 0.0, tripleAtThree
    )
    val stepNames = listOf("Measurements", "Prices", "Customer")

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
        PageHeader("New curtain bill", "Step ${step + 1} of 3  •  ${stepNames[step]}", onBack = {
            if (step > 0) { error = ""; step-- } else onBack()
        })
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth().height(7.dp),
            color = ShopColors.Jade, trackColor = ShopColors.SoftJade)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f).verticalScroll(pageScroll), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (step) {
                0 -> {
                    Text("Add measurements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Enter each size once. The app calculates all quantities automatically.")
                    TabRow(selectedTabIndex = measurementTab) {
                        Tab(selected = measurementTab == 0, onClick = { measurementTab = 0 }, text = { Text("Windows (${windows.size})") })
                        Tab(selected = measurementTab == 1, onClick = { measurementTab = 1 }, text = { Text("Doors (${doors.size})") })
                    }
                    if (measurementTab == 0) {
                        windows.forEachIndexed { idx, m -> MeasurementItem(idx, m, "Window", onRemove = { windows.removeAt(idx) }) }
                        MeasurementEditor(kind = "WINDOW", onAdd = { windows.add(it); error = "" })
                    } else {
                        doors.forEachIndexed { idx, m -> MeasurementItem(idx, m, "Door", onRemove = { doors.removeAt(idx) }) }
                        MeasurementEditor(kind = "DOOR", onAdd = { doors.add(it); error = "" })
                    }
                    Section("Live measurement total") {
                        SummaryPair("Windows", "${qty(totals.windowFeet)} ft")
                        SummaryPair("Doors", "${qty(totals.doorFeet)} ft")
                        SummaryPair("GRAND LENGTH", "${qty(totals.grandFeet)} ft", prominent = true)
                    }
                }
                1 -> {
                    Text("Fabric & prices", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Section("Fabric needed") {
                        SummaryPair("Measured length", "${qty(totals.grandFeet)} ft")
                        Text("1 measured foot = 1 metre of fabric (SMS Curtains rule)", style = MaterialTheme.typography.bodySmall)
                        Choice("Extra fabric", "${extra} m", listOf("0", "0.5", "1", "1.5", "1.75", "2", "2.5").map { "$it m" }) {
                            extra = it.removeSuffix(" m")
                        }
                        SummaryPair("Fabric metres", "${qty(totals.clothMetres)} m", prominent = true)
                    }
                    Section("Enter prices in LKR") {
                        Input("Fabric name *", materialName, { materialName = it })
                        Input("Fabric Rs/m *", materialRate, { materialRate = it }, numeric = true)
                        TwoFields(
                            first = { mod -> Input("Eyelet Rs/pc", eyeletRate, { eyeletRate = it }, mod, numeric = true) },
                            second = { mod -> Input("Fusion Rs/m", stripRate, { stripRate = it }, mod, numeric = true) }
                        )
                        TwoFields(
                            first = { mod -> Input("Sew Rs/m", sewingRate, { sewingRate = it }, mod, numeric = true) },
                            second = { mod -> Input("Wood Rs/ft", woodRate, { woodRate = it }, mod, numeric = true) }
                        )
                        TwoFields(
                            first = { mod -> Input("Bracket Rs/pc", bracketRate, { bracketRate = it }, mod, numeric = true) },
                            second = { mod -> Input("Endcap Rs/pc", endcapRate, { endcapRate = it }, mod, numeric = true) }
                        )
                        Text("${totals.eyelets} eyelets · ${totals.brackets} brackets · ${totals.endcaps} endcaps", style = MaterialTheme.typography.bodySmall)
                        Text("Blank bracket rate = wood-bar rate.", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = { moreOptions = !moreOptions }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                        Text(if (moreOptions) "Hide fabric options" else "Optional: colors, quality & sewing type")
                    }
                    if (moreOptions) Section("Item options") {
                        TwoFields(
                            first = { mod -> Choice("Eyelet color", eyeletColor, listOf("Gold", "S.Gold", "Silver", "S.Silver", "M.Silver", "M.Gold"), mod) { eyeletColor = it } },
                            second = { mod -> Choice("Fusion type", stripType, listOf("Normal", "Best", "Cut", "Other"), mod) { stripType = it } }
                        )
                        TwoFields(
                            first = { mod -> Choice("Sewing", sewingType, listOf("In-house", "Shop", "Employee", "Contract"), mod) { sewingType = it } },
                            second = { mod -> Choice("Wood color", woodColor, listOf("Teak", "Brown"), mod) { woodColor = it } }
                        )
                    }
                    Section("Live bill estimate") {
                        SummaryPair("Fabric", money(totals.materialTotal))
                        SummaryPair("Accessories", money(totals.bracketTotal + totals.endcapTotal))
                        SummaryPair("Other charges", money(totals.eyeletTotal + totals.stripTotal + totals.sewingTotal + totals.woodTotal))
                        HorizontalDivider()
                        SummaryPair("GRAND TOTAL", money(totals.grandTotal), prominent = true)
                    }
                }
                else -> {
                    Section("Customer details") {
                        Input("Customer name *", customer, { customer = it })
                        Input("Phone", phone, { phone = it })
                        Input("Address", address, { address = it }, singleLine = false)
                        TwoFields(
                            first = { mod -> DateChoice("Completion date", dueDate, mod) { dueDate = it } },
                            second = { mod -> Choice("Payment type", payPref, listOf("CASH", "CARD", "CREDIT"), mod) { payPref = it } }
                        )
                    }
                    Section("Final bill summary") {
                        SummaryPair("Window length", "${qty(totals.windowFeet)} ft")
                        SummaryPair("Door length", "${qty(totals.doorFeet)} ft")
                        SummaryPair("Fabric", "${qty(totals.clothMetres)} m")
                        SummaryPair("Eyelets", "${totals.eyelets} pcs")
                        SummaryPair("Brackets", "${totals.brackets} pcs")
                        SummaryPair("Endcaps", "${totals.endcaps} pcs")
                        HorizontalDivider()
                        SummaryPair("GRAND TOTAL", money(totals.grandTotal), prominent = true)
                        Text("After saving, you can record dated advances, track work and print the receipt.")
                    }
                }
            }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(12.dp))
        BigButton(if (step == 2) "Save bill & view receipt" else "Continue  →", onClick = {
            error = ""
            if (step == 0 && windows.isEmpty() && doors.isEmpty()) {
                error = "Add at least one window or door measurement."
            } else if (step == 1) {
                val mandatory = materialRate.toDoubleOrNull()
                val prices = listOf(eyeletRate, stripRate, sewingRate, woodRate, endcapRate) + listOfNotNull(bracketRate.takeIf { it.isNotBlank() })
                if (materialName.isBlank() || mandatory == null || !mandatory.isFinite() || mandatory < 0 || prices.any { (it.toDoubleOrNull()?.takeIf(Double::isFinite) ?: -1.0) < 0.0 }) {
                    error = "Enter a fabric name and valid non-negative prices. Use 0 where not charged."
                } else step++
            } else if (step == 2) {
                if (customer.isBlank()) error = "Please enter the customer's name."
                else if (!saving) {
                    saving = true
                    onSave(Invoice(
                        customer = customer.trim(), phone = phone.trim(), address = address.trim(), dueDate = dueDate,
                        paymentPreference = payPref,
                        windowFeet = totals.windowFeet, doorFeet = totals.doorFeet, grandFeet = totals.grandFeet,
                        fabricMetres = totals.clothMetres, extraMetres = extra.toDoubleOrNull() ?: 0.0,
                        materialName = materialName.trim(), materialRate = materialRate.toDouble(), materialTotal = totals.materialTotal,
                        stripType = stripType, stripRate = stripRate.toDouble(), stripTotal = totals.stripTotal,
                        eyeletColor = eyeletColor, eyeletRate = eyeletRate.toDouble(), eyeletCount = totals.eyelets, eyeletTotal = totals.eyeletTotal,
                        woodColor = woodColor, woodRate = woodRate.toDouble(), woodTotal = totals.woodTotal,
                        bracketCount = totals.brackets, bracketRate = bracketRate.toDoubleOrNull() ?: woodRate.toDouble(), bracketTotal = totals.bracketTotal,
                        endcapCount = totals.endcaps, endcapRate = endcapRate.toDouble(), endcapTotal = totals.endcapTotal,
                        sewingType = sewingType, sewingRate = sewingRate.toDouble(), sewingTotal = totals.sewingTotal,
                        grandTotal = totals.grandTotal
                    ), windows.toList(), doors.toList())
                }
            } else step++
        }, enabled = !saving)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MeasurementItem(index: Int, m: Measurement, kind: String, onRemove: () -> Unit) {
    Section("$kind ${index + 1} — ${m.type}") {
        SummaryPair("Length", "${qty(m.length)} ${m.unit} × ${m.frames} frames")
        SummaryPair("Feet", "${qty(CurtainMath.feet(m.length, m.unit) * m.frames)} ft")
        if (kind == "Window") SummaryPair("Windows per frame", "${m.openings}")
        if (m.height != null) SummaryPair("Height (reference)", "${qty(m.height)} ${m.heightUnit} (${qty(CurtainMath.feet(m.height, m.heightUnit))} ft)")
        TextButton(onClick = onRemove) { Text("Remove this measurement") }
    }
}

@Composable
private fun MeasurementEditor(kind: String, onAdd: (Measurement) -> Unit) {
    val isWindow = kind == "WINDOW"
    var type by remember { mutableStateOf(if (isWindow) "Single" else "Large") }
    var length by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("ft") }
    var height by remember { mutableStateOf("") }
    var heightUnit by remember { mutableStateOf("ft") }
    var openings by remember { mutableStateOf("1") }
    var frames by remember { mutableStateOf("1") }
    var localError by remember { mutableStateOf("") }
    var showHeight by remember { mutableStateOf(false) }

    Section(if (isWindow) "+ Add window measurement" else "+ Add door measurement") {
        TwoFields(
            first = { mod -> Choice("${if (isWindow) "Window" else "Door"} type", type,
                if (isWindow) listOf("Single", "Double", "Triple") else listOf("Large", "Medium", "Small", "Arch"), mod) {
                type = it
                if (isWindow) openings = when (it) { "Double" -> "2"; "Triple" -> "3"; else -> "1" }
            } },
            second = { mod -> Choice("Unit", unit, listOf("ft", "inch", "cm", "m"), mod) { unit = it } }
        )
        Input("Length *", length, { length = it }, numeric = true)
        if (!showHeight) TextButton(onClick = { showHeight = true }) { Text("+ Optional height") }
        else {
            TwoFields(
                first = { mod -> Input("Height (optional)", height, { height = it }, mod, numeric = true) },
                second = { mod -> Choice("Height unit", heightUnit, listOf("ft", "inch", "cm", "m"), mod) { heightUnit = it } }
            )
        }
        if (isWindow) TwoFields(
            first = { mod -> Input("Openings *", openings, { openings = it }, mod, numeric = true) },
            second = { mod -> Input("Frames *", frames, { frames = it }, mod, numeric = true) }
        ) else Input("Number of doors *", frames, { frames = it }, numeric = true)
        if (localError.isNotBlank()) Text(localError, color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = {
            val len = length.toDoubleOrNull()
            val h = height.takeIf { it.isNotBlank() }?.toDoubleOrNull()
            val count = if (isWindow) openings.toIntOrNull() else 1
            val frameCount = frames.toIntOrNull()
            if (len == null || !len.isFinite() || len <= 0 || (height.isNotBlank() && (h == null || !h.isFinite() || h <= 0)) || count == null || count <= 0 || frameCount == null || frameCount <= 0) {
                localError = "Enter positive length and whole-number counts. Height may be left blank."
            } else {
                onAdd(Measurement(type, len, unit, h, heightUnit, count, frameCount))
                length = ""; height = ""; openings = if (isWindow) when (type) { "Double" -> "2"; "Triple" -> "3"; else -> "1" } else "1"
                frames = "1"; localError = ""
            }
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("+ Add measurement") }
    }
}
