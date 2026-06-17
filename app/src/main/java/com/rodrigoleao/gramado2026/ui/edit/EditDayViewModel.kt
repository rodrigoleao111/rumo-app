package com.rodrigoleao.gramado2026.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.db.entity.TravelDayEntity
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditDayState(
    val entity: TravelDayEntity? = null,
    val title: String = "",
    val dayAlert: String = "",
    val dayLinkUrl: String = "",
    val dayLinkLabel: String = "",
    val dayDocumentPath: String = "",
    val dayDocumentName: String = "",
    val dayDocumentTitle: String = "",
    val isSaving: Boolean = false
)

class EditDayViewModel(
    private val repo: TripRepository,
    private val tripId: Long,
    private val dayNumber: Int
) : ViewModel() {

    private val _state = MutableStateFlow(EditDayState())
    val state: StateFlow<EditDayState> = _state.asStateFlow()

    val isDirty: StateFlow<Boolean> = _state.map { s ->
        val e = s.entity ?: return@map false
        s.title != e.title || s.dayAlert != (e.dayAlert ?: "") ||
        s.dayLinkUrl != (e.dayLinkUrl ?: "") || s.dayLinkLabel != e.dayLinkLabel ||
        s.dayDocumentPath != (e.dayDocumentPath ?: "") || s.dayDocumentName != e.dayDocumentName ||
        s.dayDocumentTitle != e.dayDocumentTitle
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            val e = repo.getDayEntity(tripId, dayNumber) ?: return@launch
            _state.value = EditDayState(
                entity          = e,
                title           = e.title,
                dayAlert        = e.dayAlert ?: "",
                dayLinkUrl      = e.dayLinkUrl ?: "",
                dayLinkLabel    = e.dayLinkLabel,
                dayDocumentPath  = e.dayDocumentPath ?: "",
                dayDocumentName  = e.dayDocumentName,
                dayDocumentTitle = e.dayDocumentTitle
            )
        }
    }

    fun updateTitle(v: String)          { _state.value = _state.value.copy(title = v) }
    fun updateDayAlert(v: String)       { _state.value = _state.value.copy(dayAlert = v) }
    fun updateDayLinkUrl(v: String)     { _state.value = _state.value.copy(dayLinkUrl = v) }
    fun updateDayLinkLabel(v: String)   { _state.value = _state.value.copy(dayLinkLabel = v) }
    fun updateDocument(path: String, name: String) {
        _state.value = _state.value.copy(dayDocumentPath = path, dayDocumentName = name)
    }
    fun updateDocumentTitle(v: String) { _state.value = _state.value.copy(dayDocumentTitle = v) }
    fun clearDocument() {
        _state.value = _state.value.copy(dayDocumentPath = "", dayDocumentName = "", dayDocumentTitle = "")
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val e = s.entity ?: return
        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            repo.updateDay(e.copy(
                title           = s.title.trim(),
                dayAlert        = s.dayAlert.trim().ifEmpty { null },
                dayLinkUrl      = s.dayLinkUrl.trim().ifEmpty { null },
                dayLinkLabel    = s.dayLinkLabel.trim(),
                dayDocumentPath  = s.dayDocumentPath.trim().ifEmpty { null },
                dayDocumentName  = s.dayDocumentName.trim(),
                dayDocumentTitle = s.dayDocumentTitle.trim()
            ))
            onDone()
        }
    }

    companion object {
        fun Factory(repo: TripRepository, tripId: Long, dayNumber: Int) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EditDayViewModel(repo, tripId, dayNumber) as T
        }
    }
}
