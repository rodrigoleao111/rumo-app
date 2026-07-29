package com.rodrigoleao.pipa.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.pipa.data.preferences.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {

    val autoOpenActiveTrip: StateFlow<Boolean> = settings.autoOpenActiveTrip
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showEmergencyContacts: StateFlow<Boolean> = settings.showEmergencyContacts
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val sortTripsByProximity: StateFlow<Boolean> = settings.sortTripsByProximity
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val hideCompletedTrips: StateFlow<Boolean> = settings.hideCompletedTrips
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setAutoOpenActiveTrip(enabled: Boolean) {
        viewModelScope.launch { settings.setAutoOpenActiveTrip(enabled) }
    }

    fun setShowEmergencyContacts(enabled: Boolean) {
        viewModelScope.launch { settings.setShowEmergencyContacts(enabled) }
    }

    fun setSortTripsByProximity(enabled: Boolean) {
        viewModelScope.launch { settings.setSortTripsByProximity(enabled) }
    }

    fun setHideCompletedTrips(enabled: Boolean) {
        viewModelScope.launch { settings.setHideCompletedTrips(enabled) }
    }

}
