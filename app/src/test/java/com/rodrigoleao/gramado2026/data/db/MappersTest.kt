package com.rodrigoleao.gramado2026.data.db

import com.rodrigoleao.gramado2026.data.db.entity.ActivityBadgeEntity
import com.rodrigoleao.gramado2026.data.db.entity.ContactEntity
import com.rodrigoleao.gramado2026.data.db.entity.TravelActivityEntity
import com.rodrigoleao.gramado2026.data.db.entity.TravelDayEntity
import com.rodrigoleao.gramado2026.data.db.entity.TripEntity
import com.rodrigoleao.gramado2026.data.db.entity.VoucherEntity
import com.rodrigoleao.gramado2026.data.db.entity.WalkStopEntity
import com.rodrigoleao.gramado2026.data.model.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Testes unitários para Mappers.kt — entity ↔ domain.
 *
 * Convenções desta suite:
 * - Uma função de teste por mapper (cobrindo os campos críticos).
 * - Funções auxiliares `make*` criam fixtures reutilizáveis com defaults razoáveis.
 * - Ao adicionar um novo campo em uma entity, adicionar a assertiva correspondente aqui.
 */
class MappersTest {

    // ── TripEntity ────────────────────────────────────────────────────────────

    @Test
    fun `TripEntity toDomain preserva todos os campos`() {
        val entity = makeTripEntity()
        val domain = entity.toDomain()

        assertEquals(entity.id,              domain.id)
        assertEquals(entity.name,            domain.name)
        assertEquals(entity.destination,     domain.destination)
        assertEquals(entity.coverEmoji,      domain.coverEmoji)
        assertEquals(entity.hotelName,       domain.hotelName)
        assertEquals(entity.hotelAddress,    domain.hotelAddress)
        assertEquals(entity.hotelPhone,      domain.hotelPhone)
        assertEquals(entity.startDate,       domain.startDate)
        assertEquals(entity.endDate,         domain.endDate)
        assertEquals(entity.latitude,        domain.latitude)
        assertEquals(entity.longitude,       domain.longitude)
        assertEquals(entity.voucherSortMode, domain.voucherSortMode)
    }

    @Test
    fun `TripEntity toDomain com campos nulos preserva nulls`() {
        val entity = makeTripEntity(
            startDate = null,
            endDate   = null,
            latitude  = null,
            longitude = null
        )
        val domain = entity.toDomain()

        assertNull(domain.startDate)
        assertNull(domain.endDate)
        assertNull(domain.latitude)
        assertNull(domain.longitude)
    }

    // ── TravelDayEntity ───────────────────────────────────────────────────────

    @Test
    fun `TravelDayEntity toDomain preserva campos básicos`() {
        val entity = makeDayEntity()
        val domain = entity.toDomain(activities = emptyList())

        assertEquals(entity.dayNumber,        domain.id)
        assertEquals(entity.id,               domain.dbId)
        assertEquals("2026-06-09",            domain.date.toString())
        assertEquals(entity.dayOfWeek,        domain.dayOfWeek)
        assertEquals(entity.title,            domain.title)
        assertEquals(entity.dayAlert,         domain.dayAlert)
        assertEquals(entity.dayLinkUrl,       domain.dayLinkUrl)
        assertEquals(entity.dayLinkLabel,     domain.dayLinkLabel)
        assertEquals(entity.dayDocumentPath,  domain.dayDocumentPath)
        assertEquals(entity.dayDocumentName,  domain.dayDocumentName)
        assertEquals(entity.dayDocumentTitle, domain.dayDocumentTitle)
    }

    @Test
    fun `TravelDayEntity toDomain monta WeatherInfo corretamente`() {
        val entity = makeDayEntity(
            minTemp          = 12,
            maxTemp          = 22,
            weatherCondition = "Parcialmente nublado",
            weatherEmoji     = "⛅"
        )
        val domain = entity.toDomain(activities = emptyList())

        assertEquals(12,                    domain.weather.minTemp)
        assertEquals(22,                    domain.weather.maxTemp)
        assertEquals("Parcialmente nublado", domain.weather.condition)
        assertEquals("⛅",                  domain.weather.emoji)
    }

    @Test
    fun `TravelDayEntity toDomain repassa atividades`() {
        val activity = TravelActivity(id = 1, time = "09:00", emoji = "🏔️", name = "Trilha", detail = "")
        val domain = makeDayEntity().toDomain(activities = listOf(activity))

        assertEquals(1, domain.activities.size)
        assertEquals("Trilha", domain.activities.first().name)
    }

    // ── TravelActivityEntity ──────────────────────────────────────────────────

