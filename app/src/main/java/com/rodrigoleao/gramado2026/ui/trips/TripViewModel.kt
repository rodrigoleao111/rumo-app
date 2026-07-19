package com.rodrigoleao.gramado2026.ui.trips

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.model.Contact
import com.rodrigoleao.gramado2026.data.model.Voucher
import com.rodrigoleao.gramado2026.data.model.reindexedByGroup
import com.rodrigoleao.gramado2026.data.repository.ActivityRepository
import com.rodrigoleao.gramado2026.data.repository.ContactRepository
import com.rodrigoleao.gramado2026.data.repository.TripData
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import com.rodrigoleao.gramado2026.data.repository.VoucherRepository
import com.rodrigoleao.gramado2026.ui.vouchers.VoucherSortMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripViewModel @Inject constructor(
    private val tripRepo: TripRepository,
    private val voucherRepo: VoucherRepository,
    private val contactRepo: ContactRepository,
    private val activityRepo: ActivityRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle["tripId"])

    private val _tripData = MutableStateFlow<TripData?>(null)
    val tripData: StateFlow<TripData?> = _tripData.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    fun setVoucherSortMode(mode: VoucherSortMode) {
        viewModelScope.launch { tripRepo.saveVoucherSortMode(tripId, mode.name) }
    }

    // ── Vouchers ──────────────────────────────────────────────────────────────

    fun deleteVoucher(voucherId: Long) {
        val remaining = _tripData.value?.vouchers?.filter { it.id != voucherId } ?: return
        _tripData.update { it?.copy(vouchers = remaining.reindexedByGroup()) }
        viewModelScope.launch { voucherRepo.deleteVoucherAndReindex(voucherId, tripId) }
        touch()
    }

    fun toggleVoucherUsed(voucherId: Long, isUsed: Boolean) {
        _tripData.update { data ->
            data?.copy(vouchers = data.vouchers.map {
                if (it.id == voucherId) it.copy(isUsed = isUsed) else it
            })
        }
        viewModelScope.launch { voucherRepo.toggleVoucherUsed(voucherId, isUsed) }
        touch()
    }

    fun reorderVouchers(ordered: List<Voucher>) {
        _tripData.update { it?.copy(vouchers = ordered) }
        viewModelScope.launch { voucherRepo.reorderVouchers(ordered) }
        touch()
    }

    // ── Contatos ──────────────────────────────────────────────────────────────

    fun deleteContact(contactId: Long) {
        _tripData.update { data ->
            data?.copy(contacts = data.contacts.filter { it.id != contactId })
        }
        viewModelScope.launch { contactRepo.deleteContact(contactId) }
        touch()
    }

    fun reorderContacts(contacts: List<Contact>) {
        _tripData.update { it?.copy(contacts = contacts) }
        viewModelScope.launch { contactRepo.reorderContacts(contacts) }
        touch()
    }

    fun toggleFavoriteContact(contactId: Long, isFavorite: Boolean) {
        _tripData.update { data ->
            data?.copy(contacts = data.contacts.map {
                if (it.id == contactId) it.copy(isFavorite = isFavorite) else it
            })
        }
        viewModelScope.launch { contactRepo.toggleFavoriteContact(contactId, isFavorite) }
        touch()
    }

    // ── Atividades ────────────────────────────────────────────────────────────

    fun deleteActivity(activityId: Long) {
        viewModelScope.launch {
            activityRepo.deleteActivity(activityId)
            load()
        }
        touch()
    }

    fun swapActivityPositions(id1: Long, pos1: Int, id2: Long, pos2: Int) {
        viewModelScope.launch {
            activityRepo.swapActivityPositions(id1, pos1, id2, pos2)
            load()
        }
        touch()
    }

    // F1: registra edição de conteúdo. A importação NÃO passa por aqui (usa o
    // TravelImporter direto), então o lastEditedAt do arquivo é preservado.
    private fun touch() {
        viewModelScope.launch { tripRepo.touchLastEditedAt(tripId) }
    }

    private fun load() {
        viewModelScope.launch { _tripData.value = tripRepo.getTripData(tripId) }
    }
}
