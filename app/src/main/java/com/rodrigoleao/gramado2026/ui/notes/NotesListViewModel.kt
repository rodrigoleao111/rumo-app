package com.rodrigoleao.gramado2026.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.model.Note
import com.rodrigoleao.gramado2026.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lista de notas de um escopo. Usado na rota de **notas de um dia** — lê `tripId` e
 * `dayNumber` do SavedStateHandle. As notas gerais da viagem (aba do pager) são
 * geridas pelo TripViewModel, não aqui.
 */
@HiltViewModel
class NotesListViewModel @Inject constructor(
    private val repo: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle["tripId"])
    private val dayNumber: Int? = savedStateHandle.get<Int>("dayNumber")

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    init { load() }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch { _notes.value = repo.getNotes(tripId, dayNumber) }
    }

    /** Cria uma nota vazia e devolve o id para navegar ao editor. */
    fun createNote(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repo.createNote(tripId, dayNumber)
            load()
            onCreated(id)
        }
    }

    fun deleteNote(id: Long) {
        _notes.update { list -> list.filterNot { it.id == id } }
        viewModelScope.launch { repo.deleteNote(id) }
    }

    fun reorderNotes(ordered: List<Note>) {
        _notes.update { ordered }
        viewModelScope.launch { repo.reorderNotes(ordered.map { it.id }) }
    }
}
