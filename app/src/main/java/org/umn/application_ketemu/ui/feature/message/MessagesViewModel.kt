package org.umn.application_ketemu.ui.feature.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.umn.application_ketemu.core.common.UiState
import org.umn.application_ketemu.domain.model.ChatThread
import org.umn.application_ketemu.domain.repository.ChatRepository

/** ViewModel daftar percakapan (layar Pesan) — membaca `conversations` (0019). */
class MessagesViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<UiState<List<ChatThread>>> =
        chatRepository.observeThreads()
            .map { threads ->
                if (threads.isEmpty()) UiState.Empty else UiState.Success(threads)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /** Muat ulang manual (tanpa polling). */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun onRefresh() {
        _isRefreshing.value = true
        viewModelScope.launch { try { chatRepository.refresh() } finally { _isRefreshing.value = false } }
    }
}
