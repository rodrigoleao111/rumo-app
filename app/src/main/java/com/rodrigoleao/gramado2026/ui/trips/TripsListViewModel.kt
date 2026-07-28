package com.rodrigoleao.gramado2026.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.db.entity.TripEntity
import com.rodrigoleao.gramado2026.data.model.UiEvent
import com.rodrigoleao.gramado2026.data.preferences.SettingsRepository
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class TripsListViewModel @Inject constructor(
    private val repo: TripRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    // null = ainda carregando; emptyList = carregado mas sem viagens
    // Aplica as preferências de exibição sobre a lista do repositório (createdAt ASC):
    //  · ocultar concluídas → filtra (apenas na tela; nada é apagado do banco)
    //  · ordenar por proximidade → reordena
    val trips: StateFlow<List<TripEntity>?> =
        combine(
            repo.allTrips,
            settings.sortTripsByProximity,
            settings.hideCompletedTrips
        ) { list, sortEnabled, hideCompleted ->
            val today = LocalDate.now()
            var result = list
            if (hideCompleted) result = result.filter { !isCompleted(it, today) }
            if (sortEnabled)   result = sortByProximity(result)
            result
        }.stateIn(
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

    /**
     * Ordena por proximidade da viagem: em curso primeiro, depois as futuras mais
     * próximas de começar e, por último, as concluídas (mais recentes na frente).
     * A ordenação é estável — empates preservam a ordem original (createdAt ASC).
     */
    private fun sortByProximity(trips: List<TripEntity>): List<TripEntity> {
        val today = LocalDate.now()
        return trips
            .map { it to proximityKey(it, today) }
            .sortedWith(compareBy({ it.second.first }, { it.second.second }))
            .map { it.first }
    }

    // (rank, distância). rank: 0 = em curso, 1 = futura, 2 = concluída.
    // distância é sempre crescente dentro do grupo:
    //  · em curso  → dias até terminar (termina antes = mais perto)
    //  · futura    → dias até começar  (começa antes = mais perto); sem datas vai ao fim
    //  · concluída → dias desde que terminou (terminou há menos tempo = mais perto)
    private fun proximityKey(trip: TripEntity, today: LocalDate): Pair<Int, Long> {
        val start = trip.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val end   = trip.endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return when {
            start == null || end == null -> 1 to Long.MAX_VALUE
            today.isBefore(start)        -> 1 to ChronoUnit.DAYS.between(today, start)
            today.isAfter(end)           -> 2 to ChronoUnit.DAYS.between(end, today)
            else                         -> 0 to ChronoUnit.DAYS.between(today, end)
        }
    }

    /** Concluída = tem datas válidas e hoje já passou da data final (mesma regra do status na tela). */
    private fun isCompleted(trip: TripEntity, today: LocalDate): Boolean {
        // Requer ambas as datas — sem datas o status é "planejando", nunca "concluída".
        if (trip.startDate.isNullOrBlank() || trip.endDate.isNullOrBlank()) return false
        val end = runCatching { LocalDate.parse(trip.endDate) }.getOrNull() ?: return false
        return today.isAfter(end)
    }

}
