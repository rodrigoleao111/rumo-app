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

enum class VoucherInputMode { LINK, FILE }

// Par (número do dia, título) para o seletor de dia
data class DayOption(val number: Int, val title: String)

data class EditVoucherState(
    val entity: VoucherEntity? = null,
    val emoji: String = "🎫",
    val groupName: String = "",
    val name: String = "",
    val person: String = "",
    val linkUrl: String = "",
    val filePath: String = "",
    val fileName: String = "",
    val inputMode: VoucherInputMode = VoucherInputMode.LINK,
    val dayNumber: Int? = null,
    val availableGroups: List<String> = emptyList(),
    val availableDays: List<DayOption> = emptyList(),
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
        val currentPath = if (s.inputMode == VoucherInputMode.LINK) s.linkUrl else s.filePath
        s.emoji != e.emoji || s.groupName != e.groupName || s.name != e.name ||
        s.person != (e.person ?: "") || currentPath != e.assetPath ||
        s.dayNumber != e.dayNumber
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            val savedGroups = repo.getVoucherGroups(tripId)
            val groups = (DEFAULT_GROUPS + savedGroups).distinct().sorted()
            val days = repo.getDayTitles(tripId).map { (number, title) ->
                DayOption(number = number, title = title)
            }

            if (voucherId == 0L) {
                _state.value = EditVoucherState(
                    availableGroups = groups,
                    availableDays   = days,
                    isLoading       = false
                )
            } else {
                val e = repo.getVoucherEntity(voucherId)
                val path   = e?.assetPath ?: ""
                val isLink = path.startsWith("http") || path.isBlank()
                _state.value = EditVoucherState(
                    entity          = e,
                    emoji           = e?.emoji ?: "🎫",
                    groupName       = e?.groupName ?: "",
                    name            = e?.name ?: "",
                    person          = e?.person ?: "",
                    linkUrl         = if (isLink) path else "",
                    filePath        = if (!isLink) path else "",
                    fileName        = if (!isLink) java.io.File(path).name else "",
                    inputMode       = if (isLink) VoucherInputMode.LINK else VoucherInputMode.FILE,
                    dayNumber       = e?.dayNumber,
                    availableGroups = groups,
                    availableDays   = days,
                    isLoading       = false
                )
            }
        }
    }

    fun updateEmoji(v: String)       { _state.value = _state.value.copy(emoji = v) }
    fun updateGroupName(v: String)   { _state.value = _state.value.copy(groupName = v) }
    fun updateName(v: String)        { _state.value = _state.value.copy(name = v) }
    fun updatePerson(v: String)      { _state.value = _state.value.copy(person = v) }
    fun updateLinkUrl(v: String)     { _state.value = _state.value.copy(linkUrl = v) }
    fun setInputMode(mode: VoucherInputMode) { _state.value = _state.value.copy(inputMode = mode) }
    fun selectDay(number: Int?)      { _state.value = _state.value.copy(dayNumber = number) }

    fun updateFile(path: String, name: String) {
        _state.value = _state.value.copy(filePath = path, fileName = name, inputMode = VoucherInputMode.FILE)
    }

    fun clearFile() {
        _state.value = _state.value.copy(filePath = "", fileName = "")
    }

    fun createGroup(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repo.addVoucherGroup(tripId, trimmed)
            val saved = repo.getVoucherGroups(tripId)
            val updated = (DEFAULT_GROUPS + saved).distinct().sorted()
            _state.value = _state.value.copy(availableGroups = updated, groupName = trimmed)
        }
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val assetPath = if (s.inputMode == VoucherInputMode.LINK) s.linkUrl.trim() else s.filePath.trim()
        if (s.name.isBlank()) return
        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            if (s.groupName.isNotBlank()) repo.addVoucherGroup(tripId, s.groupName)
            val entity = VoucherEntity(
                id        = voucherId,
                tripId    = tripId,
                dayNumber = s.dayNumber,
                emoji     = s.emoji,
                groupName = s.groupName.trim().ifBlank { "Geral" },
                name      = s.name.trim(),
                person    = s.person.trim().ifEmpty { null },
                assetPath = assetPath,
                sortOrder = s.entity?.sortOrder ?: 0,
                isUsed    = s.entity?.isUsed ?: false
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
        val DEFAULT_GROUPS = listOf("Hospedagem", "Parques", "Passeios", "Refeições", "Shows", "Transporte")

        fun Factory(repo: TripRepository, tripId: Long, voucherId: Long) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EditVoucherViewModel(repo, tripId, voucherId) as T
            }
    }
}