    @Test
    fun `TravelActivityEntity toDomain preserva todos os campos`() {
        val entity = TravelActivityEntity(
            id              = 10,
            dayId           = 1,
            position        = 0,
            time            = "10:00",
            emoji           = "🎢",
            name            = "Bondinho",
            detail          = "Subida panorâmica",
            mapQuery        = "Bondinho Parque da Neve",
            uberDestination = "Bondinho Parque da Neve"
        )
        val domain = entity.toDomain(badges = emptyList(), walkStops = emptyList())

        assertEquals(10L,                    domain.id)
        assertEquals("10:00",               domain.time)
        assertEquals("🎢",                  domain.emoji)
        assertEquals("Bondinho",            domain.name)
        assertEquals("Subida panorâmica",   domain.detail)
        assertEquals("Bondinho Parque da Neve", domain.mapQuery)
        assertEquals("Bondinho Parque da Neve", domain.uberDestination)
        assertTrue(domain.badges.isEmpty())
        assertTrue(domain.walkStops.isEmpty())
    }

    // ── ActivityBadgeEntity ───────────────────────────────────────────────────

    @Test
    fun `ActivityBadgeEntity toDomain converte BadgeType corretamente`() {
        val entity = ActivityBadgeEntity(id = 1, activityId = 1, badgeType = "PAID", label = "Pago", color = null)
        val domain = entity.toDomain()

        assertEquals(BadgeType.PAID, domain.type)
        assertEquals("Pago",         domain.label)
        assertNull(domain.color)
    }

    @Test
    fun `ActivityBadgeEntity toDomain com cor personalizada`() {
        val entity = ActivityBadgeEntity(id = 2, activityId = 1, badgeType = "CUSTOM", label = "VIP", color = "#FF5733")
        val domain = entity.toDomain()

        assertEquals(BadgeType.CUSTOM, domain.type)
        assertEquals("#FF5733",        domain.color)
    }

    // ── WalkStopEntity ────────────────────────────────────────────────────────

    @Test
    fun `WalkStopEntity toDomain preserva sublabel nulo`() {
        val entity = WalkStopEntity(id = 1, activityId = 1, position = 0, emoji = "📍", label = "Início", sublabel = null, isLast = false)
        val domain = entity.toDomain()

        assertEquals("📍",   domain.emoji)
        assertEquals("Início", domain.label)
        assertNull(domain.sublabel)
        assertEquals(false, domain.isLast)
    }

    @Test
    fun `WalkStopEntity toDomain com isLast true`() {
        val entity = WalkStopEntity(id = 2, activityId = 1, position = 3, emoji = "🏁", label = "Fim", sublabel = "Chegada", isLast = true)
        val domain = entity.toDomain()

        assertEquals(true, domain.isLast)
        assertEquals("Chegada", domain.sublabel)
    }

    // ── ContactEntity ─────────────────────────────────────────────────────────

    @Test
    fun `ContactEntity toDomain converte ContactType corretamente`() {
        val entity = makeContactEntity(contactType = "HOTEL")
        val domain = entity.toDomain()

        assertEquals(ContactType.HOTEL, domain.type)
    }

    @Test
    fun `ContactEntity toDomain com tipo invalido usa CUSTOM como fallback`() {
        val entity = makeContactEntity(contactType = "TIPO_INEXISTENTE")
        val domain = entity.toDomain()

        assertEquals(ContactType.CUSTOM, domain.type)
    }

    @Test
    fun `ContactEntity toDomain preserva isFavorite e sortOrder`() {
        val entity = makeContactEntity(isFavorite = true, sortOrder = 5)
        val domain = entity.toDomain()

        assertEquals(true, domain.isFavorite)
        assertEquals(5,    domain.sortOrder)
    }

    // ── VoucherEntity ─────────────────────────────────────────────────────────

    @Test
    fun `VoucherEntity toDomain preserva isUsed e sortOrder`() {
        val entity = VoucherEntity(
            id        = 1,
            tripId    = 10,
            dayNumber = 2,
            emoji     = "🎟️",
            groupName = "Atrações",
            name      = "Bondinho",
            person    = "Rodrigo",
            assetPath = "bondinhos/ingresso.pdf",
            sortOrder = 3,
            isUsed    = true
        )
        val domain = entity.toDomain()

        assertEquals(1,                      domain.id)
        assertEquals(2,                      domain.dayId)
        assertEquals("Rodrigo",              domain.person)
        assertEquals("bondinhos/ingresso.pdf", domain.assetPath)
        assertEquals(3,                      domain.sortOrder)
        assertEquals(true,                   domain.isUsed)
    }

    // ── Domain → Entity ───────────────────────────────────────────────────────

