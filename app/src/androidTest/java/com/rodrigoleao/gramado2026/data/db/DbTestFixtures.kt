package com.rodrigoleao.gramado2026.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rodrigoleao.gramado2026.data.db.entity.*

/** Banco Room em memória para testes de DAO — FKs ativas, sem migrations. */
fun inMemoryDb(): TravelDatabase =
    Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        TravelDatabase::class.java
    ).allowMainThreadQueries().build()

// ── Fixtures de entities (defaults mínimos; sobrescrever o que importa no teste) ──

fun tripEntity(
    id: Long = 0,
    name: String = "Gramado & Canela",
    createdAt: Long = 1_700_000_000_000
) = TripEntity(
    id = id, name = name, destination = "Gramado, RS", coverEmoji = "⛰️",
    hotelName = "Hotel San Lucas", hotelAddress = "Rua João Carniel, 73",
    createdAt = createdAt
)

fun dayEntity(id: Long = 0, tripId: Long, dayNumber: Int = 1) = TravelDayEntity(
    id = id, tripId = tripId, dayNumber = dayNumber, date = "2026-06-09",
    dayOfWeek = "Terça-feira", title = "Dia $dayNumber", weatherEmoji = "☀️",
    minTemp = 8, maxTemp = 18, weatherCondition = "Ensolarado", dayAlert = null
)

fun activityEntity(id: Long = 0, dayId: Long, position: Int = 0, name: String = "Atividade") =
    TravelActivityEntity(
        id = id, dayId = dayId, position = position, time = "09h00", emoji = "🎯",
        name = name, detail = "", mapQuery = null, uberDestination = null
    )

fun badgeEntity(id: Long = 0, activityId: Long, label: String = "GRÁTIS") =
    ActivityBadgeEntity(id = id, activityId = activityId, badgeType = "FREE", label = label)

fun walkStopEntity(id: Long = 0, activityId: Long, position: Int = 0, label: String = "Parada") =
    WalkStopEntity(
        id = id, activityId = activityId, position = position, emoji = "🚶",
        label = label, sublabel = null, isLast = false
    )

fun contactEntity(id: Long = 0, tripId: Long, name: String = "Contato", sortOrder: Int = 0) =
    ContactEntity(
        id = id, tripId = tripId, name = name, role = "Papel", phone = "54999990000",
        contactType = "AGENCY", hasWhatsApp = false, isEmergency = false, sortOrder = sortOrder
    )

fun voucherEntity(
    id: Long = 0,
    tripId: Long,
    name: String = "Voucher",
    groupName: String = "Passeios",
    sortOrder: Int = 0
) = VoucherEntity(
    id = id, tripId = tripId, dayNumber = null, emoji = "🎫", groupName = groupName,
    name = name, person = null, assetPath = "vouchers/$name.pdf", sortOrder = sortOrder
)
