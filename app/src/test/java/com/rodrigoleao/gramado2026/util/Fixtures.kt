package com.rodrigoleao.gramado2026.util

import com.rodrigoleao.gramado2026.data.model.*
import com.rodrigoleao.gramado2026.data.repository.TripData

/**
 * Funções auxiliares de fixture reutilizáveis em toda a suite de testes.
 *
 * Convenção: cada função aceita sobrescritas pontuais via parâmetros nomeados,
 * mas fornece defaults razoáveis para que o teste declare apenas o que importa.
 */

fun fakeTrip(
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
) = Trip(
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

fun fakeContact(
    id         : Long        = 1L,
    name       : String      = "Contato $id",
    role       : String      = "Papel",
    phone      : String?     = null,
    type       : ContactType = ContactType.CUSTOM,
    isFavorite : Boolean     = false,
    sortOrder  : Int         = 0
) = Contact(
    id         = id,
    name       = name,
    role       = role,
    phone      = phone,
    type       = type,
    isFavorite = isFavorite,
    sortOrder  = sortOrder
)

fun fakeVoucher(
    id        : Long    = 1L,
    groupName : String  = "Atrações",
    name      : String  = "Voucher $id",
    sortOrder : Int     = 0,
    isUsed    : Boolean = false
) = Voucher(
    id        = id,
    emoji     = "🎟️",
    groupName = groupName,
    name      = name,
    assetPath = "test/$id.pdf",
    sortOrder = sortOrder,
    isUsed    = isUsed
)

fun fakeTripData(
    trip          : Trip              = fakeTrip(),
    days          : List<TravelDay>   = emptyList(),
    contacts      : List<Contact>     = emptyList(),
    vouchers      : List<Voucher>     = emptyList(),
    boardingPasses: List<BoardingPass> = emptyList()
) = TripData(
    trip           = trip,
    days           = days,
    contacts       = contacts,
    vouchers       = vouchers,
    boardingPasses = boardingPasses
)
