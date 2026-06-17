package com.rodrigoleao.gramado2026.data.repository

import com.rodrigoleao.gramado2026.data.ai.ItineraryGenerator
import com.rodrigoleao.gramado2026.data.db.TravelDatabase
import com.rodrigoleao.gramado2026.data.weather.WeatherRepository
import com.rodrigoleao.gramado2026.data.db.entity.ActivityBadgeEntity
import com.rodrigoleao.gramado2026.data.db.entity.BoardingPassEntity
import com.rodrigoleao.gramado2026.data.db.entity.ContactEntity
import com.rodrigoleao.gramado2026.data.db.entity.TravelActivityEntity
import com.rodrigoleao.gramado2026.data.db.entity.TravelDayEntity
import com.rodrigoleao.gramado2026.data.db.entity.TripEntity
import com.rodrigoleao.gramado2026.data.db.entity.VoucherEntity
import com.rodrigoleao.gramado2026.data.db.toDomain
import com.rodrigoleao.gramado2026.data.model.*
import com.rodrigoleao.gramado2026.data.model.BadgeType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TripData(
    val trip: TripEntity,
    val days: List<TravelDay>,
    val contacts: List<Contact>,
    val vouchers: List<Voucher>,
    val boardingPasses: List<BoardingPass>
)

class TripRepository(private val db: TravelDatabase) {

    // Lista reativa de viagens — usada na TripsListScreen (Fase 2)
    val allTrips: Flow<List<TripEntity>> = db.tripDao().getAllTrips()

    // Carrega todos os dados de uma viagem como domain models
    suspend fun getTripData(tripId: Long): TripData? {
        val trip = db.tripDao().getById(tripId) ?: return null

        val days = db.dayDao().getDaysForTrip(tripId).map { dayEntity ->
            val activities = db.activityDao().getActivitiesForDay(dayEntity.id).map { actEntity ->
                val badges    = db.activityDao().getBadgesForActivity(actEntity.id).map { it.toDomain() }
                val walkStops = db.activityDao().getWalkStopsForActivity(actEntity.id).map { it.toDomain() }
                actEntity.toDomain(badges, walkStops)
            }
            dayEntity.toDomain(activities)
        }

        val contacts      = db.contactDao().getContactsForTrip(tripId).map { it.toDomain() }
        val vouchers      = db.voucherDao().getVouchersForTrip(tripId).map { it.toDomain() }
        val boardingPasses = db.boardingPassDao().getPassesForTrip(tripId).map { it.toDomain() }

        return TripData(
            trip          = trip,
            days          = days,
            contacts      = contacts,
            vouchers      = vouchers,
            boardingPasses = boardingPasses
        )
    }

    // Retorna apenas os dias de uma viagem (para DayDetailScreen)
    suspend fun getDays(tripId: Long): List<TravelDay> =
        getTripData(tripId)?.days ?: emptyList()

    suspend fun getContacts(tripId: Long): List<Contact> =
        db.contactDao().getContactsForTrip(tripId).map { it.toDomain() }

    suspend fun getVouchers(tripId: Long): List<Voucher> =
        db.voucherDao().getVouchersForTrip(tripId).map { it.toDomain() }

    suspend fun getBoardingPasses(tripId: Long): List<BoardingPass> =
        db.boardingPassDao().getPassesForTrip(tripId).map { it.toDomain() }

    // ── Edição ────────────────────────────────────────────────────────────────

    suspend fun updateTrip(entity: TripEntity) = db.tripDao().update(entity)

    suspend fun deleteTrip(entity: TripEntity) = db.tripDao().delete(entity)

    suspend fun getTripEntity(tripId: Long): TripEntity? = db.tripDao().getById(tripId)

    suspend fun getDayEntity(tripId: Long, dayNumber: Int): TravelDayEntity? =
        db.dayDao().getByTripAndDayNumber(tripId, dayNumber)

    suspend fun updateDay(entity: TravelDayEntity) = db.dayDao().update(entity)

    suspend fun getActivity(activityId: Long): TravelActivityEntity? =
        db.activityDao().getById(activityId)

    suspend fun upsertActivity(
        dayEntityId: Long,
        entity: TravelActivityEntity,
        badges: List<ActivityBadgeEntity>
    ): Long {
        val actId = if (entity.id == 0L) {
            val position = db.activityDao().countForDay(dayEntityId)
            db.activityDao().insertActivity(entity.copy(dayId = dayEntityId, position = position))
        } else {
            db.activityDao().updateActivity(entity)
            entity.id
        }
        db.activityDao().deleteBadgesForActivity(actId)
        badges.forEach { db.activityDao().insertBadge(it.copy(activityId = actId)) }
        return actId
    }

    suspend fun insertWalkStop(entity: com.rodrigoleao.gramado2026.data.db.entity.WalkStopEntity) =
        db.activityDao().insertWalkStop(entity)

    suspend fun getBadgesForActivity(activityId: Long) =
        db.activityDao().getBadgesForActivity(activityId)

