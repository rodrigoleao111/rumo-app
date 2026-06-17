package com.rodrigoleao.gramado2026.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.db.entity.TripEntity
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TripsListViewModel(
    private val repo: TripRepository
) : ViewModel() {

    // null = ainda carregando; emptyList = carregado mas sem viagens
    val trips: StateFlow<List<TripEntity>?> = repo.allTrips
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun deleteTrip(trip: TripEntity) {
        viewModelScope.launch { repo.deleteTrip(trip) }
    }

    companion object {
        fun Factory(repo: TripRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TripsListViewModel(repo) as T
            }
    }
}
