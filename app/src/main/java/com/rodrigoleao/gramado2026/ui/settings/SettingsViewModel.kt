package com.rodrigoleao.gramado2026.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.preferences.SettingsRepository
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

    fun setAutoOpenActiveTrip(enabled: Boolean) {
        viewModelScope.launch { settings.setAutoOpenActiveTrip(enabled) }
    }

    fun setShowEmergencyContacts(enabled: Boolean) {
        viewModelScope.launch { settings.setShowEmergencyContacts(enabled) }
    }

}
