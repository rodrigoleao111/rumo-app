package com.rodrigoleao.gramado2026.data.repository

import androidx.room.withTransaction
import com.rodrigoleao.gramado2026.data.db.TravelDatabase
import com.rodrigoleao.gramado2026.data.db.entity.ActivityBadgeEntity
import com.rodrigoleao.gramado2026.data.db.entity.TravelActivityEntity
import com.rodrigoleao.gramado2026.data.db.entity.WalkStopEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(private val db: TravelDatabase) {

    suspend fun getActivity(activityId: Long): TravelActivityEntity? =
        db.activityDao().getById(activityId)

    suspend fun upsertActivity(
        dayEntityId: Long,
        entity: TravelActivityEntity,
        badges: List<ActivityBadgeEntity>
    ): Long = db.withTransaction {
        val actId = if (entity.id == 0L) {
            val position = db.activityDao().countForDay(dayEntityId)
            db.activityDao().insertActivity(entity.copy(dayId = dayEntityId, position = position))
        } else {
            db.activityDao().updateActivity(entity)
            entity.id
        }
        db.activityDao().deleteBadgesForActivity(actId)
        if (badges.isNotEmpty()) db.activityDao().insertBadges(badges.map { it.copy(activityId = actId) })
        actId
    }

    suspend fun insertWalkStop(entity: WalkStopEntity) =
        db.activityDao().insertWalkStop(entity)

    suspend fun getBadgesForActivity(activityId: Long) =
        db.activityDao().getBadgesForActivity(activityId)

    suspend fun deleteActivity(activityId: Long) = db.activityDao().deleteById(activityId)

    suspend fun swapActivityPositions(id1: Long, pos1: Int, id2: Long, pos2: Int) {
        db.activityDao().updatePosition(id1, pos1)
        db.activityDao().updatePosition(id2, pos2)
    }
}
