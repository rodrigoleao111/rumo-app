package com.rodrigoleao.gramado2026.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.db.entity.BoardingPassEntity
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditBoardingPassState(
    val entity: BoardingPassEntity? = null,
    val origin: String = "",
    val originCity: String = "",
    val destination: String = "",
    val destinationCity: String = "",
    val flightNumber: String = "",
    val date: String = "",
    val boardingTime: String = "",
    val passenger: String = "",
    val walletUrl: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
)

class EditBoardingPassViewModel(
    private val repo: TripRepository,
    private val tripId: Long,
    private val passId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(EditBoardingPassState())
    val state: StateFlow<EditBoardingPassState> = _state.asStateFlow()

    val isDirty: StateFlow<Boolean> = _state.map { s ->
        val e = s.entity ?: return@map false
        s.origin != e.origin || s.destination != e.destination || s.flightNumber != e.flightNumber ||
        s.passenger != e.passenger || s.date != e.date || s.boardingTime != e.boardingTime
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            if (passId == 0L) {
                _state.value = EditBoardingPassState(isLoading = false)
            } else {
                val e = repo.getBoardingPassEntity(passId)
                _state.value = EditBoardingPassState(
                    entity          = e,
                    origin          = e?.origin ?: "",
                    originCity      = e?.originCity ?: "",
                    destination     = e?.destination ?: "",
                    destinationCity = e?.destinationCity ?: "",
                    flightNumber    = e?.flightNumber ?: "",
                    date            = e?.date ?: "",
                    boardingTime    = e?.boardingTime ?: "",
                    passenger       = e?.passenger ?: "",
                    walletUrl       = e?.walletUrl ?: "",
                    isLoading       = false
                )
            }
        }
    }

    fun updateOrigin(v: String)          { _state.value = _state.value.copy(origin = v.uppercase().take(3)) }
    fun updateOriginCity(v: String)      { _state.value = _state.value.copy(originCity = v) }
    fun updateDestination(v: String)     { _state.value = _state.value.copy(destination = v.uppercase().take(3)) }
    fun updateDestinationCity(v: String) { _state.value = _state.value.copy(destinationCity = v) }
    fun updateFlightNumber(v: String)    { _state.value = _state.value.copy(flightNumber = v) }
    fun updateDate(v: String)            { _state.value = _state.value.copy(date = v) }
    fun updateBoardingTime(v: String)    { _state.value = _state.value.copy(boardingTime = v) }
    fun updatePassenger(v: String)       { _state.value = _state.value.copy(passenger = v) }
    fun updateWalletUrl(v: String)       { _state.value = _state.value.copy(walletUrl = v) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.origin.isBlank() || s.destination.isBlank() || s.passenger.isBlank()) return
        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            val entity = BoardingPassEntity(
                id              = passId,
                tripId          = tripId,
                origin          = s.origin.trim(),
                originCity      = s.originCity.trim(),
                destination     = s.destination.trim(),
                destinationCity = s.destinationCity.trim(),
                flightNumber    = s.flightNumber.trim(),
                date            = s.date.trim(),
                boardingTime    = s.boardingTime.trim(),
                passenger       = s.passenger.trim(),
                walletUrl       = s.walletUrl.trim().ifEmpty { null }
            )
            repo.upsertBoardingPass(tripId, entity)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (passId == 0L) return
        viewModelScope.launch { repo.deleteBoardingPass(passId); onDone() }
    }

    companion object {
        fun Factory(repo: TripRepository, tripId: Long, passId: Long) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EditBoardingPassViewModel(repo, tripId, passId) as T
            }
    }
}
