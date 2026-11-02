package lk.smscurtains.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val customer: String, val phone: String, val address: String,
    val dueDate: String, val paymentPreference: String,
    val windowFeet: Double, val doorFeet: Double, val grandFeet: Double,
    val fabricMetres: Double, val extraMetres: Double,
    val materialName: String, val materialRate: Double, val materialTotal: Double,
    val stripType: String, val stripRate: Double, val stripTotal: Double,
    val eyeletColor: String, val eyeletRate: Double, val eyeletCount: Int, val eyeletTotal: Double,
    val woodColor: String, val woodRate: Double, val woodTotal: Double,
    val bracketCount: Int, val bracketRate: Double, val bracketTotal: Double,
    val endcapCount: Int, val endcapRate: Double, val endcapTotal: Double,
    val sewingType: String, val sewingRate: Double, val sewingTotal: Double,
    val grandTotal: Double,
    val workStatus: String = "TO_START",
    @ColumnInfo(defaultValue = "''") val uid: String = UUID.randomUUID().toString()
)

@Entity(tableName = "measurements", foreignKeys = [ForeignKey(entity = Invoice::class, parentColumns = ["id"], childColumns = ["invoiceId"], onDelete = ForeignKey.CASCADE)], indices = [Index("invoiceId")])
data class StoredMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val kind: String,
    val type: String,
    val length: Double,
    val unit: String,
    val height: Double?,
    val heightUnit: String,
    val openings: Int,
    val frames: Int
)

@Entity(tableName = "payments", foreignKeys = [ForeignKey(entity = Invoice::class, parentColumns = ["id"], childColumns = ["invoiceId"], onDelete = ForeignKey.CASCADE)], indices = [Index("invoiceId")])
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val amount: Double,
    val method: String,
    val kind: String,
    val paidAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val uid: String = UUID.randomUUID().toString()
)

@Entity(tableName = "approval_requests")
data class ApprovalRequest(
    @PrimaryKey val uid: String = UUID.randomUUID().toString(),
    val billUid: String,
    val type: String, // PAYMENT / MODIFY
    val payload: String,
    val note: String = "",
    val status: String = "PENDING",
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val decidedBy: String = ""
)

@Entity(tableName = "sync_events")
data class SyncEvent(
    @PrimaryKey val eventId: String = UUID.randomUUID().toString(),
    val actor: String,
    val type: String,
    val billUid: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class InvoiceSummary(@Embedded val invoice: Invoice, val paidAmount: Double)

@Dao
interface ShopDao {
    @Query("SELECT invoices.*, COALESCE(SUM(payments.amount), 0) AS paidAmount FROM invoices LEFT JOIN payments ON invoices.id = payments.invoiceId GROUP BY invoices.id ORDER BY invoices.createdAt DESC, invoices.id DESC")
    fun watchSummaries(): Flow<List<InvoiceSummary>>
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC, id DESC")
    fun watchInvoices(): Flow<List<Invoice>>
    @Query("SELECT * FROM invoices") suspend fun allInvoices(): List<Invoice>
    @Query("SELECT * FROM invoices WHERE uid = ''") suspend fun legacyInvoices(): List<Invoice>
    @Query("SELECT * FROM invoices WHERE uid = :uid LIMIT 1") suspend fun findByUid(uid: String): Invoice?
    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId ORDER BY paidAt DESC, id DESC")
    fun watchPayments(invoiceId: Long): Flow<List<Payment>>
    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId") suspend fun paymentsFor(invoiceId: Long): List<Payment>
    @Query("SELECT * FROM measurements WHERE invoiceId = :invoiceId ORDER BY id")
    fun watchMeasurements(invoiceId: Long): Flow<List<StoredMeasurement>>
    @Query("SELECT * FROM measurements WHERE invoiceId = :invoiceId") suspend fun measurementsFor(invoiceId: Long): List<StoredMeasurement>
    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE invoiceId = :invoiceId") suspend fun paidTotal(invoiceId: Long): Double
    @Query("SELECT * FROM approval_requests ORDER BY createdAt DESC") fun watchRequests(): Flow<List<ApprovalRequest>>
    @Query("SELECT * FROM approval_requests ORDER BY createdAt DESC") suspend fun allRequests(): List<ApprovalRequest>
    @Query("SELECT * FROM sync_events WHERE actor = :actor ORDER BY createdAt ASC LIMIT 25") suspend fun eventsFor(actor: String): List<SyncEvent>
    @Query("SELECT COUNT(*) FROM sync_events") fun watchQueueSize(): Flow<Int>
    @Query("SELECT DISTINCT billUid FROM sync_events") suspend fun pendingBillUids(): List<String>
    @Query("DELETE FROM sync_events WHERE eventId = :id") suspend fun removeEvent(id: String)
    @Query("DELETE FROM payments WHERE invoiceId = :invoiceId") suspend fun clearPayments(invoiceId: Long)
    @Query("DELETE FROM measurements WHERE invoiceId = :invoiceId") suspend fun clearMeasurements(invoiceId: Long)
    @Insert suspend fun insertInvoice(invoice: Invoice): Long
    @Insert suspend fun insertMeasurements(rows: List<StoredMeasurement>)
    @Insert suspend fun insertPayment(payment: Payment)
    @Update suspend fun updatePayment(payment: Payment)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveRequest(request: ApprovalRequest)
    @Insert suspend fun enqueue(event: SyncEvent)
    @Update suspend fun updateInvoice(invoice: Invoice)
}

@Database(entities = [Invoice::class, StoredMeasurement::class, Payment::class, ApprovalRequest::class, SyncEvent::class], version = 2, exportSchema = false)
abstract class ShopDatabase : RoomDatabase() {
    abstract fun dao(): ShopDao
    companion object {
        @Volatile private var instance: ShopDatabase? = null
        private val migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE payments ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS `approval_requests` (`uid` TEXT NOT NULL, `billUid` TEXT NOT NULL, `type` TEXT NOT NULL, `payload` TEXT NOT NULL, `note` TEXT NOT NULL, `status` TEXT NOT NULL, `createdBy` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `decidedBy` TEXT NOT NULL, PRIMARY KEY(`uid`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `sync_events` (`eventId` TEXT NOT NULL, `actor` TEXT NOT NULL, `type` TEXT NOT NULL, `billUid` TEXT NOT NULL, `payload` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`eventId`))")
            }
        }
        fun get(context: Context): ShopDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ShopDatabase::class.java, "sms_curtains_offline.db")
                .addMigrations(migration).build().also { instance = it }
        }
    }
}
