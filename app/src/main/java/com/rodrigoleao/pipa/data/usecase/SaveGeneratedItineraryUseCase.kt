package com.rodrigoleao.pipa.data.usecase

import androidx.room.withTransaction
import com.rodrigoleao.pipa.data.ai.ItineraryGenerator
import com.rodrigoleao.pipa.data.db.TravelDatabase
import com.rodrigoleao.pipa.data.db.entity.ActivityBadgeEntity
import com.rodrigoleao.pipa.data.db.entity.TravelActivityEntity
import com.rodrigoleao.pipa.data.model.BadgeType
import com.rodrigoleao.pipa.data.repository.DayRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveGeneratedItineraryUseCase @Inject constructor(
    private val db: TravelDatabase,
    private val dayRepo: DayRepository
) {
    private val badgeLabels = mapOf(
        "FREE"     to "Grátis",
        "PAID"     to "Pago",
        "BOOKED"   to "Reservado",
        "INCLUDED" to "Incluso",
        "UBER"     to "Uber",
        "WALKING"  to "A pé"
    )

    suspend operator fun invoke(tripId: Long, days: List<ItineraryGenerator.GeneratedDay>) =
        db.withTransaction {
            for (gen in days) {
                val dayEntity = dayRepo.getDayEntity(tripId, gen.dayNumber) ?: continue
                dayRepo.updateDay(dayEntity.copy(
                    title    = gen.title,
                    dayAlert = gen.dayAlert
                ))
                gen.activities.forEachIndexed { index, act ->
                    val addr  = act.address?.trim()?.ifEmpty { null }
                    val actId = db.activityDao().insertActivity(
                        TravelActivityEntity(
                            dayId           = dayEntity.id,
                            position        = index,
                            time            = act.time,
                            emoji           = act.emoji,
                            name            = act.name,
                            detail          = act.detail,
                            mapQuery        = addr,
                            uberDestination = addr
                        )
                    )
                    act.badges.forEach { badgeStr ->
                        val type = runCatching { BadgeType.valueOf(badgeStr) }.getOrNull()
                            ?: return@forEach
                        db.activityDao().insertBadge(
                            ActivityBadgeEntity(
                                activityId = actId,
                                badgeType  = type.name,
                                label      = badgeLabels[badgeStr] ?: badgeStr
                            )
                        )
                    }
                }
            }
        }
}
