package org.umn.application_ketemu.ui.feature.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.umn.application_ketemu.domain.model.ChatMessage
import org.umn.application_ketemu.domain.model.ChatThread
import org.umn.application_ketemu.domain.model.TemplateReply
import org.umn.application_ketemu.domain.repository.ChatRepository

/**
 * ViewModel layar Percakapan (db/0019).
 *
 * Tidak ada lagi balasan simulasi: pesan lawan bicara datang lewat Supabase
 * Realtime selama layar ini terbuka, jadi yang tampil adalah percakapan
 * sungguhan antara dua akun.
 */
class ConversationViewModel(
    private val chatRepository: ChatRepository,
    private val conversationId: String
) : ViewModel() {

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    /** Pesan pada percakapan ini, terlama → terbaru. */
    val messages: StateFlow<List<ChatMessage>> = chatRepository
        .observeMessages(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Info lawan bicara + barang yang dibicarakan (null selagi dimuat). */
    val thread: StateFlow<ChatThread?> = chatRepository
        .observeThreads()
        .map { threads -> threads.firstOrNull { it.id == conversationId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Balasan cepat untuk petugas (opsi 5C). */
    val templates: List<TemplateReply> = chatRepository.templates()

    val canSend: StateFlow<Boolean> = draft
        .map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onDraftChange(value: String) {
        _draft.value = value
    }

    fun onSend(isTemplate: Boolean = false) {
        val text = _draft.value.trim()
        if (text.isEmpty() || _isSending.value) return

        _draft.value = ""
        _isSending.value = true
        viewModelScope.launch {
            chatRepository.send(conversationId, text, isTemplate)
            _isSending.value = false
        }
    }

    /** Kirim gambar yang sudah dikompres (dipanggil dari tombol lampiran). */
    fun onImagePicked(bytes: ByteArray, width: Int, height: Int) {
        android.util.Log.d("Chat", "onImagePicked: sedangKirim=" + _isSending.value + ", percakapan=" + conversationId)
        if (_isSending.value) {
            android.util.Log.w("Chat", "onImagePicked diabaikan: masih ada pengiriman lain")
            return
        }
        _isSending.value = true
        viewModelScope.launch {
            android.util.Log.d("Chat", "onImagePicked: memanggil sendImage")
            chatRepository.sendImage(
                conversationId = conversationId,
                bytes = bytes,
                width = width,
                height = height,
                caption = _draft.value.trim()
            )
            _draft.value = ""
            _isSending.value = false
        }
    }

    /** Kirim ulang satu pesan yang tertahan (ikon putar ulang di gelembung). */
    fun onRetry(messageId: String) {
        viewModelScope.launch { chatRepository.retry(messageId) }
    }

    /** Hapus pesan (dipanggil dari dialog tekan-lama). */
    fun onDelete(messageId: String) {
        android.util.Log.d("Chat", "onDelete dipanggil: " + messageId)
        viewModelScope.launch {
            val hasil = chatRepository.deleteMessage(messageId)
            android.util.Log.d("Chat", "hasil hapus " + messageId + " = " + hasil)
        }
    }

    /** Tombol Muat ulang: jaring pengaman bila Realtime terputus. */
    fun onRefresh() {
        viewModelScope.launch { chatRepository.refresh() }
    }
}
