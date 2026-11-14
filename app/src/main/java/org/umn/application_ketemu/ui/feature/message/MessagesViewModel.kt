package org.umn.application_ketemu.ui.feature.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.umn.application_ketemu.core.common.UiState
import org.umn.application_ketemu.domain.model.ConversationThread
import org.umn.application_ketemu.domain.repository.ConversationRepository

/** ViewModel daftar percakapan (layar Pesan). */
class MessagesViewModel(
    conversationRepository: ConversationRepository
) : ViewModel() {

    val uiState: StateFlow<UiState<List<ConversationThread>>> =
        conversationRepository.observeThreads()
            .map { threads ->
                if (threads.isEmpty()) UiState.Empty else UiState.Success(threads)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
