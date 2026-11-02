package lk.smscurtains.app.print

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lk.smscurtains.app.data.Invoice
import lk.smscurtains.app.data.Payment
import lk.smscurtains.app.model.billNumber
import lk.smscurtains.app.model.date
import lk.smscurtains.app.model.qty
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil

object ReceiptText {
    private fun fmt(amount: Double) = String.format(Locale.US, "%,.2f", amount)
    private fun wrap(value: String, width: Int): List<String> {
        val out = mutableListOf<String>()
        var text = value.trim()
        while (text.length > width) {
            var split = text.lastIndexOf(' ', width)
            if (split < 1) split = width
            out += text.substring(0, split).trim()
            text = text.substring(split).trim()
        }
        if (text.isNotBlank()) out += text
        return out
    }
    private fun amount(label: String, quantity: String, rate: Double, total: Double, width: Int): List<String> {
        return wrap(label, width) + listOf(
            "  $quantity x ${fmt(rate)}",
            ("Rs. " + fmt(total)).padStart(width.coerceAtLeast(18))
        )
    }
    fun lines(i: Invoice, payments: List<Payment>, columns: Int): List<String> {
        val paid = payments.sumOf { it.amount }
        val balance = (i.grandTotal - paid).coerceAtLeast(0.0)
        val width = columns
        val result = mutableListOf<String>()
        fun add(text: String) { result.addAll(wrap(text, width)) }
        result += "SMS CURTAINS"
        result += "MAWATHAGAMA"
        result += "SAMODAYA RD"
        result += "077 297 3797"
        result += "-".repeat(width)
        add("BILL #${billNumber(i.uid, i.id)}   DATE: ${date(i.createdAt)}")
        add("CUSTOMER: ${i.customer}")
        if (i.phone.isNotBlank()) add("PHONE: ${i.phone}")
        if (i.address.isNotBlank()) add("ADDRESS: ${i.address}")
        if (i.dueDate.isNotBlank()) add("COMPLETION: ${i.dueDate}")
        add("PAYMENT TYPE: ${i.paymentPreference}")
        result += "-".repeat(width)
        result += "ITEM / QTY x RATE / AMOUNT"
        result += "-".repeat(width)
        result += amount("1. FABRIC - ${i.materialName}", "${qty(i.fabricMetres)}m", i.materialRate, i.materialTotal, width)
        result += amount("2. FUSION - ${i.stripType}", "${qty(i.fabricMetres)}m", i.stripRate, i.stripTotal, width)
        result += amount("3. EYELETS - ${i.eyeletColor}", "${i.eyeletCount}pcs", i.eyeletRate, i.eyeletTotal, width)
        result += amount("4. WOOD BAR - ${i.woodColor}", "${qty(i.grandFeet)}ft", i.woodRate, i.woodTotal, width)
        result += amount("5a. BRACKETS", "${i.bracketCount}pcs", i.bracketRate, i.bracketTotal, width)
        result += amount("5b. END CAPS", "${i.endcapCount}pcs", i.endcapRate, i.endcapTotal, width)
        result += amount("6. SEWING - ${i.sewingType}", "${qty(i.fabricMetres)}m", i.sewingRate, i.sewingTotal, width)
        result += "=".repeat(width)
        add("GRAND TOTAL: Rs. ${fmt(i.grandTotal)}")
        add("PAID TO DATE: Rs. ${fmt(paid)}")
        add("BALANCE: Rs. ${fmt(balance)}")
        if (payments.isNotEmpty()) {
            result += "-".repeat(width)
            result += "PAYMENT HISTORY"
            payments.sortedBy { it.paidAt }.forEach { p ->
                add("${date(p.paidAt)} ${p.kind} ${p.method}")
                add("  +Rs. ${fmt(p.amount)}")
            }
        }
        result += "-".repeat(width)
        result += "THANK YOU!"
        return result
    }
}

data class PairedPrinter(val name: String, val address: String)

