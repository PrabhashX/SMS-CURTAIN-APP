package lk.smscurtains.app

import android.content.Context
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.room.withTransaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import lk.smscurtains.app.data.*
import lk.smscurtains.app.model.Measurement
import lk.smscurtains.app.model.OfflineAlertSchedule
import lk.smscurtains.app.sync.*
import lk.smscurtains.app.ui.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MainActivity : ComponentActivity() {
    // Foreground only: Android restricts recurring toast notifications in the background.
    private var appInForeground by mutableStateOf(false)
    private var offlineToast: Toast? = null
    private var offlinePlayer: MediaPlayer? = null

    override fun onResume() { super.onResume(); appInForeground = true }
    override fun onPause() {
        appInForeground = false
        stopOfflineAlert()
        super.onPause()
    }
    override fun onDestroy() {
        stopOfflineAlert()
        super.onDestroy()
    }

    private fun hasInternet(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun showOfflineAlert() {
        // A fresh toast + a beep on each timer tick, while internet is unavailable.
        offlineToast?.cancel()
        offlineToast = Toast.makeText(
            this,
            "You're offline. Bills are saved and will sync when online.",
            Toast.LENGTH_SHORT
        ).also { it.show() }
        runCatching {
            offlinePlayer?.release()
            offlinePlayer = MediaPlayer.create(this, R.raw.offline_alert)?.apply {
                setOnCompletionListener { player ->
                    if (offlinePlayer === player) offlinePlayer = null
                    player.release()
                }
                setOnErrorListener { player, _, _ ->
                    if (offlinePlayer === player) offlinePlayer = null
                    player.release()
                    true
                }
                start()
            }
        }
    }

    private fun stopOfflineAlert() {
        offlineToast?.cancel()
        offlineToast = null
        offlinePlayer?.let { player ->
            runCatching {
                if (player.isPlaying) player.stop()
                player.release()
            }
        }
        offlinePlayer = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15 enforces edge-to-edge. Compose safeDrawing protects both system bars.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        val database = ShopDatabase.get(applicationContext)
        val session = SessionStore(applicationContext)
        SyncScheduler.schedule(applicationContext)
        setContent {
            CurtainsTheme {
                val dao = remember { database.dao() }
                val summaries by dao.watchSummaries().collectAsState(initial = emptyList())
                val approvals by dao.watchRequests().collectAsState(initial = emptyList())
                val queued by dao.watchQueueSize().collectAsState(initial = 0)
                val scope = rememberCoroutineScope()
                // Do not auto-enter a leftover V5 local-demo session (which has no server token).
                var role by remember { mutableStateOf(if (session.token.isNotBlank()) session.role else "") }
                var splashVisible by remember { mutableStateOf(true) }
                var profileOpen by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(1050L); splashVisible = false }
                var online by remember { mutableStateOf(hasInternet()) }
                var signingIn by remember { mutableStateOf(false) }
                var loginError by remember { mutableStateOf("") }
                var cloudUnavailable by remember { mutableStateOf(false) }
                var route by remember { mutableStateOf("HOME") }
                var selectedId by remember { mutableLongStateOf(0L) }
                val selected = summaries.firstOrNull { it.invoice.id == selectedId }
                fun triggerSync() { if (!session.demoMode && online) SyncScheduler.soon(applicationContext) }

                // Poll while the app is visible. Some Android devices fail to deliver
                // a timely network callback when Wi-Fi stays connected but internet drops.
                // The alert schedule is independent of connection callbacks: first beep/toast
                // happens as soon as we detect offline, then every 5 seconds continuously.
                LaunchedEffect(appInForeground) {
                    val alerts = OfflineAlertSchedule(intervalMillis = 5_000L)
                    if (appInForeground) {
                        while (isActive) {
                            val connected = hasInternet()
                            if (online != connected) {
                                online = connected
                                if (connected) triggerSync()
                            }
                            if (alerts.shouldAlert(
                                    isOnline = connected,
                                    inForeground = true,
                                    nowMillis = SystemClock.elapsedRealtime()
                                )) {
                                showOfflineAlert()
                            } else if (connected) {
                                // Cancel any visible toast and active audio on reconnection.
                                stopOfflineAlert()
                            }
                            delay(500L) // Detect network changes within about half a second.
                        }
                    } else {
                        stopOfflineAlert()
                    }
                }
                LaunchedEffect(role, online) {
                    if (role.isNotBlank() && online && !session.demoMode) {
                        while (true) {
                            cloudUnavailable = !CloudSync(applicationContext).sync()
                            delay(30_000)
                        }
                    }
                }
                BackHandler(role.isNotBlank() && route != "HOME") { route = "HOME" }
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
                    if (!online) Surface(color = Color(0xFFFFF0D8)) {
                        Text("You're offline • Changes saved locally until sync", modifier = Modifier.fillMaxWidth().padding(10.dp), color = Color(0xFF704A00))
                    } else if (cloudUnavailable && role.isNotBlank() && !session.demoMode) Surface(color = Color(0xFFFFF0D8)) {
                        Text("Cloud unavailable • Saved changes are queued", modifier = Modifier.fillMaxWidth().padding(10.dp), color = Color(0xFF704A00))
                    }
                    if (splashVisible) {
                        BrandSplash()
                    } else if (role.isBlank()) {
                        LoginScreen(busy = signingIn, error = loginError, online = online,
                            onLogin = { username, password ->
                                signingIn = true
                                loginError = ""
                                scope.launch {
                                    try {
                                        val result = CloudSync(applicationContext).login(username, password)
                                        role = result.second
                                        route = "HOME"
                                        triggerSync()
                                    } catch (e: Exception) {
                                        if (!online && session.cachedOfflineLogin(username, password) && session.token.isNotBlank()) {
                                            role = session.role
                                            route = "HOME"
                                            toast("Offline login. Changes will sync when connected.")
                                        } else {
                                            loginError = if (!online) "Internet required for your first sign-in on this phone."
                                                else (e.localizedMessage ?: "Could not sign in. Check your account and connection.")
                                        }
                                    } finally { signingIn = false }
                                }
                            }
                        )
                    } else {
                        Scaffold(
                            modifier = Modifier.weight(1f),
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                // Hide tabs during form editing to avoid losing a half-entered bill.
                                if (route != "NEW" && route != "DETAIL") {
                                    NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0),
                                        containerColor = MaterialTheme.colorScheme.surface) {
                                        listOf("HOME", "BILLS", "NEW", "REVIEWS").forEach { destination ->
                                            val title = when (destination) {
                                                "HOME" -> "Home"
                                                "BILLS" -> "Bills"
                                                "NEW" -> "New bill"
                                                else -> "Reviews"
                                            }
                                            NavigationBarItem(
                                                selected = route == destination,
                                                onClick = {
                                                    if (destination == "REVIEWS") triggerSync()
                                                    route = destination
                                                },
                                                icon = {
                                                    val icon = when (destination) {
                                                        "HOME" -> Icons.Outlined.Home
                                                        "BILLS" -> Icons.Outlined.ListAlt
                                                        "NEW" -> Icons.Outlined.AddCircleOutline
                                                        else -> Icons.Outlined.FactCheck
                                                    }
                                                    Icon(icon, contentDescription = title)
                                                },
                                                label = { Text(title, style = MaterialTheme.typography.labelMedium) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = ShopColors.Forest,
                                                    selectedTextColor = ShopColors.Forest,
                                                    indicatorColor = ShopColors.SoftJade,
                                                    unselectedIconColor = ShopColors.Muted,
                                                    unselectedTextColor = ShopColors.Muted),
                                                alwaysShowLabel = true
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                          Box(Modifier.fillMaxSize().padding(innerPadding)) {
                            when (route) {
                                "HOME" -> Dashboard(summaries, role, approvals.count { it.status in listOf("PENDING", "PENDING_UPLOAD", "AWAITING_SYNC") }, queued, session.demoMode,
                                    onNew = { route = "NEW" }, onOpen = { id -> selectedId = id; route = "DETAIL" },
                                    onApprovals = { triggerSync(); route = "REVIEWS" }, onLogout = {
                                        session.logout(); role = ""; route = "HOME"
                                    }, onAllBills = { route = "BILLS" },
                                    onProfile = { profileOpen = true })
                                "BILLS" -> BillsPage(summaries, onOpen = { id -> selectedId = id; route = "DETAIL" })
                                "NEW" -> NewBillScreen(
                                    onBack = { route = "HOME" },
                                    onSave = { invoice, windows, doors -> scope.launch {
                                        val id = database.withTransaction {
                                            val newId = dao.insertInvoice(invoice)
                                            val measurements = windows.map { it.toStored(newId, "WINDOW") } + doors.map { it.toStored(newId, "DOOR") }
                                            dao.insertMeasurements(measurements)
                                            val payload = JSONObject().put("invoice", JsonCodec.invoice(invoice))
                                                .put("measurements", JSONArray().apply { measurements.forEach { put(JsonCodec.measurement(it)) } })
                                            dao.enqueue(SyncEvent(actor = session.username, type = "CREATE_BILL", billUid = invoice.uid, payload = payload.toString()))
                                            newId
                                        }
                                        selectedId = id; route = "DETAIL"; triggerSync()
                                    } }
                                )
                                "DETAIL" -> if (selected != null) BillDetailsScreen(
                                    summary = selected, dao = dao, role = role, onBack = { route = "HOME" },
                                    onWorkStatus = { status -> scope.launch {
                                        database.withTransaction {
                                            dao.updateInvoice(selected.invoice.copy(workStatus = status))
                                            dao.enqueue(SyncEvent(actor = session.username, type = "WORK_STATUS", billUid = selected.invoice.uid,
                                                payload = JSONObject().put("status", status).toString()))
                                        }
                                        triggerSync()
                                    } },
                                    onRecordPayment = { payment -> scope.launch {
                                        database.withTransaction {
                                            val paid = dao.paidTotal(selected.invoice.id)
                                            if (payment.amount > 0.0 && payment.amount <= selected.invoice.grandTotal - paid + 0.005) {
                                                if (role == "admin") {
                                                    dao.insertPayment(payment)
                                                    dao.enqueue(SyncEvent(actor = session.username, type = "PAYMENT", billUid = selected.invoice.uid,
                                                        payload = JsonCodec.payment(payment).toString()))
                                                } else {
                                                    val review = ApprovalRequest(billUid = selected.invoice.uid, type = "PAYMENT",
                                                        payload = JsonCodec.payment(payment).toString(), status = "PENDING_UPLOAD", createdBy = session.username)
                                                    dao.saveRequest(review)
                                                    dao.enqueue(SyncEvent(actor = session.username, type = "REQUEST_PAYMENT", billUid = selected.invoice.uid,
                                                        payload = JSONObject().put("uid", review.uid).put("payment", JsonCodec.payment(payment)).toString()))
                                                }
                                            }
                                        }
                                        toast(if (role == "admin") "Payment saved" else "Payment request saved. Admin can review after sync.")
                                        triggerSync()
                                    } },
                                    onModify = { changes, note -> scope.launch {
                                        if (role == "admin") {
                                            val next = applyBillChanges(selected.invoice, changes)
                                            val paid = dao.paidTotal(selected.invoice.id)
                                            if (next.grandTotal < paid - 0.005) {
                                                toast("New total cannot be less than money already received")
                                            } else {
                                                database.withTransaction {
                                                    dao.updateInvoice(next)
                                                    dao.enqueue(SyncEvent(actor = session.username, type = "MODIFY", billUid = next.uid,
                                                        payload = JSONObject().put("changes", changes).toString()))
                                                }
                                                toast("Bill changes saved"); triggerSync()
                                            }
                                        } else {
                                            database.withTransaction {
                                                val request = ApprovalRequest(billUid = selected.invoice.uid,
                                                    type = "MODIFY", payload = changes.toString(), note = note, status = "PENDING_UPLOAD", createdBy = session.username)
                                                dao.saveRequest(request)
                                                dao.enqueue(SyncEvent(actor = session.username, type = "REQUEST_MODIFY", billUid = selected.invoice.uid,
                                                    payload = JSONObject().put("uid", request.uid).put("changes", changes).put("note", note).toString()))
                                            }
                                            toast("Change request saved. Admin can review after sync."); triggerSync()
                                        }
                                    } }
                                ) else Text("Bill not found", modifier = Modifier.padding(20.dp))
                                "REVIEWS" -> AdminReviewsScreen(
                                    items = if (role == "admin") approvals else approvals.filter { it.createdBy == session.username },
                                    canDecide = role == "admin", onBack = { route = "HOME" },
                                    onDecide = { review, decision -> scope.launch {
                                        database.withTransaction {
                                            dao.saveRequest(review.copy(status = "AWAITING_SYNC"))
                                            dao.enqueue(SyncEvent(actor = session.username, type = "DECIDE_REQUEST", billUid = review.billUid,
                                                payload = JSONObject().put("uid", review.uid).put("decision", decision).toString()))
                                        }
                                        toast("Decision queued for sync"); triggerSync()
                                    } }
                                )
                            }
                          }
                        }
                        if (profileOpen) {
                            ProfileSheet(role = role, username = session.username, online = online,
                                queued = queued, onDismiss = { profileOpen = false },
                                onLogout = {
                                    profileOpen = false
                                    session.logout()
                                    role = ""
                                    route = "HOME"
                                })
                        }
                    }
                }
            }
        }
    }
}

private fun Measurement.toStored(id: Long, kind: String) = StoredMeasurement(
    invoiceId = id, kind = kind, type = type, length = length, unit = unit,
    height = height, heightUnit = heightUnit, openings = openings, frames = frames
)
