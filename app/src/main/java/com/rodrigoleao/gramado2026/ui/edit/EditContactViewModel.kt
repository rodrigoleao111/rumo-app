package com.rodrigoleao.gramado2026.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.db.entity.ContactEntity
import com.rodrigoleao.gramado2026.data.model.ContactType
import com.rodrigoleao.gramado2026.data.preferences.ContactCategoryRepository
import com.rodrigoleao.gramado2026.data.repository.ContactRepository
import com.rodrigoleao.gramado2026.data.model.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditContactState(
    val entity: ContactEntity? = null,
    val name: String = "",
    val role: String = "",
    val phone: String = "",
    val selectedCategory: String = "ATTRACTION",
    val customCategories: List<String> = emptyList(),
    val hasWhatsApp: Boolean = false,
    val isEmergency: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
)

@HiltViewModel
class EditContactViewModel @Inject constructor(
    private val repo: ContactRepository,
    private val categoryRepo: ContactCategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle["tripId"])
    private val contactId: Long = checkNotNull(savedStateHandle["contactId"])

    private val _state = MutableStateFlow(EditContactState())
    val state: StateFlow<EditContactState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val isDirty: StateFlow<Boolean> = _state.map { s ->
        val e = s.entity
        if (e == null) return@map s.name.isNotBlank()
        val entityCategory = if (e.contactType == ContactType.CUSTOM.name) e.customTypeName else e.contactType
        s.name != e.name || s.role != e.role || s.phone != (e.phone ?: "") ||
        s.selectedCategory != entityCategory || s.hasWhatsApp != e.hasWhatsApp || s.isEmergency != e.isEmergency
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        val customCategories = categoryRepo.getCustomCategories()
        viewModelScope.launch {
            if (contactId == 0L) {
                _state.value = EditContactState(
                    customCategories = customCategories,
                    isLoading = false
                )
            } else {
                val e = repo.getContactEntity(contactId)
                val selectedCategory = if (e?.contactType == ContactType.CUSTOM.name) {
                    e.customTypeName
                } else {
                    e?.contactType ?: "ATTRACTION"
                }
                val enrichedCategories = if (
                    e?.contactType == ContactType.CUSTOM.name &&
                    e.customTypeName.isNotBlank() &&
                    !customCategories.contains(e.customTypeName)
                ) {
                    (customCategories + e.customTypeName).sorted()
                } else {
                    customCategories
                }
                _state.value = EditContactState(
                    entity           = e,
                    name             = e?.name ?: "",
                    role             = e?.role ?: "",
                    phone            = e?.phone ?: "",
                    selectedCategory = selectedCategory,
                    customCategories = enrichedCategories,
                    hasWhatsApp      = e?.hasWhatsApp ?: false,
                    isEmergency      = e?.isEmergency ?: false,
                    isLoading        = false
                )
            }
        }
    }

    fun updateName(v: String)  { _state.value = _state.value.copy(name = v) }
    fun updateRole(v: String)  { _state.value = _state.value.copy(role = v) }
    fun updatePhone(v: String) { _state.value = _state.value.copy(phone = v) }
    fun toggleWhatsApp()       { _state.value = _state.value.copy(hasWhatsApp = !_state.value.hasWhatsApp) }

    fun updateCategory(v: String) {
        _state.value = _state.value.copy(
            selectedCategory = v,
            isEmergency = v == ContactType.EMERGENCY.name
        )
    }

    fun addCustomCategory(name: String) {
        categoryRepo.addCategory(name)
        _state.value = _state.value.copy(customCategories = categoryRepo.getCustomCategories())
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) return
        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            val isBuiltin = ContactType.entries.any { it.name == s.selectedCategory && it != ContactType.CUSTOM }
            val entity = ContactEntity(
                id             = contactId,
                tripId         = tripId,
                name           = s.name.trim(),
                role           = s.role.trim(),
                phone          = s.phone.trim().ifEmpty { null },
                contactType    = if (isBuiltin) s.selectedCategory else ContactType.CUSTOM.name,
                customTypeName = if (isBuiltin) "" else s.selectedCategory,
                hasWhatsApp    = s.hasWhatsApp,
                isEmergency    = s.isEmergency
            )
            runCatching { repo.upsertContact(tripId, entity) }
                .onSuccess { _uiEvent.send(UiEvent.NavigateBack) }
                .onFailure { _state.value = _state.value.copy(isSaving = false); _uiEvent.send(UiEvent.ShowSnackbar("Erro ao salvar contato")) }
        }
    }

    fun delete() {
        if (contactId == 0L) return
        viewModelScope.launch {
            runCatching { repo.deleteContact(contactId) }
                .onSuccess { _uiEvent.send(UiEvent.NavigateBack) }
                .onFailure { _uiEvent.send(UiEvent.ShowSnackbar("Erro ao excluir contato")) }
        }
    }
}
