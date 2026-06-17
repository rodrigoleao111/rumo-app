package com.rodrigoleao.gramado2026.data.db.dao

import androidx.room.*
import com.rodrigoleao.gramado2026.data.db.entity.TravelDayEntity

data class DayTitleRow(val dayNumber: Int, val title: String)

@Dao
interface TravelDayDao {
    @Query("SELECT * FROM travel_days WHERE tripId = :tripId ORDER BY dayNumber ASC")
    suspend fun getDaysForTrip(tripId: Long): List<TravelDayEntity>

    @Query("SELECT dayNumber, title FROM travel_days WHERE tripId = :tripId ORDER BY dayNumber ASC")
    suspend fun getDayTitlesForTrip(tripId: Long): List<DayTitleRow>

    @Query("SELECT * FROM travel_days WHERE id = :dayId")
    suspend fun getById(dayId: Long): TravelDayEntity?

    @Query("SELECT * FROM travel_days WHERE tripId = :tripId AND dayNumber = :dayNumber LIMIT 1")
    suspend fun getByTripAndDayNumber(tripId: Long, dayNumber: Int): TravelDayEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(day: TravelDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(days: List<TravelDayEntity>)

    @Update
    suspend fun update(day: TravelDayEntity)
}
