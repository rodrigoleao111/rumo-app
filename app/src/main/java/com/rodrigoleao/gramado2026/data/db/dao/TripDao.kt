package com.rodrigoleao.gramado2026.data.db.dao

import androidx.room.*
import com.rodrigoleao.gramado2026.data.db.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY createdAt ASC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getById(tripId: Long): TripEntity?

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun count(): Int

    @Query("SELECT * FROM trips WHERE tripUuid = :uuid LIMIT 1")
    suspend fun findByUuid(uuid: String): TripEntity?

    @Query("UPDATE trips SET lastEditedAt = :timestamp WHERE id = :id")
    suspend fun touchLastEditedAt(id: Long, timestamp: Long)

    // F1 (heal): só atribui UUID se ainda estiver vazio (evita corrida).
    @Query("UPDATE trips SET tripUuid = :uuid WHERE id = :id AND tripUuid = ''")
    suspend fun healUuid(id: Long, uuid: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(trip: TripEntity): Long

    @Update
    suspend fun update(trip: TripEntity)

    @Delete
    suspend fun delete(trip: TripEntity)

    @Query("UPDATE trips SET latitude = :lat, longitude = :lon WHERE id = :id")
    suspend fun updateCoordinates(id: Long, lat: Double, lon: Double)

    @Query("UPDATE trips SET voucherSortMode = :mode WHERE id = :id")
    suspend fun updateVoucherSortMode(id: Long, mode: String)
}
