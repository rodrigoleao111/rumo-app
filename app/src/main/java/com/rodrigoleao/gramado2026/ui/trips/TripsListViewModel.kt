package com.rodrigoleao.gramado2026.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.db.entity.TripEntity
import com.rodrigoleao.gramado2026.data.model.UiEvent
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripsListViewModel @Inject constructor(
    private val repo: TripRepository
) : ViewModel() {

    // null = ainda carregando; emptyList = carregado mas sem viagens
    val trips: StateFlow<List<TripEntity>?> = repo.allTrips
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun deleteTrip(trip: TripEntity) {
        viewModelScope.launch {
            runCatching { repo.deleteTrip(trip) }
                .onFailure { _uiEvent.send(UiEvent.ShowSnackbar("Erro ao excluir viagem")) }
        }
    }

}
