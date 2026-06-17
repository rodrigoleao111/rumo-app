package com.rodrigoleao.gramado2026.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.db.entity.ContactEntity
import com.rodrigoleao.gramado2026.data.model.ContactType
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class EditContactState(
    val entity: ContactEntity? = null,
    val name: String = "",
    val role: String = "",
    val phone: String = "",
    val contactType: ContactType = ContactType.ATTRACTION,
    val hasWhatsApp: Boolean = false,
    val isEmergency: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
)

class EditContactViewModel(
    private val repo: TripRepository,
    private val tripId: Long,
    private val contactId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(EditContactState())
    val state: StateFlow<EditContactState> = _state.asStateFlow()

    val isDirty: StateFlow<Boolean> = _state.map { s ->
        val e = s.entity ?: return@map false
        s.name != e.name || s.role != e.role || s.phone != (e.phone ?: "") ||
        s.contactType.name != e.contactType || s.hasWhatsApp != e.hasWhatsApp || s.isEmergency != e.isEmergency
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            if (contactId == 0L) {
                _state.value = EditContactState(isLoading = false)
            } else {
                val e = repo.getContactEntity(contactId)
                _state.value = EditContactState(
                    entity      = e,
                    name        = e?.name ?: "",
                    role        = e?.role ?: "",
                    phone       = e?.phone ?: "",
                    contactType = e?.contactType?.let { ContactType.valueOf(it) } ?: ContactType.ATTRACTION,
                    hasWhatsApp = e?.hasWhatsApp ?: false,
                    isEmergency = e?.isEmergency ?: false,
                    isLoading   = false
                )
            }
        }
    }

    fun updateName(v: String)              { _state.value = _state.value.copy(name = v) }
    fun updateRole(v: String)              { _state.value = _state.value.copy(role = v) }
    fun updatePhone(v: String)             { _state.value = _state.value.copy(phone = v) }
    fun updateType(v: ContactType)         { _state.value = _state.value.copy(contactType = v, isEmergency = v == ContactType.EMERGENCY) }
    fun toggleWhatsApp()                   { _state.value = _state.value.copy(hasWhatsApp = !_state.value.hasWhatsApp) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) return
        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            val entity = ContactEntity(
                id          = contactId,
                tripId      = tripId,
                name        = s.name.trim(),
                role        = s.role.trim(),
                phone       = s.phone.trim().ifEmpty { null },
                contactType = s.contactType.name,
                hasWhatsApp = s.hasWhatsApp,
                isEmergency = s.isEmergency
            )
            repo.upsertContact(tripId, entity)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (contactId == 0L) return
        viewModelScope.launch { repo.deleteContact(contactId); onDone() }
    }

    companion object {
        fun Factory(repo: TripRepository, tripId: Long, contactId: Long) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EditContactViewModel(repo, tripId, contactId) as T
            }
    }
}
