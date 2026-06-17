package com.rodrigoleao.gramado2026.data.db.dao

import androidx.room.*
import com.rodrigoleao.gramado2026.data.db.entity.VoucherEntity

@Dao
interface VoucherDao {
    @Query("SELECT * FROM vouchers WHERE tripId = :tripId")
    suspend fun getVouchersForTrip(tripId: Long): List<VoucherEntity>

    @Query("SELECT * FROM vouchers WHERE id = :id")
    suspend fun getById(id: Long): VoucherEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(voucher: VoucherEntity): Long

    @Update
    suspend fun update(voucher: VoucherEntity)

    @Delete
    suspend fun delete(voucher: VoucherEntity)
}
