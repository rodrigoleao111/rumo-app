package com.rodrigoleao.gramado2026.data.db

import com.rodrigoleao.gramado2026.data.db.entity.*
import com.rodrigoleao.gramado2026.data.model.*
import java.time.LocalDate

// ── TripEntity → Domain ──────────────────────────────────────────────────────

fun TripEntity.toDomain(): Trip = Trip(
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
    voucherSortMode = voucherSortMode,
    tripUuid        = tripUuid,
    lastEditedAt    = lastEditedAt
)

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
    dayAlert         = dayAlert,
    dayLinkUrl       = dayLinkUrl,
    dayLinkLabel     = dayLinkLabel,
    dayDocumentPath  = dayDocumentPath,
    dayDocumentName  = dayDocumentName,
    dayDocumentTitle = dayDocumentTitle
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
    tripId         = tripId,
    name           = name,
    role           = role,
    phone          = phone,
    contactType    = type.name,
    hasWhatsApp    = hasWhatsApp,
    isEmergency    = isEmergency,
    customTypeName = customTypeName,
    sortOrder      = sortOrder,
    isFavorite     = isFavorite
)

fun Voucher.toEntity(tripId: Long): VoucherEntity = VoucherEntity(
    tripId    = tripId,
    dayNumber = dayId,
    emoji     = emoji,
    groupName = groupName,
    name      = name,
    person    = person,
    assetPath = assetPath,
    sortOrder = sortOrder,
    isUsed    = isUsed
)

fun BoardingPass.toEntity(tripId: Long): BoardingPassEntity = BoardingPassEntity(
    tripId          = tripId,
    transportType   = transportType,
    origin          = origin,
    originCity      = originCity,
    destination     = destination,
    destinationCity = destinationCity,
    flightNumber    = flightNumber,
    date            = date,
    boardingTime    = boardingTime,
    passenger       = passenger,
    walletUrl       = walletUrl,
    documentPath    = documentPath,
    documentName    = documentName,
    notes           = notes
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
    id             = id,
    name           = name,
    role           = role,
    phone          = phone,
    type           = runCatching { ContactType.valueOf(contactType) }.getOrDefault(ContactType.CUSTOM),
    hasWhatsApp    = hasWhatsApp,
    isEmergency    = isEmergency,
    customTypeName = customTypeName,
    sortOrder      = sortOrder,
    isFavorite     = isFavorite
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
    transportType   = transportType,
    origin          = origin,
    originCity      = originCity,
    destination     = destination,
    destinationCity = destinationCity,
    flightNumber    = flightNumber,
    date            = date,
    boardingTime    = boardingTime,
    passenger       = passenger,
    walletUrl       = walletUrl,
    documentPath    = documentPath,
    documentName    = documentName,
    notes           = notes
)

// ── Notas (F4) ───────────────────────────────────────────────────────────────

fun ChecklistItemEntity.toDomain(): ChecklistItem = ChecklistItem(
    id = id, text = text, isChecked = isChecked, sortOrder = sortOrder
)

fun NoteBlockEntity.toDomain(items: List<ChecklistItem>): NoteBlock = when (type) {
    NoteBlockType.CHECKLIST.name -> NoteBlock.ChecklistBlock(id = id, items = items, sortOrder = sortOrder)
    NoteBlockType.HEADING.name   -> NoteBlock.HeadingBlock(id = id, content = content, sortOrder = sortOrder)
    else                         -> NoteBlock.TextBlock(id = id, content = content, sortOrder = sortOrder)
}

fun NoteEntity.toDomain(blocks: List<NoteBlock>): Note = Note(
    id = id, tripId = tripId, dayId = dayId, title = title,
    blocks = blocks, sortOrder = sortOrder, createdAt = createdAt, updatedAt = updatedAt
)

// domain → entity (usado no import). Blocos/itens são inseridos separadamente.
fun Note.toEntity(): NoteEntity = NoteEntity(
    tripId = tripId, dayId = dayId, title = title,
    sortOrder = sortOrder, createdAt = createdAt, updatedAt = updatedAt
)

/** Deriva o tipo persistido de um bloco de domínio. */
fun NoteBlock.typeName(): String = when (this) {
    is NoteBlock.TextBlock      -> NoteBlockType.TEXT.name
    is NoteBlock.ChecklistBlock -> NoteBlockType.CHECKLIST.name
    is NoteBlock.HeadingBlock   -> NoteBlockType.HEADING.name
}

/** Conteúdo textual de um bloco (vazio para checklist). */
fun NoteBlock.contentText(): String = when (this) {
    is NoteBlock.TextBlock      -> content
    is NoteBlock.HeadingBlock   -> content
    is NoteBlock.ChecklistBlock -> ""
}
