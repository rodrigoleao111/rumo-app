package com.rodrigoleao.gramado2026.ui.import_trip

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.import_trip.TravelImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ImportPhase {
    object Idle      : ImportPhase()
    object Importing : ImportPhase()
    data class Done(val tripId: Long)      : ImportPhase()
    data class Error(val message: String)  : ImportPhase()
}

@HiltViewModel
class ImportTripViewModel @Inject constructor(
    private val importer: TravelImporter
) : ViewModel() {

    private val _phase = MutableStateFlow<ImportPhase>(ImportPhase.Idle)
    val phase: StateFlow<ImportPhase> = _phase.asStateFlow()

    fun startImport(uri: Uri) {
        if (_phase.value is ImportPhase.Importing) return
        _phase.value = ImportPhase.Importing
        viewModelScope.launch {
            _phase.value = try {
                ImportPhase.Done(importer.import(uri))
            } catch (e: Exception) {
                ImportPhase.Error(e.message ?: "Erro ao importar viagem.")
            }
        }
    }

    fun dismissError() { _phase.value = ImportPhase.Idle }
}
