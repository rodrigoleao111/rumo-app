package com.rodrigoleao.gramado2026.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.db.entity.VoucherEntity
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditVoucherState(
    val entity: VoucherEntity? = null,
    val emoji: String = "🎫",
    val groupName: String = "",
    val name: String = "",
    val person: String = "",
    val assetPath: String = "",   // URL ou caminho de asset
    val dayNumber: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
)

class EditVoucherViewModel(
    private val repo: TripRepository,
    private val tripId: Long,
    private val voucherId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(EditVoucherState())
    val state: StateFlow<EditVoucherState> = _state.asStateFlow()

    val isDirty: StateFlow<Boolean> = _state.map { s ->
        val e = s.entity ?: return@map false
        s.emoji != e.emoji || s.groupName != e.groupName || s.name != e.name ||
        s.person != (e.person ?: "") || s.assetPath != e.assetPath
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            if (voucherId == 0L) {
                _state.value = EditVoucherState(isLoading = false)
            } else {
                val e = repo.getVoucherEntity(voucherId)
                _state.value = EditVoucherState(
                    entity    = e,
                    emoji     = e?.emoji ?: "🎫",
                    groupName = e?.groupName ?: "",
                    name      = e?.name ?: "",
                    person    = e?.person ?: "",
                    assetPath = e?.assetPath ?: "",
                    dayNumber = e?.dayNumber?.toString() ?: "",
                    isLoading = false
                )
            }
        }
    }

    fun updateEmoji(v: String)     { _state.value = _state.value.copy(emoji = v) }
    fun updateGroupName(v: String) { _state.value = _state.value.copy(groupName = v) }
    fun updateName(v: String)      { _state.value = _state.value.copy(name = v) }
    fun updatePerson(v: String)    { _state.value = _state.value.copy(person = v) }
    fun updateAssetPath(v: String) { _state.value = _state.value.copy(assetPath = v) }
    fun updateDayNumber(v: String) { _state.value = _state.value.copy(dayNumber = v) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank() || s.groupName.isBlank()) return
        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            val entity = VoucherEntity(
                id        = voucherId,
                tripId    = tripId,
                dayNumber = s.dayNumber.toIntOrNull(),
                emoji     = s.emoji,
                groupName = s.groupName.trim(),
                name      = s.name.trim(),
                person    = s.person.trim().ifEmpty { null },
                assetPath = s.assetPath.trim()
            )
            repo.upsertVoucher(tripId, entity)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (voucherId == 0L) return
        viewModelScope.launch { repo.deleteVoucher(voucherId); onDone() }
    }

    companion object {
        fun Factory(repo: TripRepository, tripId: Long, voucherId: Long) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EditVoucherViewModel(repo, tripId, voucherId) as T
            }
    }
}
