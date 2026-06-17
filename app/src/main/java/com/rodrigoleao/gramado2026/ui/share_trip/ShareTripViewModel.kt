package com.rodrigoleao.gramado2026.ui.share_trip

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.export.TravelExporter
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SharePhase {
    object Idle      : SharePhase()
    object Exporting : SharePhase()
    data class Ready(val uri: Uri)       : SharePhase()
    data class Error(val message: String) : SharePhase()
}

class ShareTripViewModel(
    private val tripId: Long,
    private val repo: TripRepository,
    appContext: Context
) : ViewModel() {

    private val exporter = TravelExporter(appContext, repo)

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

    companion object {
        fun Factory(repo: TripRepository, tripId: Long, context: Context) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ShareTripViewModel(tripId, repo, context.applicationContext) as T
            }
    }
}
