package com.rodrigoleao.gramado2026.data.db.dao

import androidx.room.*
import com.rodrigoleao.gramado2026.data.db.entity.VoucherGroupEntity

@Dao
interface VoucherGroupDao {
    @Query("SELECT * FROM voucher_groups WHERE tripId = :tripId ORDER BY name ASC")
    suspend fun getForTrip(tripId: Long): List<VoucherGroupEntity>

    @Query("SELECT COUNT(*) FROM voucher_groups WHERE tripId = :tripId AND name = :name")
    suspend fun exists(tripId: Long, name: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(group: VoucherGroupEntity): Long

    @Delete
    suspend fun delete(group: VoucherGroupEntity)
}
