package com.rodrigoleao.pipa.ui.aiconversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.pipa.data.model.AiConversation
import com.rodrigoleao.pipa.data.repository.AiConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiConversationsViewModel @Inject constructor(
    private val repo: AiConversationRepository
) : ViewModel() {

    /** `null` enquanto carrega; lista (possivelmente vazia) quando pronto. */
    val conversations: StateFlow<List<AiConversation>?> =
        repo.conversations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }
}
