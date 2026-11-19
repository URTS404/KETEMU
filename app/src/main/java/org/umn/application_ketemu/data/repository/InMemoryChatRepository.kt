package org.umn.application_ketemu.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.umn.application_ketemu.domain.model.ChatMessage
import org.umn.application_ketemu.domain.model.ChatThread
import org.umn.application_ketemu.domain.model.TemplateReply
import org.umn.application_ketemu.domain.repository.ChatRepository
import java.util.UUID

/**
 * Versi tanpa jaringan dari [ChatRepository].
 *
 * Dipakai ketika `local.properties` belum berisi Supabase, supaya layar Pesan
 * tetap bisa diperagakan di kelas tanpa koneksi. Semua data hilang saat proses
 * aplikasi berakhir — itu memang tujuannya.
 */
class InMemoryChatRepository(
    initial: List<ChatThread> = emptyList()
) : ChatRepository {

    private val threads = MutableStateFlow(initial)
    private val messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    private var counter = initial.size

    override fun observeThreads(): Flow<List<ChatThread>> = threads.asStateFlow()

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        messages.asStateFlow().map { it[conversationId].orEmpty() }

    override suspend fun startConversation(reportId: String): String {
        threads.value.firstOrNull { it.reportId == reportId }?.let { return it.id }
        val id = "local-chat-${++counter}"
        threads.value = threads.value + ChatThread(
            id = id,
            reportId = reportId,
            reportTitle = "Laporan $reportId",
            reportType = "FOUND",
            partnerName = "Pengguna lain",
            partnerRole = "USER",
            lastMessage = "",
            lastMessageAt = System.currentTimeMillis()
        )
        return id
    }

    override suspend fun send(conversationId: String, body: String, isTemplate: Boolean): Boolean {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            threadId = conversationId,
            text = body.trim(),
            fromMe = true,
            sentAt = System.currentTimeMillis()
        )
        messages.value = messages.value + (conversationId to (messages.value[conversationId].orEmpty() + message))
        threads.value = threads.value.map {
            if (it.id == conversationId) it.copy(lastMessage = message.text, lastMessageAt = message.sentAt) else it
        }
        return true
    }

    override suspend fun refresh() = Unit

    override fun templates(): List<TemplateReply> = listOf(
        TemplateReply("Barang ada di Pos", "Barangnya ada di Pos Basement, silakan datang dengan membawa KTM."),
        TemplateReply("Sedang diperiksa", "Terima kasih informasinya, barang sedang kami periksa di Pos.")
    )
}
