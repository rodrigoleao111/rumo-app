package com.rodrigoleao.gramado2026.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rodrigoleao.gramado2026.data.model.Voucher
import com.rodrigoleao.gramado2026.data.repository.TripData
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import com.rodrigoleao.gramado2026.ui.vouchers.VoucherSortMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TripViewModel(
    private val repo: TripRepository,
    private val tripId: Long
) : ViewModel() {

    private val _tripData = MutableStateFlow<TripData?>(null)
    val tripData: StateFlow<TripData?> = _tripData.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    fun setVoucherSortMode(mode: VoucherSortMode) {
        viewModelScope.launch { repo.saveVoucherSortMode(tripId, mode.name) }
    }

    fun deleteVoucher(voucherId: Long) {
        viewModelScope.launch {
            repo.deleteVoucher(voucherId)
            val remaining = _tripData.value?.vouchers
                ?.filter { it.id != voucherId }
                ?: return@launch
            // Reindexar sort_order dentro de cada grupo para fechar o gap
            val reindexed = remaining
                .groupBy { it.groupName }
                .flatMap { (_, items) -> items.mapIndexed { i, v -> v.copy(sortOrder = i) } }
            repo.reorderVouchers(reindexed)
            _tripData.value = _tripData.value?.copy(vouchers = reindexed)
        }
    }

    fun toggleVoucherUsed(voucherId: Long, isUsed: Boolean) {
        _tripData.value = _tripData.value?.let { data ->
            data.copy(vouchers = data.vouchers.map {
                if (it.id == voucherId) it.copy(isUsed = isUsed) else it
            })
        }
        viewModelScope.launch { repo.toggleVoucherUsed(voucherId, isUsed) }
    }

    fun reorderVouchers(ordered: List<Voucher>) {
        viewModelScope.launch {
            repo.reorderVouchers(ordered)
            // Atualiza a lista em memória sem recarregar tudo do banco
            _tripData.value = _tripData.value?.let { data ->
                data.copy(vouchers = ordered)
            }
        }
    }

    private fun load() {
        viewModelScope.launch { _tripData.value = repo.getTripData(tripId) }
    }

    companion object {
        fun Factory(repo: TripRepository, tripId: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TripViewModel(repo, tripId) as T
        }
    }
}
