package com.rodrigoleao.gramado2026.data.db.dao

import androidx.room.*
import com.rodrigoleao.gramado2026.data.db.entity.ActivityBadgeEntity
import com.rodrigoleao.gramado2026.data.db.entity.TravelActivityEntity
import com.rodrigoleao.gramado2026.data.db.entity.WalkStopEntity

@Dao
interface TravelActivityDao {
    @Query("SELECT * FROM travel_activities WHERE dayId = :dayId ORDER BY position ASC")
    suspend fun getActivitiesForDay(dayId: Long): List<TravelActivityEntity>

    // Bulk: busca atividades de vários dias de uma só vez (evita N queries no getTripData)
    @Query("SELECT * FROM travel_activities WHERE dayId IN (:dayIds) ORDER BY dayId ASC, position ASC")
    suspend fun getActivitiesForDays(dayIds: List<Long>): List<TravelActivityEntity>

    // Bulk: busca badges de várias atividades de uma só vez
    @Query("SELECT * FROM activity_badges WHERE activityId IN (:activityIds)")
    suspend fun getBadgesForActivities(activityIds: List<Long>): List<ActivityBadgeEntity>

    // Bulk: busca walk stops de várias atividades de uma só vez
    @Query("SELECT * FROM walk_stops WHERE activityId IN (:activityIds) ORDER BY activityId ASC, position ASC")
    suspend fun getWalkStopsForActivities(activityIds: List<Long>): List<WalkStopEntity>

    @Query("SELECT * FROM travel_activities WHERE id = :activityId")
    suspend fun getById(activityId: Long): TravelActivityEntity?

    @Query("SELECT COUNT(*) FROM travel_activities WHERE dayId = :dayId")
    suspend fun countForDay(dayId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActivity(activity: TravelActivityEntity): Long

    @Update
    suspend fun updateActivity(activity: TravelActivityEntity)

    @Delete
    suspend fun deleteActivity(activity: TravelActivityEntity)

    @Query("DELETE FROM travel_activities WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM activity_badges WHERE activityId = :activityId")
    suspend fun getBadgesForActivity(activityId: Long): List<ActivityBadgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: ActivityBadgeEntity): Long

    // Inserção em lote de badges (evita N round-trips)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<ActivityBadgeEntity>)

    @Query("DELETE FROM activity_badges WHERE activityId = :activityId")
    suspend fun deleteBadgesForActivity(activityId: Long)

    @Query("SELECT * FROM walk_stops WHERE activityId = :activityId ORDER BY position ASC")
    suspend fun getWalkStopsForActivity(activityId: Long): List<WalkStopEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWalkStop(stop: WalkStopEntity): Long

    @Query("UPDATE travel_activities SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)
}