object BluetoothReceiptPrinter {
    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    @SuppressLint("MissingPermission")
    fun paired(): List<PairedPrinter> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return adapter.bondedDevices.orEmpty()
            .map { PairedPrinter(it.name ?: "Bluetooth printer", it.address) }
            .sortedBy { it.name }
    }

    @SuppressLint("MissingPermission")
    suspend fun print(address: String, paperMm: Int, invoice: Invoice, payments: List<Payment>): Unit = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: error("Bluetooth is not available")
        if (!adapter.isEnabled) error("Enable Bluetooth first")
        val device: BluetoothDevice = adapter.getRemoteDevice(address)
        val socket = device.createRfcommSocketToServiceRecord(sppUuid)
        try {
            socket.connect()
            socket.outputStream.use { out ->
                out.write(byteArrayOf(0x1b, 0x40)) // ESC @ initialize
                val lines = ReceiptText.lines(invoice, payments, if (paperMm == 58) 32 else 48)
                lines.forEachIndexed { index, line ->
                    out.write(byteArrayOf(0x1b, 0x61, (if (index < 4) 1 else 0).toByte()))
                    out.write((line + "\n").toByteArray(Charsets.US_ASCII))
                }
                out.write("\n\n\n".toByteArray(Charsets.US_ASCII))
                out.flush()
            }
        } finally { try { socket.close() } catch (_: Exception) {} }
    }
}

/** Android Print Services / Print to PDF fallback. Bluetooth ESC/POS does not require a print service. */
object SystemReceiptPrinter {
    fun print(context: Context, invoice: Invoice, payments: List<Payment>, paperMm: Int) {
        val columns = if (paperMm == 58) 32 else 48
        val lines = ReceiptText.lines(invoice, payments, columns)
        val widthPt = if (paperMm == 58) 164 else 227
        val fontSize = if (paperMm == 58) 7.7f else 8.2f
        val lineHeight = fontSize * 1.35f
        val heightPt = maxOf(250, ceil(lines.size * lineHeight + 25).toInt())
        val heightMils = ceil(heightPt * (1000.0 / 72.0)).toInt()
        // Precise 58/80-mm widths in mils (1 inch = 25.4 mm).
        val media = PrintAttributes.MediaSize("SMS_${paperMm}_$heightMils", "${paperMm}mm roll", (paperMm * 1000.0 / 25.4).toInt(), heightMils)
        val attributes = PrintAttributes.Builder()
            .setMediaSize(media).setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .setResolution(PrintAttributes.Resolution("roll", "Receipt", 203, 203))
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME).build()
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal?, callback: LayoutResultCallback, extras: android.os.Bundle?) {
                callback.onLayoutFinished(PrintDocumentInfo.Builder("SMS_Bill_${billNumber(invoice.uid, invoice.id)}.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1).build(), true)
            }
            override fun onWrite(pages: Array<out android.print.PageRange>?, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal?, callback: WriteResultCallback) {
                val document = PdfDocument()
                try {
                    val pageInfo = PdfDocument.PageInfo.Builder(widthPt, heightPt, 1).create()
                    val page = document.startPage(pageInfo)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = fontSize; typeface = Typeface.MONOSPACE; color = android.graphics.Color.BLACK }
                    // Adapt horizontal scale to the narrow roll without cutting a column.
                    val maxWidth = lines.maxOfOrNull { paint.measureText(it) } ?: 1f
                    if (maxWidth > widthPt - 12f) paint.textScaleX = (widthPt - 12f) / maxWidth
                    lines.forEachIndexed { idx, line ->
                        val x = if (idx < 4) (widthPt - paint.measureText(line)) / 2 else 6f
                        page.canvas.drawText(line, x, 13f + idx * lineHeight, paint)
                    }
                    document.finishPage(page)
                    FileOutputStream(destination.fileDescriptor).use { document.writeTo(it) }
                    callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) { callback.onWriteFailed(e.message) }
                finally { document.close() }
            }
        }
        (context.getSystemService(Context.PRINT_SERVICE) as PrintManager).print("SMS Curtains #${invoice.id}", adapter, attributes)
    }
}
