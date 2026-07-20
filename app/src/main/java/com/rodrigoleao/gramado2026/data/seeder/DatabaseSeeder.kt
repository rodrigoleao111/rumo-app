package com.rodrigoleao.gramado2026.data.seeder

import com.rodrigoleao.gramado2026.data.db.TravelDatabase
import com.rodrigoleao.gramado2026.data.db.entity.TripEntity
import com.rodrigoleao.gramado2026.data.db.toEntity
import com.rodrigoleao.gramado2026.data.repository.RoteiroRepository

object DatabaseSeeder {

    suspend fun seedIfEmpty(db: TravelDatabase) {
        if (db.tripDao().count() > 0) return

        val days = RoteiroRepository.days
        val startDate = days.minOfOrNull { it.date }?.toString()
        val endDate   = days.maxOfOrNull { it.date }?.toString()

        val tripId = db.tripDao().insert(
            TripEntity(
                name         = "Gramado & Canela",
                destination  = "Gramado, RS",
                coverEmoji   = "⛰️",
                hotelName    = "Hotel San Lucas",
                hotelAddress = "Rua João Carniel, 73, Gramado, RS",
                startDate    = startDate,
                endDate      = endDate,
                tripUuid     = java.util.UUID.randomUUID().toString(),   // F1: nasce com UUID
                lastEditedAt = System.currentTimeMillis()
            )
        )

        days.forEach { day ->
            val dayId = db.dayDao().insert(day.toEntity(tripId))
            day.activities.forEachIndexed { idx, activity ->
                val activityId = db.activityDao().insertActivity(activity.toEntity(dayId, idx))
                activity.badges.forEach { badge ->
                    db.activityDao().insertBadge(badge.toEntity(activityId))
                }
                activity.walkStops.forEachIndexed { stopIdx, stop ->
                    db.activityDao().insertWalkStop(stop.toEntity(activityId, stopIdx))
                }
            }
        }

        RoteiroRepository.contacts.forEach { contact ->
            db.contactDao().insert(contact.toEntity(tripId))
        }

        RoteiroRepository.vouchers.forEach { voucher ->
            db.voucherDao().insert(voucher.toEntity(tripId))
        }

        RoteiroRepository.boardingPasses.forEach { pass ->
            db.boardingPassDao().insert(pass.toEntity(tripId))
        }
    }
}
