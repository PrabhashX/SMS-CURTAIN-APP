package lk.smscurtains.app.sync

import android.content.Context
import androidx.room.withTransaction
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import lk.smscurtains.app.data.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("sms_session_v2", Context.MODE_PRIVATE)
    val username: String get() = prefs.getString("username", "") ?: ""
    val role: String get() = prefs.getString("role", "") ?: ""
    val token: String get() = prefs.getString("token", "") ?: ""
    // Phone-only edition: the shop API is preconfigured. Never embed MongoDB secrets in the app.
    val server: String get() = "https://sms-curtain-app.onrender.com"
    val demoMode: Boolean get() = false
    fun logout() = prefs.edit().remove("username").remove("role").remove("token").apply()
    fun saveLogin(user: String, accessRole: String, sessionToken: String, password: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString("username", user).putString("role", accessRole)
            .putString("token", sessionToken).putString("salt_$user", salt.toHex())
            .putString("hash_$user", derive(password, salt)).apply()
    }
    fun cachedOfflineLogin(user: String, password: String): Boolean {
        val salt = prefs.getString("salt_$user", null)?.fromHex() ?: return false
        val expected = prefs.getString("hash_$user", null) ?: return false
        return java.security.MessageDigest.isEqual(derive(password, salt).toByteArray(), expected.toByteArray()) &&
            user == username && role in listOf("admin", "employee")
    }
    private fun derive(password: String, salt: ByteArray): String {
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded.toHex()
    }
}
private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
private fun String.fromHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

object JsonCodec {
    fun invoice(i: Invoice): JSONObject = JSONObject().apply {
        put("uid", i.uid); put("createdAt", i.createdAt)
        put("customer", i.customer); put("phone", i.phone); put("address", i.address)
        put("dueDate", i.dueDate); put("paymentPreference", i.paymentPreference)
        put("windowFeet", i.windowFeet); put("doorFeet", i.doorFeet); put("grandFeet", i.grandFeet)
        put("fabricMetres", i.fabricMetres); put("extraMetres", i.extraMetres)
        put("materialName", i.materialName); put("materialRate", i.materialRate); put("materialTotal", i.materialTotal)
        put("stripType", i.stripType); put("stripRate", i.stripRate); put("stripTotal", i.stripTotal)
        put("eyeletColor", i.eyeletColor); put("eyeletRate", i.eyeletRate); put("eyeletCount", i.eyeletCount); put("eyeletTotal", i.eyeletTotal)
        put("woodColor", i.woodColor); put("woodRate", i.woodRate); put("woodTotal", i.woodTotal)
        put("bracketCount", i.bracketCount); put("bracketRate", i.bracketRate); put("bracketTotal", i.bracketTotal)
        put("endcapCount", i.endcapCount); put("endcapRate", i.endcapRate); put("endcapTotal", i.endcapTotal)
        put("sewingType", i.sewingType); put("sewingRate", i.sewingRate); put("sewingTotal", i.sewingTotal)
        put("grandTotal", i.grandTotal); put("workStatus", i.workStatus)
    }
    fun fromInvoice(j: JSONObject, uid: String): Invoice = Invoice(
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        customer = j.getString("customer"), phone = j.optString("phone"), address = j.optString("address"),
        dueDate = j.optString("dueDate"), paymentPreference = j.optString("paymentPreference", "CASH"),
        windowFeet = j.optDouble("windowFeet"), doorFeet = j.optDouble("doorFeet"), grandFeet = j.getDouble("grandFeet"),
        fabricMetres = j.getDouble("fabricMetres"), extraMetres = j.optDouble("extraMetres"),
        materialName = j.getString("materialName"), materialRate = j.getDouble("materialRate"), materialTotal = j.getDouble("materialTotal"),
        stripType = j.optString("stripType", "Normal"), stripRate = j.getDouble("stripRate"), stripTotal = j.getDouble("stripTotal"),
        eyeletColor = j.optString("eyeletColor", "Gold"), eyeletRate = j.getDouble("eyeletRate"), eyeletCount = j.getInt("eyeletCount"), eyeletTotal = j.getDouble("eyeletTotal"),
        woodColor = j.optString("woodColor", "Teak"), woodRate = j.getDouble("woodRate"), woodTotal = j.getDouble("woodTotal"),
        bracketCount = j.getInt("bracketCount"), bracketRate = j.getDouble("bracketRate"), bracketTotal = j.getDouble("bracketTotal"),
        endcapCount = j.getInt("endcapCount"), endcapRate = j.getDouble("endcapRate"), endcapTotal = j.getDouble("endcapTotal"),
        sewingType = j.optString("sewingType", "Shop"), sewingRate = j.getDouble("sewingRate"), sewingTotal = j.getDouble("sewingTotal"),
        grandTotal = j.getDouble("grandTotal"), workStatus = j.optString("workStatus", "TO_START"), uid = uid
    )
    fun measurement(m: StoredMeasurement): JSONObject = JSONObject().apply {
        put("kind", m.kind); put("type", m.type); put("length", m.length); put("unit", m.unit)
        put("height", m.height ?: JSONObject.NULL); put("heightUnit", m.heightUnit)
        put("openings", m.openings); put("frames", m.frames)
    }
    fun fromMeasurement(j: JSONObject, invoiceId: Long): StoredMeasurement = StoredMeasurement(
        invoiceId = invoiceId, kind = j.getString("kind"), type = j.getString("type"),
        length = j.getDouble("length"), unit = j.getString("unit"),
        height = if (j.isNull("height")) null else j.optDouble("height"),
        heightUnit = j.optString("heightUnit", "ft"), openings = j.optInt("openings", 1), frames = j.getInt("frames")
    )
    fun payment(p: Payment): JSONObject = JSONObject().apply {
        put("uid", p.uid); put("amount", p.amount); put("method", p.method)
        put("kind", p.kind.uppercase()); put("paidAt", p.paidAt)
    }
    fun fromPayment(j: JSONObject, invoiceId: Long) = Payment(
        invoiceId = invoiceId, amount = j.getDouble("amount"), method = j.getString("method"),
        kind = j.getString("kind"), paidAt = j.getLong("paidAt"), uid = j.getString("uid")
    )
}

