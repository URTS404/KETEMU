package org.umn.application_ketemu.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.umn.application_ketemu.domain.model.ChatMessage
import org.umn.application_ketemu.domain.model.ConversationThread
import org.umn.application_ketemu.domain.repository.ConversationRepository
import java.util.UUID

/**
 * Penerapan sementara di memori (memakai data contoh).
 *
 * Polanya sama seperti [InMemoryReportRepository]: seluruh pembacaan mengalir
 * lewat StateFlow sehingga UI otomatis berubah saat ada pesan baru - persis
 * perilaku Realtime Supabase pada versi final.
 */
class InMemoryConversationRepository(
    initialThreads: List<ConversationThread>,
    initialMessages: List<ChatMessage>
) : ConversationRepository {

    private val threads = MutableStateFlow(initialThreads)
    private val messages = MutableStateFlow(initialMessages)

    override fun observeThreads(): Flow<List<ConversationThread>> =
        threads.map { list -> list.sortedByDescending { it.lastMessageAt } }

    override fun observeMessages(threadId: String): Flow<List<ChatMessage>> =
        messages.map { list ->
            list.filter { it.threadId == threadId }.sortedBy { it.sentAt }
        }

    override suspend fun addMessage(threadId: String, text: String, fromMe: Boolean) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val now = System.currentTimeMillis()

        messages.value = messages.value + ChatMessage(
            id = UUID.randomUUID().toString(),
            threadId = threadId,
            text = trimmed,
            fromMe = fromMe,
            sentAt = now
        )

        threads.value = threads.value.map { thread ->
            if (thread.id == threadId) {
                thread.copy(
                    lastMessage = trimmed,
                    lastMessageAt = now,
                    unreadCount = if (fromMe) 0 else thread.unreadCount + 1
                )
            } else {
                thread
            }
        }
    }

    override suspend fun markRead(threadId: String) {
        threads.value = threads.value.map { thread ->
            if (thread.id == threadId) thread.copy(unreadCount = 0) else thread
        }
    }
}
