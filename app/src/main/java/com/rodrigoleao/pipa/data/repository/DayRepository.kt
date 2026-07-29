package com.rodrigoleao.pipa.data.repository

import com.rodrigoleao.pipa.data.db.TravelDatabase
import com.rodrigoleao.pipa.data.db.entity.TravelDayEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DayRepository @Inject constructor(private val db: TravelDatabase) {

    suspend fun getDayEntity(tripId: Long, dayNumber: Int): TravelDayEntity? =
        db.dayDao().getByTripAndDayNumber(tripId, dayNumber)

    suspend fun updateDay(entity: TravelDayEntity) = db.dayDao().update(entity)
}
