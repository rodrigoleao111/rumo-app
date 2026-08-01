package com.rodrigoleao.pipa.ui.import_trip

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.pipa.R
import com.rodrigoleao.pipa.data.analytics.AnalyticsService
import com.rodrigoleao.pipa.data.import_trip.TravelImporter
import com.rodrigoleao.pipa.data.model.DuplicateTripException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

    /** F1: o UUID importado já existe no banco — usuário decide manter/substituir. */
    data class Duplicate(
        val existingTripId: Long,
        val existingTripName: String,
        val existingLastEditedAt: Long,
        val incomingLastEditedAt: Long,
        val pendingUri: Uri
    ) : ImportPhase()
}

@HiltViewModel
class ImportTripViewModel @Inject constructor(
    private val importer: TravelImporter,
    private val analytics: AnalyticsService,
    @ApplicationContext private val appContext: android.content.Context
) : ViewModel() {

    private val _phase = MutableStateFlow<ImportPhase>(ImportPhase.Idle)
    val phase: StateFlow<ImportPhase> = _phase.asStateFlow()

    fun startImport(uri: Uri) {
        if (_phase.value is ImportPhase.Importing) return
        _phase.value = ImportPhase.Importing
        viewModelScope.launch {
            _phase.value = try {
                val tripId = importer.import(uri)
                analytics.logTripImported(overwrite = false)
                ImportPhase.Done(tripId)
            } catch (dup: DuplicateTripException) {
                ImportPhase.Duplicate(
                    existingTripId       = dup.existingTripId,
                    existingTripName     = dup.existingTripName,
                    existingLastEditedAt = dup.existingLastEditedAt,
                    incomingLastEditedAt = dup.incomingLastEditedAt,
                    pendingUri           = uri
                )
            } catch (e: Exception) {
                ImportPhase.Error(e.message ?: appContext.getString(R.string.import_error_fallback))
            }
        }
    }

    /** F1: usuário confirmou sobrescrever a viagem local existente. */
    fun overwriteImport(uri: Uri, existingTripId: Long) {
        _phase.value = ImportPhase.Importing
        viewModelScope.launch {
            _phase.value = try {
                val tripId = importer.overwriteImport(uri, existingTripId)
                analytics.logTripImported(overwrite = true)
                ImportPhase.Done(tripId)
            } catch (e: Exception) {
                ImportPhase.Error(e.message ?: appContext.getString(R.string.import_error_overwrite_fallback))
            }
        }
    }

    /** F1: usuário optou por manter a versão local. */
    fun dismissDuplicate() { _phase.value = ImportPhase.Idle }

    fun dismissError() { _phase.value = ImportPhase.Idle }
}
