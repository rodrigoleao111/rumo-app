package com.rodrigoleao.gramado2026.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.db.entity.TripEntity
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import com.rodrigoleao.gramado2026.data.weather.GeocodingResult
import com.rodrigoleao.gramado2026.data.weather.WeatherRepository
import com.rodrigoleao.gramado2026.data.model.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditTripState(
    val entity: TripEntity? = null,
    val name: String = "",
    val destination: String = "",
    val coverEmoji: String = "",
    val hotelName: String = "",
    val hotelAddress: String = "",
    val hotelPhone: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false
)

@HiltViewModel
class EditTripViewModel @Inject constructor(
    private val repo: TripRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle["tripId"])

    private val _state = MutableStateFlow(EditTripState())
    val state: StateFlow<EditTripState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    val isDirty: StateFlow<Boolean> = _state.map { s ->
        val e = s.entity ?: return@map false
        s.name != e.name || s.destination != e.destination || s.coverEmoji != e.coverEmoji ||
        s.hotelName != e.hotelName || s.hotelAddress != e.hotelAddress || s.hotelPhone != e.hotelPhone ||
        s.latitude != e.latitude || s.longitude != e.longitude
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            val e = repo.getTripEntity(tripId) ?: return@launch
            _state.value = EditTripState(
                entity       = e,
                name         = e.name,
                destination  = e.destination,
                coverEmoji   = e.coverEmoji,
                hotelName    = e.hotelName,
                hotelAddress = e.hotelAddress,
                hotelPhone   = e.hotelPhone,
                latitude     = e.latitude,
                longitude    = e.longitude
            )
        }
    }

    fun updateName(v: String)         { _state.value = _state.value.copy(name = v) }
    fun updateEmoji(v: String)        { _state.value = _state.value.copy(coverEmoji = v) }
    fun updateHotelName(v: String)    { _state.value = _state.value.copy(hotelName = v) }
    fun updateHotelAddress(v: String) { _state.value = _state.value.copy(hotelAddress = v) }
    fun updateHotelPhone(v: String)   { _state.value = _state.value.copy(hotelPhone = v) }

    fun updateDestination(v: String) {
        _state.value = _state.value.copy(destination = v, latitude = null, longitude = null)
        searchJob?.cancel()
        if (v.length < 3) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _isSearching.value = true
            _searchResults.value = runCatching { WeatherRepository.searchLocations(v) }.getOrDefault(emptyList())
            _isSearching.value = false
        }
    }

    fun selectResult(result: GeocodingResult) {
        val label = buildString {
            append(result.name)
            result.admin1?.let { append(", $it") }
            append(", ${result.country}")
        }
        _state.value = _state.value.copy(
            destination = label,
            latitude    = result.latitude,
            longitude   = result.longitude
        )
        _searchResults.value = emptyList()
    }

    fun dismissSearch() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun save() {
        val s = _state.value
        val e = s.entity ?: return
        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            runCatching {
                repo.updateTrip(e.copy(
                    name         = s.name.trim(),
                    destination  = s.destination.trim(),
                    coverEmoji   = s.coverEmoji,
                    hotelName    = s.hotelName.trim(),
                    hotelAddress = s.hotelAddress.trim(),
                    hotelPhone   = s.hotelPhone.trim(),
                    latitude     = s.latitude,
                    longitude    = s.longitude
                ))
            }
                .onSuccess { repo.touchLastEditedAt(e.id); _uiEvent.send(UiEvent.NavigateBack) }
                .onFailure { _state.value = _state.value.copy(isSaving = false); _uiEvent.send(UiEvent.ShowSnackbar("Erro ao salvar viagem")) }
        }
    }

    fun deleteTrip() {
        val e = _state.value.entity ?: return
        _state.value = _state.value.copy(isDeleting = true)
        viewModelScope.launch {
            runCatching { repo.deleteTrip(e) }
                .onSuccess { _uiEvent.send(UiEvent.NavigateAfterDelete) }
                .onFailure { _state.value = _state.value.copy(isDeleting = false); _uiEvent.send(UiEvent.ShowSnackbar("Erro ao excluir viagem")) }
        }
    }

}
