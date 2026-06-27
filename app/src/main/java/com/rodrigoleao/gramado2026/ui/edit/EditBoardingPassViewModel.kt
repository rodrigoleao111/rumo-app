package com.rodrigoleao.gramado2026.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.db.entity.BoardingPassEntity
import com.rodrigoleao.gramado2026.data.repository.BoardingPassRepository
import com.rodrigoleao.gramado2026.data.model.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PassInputMode { LINK, FILE }

data class EditBoardingPassState(
    val entity: BoardingPassEntity? = null,
    val transportType: String = "FLIGHT",
    val origin: String = "",
    val originCity: String = "",
    val destination: String = "",
    val destinationCity: String = "",
    val flightNumber: String = "",
    val date: String = "",
    val boardingTime: String = "",
    val passenger: String = "",
    val inputMode: PassInputMode = PassInputMode.LINK,
    val walletUrl: String = "",
    val documentPath: String = "",
    val documentName: String = "",
    val notes: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
)

@HiltViewModel
class EditBoardingPassViewModel @Inject constructor(
    private val repo: BoardingPassRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle["tripId"])
    private val passId: Long = checkNotNull(savedStateHandle["passId"])

    private val _state = MutableStateFlow(EditBoardingPassState())
    val state: StateFlow<EditBoardingPassState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val isDirty: StateFlow<Boolean> = _state.map { s ->
        val e = s.entity ?: return@map s.origin.isNotBlank() || s.destination.isNotBlank() || s.passenger.isNotBlank()
        s.transportType != e.transportType ||
        s.origin != e.origin || s.destination != e.destination ||
        s.flightNumber != e.flightNumber || s.passenger != e.passenger ||
        s.date != e.date || s.boardingTime != e.boardingTime ||
        s.walletUrl != (e.walletUrl ?: "") || s.documentPath != (e.documentPath ?: "") ||
        s.notes != e.notes
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            if (passId == 0L) {
                _state.value = EditBoardingPassState(isLoading = false)
            } else {
                val e = repo.getBoardingPassEntity(passId)
                val hasFile = !e?.documentPath.isNullOrBlank()
                _state.value = EditBoardingPassState(
                    entity          = e,
                    transportType   = e?.transportType ?: "FLIGHT",
                    origin          = e?.origin ?: "",
                    originCity      = e?.originCity ?: "",
                    destination     = e?.destination ?: "",
                    destinationCity = e?.destinationCity ?: "",
                    flightNumber    = e?.flightNumber ?: "",
                    date            = e?.date ?: "",
                    boardingTime    = e?.boardingTime ?: "",
                    passenger       = e?.passenger ?: "",
                    inputMode       = if (hasFile) PassInputMode.FILE else PassInputMode.LINK,
                    walletUrl       = e?.walletUrl ?: "",
                    documentPath    = e?.documentPath ?: "",
                    documentName    = e?.documentName ?: "",
                    notes           = e?.notes ?: "",
                    isLoading       = false
                )
            }
        }
    }

    fun updateTransportType(v: String) {
        val s = _state.value
        val wasFlight = s.transportType == "FLIGHT"
        val isFlight  = v == "FLIGHT"
        _state.value = when {
            wasFlight && !isFlight -> s.copy(
                transportType = v,
                origin      = s.originCity.ifBlank { s.origin },
                originCity  = s.originCity.ifBlank { s.origin },
                destination      = s.destinationCity.ifBlank { s.destination },
                destinationCity  = s.destinationCity.ifBlank { s.destination }
            )
            !wasFlight && isFlight -> s.copy(
                transportType = v,
                origin      = "",
                destination = ""
            )
            else -> s.copy(transportType = v)
        }
    }

    fun updateOrigin(v: String)          { _state.value = _state.value.copy(origin = v.uppercase().take(3)) }
    fun updateOriginCity(v: String)      { _state.value = _state.value.copy(originCity = v) }
    fun updateDestination(v: String)     { _state.value = _state.value.copy(destination = v.uppercase().take(3)) }
    fun updateDestinationCity(v: String) { _state.value = _state.value.copy(destinationCity = v) }
    fun updateOriginSingle(v: String)      { _state.value = _state.value.copy(origin = v, originCity = v) }
    fun updateDestinationSingle(v: String) { _state.value = _state.value.copy(destination = v, destinationCity = v) }
    fun updateFlightNumber(v: String)    { _state.value = _state.value.copy(flightNumber = v) }
    fun updateDate(v: String)            { _state.value = _state.value.copy(date = v) }
    fun updateBoardingTime(v: String)    { _state.value = _state.value.copy(boardingTime = v) }
    fun updatePassenger(v: String)       { _state.value = _state.value.copy(passenger = v) }
    fun setInputMode(mode: PassInputMode){ _state.value = _state.value.copy(inputMode = mode) }
    fun updateWalletUrl(v: String)       { _state.value = _state.value.copy(walletUrl = v) }
    fun updateFile(path: String, name: String) { _state.value = _state.value.copy(documentPath = path, documentName = name) }
    fun clearFile()                      { _state.value = _state.value.copy(documentPath = "", documentName = "") }
    fun updateNotes(v: String)           { _state.value = _state.value.copy(notes = v) }

    fun save() {
        val s = _state.value
        if (s.origin.isBlank() || s.destination.isBlank() || s.passenger.isBlank()) return
        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            val entity = BoardingPassEntity(
                id              = passId,
                tripId          = tripId,
                transportType   = s.transportType,
                origin          = s.origin.trim(),
                originCity      = s.originCity.trim(),
                destination     = s.destination.trim(),
                destinationCity = s.destinationCity.trim(),
                flightNumber    = s.flightNumber.trim(),
                date            = s.date.trim(),
                boardingTime    = s.boardingTime.trim(),
                passenger       = s.passenger.trim(),
                walletUrl       = if (s.inputMode == PassInputMode.LINK) s.walletUrl.trim().ifEmpty { null } else null,
                documentPath    = if (s.inputMode == PassInputMode.FILE) s.documentPath.ifEmpty { null } else null,
                documentName    = if (s.inputMode == PassInputMode.FILE) s.documentName else "",
                notes           = s.notes.trim()
            )
            runCatching { repo.upsertBoardingPass(tripId, entity) }
                .onSuccess { _uiEvent.send(UiEvent.NavigateBack) }
                .onFailure { _state.value = _state.value.copy(isSaving = false); _uiEvent.send(UiEvent.ShowSnackbar("Erro ao salvar passagem")) }
        }
    }

    fun delete() {
        if (passId == 0L) return
        viewModelScope.launch {
            runCatching { repo.deleteBoardingPass(passId) }
                .onSuccess { _uiEvent.send(UiEvent.NavigateBack) }
                .onFailure { _uiEvent.send(UiEvent.ShowSnackbar("Erro ao excluir passagem")) }
        }
    }
}
