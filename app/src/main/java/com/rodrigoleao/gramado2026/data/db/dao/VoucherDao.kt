package com.rodrigoleao.gramado2026.data.db.dao

import androidx.room.*
import com.rodrigoleao.gramado2026.data.db.entity.VoucherEntity

@Dao
interface VoucherDao {
    @Query("SELECT * FROM vouchers WHERE tripId = :tripId ORDER BY groupName ASC, sort_order ASC, id ASC")
    suspend fun getVouchersForTrip(tripId: Long): List<VoucherEntity>

    @Query("UPDATE vouchers SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("UPDATE vouchers SET is_used = :isUsed WHERE id = :id")
    suspend fun updateIsUsed(id: Long, isUsed: Boolean)

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM vouchers WHERE tripId = :tripId AND groupName = :groupName")
    suspend fun getMaxSortOrderInGroup(tripId: Long, groupName: String): Int

    @Query("SELECT * FROM vouchers WHERE id = :id")
    suspend fun getById(id: Long): VoucherEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(voucher: VoucherEntity): Long

    @Update
    suspend fun update(voucher: VoucherEntity)

    @Delete
    suspend fun delete(voucher: VoucherEntity)

    @Query("DELETE FROM vouchers WHERE id = :id")
    suspend fun deleteById(id: Long)
}
