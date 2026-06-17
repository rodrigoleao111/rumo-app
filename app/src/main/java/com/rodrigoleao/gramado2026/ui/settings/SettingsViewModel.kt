package com.rodrigoleao.gramado2026.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rodrigoleao.gramado2026.data.preferences.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val settings: SettingsRepository
) : ViewModel() {

    private val _autoOpenActiveTrip = MutableStateFlow(settings.autoOpenActiveTrip)
    val autoOpenActiveTrip: StateFlow<Boolean> = _autoOpenActiveTrip.asStateFlow()

    fun setAutoOpenActiveTrip(enabled: Boolean) {
        settings.autoOpenActiveTrip = enabled
        _autoOpenActiveTrip.value = enabled
    }

    companion object {
        fun Factory(settings: SettingsRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(settings) as T
            }
    }
}