    suspend fun deleteActivity(activityId: Long) {
        db.activityDao().getById(activityId)?.let { db.activityDao().deleteActivity(it) }
    }

    suspend fun swapActivityPositions(id1: Long, pos1: Int, id2: Long, pos2: Int) {
        db.activityDao().updatePosition(id1, pos1)
        db.activityDao().updatePosition(id2, pos2)
    }

    // ── Contatos ──────────────────────────────────────────────────────────────

    suspend fun getContactEntity(id: Long): ContactEntity? = db.contactDao().getById(id)

    suspend fun upsertContact(tripId: Long, entity: ContactEntity): Long =
        if (entity.id == 0L) db.contactDao().insert(entity.copy(tripId = tripId))
        else { db.contactDao().update(entity); entity.id }

    suspend fun deleteContact(id: Long) {
        db.contactDao().getById(id)?.let { db.contactDao().delete(it) }
    }

    // ── Vouchers ──────────────────────────────────────────────────────────────

    suspend fun getVoucherEntity(id: Long): VoucherEntity? = db.voucherDao().getById(id)

    suspend fun upsertVoucher(tripId: Long, entity: VoucherEntity): Long =
        if (entity.id == 0L) db.voucherDao().insert(entity.copy(tripId = tripId))
        else { db.voucherDao().update(entity); entity.id }

    suspend fun deleteVoucher(id: Long) {
        db.voucherDao().getById(id)?.let { db.voucherDao().delete(it) }
    }

    // ── Boarding Passes ───────────────────────────────────────────────────────

    suspend fun getBoardingPassEntity(id: Long): BoardingPassEntity? = db.boardingPassDao().getById(id)

    suspend fun upsertBoardingPass(tripId: Long, entity: BoardingPassEntity): Long =
        if (entity.id == 0L) db.boardingPassDao().insert(entity.copy(tripId = tripId))
        else { db.boardingPassDao().update(entity); entity.id }

    suspend fun deleteBoardingPass(id: Long) {
        db.boardingPassDao().getById(id)?.let { db.boardingPassDao().delete(it) }
    }

    suspend fun createTrip(
        name: String,
        destination: String,
        coverEmoji: String,
        startDate: String,
        endDate: String,
        latitude: Double? = null,
        longitude: Double? = null,
        hotelName: String = "",
        hotelAddress: String = "",
        hotelPhone: String = ""
    ): Long {
        val tripId = db.tripDao().insert(
            TripEntity(
                name         = name,
                destination  = destination,
                coverEmoji   = coverEmoji,
                hotelName    = hotelName,
                hotelAddress = hotelAddress,
                hotelPhone   = hotelPhone,
                startDate    = startDate,
                endDate      = endDate,
                latitude     = latitude,
                longitude    = longitude
            )
        )

        val dowFmt = DateTimeFormatter.ofPattern("EEEE", Locale("pt", "BR"))
        var current = LocalDate.parse(startDate)
        val end     = LocalDate.parse(endDate)
        var dayNumber = 1
        while (!current.isAfter(end)) {
            val dow = current.format(dowFmt).replaceFirstChar { it.uppercase() }
            db.dayDao().insert(
                TravelDayEntity(
                    tripId           = tripId,
                    dayNumber        = dayNumber,
                    date             = current.toString(),
                    dayOfWeek        = dow,
                    title            = "Dia $dayNumber — $destination",
                    weatherEmoji     = "🌤️",
                    minTemp          = 0,
                    maxTemp          = 0,
                    weatherCondition = "",
                    dayAlert         = null
                )
            )
            current = current.plusDays(1)
            dayNumber++
        }

        return tripId
    }

    /**
     * Geocodifica o nome do destino e salva lat/lon no banco.
     * Chamado de forma assíncrona após criar uma nova viagem.
     */
    suspend fun geocodeAndSaveCoordinates(tripId: Long, destination: String) {
        val coords = WeatherRepository.geocode(destination) ?: return
        db.tripDao().updateCoordinates(tripId, coords.first, coords.second)
    }

    /**
     * Salva o roteiro gerado pela IA no banco de dados.
     * Atualiza título/alerta de cada dia e insere as atividades com badges.
     */
    suspend fun saveGeneratedItinerary(tripId: Long, days: List<ItineraryGenerator.GeneratedDay>) {
        val badgeLabels = mapOf(
            "FREE"     to "Grátis",
            "PAID"     to "Pago",
            "BOOKED"   to "Reservado",
            "INCLUDED" to "Incluso",
            "UBER"     to "Uber",
            "WALKING"  to "A pé"
        )
        for (gen in days) {
            val dayEntity = db.dayDao().getByTripAndDayNumber(tripId, gen.dayNumber) ?: continue
            db.dayDao().update(dayEntity.copy(
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
                    val type = runCatching { BadgeType.valueOf(badgeStr) }.getOrNull() ?: return@forEach
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
