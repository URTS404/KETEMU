package org.umn.application_ketemu.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.umn.application_ketemu.domain.model.AppNotification
import org.umn.application_ketemu.domain.model.AppNotificationType
import org.umn.application_ketemu.domain.repository.NotificationRepository
import java.time.OffsetDateTime

/**
 * [NotificationRepository] di atas tabel `notifications` (db/0016).
 *
 * Pola sinkronisasinya sama dengan [SupabaseReportRepository]: cache di memori
 * yang di-refresh saat pertama dibaca dan setiap [REFRESH_INTERVAL_MS]. RLS
 * memastikan pengguna hanya menerima baris untuk dirinya.
 *
 * Pembagian tugas dengan server:
 *  - Konfirmasi unggahan barang temuan dan siaran "barang temuan baru" dibuat
 *    oleh trigger `reports_notify_found` supaya SEMUA pengguna mendapatkannya,
 *    bukan hanya pengguna yang kebetulan sedang membuka aplikasi.
 *  - Notifikasi lain (laporan hilang, perubahan status, keputusan klaim)
 *    dikirim dari perangkat karena hanya menyangkut satu penerima.
 */
class SupabaseNotificationRepository(
    private val client: SupabaseClient
) : NotificationRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = MutableStateFlow<List<AppNotification>>(emptyList())

    init {
        scope.launch {
            while (isActive) {
                runCatching { refresh() }
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    override fun observeNotifications(): Flow<List<AppNotification>> =
        cache.asStateFlow().onStart { runCatching { refresh() } }

    override suspend fun publish(notification: AppNotification) {
        // Dua jenis ini dibangkitkan trigger server; mengirim dari klien akan
        // menggandakan baris untuk penerima.
        if (notification.type == AppNotificationType.NEW_FOUND_ITEM ||
            notification.type == AppNotificationType.FOUND_REPORT_CREATED
        ) {
            return
        }
        // Siaran lintas pengguna hanya didukung pada mode demo in-memory.
        val recipientId = notification.recipientId ?: return

        runCatching {
            client.postgrest.from(TABLE).insert(
                NotificationInsert(
                    id = notification.id,
                    recipientId = recipientId,
                    actorId = notification.actorId,
                    type = notification.type.name,
                    title = notification.title,
                    message = notification.message,
                    reportId = notification.reportId,
                    isRead = notification.isRead,
                    createdAt = notification.createdAt.toIsoString()
                )
            )
            refresh()
        }.onFailure {
            android.util.Log.w(TAG, "gagal mengirim notifikasi: ${it.message}")
        }
    }

    override suspend fun markRead(notificationId: String) {
        runCatching {
            client.postgrest
                .from(TABLE)
                .update({ set("is_read", true) }) { filter { eq("id", notificationId) } }
            refresh()
        }.onFailure {
            android.util.Log.w(TAG, "gagal menandai notifikasi dibaca: ${it.message}")
        }
    }

    override suspend fun markAllRead(userId: String) {
        runCatching {
            client.postgrest
                .from(TABLE)
                .update({ set("is_read", true) }) {
                    filter {
                        eq("recipient_id", userId)
                        eq("is_read", false)
                    }
                }
            refresh()
        }.onFailure {
            android.util.Log.w(TAG, "gagal menandai semua notifikasi dibaca: ${it.message}")
        }
    }

    private suspend fun refresh() {
        cache.value = client.postgrest
            .from(TABLE)
            .select(columns = Columns.raw(SELECT_COLUMNS)) {
                order("created_at", Order.DESCENDING)
            }
            .decodeList<NotificationRow>()
            .mapNotNull { it.toDomain() }
    }

    private fun NotificationRow.toDomain(): AppNotification? {
        val parsedCreatedAt = createdAt.toEpochMillisOrNull() ?: return null
        return AppNotification(
            id = id,
            type = runCatching { AppNotificationType.valueOf(type) }
                .getOrDefault(AppNotificationType.REPORT_STATUS_CHANGED),
            title = title,
            message = message,
            createdAt = parsedCreatedAt,
            reportId = reportId,
            recipientId = recipientId,
            actorId = actorId,
            isRead = isRead
        )
    }

    private fun Long.toIsoString(): String = java.time.Instant.ofEpochMilli(this).toString()

    private companion object {
        const val TAG = "SupabaseNotification"
        const val TABLE = "notifications"
        const val REFRESH_INTERVAL_MS = 30_000L
        const val SELECT_COLUMNS =
            "id,recipient_id,actor_id,type,title,message,report_id,is_read,created_at"
    }
}

@Serializable
private data class NotificationRow(
    val id: String,
    @SerialName("recipient_id") val recipientId: String? = null,
    @SerialName("actor_id") val actorId: String? = null,
    val type: String,
    val title: String,
    val message: String,
    @SerialName("report_id") val reportId: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
private data class NotificationInsert(
    val id: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("actor_id") val actorId: String?,
    val type: String,
    val title: String,
    val message: String,
    @SerialName("report_id") val reportId: String?,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("created_at") val createdAt: String
)

private fun String?.toEpochMillisOrNull(): Long? =
    this?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }
