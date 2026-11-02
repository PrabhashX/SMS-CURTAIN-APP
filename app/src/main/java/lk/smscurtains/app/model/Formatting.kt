package lk.smscurtains.app.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun money(amount: Double): String = "Rs. " + String.format(Locale.US, "%,.2f", CurtainMath.round2(amount))
fun qty(amount: Double): String = String.format(Locale.US, "%.2f", CurtainMath.round2(amount)).trimEnd('0').trimEnd('.')
fun date(time: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(time))
fun dateTime(time: Long): String = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date(time))

// The same bill number on every phone: local Room IDs differ between devices.
fun billNumber(uid: String, localId: Long): String = if (uid.isNotBlank()) uid.replace("-", "").take(12).uppercase(Locale.US) else localId.toString()
