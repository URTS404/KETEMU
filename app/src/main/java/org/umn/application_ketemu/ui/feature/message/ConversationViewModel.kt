package org.umn.application_ketemu.ui.feature.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.umn.application_ketemu.domain.model.ChatMessage
import org.umn.application_ketemu.domain.model.ConversationThread
import org.umn.application_ketemu.domain.repository.ConversationRepository

/**
 * ViewModel layar Percakapan.
 *
 * Setelah pengguna mengirim pesan, pihak lain MENIRU balasan beberapa saat
 * kemudian (lengkap dengan indikator "sedang menulis") supaya alur percakapan
 * terasa hidup saat didemokan. Pada versi final balasan datang dari Supabase
 * Realtime, jadi bagian simulasi ini cukup dihapus.
 */
class ConversationViewModel(
    private val conversationRepository: ConversationRepository,
    private val threadId: String
) : ViewModel() {

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping: StateFlow<Boolean> = _isPartnerTyping.asStateFlow()

    /** Pesan pada percakapan ini, terlama -> terbaru. */
    val messages: StateFlow<List<ChatMessage>> = conversationRepository
        .observeMessages(threadId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Info lawan bicara + barang yang dibicarakan (null selagi dimuat). */
    val thread: StateFlow<ConversationThread?> = conversationRepository
        .observeThreads()
        .map { threads -> threads.firstOrNull { it.id == threadId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Tombol kirim aktif hanya bila ada teks. */
    val canSend: StateFlow<Boolean> = draft
        .map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        // Membuka percakapan berarti pesannya sudah dibaca.
        viewModelScope.launch { conversationRepository.markRead(threadId) }
    }

    fun onDraftChange(value: String) {
        _draft.value = value
    }

    fun onSend() {
        val text = _draft.value.trim()
        if (text.isEmpty()) return
        _draft.value = ""

        viewModelScope.launch {
            conversationRepository.addMessage(threadId, text, fromMe = true)

            // Simulasi balasan pihak lain (hanya untuk demo).
            delay(AUTO_REPLY_DELAY_MS)
            _isPartnerTyping.value = true
            delay(TYPING_MS)
            _isPartnerTyping.value = false
            conversationRepository.addMessage(threadId, AUTO_REPLY, fromMe = false)
        }
    }

    companion object {
        private const val AUTO_REPLY_DELAY_MS = 900L
        private const val TYPING_MS = 1400L
        private const val AUTO_REPLY = "Baik, saya catat ya. Nanti saya kabari lagi."
    }
}
