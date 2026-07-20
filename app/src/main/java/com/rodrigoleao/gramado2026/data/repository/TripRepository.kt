package com.rodrigoleao.gramado2026.data.repository

import com.rodrigoleao.gramado2026.data.db.TravelDatabase
import com.rodrigoleao.gramado2026.data.db.entity.TravelDayEntity
import com.rodrigoleao.gramado2026.data.db.entity.TripEntity
import com.rodrigoleao.gramado2026.data.db.toDomain
import com.rodrigoleao.gramado2026.data.model.*
import com.rodrigoleao.gramado2026.data.weather.WeatherRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

data class TripData(
    val trip: Trip,
    val days: List<TravelDay>,
    val contacts: List<Contact>,
    val vouchers: List<Voucher>,
    val boardingPasses: List<BoardingPass>
)

class TripRepository(private val db: TravelDatabase) {

    val allTrips: Flow<List<TripEntity>> = db.tripDao().getAllTrips()

    // Carrega todos os dados de uma viagem como domain models.
    // Usa queries bulk (IN) para evitar N+1: 1 trip + 1 days + 1 activities + 1 badges
    // + 1 walkstops + 1 contacts + 1 vouchers + 1 boardingPasses = 8 queries fixas.
    suspend fun getTripData(tripId: Long): TripData? {
        var trip = db.tripDao().getById(tripId) ?: return null

        // F1 (heal): viagens criadas antes da F1 têm tripUuid vazio. Gera um UUID
        // estável na primeira vez que a viagem é carregada, para que a detecção de
        // duplicata funcione também para viagens antigas.
        if (trip.tripUuid.isBlank()) {
            val uuid = UUID.randomUUID().toString()
            db.tripDao().healUuid(tripId, uuid)
            trip = trip.copy(tripUuid = uuid)
        }

        val dayEntities = db.dayDao().getDaysForTrip(tripId)
        val dayIds      = dayEntities.map { it.id }

        val allActivities = if (dayIds.isNotEmpty()) db.activityDao().getActivitiesForDays(dayIds) else emptyList()
        val activityIds   = allActivities.map { it.id }

        val allBadges    = if (activityIds.isNotEmpty()) db.activityDao().getBadgesForActivities(activityIds) else emptyList()
        val allWalkStops = if (activityIds.isNotEmpty()) db.activityDao().getWalkStopsForActivities(activityIds) else emptyList()

        val badgesByActivity = allBadges.groupBy { it.activityId }
        val stopsByActivity  = allWalkStops.groupBy { it.activityId }
        val activitiesByDay  = allActivities.groupBy { it.dayId }

        val days = dayEntities.map { dayEntity ->
            val activities = (activitiesByDay[dayEntity.id] ?: emptyList()).map { actEntity ->
                actEntity.toDomain(
                    badges    = (badgesByActivity[actEntity.id] ?: emptyList()).map { it.toDomain() },
                    walkStops = (stopsByActivity[actEntity.id] ?: emptyList()).map { it.toDomain() }
                )
            }
            dayEntity.toDomain(activities)
        }

        val contacts      = db.contactDao().getContactsForTrip(tripId).map { it.toDomain() }
        val vouchers      = db.voucherDao().getVouchersForTrip(tripId).map { it.toDomain() }
        val boardingPasses = db.boardingPassDao().getPassesForTrip(tripId).map { it.toDomain() }

        return TripData(trip.toDomain(), days, contacts, vouchers, boardingPasses)
    }

    suspend fun getDays(tripId: Long): List<TravelDay> =
        getTripData(tripId)?.days ?: emptyList()

    // Query leve: só dayNumber + title, sem carregar atividades/badges/etc.
    suspend fun getDayTitles(tripId: Long): List<Pair<Int, String>> =
        db.dayDao().getDayTitlesForTrip(tripId).map { row -> row.dayNumber to row.title }

    suspend fun getContacts(tripId: Long): List<Contact> =
        db.contactDao().getContactsForTrip(tripId).map { it.toDomain() }

    suspend fun getVouchers(tripId: Long): List<Voucher> =
        db.voucherDao().getVouchersForTrip(tripId).map { it.toDomain() }

    suspend fun getBoardingPasses(tripId: Long): List<BoardingPass> =
        db.boardingPassDao().getPassesForTrip(tripId).map { it.toDomain() }

    // ── Viagem ────────────────────────────────────────────────────────────────

    suspend fun getTripEntity(tripId: Long): TripEntity? = db.tripDao().getById(tripId)

    suspend fun updateTrip(entity: TripEntity) = db.tripDao().update(entity)

    suspend fun deleteTrip(entity: TripEntity) = db.tripDao().delete(entity)

    /** Busca uma viagem pelo UUID estável. Retorna null para UUID vazio (F1 — detecção de duplicata). */
    suspend fun findByUuid(uuid: String): TripEntity? =
        if (uuid.isBlank()) null else db.tripDao().findByUuid(uuid)

    /** Registra que o conteúdo da viagem foi editado agora. Chamado pela camada ViewModel (F1). */
    suspend fun touchLastEditedAt(tripId: Long) =
        db.tripDao().touchLastEditedAt(tripId, System.currentTimeMillis())

    suspend fun saveVoucherSortMode(tripId: Long, mode: String) =
        db.tripDao().updateVoucherSortMode(tripId, mode)

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
        hotelPhone: String = "",
        // F1: null → gera novo UUID / usa "agora" (criação local).
        // Não-null → preserva os valores do arquivo (usado pelo TravelImporter).
        tripUuid: String? = null,
        lastEditedAt: Long? = null
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
                longitude    = longitude,
                tripUuid     = tripUuid ?: UUID.randomUUID().toString(),
                lastEditedAt = lastEditedAt ?: System.currentTimeMillis()
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

    suspend fun geocodeAndSaveCoordinates(tripId: Long, destination: String) {
        val coords = WeatherRepository.geocode(destination) ?: return
        db.tripDao().updateCoordinates(tripId, coords.first, coords.second)
    }

}
