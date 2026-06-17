package com.rodrigoleao.gramado2026.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.repository.TripData
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TripViewModel(
    private val repo: TripRepository,
    private val tripId: Long
) : ViewModel() {

    private val _tripData = MutableStateFlow<TripData?>(null)
    val tripData: StateFlow<TripData?> = _tripData.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch { _tripData.value = repo.getTripData(tripId) }
    }

    companion object {
        fun Factory(repo: TripRepository, tripId: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TripViewModel(repo, tripId) as T
        }
    }
}