class CloudSync(private val context: Context) {
    private val database = ShopDatabase.get(context)
    private val dao = database.dao()
    private val session = SessionStore(context)
    companion object { private val lock = Mutex() }
    // HTTPS is required: API credentials must never travel over cleartext.
    private fun endpoint(path: String): URL {
        require(session.server.startsWith("https://")) { "Use a HTTPS server URL" }
        return URL(session.server + path)
    }
    private fun post(path: String, body: JSONObject, token: String = ""): JSONObject {
        val con = endpoint(path).openConnection() as HttpURLConnection
        try {
            con.requestMethod = "POST"
            con.connectTimeout = 12_000; con.readTimeout = 20_000
            con.setRequestProperty("Content-Type", "application/json")
            if (token.isNotEmpty()) con.setRequestProperty("Authorization", "Bearer $token")
            con.doOutput = true
            con.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val data = (if (con.responseCode in 200..299) con.inputStream else con.errorStream)?.bufferedReader()?.use { it.readText() } ?: "{}"
            val reply = JSONObject(data)
            if (con.responseCode !in 200..299) error(reply.optString("error", "HTTP ${con.responseCode}"))
            return reply
        } finally { con.disconnect() }
    }
    suspend fun login(username: String, password: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val result = post("/auth/login", JSONObject().put("username", username).put("password", password))
        val role = result.getString("role")
        session.saveLogin(username, role, result.getString("token"), password)
        username to role
    }
    suspend fun sync(): Boolean = lock.withLock {
        if (session.demoMode || session.username.isBlank() || session.token.isBlank()) return@withLock true
        withContext(Dispatchers.IO) {
            try {
                // Preserve pre-v2 Room bills and upload them on the first admin cloud session.
                // Employee cannot migrate historical payments because only admins may record them.
                if (session.role == "admin") database.withTransaction {
                    dao.legacyInvoices().forEach { legacy ->
                        val fresh = legacy.copy(uid = UUID.randomUUID().toString())
                        dao.updateInvoice(fresh)
                        val measurements = dao.measurementsFor(fresh.id)
                        dao.enqueue(SyncEvent(actor = session.username, type = "CREATE_BILL", billUid = fresh.uid,
                            payload = JSONObject().put("invoice", JsonCodec.invoice(fresh))
                                .put("measurements", JSONArray().apply { measurements.forEach { put(JsonCodec.measurement(it)) } }).toString()))
                        dao.paymentsFor(fresh.id).forEach { old ->
                            val fixed = old.copy(uid = UUID.randomUUID().toString())
                            dao.updatePayment(fixed)
                            dao.enqueue(SyncEvent(actor = session.username, type = "PAYMENT", billUid = fresh.uid,
                                payload = JsonCodec.payment(fixed).toString()))
                        }
                        if (fresh.workStatus != "TO_START") dao.enqueue(SyncEvent(actor = session.username,
                            type = "WORK_STATUS", billUid = fresh.uid,
                            payload = JSONObject().put("status", fresh.workStatus).toString()))
                    }
                }
                val out = dao.eventsFor(session.username)
                val outgoing = JSONArray()
                out.forEach { e -> outgoing.put(JSONObject().apply {
                    put("eventId", e.eventId); put("type", e.type); put("actor", e.actor)
                    put("billUid", e.billUid); put("payload", JSONObject(e.payload))
                }) }
                val response = post("/api/sync", JSONObject().put("events", outgoing), session.token)
                val accepted = response.getJSONArray("accepted")
                database.withTransaction { for (idx in 0 until accepted.length()) dao.removeEvent(accepted.getString(idx)) }
                val pending = dao.pendingBillUids().toSet()
                val bills = response.getJSONArray("bills")
                database.withTransaction {
                    for (index in 0 until bills.length()) {
                        val cloud = bills.getJSONObject(index)
                        val uid = cloud.getString("uid")
                        if (uid in pending) continue
                        val remoteInvoice = JsonCodec.fromInvoice(cloud.getJSONObject("invoice"), uid)
                            .copy(workStatus = cloud.getString("workStatus"))
                        val prior = dao.findByUid(uid)
                        val localId = if (prior == null) dao.insertInvoice(remoteInvoice) else {
                            dao.updateInvoice(remoteInvoice.copy(id = prior.id)); prior.id
                        }
                        if (prior == null) {
                            val ms = cloud.optJSONArray("measurements") ?: JSONArray()
                            val measurements = (0 until ms.length()).map { JsonCodec.fromMeasurement(ms.getJSONObject(it), localId) }
                            if (measurements.isNotEmpty()) dao.insertMeasurements(measurements)
                        }
                        val currentPayments = dao.paymentsFor(localId).map { it.uid }.toSet()
                        val remotePayments = cloud.optJSONArray("payments") ?: JSONArray()
                        for (p in 0 until remotePayments.length()) {
                            val payment = JsonCodec.fromPayment(remotePayments.getJSONObject(p), localId)
                            if (payment.uid !in currentPayments) dao.insertPayment(payment)
                        }
                    }
                    val reviews = response.getJSONArray("requests")
                    for (index in 0 until reviews.length()) {
                        val r = reviews.getJSONObject(index)
                        val details = if (r.getString("type") == "PAYMENT") r.getJSONObject("payment")
                                      else r.getJSONObject("changes")
                        dao.saveRequest(ApprovalRequest(
                            uid = r.getString("uid"), billUid = r.getString("billUid"),
                            type = r.getString("type"), payload = details.toString(),
                            note = r.optString("note"), status = r.getString("status"),
                            createdBy = r.getString("createdBy"), createdAt = r.getLong("createdAt"),
                            decidedBy = r.optString("decidedBy")
                        ))
                    }
                }
                response.isNull("rejected")
            } catch (_: Exception) {
                // Pending writes stay in Room and can be retried later.
                false
            }
        }
    }
}

class CurtainsSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = if (CloudSync(applicationContext).sync()) Result.success() else Result.retry()
}

object SyncScheduler {
    fun schedule(context: Context) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val worker = PeriodicWorkRequestBuilder<CurtainsSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sms_curtains_cloud_sync", ExistingPeriodicWorkPolicy.KEEP, worker
        )
    }
    fun soon(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "sms_curtains_immediate_sync", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<CurtainsSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        )
    }
}
