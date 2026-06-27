package com.rodrigoleao.gramado2026.data.repository

import androidx.room.withTransaction
import com.rodrigoleao.gramado2026.data.db.TravelDatabase
import com.rodrigoleao.gramado2026.data.db.entity.VoucherEntity
import com.rodrigoleao.gramado2026.data.db.entity.VoucherGroupEntity
import com.rodrigoleao.gramado2026.data.db.toDomain
import com.rodrigoleao.gramado2026.data.model.Voucher
import com.rodrigoleao.gramado2026.data.model.reindexedByGroup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoucherRepository @Inject constructor(private val db: TravelDatabase) {

    suspend fun getVoucherEntity(id: Long): VoucherEntity? = db.voucherDao().getById(id)

    suspend fun upsertVoucher(tripId: Long, entity: VoucherEntity): Long =
        if (entity.id == 0L) {
            val nextOrder = db.voucherDao().getMaxSortOrderInGroup(tripId, entity.groupName) + 1
            db.voucherDao().insert(entity.copy(tripId = tripId, sortOrder = nextOrder))
        } else {
            db.voucherDao().update(entity)
            entity.id
        }

    suspend fun deleteVoucher(id: Long) = db.voucherDao().deleteById(id)

    suspend fun deleteVoucherAndReindex(voucherId: Long, tripId: Long) = db.withTransaction {
        db.voucherDao().deleteById(voucherId)
        val remaining = db.voucherDao().getVouchersForTrip(tripId).map { it.toDomain() }
        remaining.reindexedByGroup().forEach { v -> db.voucherDao().updateSortOrder(v.id, v.sortOrder) }
    }

    suspend fun reorderVouchers(ordered: List<Voucher>) = db.withTransaction {
        ordered.forEachIndexed { index, voucher ->
            db.voucherDao().updateSortOrder(voucher.id, index)
        }
    }

    suspend fun toggleVoucherUsed(voucherId: Long, isUsed: Boolean) =
        db.voucherDao().updateIsUsed(voucherId, isUsed)

    suspend fun getVoucherGroups(tripId: Long): List<String> {
        val saved = db.voucherGroupDao().getForTrip(tripId).map { it.name }
        val fromVouchers = db.voucherDao().getVouchersForTrip(tripId)
            .map { it.groupName }
            .filter { it.isNotBlank() }
            .distinct()
        return (saved + fromVouchers).distinct().sorted()
    }

    suspend fun addVoucherGroup(tripId: Long, name: String): Boolean {
        if (db.voucherGroupDao().exists(tripId, name.trim()) > 0) return false
        db.voucherGroupDao().insert(VoucherGroupEntity(tripId = tripId, name = name.trim()))
        return true
    }
}
