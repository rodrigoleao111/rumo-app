package com.rodrigoleao.gramado2026.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.model.NoteBlock
import com.rodrigoleao.gramado2026.data.model.NoteBlockType
import com.rodrigoleao.gramado2026.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteEditorState(
    val isLoading: Boolean = true,
    val title: String = "",
    val blocks: List<NoteBlock> = emptyList(),
    // Sinalizadores de foco automático após inserção (consumidos pela UI). Bloco e item
    // vivem em tabelas distintas, então os ids são rastreados separadamente.
    val focusBlockId: Long? = null,
    val focusItemId: Long? = null
)

/**
 * Editor de uma nota (F4). O estado em memória é a fonte de verdade da UI (edição de
 * texto fluida, sem recarregar a cada tecla); a persistência acontece em background.
 * Só recarrega do banco quando a estrutura muda e é preciso conhecer o id gerado
 * (adicionar bloco/item) — nesses casos emite um sinal de foco para o novo elemento.
 */
@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val repo: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: Long = checkNotNull(savedStateHandle["noteId"])

    private val _state = MutableStateFlow(NoteEditorState())
    val state: StateFlow<NoteEditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val note = repo.getNote(noteId)
            _state.update {
                it.copy(isLoading = false, title = note?.title ?: "", blocks = note?.blocks ?: emptyList())
            }
        }
    }

    // ── Título ──────────────────────────────────────────────────────────────
    fun updateTitle(title: String) {
        _state.update { it.copy(title = title) }
        viewModelScope.launch { repo.updateNoteTitle(noteId, title) }
    }

    // ── Blocos ──────────────────────────────────────────────────────────────
    fun addBlock(type: NoteBlockType) {
        viewModelScope.launch {
            val blockId = repo.addBlock(noteId, type)
            val blocks  = repo.getNote(noteId)?.blocks ?: emptyList()
            val focusItem = if (type == NoteBlockType.CHECKLIST)
                (blocks.find { it.id == blockId } as? NoteBlock.ChecklistBlock)?.items?.firstOrNull()?.id
            else null
            _state.update {
                it.copy(
                    blocks = blocks,
                    focusBlockId = if (type != NoteBlockType.CHECKLIST) blockId else null,
                    focusItemId = focusItem
                )
            }
        }
    }

    fun updateBlockContent(blockId: Long, content: String) {
        _state.update { s ->
            s.copy(blocks = s.blocks.map { b ->
                when (b) {
                    is NoteBlock.TextBlock    -> if (b.id == blockId) b.copy(content = content) else b
                    is NoteBlock.HeadingBlock -> if (b.id == blockId) b.copy(content = content) else b
                    else -> b
                }
            })
        }
        viewModelScope.launch { repo.updateBlockContent(blockId, content); repo.touchNote(noteId) }
    }

    fun deleteBlock(blockId: Long) {
        _state.update { s -> s.copy(blocks = s.blocks.filter { it.id != blockId }) }
        viewModelScope.launch { repo.deleteBlock(blockId); repo.touchNote(noteId) }
    }

    fun reorderBlocks(orderedIds: List<Long>) {
        _state.update { s -> s.copy(blocks = orderedIds.mapNotNull { id -> s.blocks.find { it.id == id } }) }
        viewModelScope.launch { repo.reorderBlocks(orderedIds); repo.touchNote(noteId) }
    }

    // ── Itens de checklist ──────────────────────────────────────────────────
    fun addChecklistItem(blockId: Long) {
        viewModelScope.launch {
            val itemId = repo.addChecklistItem(blockId)
            repo.touchNote(noteId)
            _state.update { it.copy(blocks = repo.getNote(noteId)?.blocks ?: emptyList(), focusItemId = itemId) }
        }
    }

    fun updateItemText(blockId: Long, itemId: Long, text: String) {
        _state.update { s -> s.copy(blocks = s.blocks.mapItem(blockId, itemId) { it.copy(text = text) }) }
        viewModelScope.launch { repo.updateItemText(itemId, text); repo.touchNote(noteId) }
    }

    fun toggleChecklistItem(blockId: Long, itemId: Long, checked: Boolean) {
        _state.update { s -> s.copy(blocks = s.blocks.mapItem(blockId, itemId) { it.copy(isChecked = checked) }) }
        viewModelScope.launch { repo.toggleChecklistItem(itemId, checked); repo.touchNote(noteId) }
    }

    fun deleteChecklistItem(blockId: Long, itemId: Long) {
        _state.update { s ->
            s.copy(blocks = s.blocks.map { b ->
                if (b is NoteBlock.ChecklistBlock && b.id == blockId)
                    b.copy(items = b.items.filter { it.id != itemId })
                else b
            })
        }
        viewModelScope.launch { repo.deleteChecklistItem(itemId); repo.touchNote(noteId) }
    }

    fun consumeFocus() {
        if (_state.value.focusBlockId != null || _state.value.focusItemId != null) {
            _state.update { it.copy(focusBlockId = null, focusItemId = null) }
        }
    }
}

// Aplica uma transformação a um item específico dentro de um ChecklistBlock específico.
private inline fun List<NoteBlock>.mapItem(
    blockId: Long,
    itemId: Long,
    crossinline transform: (com.rodrigoleao.gramado2026.data.model.ChecklistItem) -> com.rodrigoleao.gramado2026.data.model.ChecklistItem
): List<NoteBlock> = map { b ->
    if (b is NoteBlock.ChecklistBlock && b.id == blockId)
        b.copy(items = b.items.map { if (it.id == itemId) transform(it) else it })
    else b
}
