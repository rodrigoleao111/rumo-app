package com.rodrigoleao.gramado2026.data.repository

import com.rodrigoleao.gramado2026.data.db.TravelDatabase
import com.rodrigoleao.gramado2026.data.db.entity.BoardingPassEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BoardingPassRepository @Inject constructor(private val db: TravelDatabase) {

    suspend fun getBoardingPassEntity(id: Long): BoardingPassEntity? =
        db.boardingPassDao().getById(id)

    suspend fun upsertBoardingPass(tripId: Long, entity: BoardingPassEntity): Long =
        if (entity.id == 0L) db.boardingPassDao().insert(entity.copy(tripId = tripId))
        else { db.boardingPassDao().update(entity); entity.id }

    suspend fun deleteBoardingPass(id: Long) = db.boardingPassDao().deleteById(id)
}
