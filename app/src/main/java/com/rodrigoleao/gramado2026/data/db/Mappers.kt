package com.rodrigoleao.gramado2026.data.db

import com.rodrigoleao.gramado2026.data.db.entity.*
import com.rodrigoleao.gramado2026.data.model.*
import java.time.LocalDate

// ── Domain → Entity (usados no seeder) ───────────────────────────────────────

fun TravelDay.toEntity(tripId: Long): TravelDayEntity = TravelDayEntity(
    tripId           = tripId,
    dayNumber        = id,
    date             = date.toString(),
    dayOfWeek        = dayOfWeek,
    title            = title,
    weatherEmoji     = weather.emoji,
    minTemp          = weather.minTemp,
    maxTemp          = weather.maxTemp,
    weatherCondition = weather.condition,
    dayAlert         = dayAlert
)

fun TravelActivity.toEntity(dayId: Long, position: Int): TravelActivityEntity = TravelActivityEntity(
    dayId           = dayId,
    position        = position,
    time            = time,
    emoji           = emoji,
    name            = name,
    detail          = detail,
    mapQuery        = mapQuery,
    uberDestination = uberDestination
)

fun Badge.toEntity(activityId: Long): ActivityBadgeEntity = ActivityBadgeEntity(
    activityId = activityId,
    badgeType  = type.name,
    label      = label,
    color      = color
)

fun WalkStop.toEntity(activityId: Long, position: Int): WalkStopEntity = WalkStopEntity(
    activityId = activityId,
    position   = position,
    emoji      = emoji,
    label      = label,
    sublabel   = sublabel,
    isLast     = isLast
)

fun Contact.toEntity(tripId: Long): ContactEntity = ContactEntity(
    tripId      = tripId,
    name        = name,
    role        = role,
    phone       = phone,
    contactType = type.name,
    hasWhatsApp = hasWhatsApp,
    isEmergency = isEmergency
)

fun Voucher.toEntity(tripId: Long): VoucherEntity = VoucherEntity(
    tripId    = tripId,
    dayNumber = dayId,
    emoji     = emoji,
    groupName = groupName,
    name      = name,
    person    = person,
    assetPath = assetPath,
    sortOrder = sortOrder
)

fun BoardingPass.toEntity(tripId: Long): BoardingPassEntity = BoardingPassEntity(
    tripId          = tripId,
    origin          = origin,
    originCity      = originCity,
    destination     = destination,
    destinationCity = destinationCity,
    flightNumber    = flightNumber,
    date            = date,
    boardingTime    = boardingTime,
    passenger       = passenger,
    walletUrl       = walletUrl
)

// ── Entity → Domain (usados no repositório) ───────────────────────────────────

fun TravelDayEntity.toDomain(
    activities: List<TravelActivity>
): TravelDay = TravelDay(
    id        = dayNumber,
    dbId      = id,
    date      = LocalDate.parse(date),
    dayOfWeek = dayOfWeek,
    title     = title,
    weather   = WeatherInfo(minTemp, maxTemp, weatherCondition, weatherEmoji),
    dayAlert         = dayAlert,
    dayLinkUrl       = dayLinkUrl,
    dayLinkLabel     = dayLinkLabel,
    dayDocumentPath  = dayDocumentPath,
    dayDocumentName  = dayDocumentName,
    dayDocumentTitle = dayDocumentTitle,
    activities       = activities
)

fun TravelActivityEntity.toDomain(
    badges: List<Badge>,
    walkStops: List<WalkStop>
): TravelActivity = TravelActivity(
    id              = id,
    time            = time,
    emoji           = emoji,
    name            = name,
    detail          = detail,
    badges          = badges,
    mapQuery        = mapQuery,
    uberDestination = uberDestination,
    walkStops       = walkStops
)

fun ActivityBadgeEntity.toDomain(): Badge = Badge(
    type  = BadgeType.valueOf(badgeType),
    label = label,
    color = color
)

fun WalkStopEntity.toDomain(): WalkStop = WalkStop(
    emoji    = emoji,
    label    = label,
    sublabel = sublabel,
    isLast   = isLast
)

fun ContactEntity.toDomain(): Contact = Contact(
    id          = id,
    name        = name,
    role        = role,
    phone       = phone,
    type        = ContactType.valueOf(contactType),
    hasWhatsApp = hasWhatsApp,
    isEmergency = isEmergency
)

fun VoucherEntity.toDomain(): Voucher = Voucher(
    id        = id,
    emoji     = emoji,
    groupName = groupName,
    name      = name,
    person    = person,
    assetPath = assetPath,
    dayId     = dayNumber,
    sortOrder = sortOrder,
    isUsed    = isUsed
)

fun BoardingPassEntity.toDomain(): BoardingPass = BoardingPass(
    id              = id,
    origin          = origin,
    originCity      = originCity,
    destination     = destination,
    destinationCity = destinationCity,
    flightNumber    = flightNumber,
    date            = date,
    boardingTime    = boardingTime,
    passenger       = passenger,
    walletUrl       = walletUrl
)
