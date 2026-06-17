package com.rodrigoleao.gramado2026.data.db.dao

import androidx.room.*
import com.rodrigoleao.gramado2026.data.db.entity.BoardingPassEntity

@Dao
interface BoardingPassDao {
    @Query("SELECT * FROM boarding_passes WHERE tripId = :tripId")
    suspend fun getPassesForTrip(tripId: Long): List<BoardingPassEntity>

    @Query("SELECT * FROM boarding_passes WHERE id = :id")
    suspend fun getById(id: Long): BoardingPassEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(pass: BoardingPassEntity): Long

    @Update
    suspend fun update(pass: BoardingPassEntity)

    @Delete
    suspend fun delete(pass: BoardingPassEntity)
}
