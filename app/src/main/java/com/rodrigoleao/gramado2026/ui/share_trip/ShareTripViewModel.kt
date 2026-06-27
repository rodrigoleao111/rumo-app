package com.rodrigoleao.gramado2026.ui.share_trip

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.export.TravelExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SharePhase {
    object Idle      : SharePhase()
    object Exporting : SharePhase()
    data class Ready(val uri: Uri)       : SharePhase()
    data class Error(val message: String) : SharePhase()
}

@HiltViewModel
class ShareTripViewModel @Inject constructor(
    private val exporter: TravelExporter,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle["tripId"])

    private val _phase = MutableStateFlow<SharePhase>(SharePhase.Idle)
    val phase: StateFlow<SharePhase> = _phase.asStateFlow()

    fun export() {
        if (_phase.value is SharePhase.Exporting) return
        _phase.value = SharePhase.Exporting
        viewModelScope.launch {
            _phase.value = try {
                SharePhase.Ready(exporter.export(tripId))
            } catch (e: Exception) {
                SharePhase.Error(e.message ?: "Erro ao preparar o arquivo.")
            }
        }
    }

    fun clearReady() { _phase.value = SharePhase.Idle }
    fun dismissError() { _phase.value = SharePhase.Idle }

}