    @Test
    fun `TravelDay toEntity preserva tripId e campos de documento`() {
        val day = makeTravelDay()
        val entity = day.toEntity(tripId = 99)

        assertEquals(99L,                     entity.tripId)
        assertEquals(day.id,                  entity.dayNumber)
        assertEquals(day.date.toString(),     entity.date)
        assertEquals(day.dayAlert,            entity.dayAlert)
        assertEquals(day.dayDocumentTitle,    entity.dayDocumentTitle)
    }

    @Test
    fun `Badge toEntity preserva cor nula`() {
        val badge = Badge(type = BadgeType.FREE, label = "Grátis", color = null)
        val entity = badge.toEntity(activityId = 1)

        assertEquals("FREE",  entity.badgeType)
        assertEquals("Grátis", entity.label)
        assertNull(entity.color)
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private fun makeTripEntity(
        id              : Long    = 1L,
        name            : String  = "Gramado 2026",
        destination     : String  = "Gramado, RS",
        coverEmoji      : String  = "🌲",
        hotelName       : String  = "Laghetto",
        hotelAddress    : String  = "Rua Principal, 100",
        hotelPhone      : String  = "+55 54 3286-0000",
        startDate       : String? = "2026-06-09",
        endDate         : String? = "2026-06-13",
        latitude        : Double? = -29.37,
        longitude       : Double? = -50.87,
        voucherSortMode : String  = "BY_CATEGORY"
    ) = TripEntity(
        id              = id,
        name            = name,
        destination     = destination,
        coverEmoji      = coverEmoji,
        hotelName       = hotelName,
        hotelAddress    = hotelAddress,
        hotelPhone      = hotelPhone,
        startDate       = startDate,
        endDate         = endDate,
        latitude        = latitude,
        longitude       = longitude,
        voucherSortMode = voucherSortMode
    )

    private fun makeDayEntity(
        id               : Long   = 1L,
        tripId           : Long   = 1L,
        dayNumber        : Int    = 1,
        date             : String = "2026-06-09",
        dayOfWeek        : String = "Terça-feira",
        title            : String = "Dia 1 — Chegada",
        weatherEmoji     : String = "☁️",
        minTemp          : Int    = 10,
        maxTemp          : Int    = 20,
        weatherCondition : String = "Nublado",
        dayAlert         : String? = null,
        dayLinkUrl       : String? = null,
        dayLinkLabel     : String = "",
        dayDocumentPath  : String? = null,
        dayDocumentName  : String = "",
        dayDocumentTitle : String = ""
    ) = TravelDayEntity(
        id               = id,
        tripId           = tripId,
        dayNumber        = dayNumber,
        date             = date,
        dayOfWeek        = dayOfWeek,
        title            = title,
        weatherEmoji     = weatherEmoji,
        minTemp          = minTemp,
        maxTemp          = maxTemp,
        weatherCondition = weatherCondition,
        dayAlert         = dayAlert,
        dayLinkUrl       = dayLinkUrl,
        dayLinkLabel     = dayLinkLabel,
        dayDocumentPath  = dayDocumentPath,
        dayDocumentName  = dayDocumentName,
        dayDocumentTitle = dayDocumentTitle
    )

    private fun makeContactEntity(
        id             : Long    = 1L,
        tripId         : Long    = 1L,
        name           : String  = "Hotel Laghetto",
        role           : String  = "Hospedagem",
        phone          : String? = "+55 54 3286-0000",
        contactType    : String  = "HOTEL",
        hasWhatsApp    : Boolean = false,
        isEmergency    : Boolean = false,
        customTypeName : String  = "",
        sortOrder      : Int     = 0,
        isFavorite     : Boolean = false
    ) = ContactEntity(
        id             = id,
        tripId         = tripId,
        name           = name,
        role           = role,
        phone          = phone,
        contactType    = contactType,
        hasWhatsApp    = hasWhatsApp,
        isEmergency    = isEmergency,
        customTypeName = customTypeName,
        sortOrder      = sortOrder,
        isFavorite     = isFavorite
    )

    private fun makeTravelDay() = TravelDay(
        id               = 1,
        dbId             = 1L,
        date             = java.time.LocalDate.parse("2026-06-09"),
        dayOfWeek        = "Terça-feira",
        title            = "Dia 1 — Chegada",
        weather          = WeatherInfo(10, 20, "Nublado", "☁️"),
        dayAlert         = "Chegada prevista às 14h",
        dayLinkUrl       = null,
        dayLinkLabel     = "",
        dayDocumentPath  = null,
        dayDocumentName  = "",
        dayDocumentTitle = "Check-in",
        activities       = emptyList()
    )
}
