package com.rodrigoleao.gramado2026.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.db.entity.ActivityBadgeEntity
import com.rodrigoleao.gramado2026.data.db.entity.TravelActivityEntity
import com.rodrigoleao.gramado2026.data.model.BadgeType
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CustomBadge(val name: String, val colorHex: String)

data class EditActivityState(
    val activityId: Long = 0L,
    val dayEntityId: Long = 0L,
    val time: String = "",
    val emoji: String = "📍",
    val name: String = "",
    val detail: String = "",
    val address: String = "",
    val selectedBadges: Set<BadgeType> = emptySet(),
    val customBadges: List<CustomBadge> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
)

class EditActivityViewModel(
    private val repo: TripRepository,
    private val tripId: Long,
    private val dayNumber: Int,
    private val activityId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(EditActivityState())
    val state: StateFlow<EditActivityState> = _state.asStateFlow()

    val isDirty: StateFlow<Boolean> = _state.map { s ->
        !s.isLoading && (s.activityId == 0L && s.name.isNotBlank() ||
        s.activityId != 0L && (s.name.isNotBlank()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            val dayEntity = repo.getDayEntity(tripId, dayNumber)
            val dayDbId = dayEntity?.id ?: 0L
            if (activityId == 0L) {
                _state.value = EditActivityState(
                    activityId  = 0L,
                    dayEntityId = dayDbId,
                    isLoading   = false
                )
            } else {
                val act = repo.getActivity(activityId)
                val badgeEntities = if (act != null) repo.getBadgesForActivity(activityId) else emptyList()
                val standardBadges = badgeEntities
                    .filter { it.badgeType != BadgeType.CUSTOM.name }
                    .map { BadgeType.valueOf(it.badgeType) }
                    .toSet()
                val customBadges = badgeEntities
                    .filter { it.badgeType == BadgeType.CUSTOM.name }
                    .map { CustomBadge(it.label, it.color ?: "#607D8B") }

                _state.value = EditActivityState(
                    activityId     = activityId,
                    dayEntityId    = dayDbId,
                    time           = act?.time ?: "",
                    emoji          = act?.emoji ?: "📍",
                    name           = act?.name ?: "",
                    detail         = act?.detail ?: "",
                    address        = act?.mapQuery ?: act?.uberDestination ?: "",
                    selectedBadges = standardBadges,
                    customBadges   = customBadges,
                    isLoading      = false
                )
            }
        }
    }

    fun updateTime(v: String)            { _state.value = _state.value.copy(time = v) }
    fun updateEmoji(v: String)           { _state.value = _state.value.copy(emoji = v) }
    fun updateName(v: String)            { _state.value = _state.value.copy(name = v) }
    fun updateDetail(v: String)          { _state.value = _state.value.copy(detail = v) }
    fun updateAddress(v: String) { _state.value = _state.value.copy(address = v) }

    fun addCustomBadge(name: String, colorHex: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        _state.value = _state.value.copy(
            customBadges = _state.value.customBadges + CustomBadge(trimmed, colorHex)
        )
    }

    fun removeCustomBadge(badge: CustomBadge) {
        _state.value = _state.value.copy(
            customBadges = _state.value.customBadges - badge
        )
    }

    fun toggleBadge(type: BadgeType) {
        val current = _state.value.selectedBadges
        _state.value = _state.value.copy(
            selectedBadges = if (type in current) current - type else current + type
        )
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) return
        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            val entity = TravelActivityEntity(
                id              = s.activityId,
                dayId           = s.dayEntityId,
                position        = 0,
                time            = s.time.trim(),
                emoji           = s.emoji,
                name            = s.name.trim(),
                detail          = s.detail.trim(),
                mapQuery        = s.address.trim().ifEmpty { null },
                uberDestination = s.address.trim().ifEmpty { null }
            )
            val badges = s.selectedBadges.map { type ->
                ActivityBadgeEntity(activityId = 0L, badgeType = type.name, label = badgeLabel(type))
            } + s.customBadges.map { cb ->
                ActivityBadgeEntity(activityId = 0L, badgeType = BadgeType.CUSTOM.name, label = cb.name, color = cb.colorHex)
            }
            repo.upsertActivity(s.dayEntityId, entity, badges)
            onDone()
        }
    }

    fun deleteActivity(onDone: () -> Unit) {
        val id = _state.value.activityId
        if (id == 0L) return
        _state.value = _state.value.copy(isSaving = true)
        viewModelScope.launch {
            repo.deleteActivity(id)
            onDone()
        }
    }

    companion object {
        fun badgeLabel(type: BadgeType): String = when (type) {
            BadgeType.FREE     -> "Grátis"
            BadgeType.PAID     -> "Pago"
            BadgeType.BOOKED   -> "Reservado"
            BadgeType.INCLUDED -> "Incluído"
            BadgeType.UBER     -> "Uber"
            BadgeType.WALKING  -> "A pé"
            BadgeType.CUSTOM   -> ""
        }
        fun Factory(repo: TripRepository, tripId: Long, dayNumber: Int, activityId: Long) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EditActivityViewModel(repo, tripId, dayNumber, activityId) as T
            }
    }
}
