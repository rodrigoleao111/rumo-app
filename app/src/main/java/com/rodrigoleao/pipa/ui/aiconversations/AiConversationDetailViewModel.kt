package com.rodrigoleao.pipa.ui.aiconversations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.pipa.data.model.AiConversation
import com.rodrigoleao.pipa.data.repository.AiConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiConversationDetailViewModel @Inject constructor(
    private val repo: AiConversationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: Long = savedStateHandle["conversationId"] ?: 0L

    private val _conversation = MutableStateFlow<AiConversation?>(null)
    val conversation: StateFlow<AiConversation?> = _conversation.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        viewModelScope.launch {
            _conversation.value = repo.getById(conversationId)
            _loaded.value = true
        }
    }

    /** Exclui a conversa e chama [onDeleted] (na main) para navegar de volta. */
    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repo.delete(conversationId)
            onDeleted()
        }
    }
}
