package com.rodrigoleao.gramado2026.data.model

import java.time.LocalDate

// ── BADGES ──────────────────────────────────────────────────────────────
enum class BadgeType { FREE, PAID, BOOKED, INCLUDED, UBER, WALKING, CUSTOM }

data class Badge(
    val type: BadgeType,
    val label: String,
    val color: String? = null
)

// ── WEATHER ─────────────────────────────────────────────────────────────
data class WeatherInfo(
    val minTemp: Int,
    val maxTemp: Int,
    val condition: String,
    val emoji: String
)

// ── WALK ROUTE ───────────────────────────────────────────────────────────
data class WalkStop(
    val emoji: String,
    val label: String,
    val sublabel: String? = null,
    val isLast: Boolean = false
)

// ── ACTIVITY ─────────────────────────────────────────────────────────────
data class TravelActivity(
    val id: Long = 0,
    val time: String,
    val emoji: String,
    val name: String,
    val detail: String,
    val badges: List<Badge> = emptyList(),
    val mapQuery: String? = null,
    val uberDestination: String? = null,
    val walkStops: List<WalkStop> = emptyList()
)

// ── DAY VOUCHER CHIP ─────────────────────────────────────────────────────
data class DayVoucher(
    val emoji: String,
    val label: String
)

// ── TRAVEL DAY ───────────────────────────────────────────────────────────
data class TravelDay(
    val id: Int,
    val dbId: Long = 0,
    val date: LocalDate,
    val dayOfWeek: String,
    val title: String,
    val weather: WeatherInfo,
    val dayAlert: String? = null,
    val dayLinkUrl: String? = null,
    val dayLinkLabel: String = "",
    val dayDocumentPath: String? = null,
    val dayDocumentName: String = "",
    val dayDocumentTitle: String = "",
    val vouchers: List<DayVoucher> = emptyList(),
    val activities: List<TravelActivity>
) {
    val isToday: Boolean get() = date == LocalDate.now()
}

// ── CONTACTS ─────────────────────────────────────────────────────────────
enum class ContactType { AGENCY, HOTEL, ATTRACTION, EMERGENCY, FAMILY, CUSTOM }

data class Contact(
    val id: Long = 0,
    val name: String,
    val role: String,
    val phone: String?,
    val type: ContactType,
    val hasWhatsApp: Boolean = false,
    val isEmergency: Boolean = false,
    val customTypeName: String = "",
    val sortOrder: Int = 0,
    val isFavorite: Boolean = false
)

// ── BOARDING PASSES ──────────────────────────────────────────────────────
data class BoardingPass(
    val id: Long = 0,
    val transportType: String = "FLIGHT",
    val origin: String,
    val originCity: String,
    val destination: String,
    val destinationCity: String,
    val flightNumber: String,
    val date: String,
    val boardingTime: String,
    val passenger: String,
    val walletUrl: String?,
    val documentPath: String? = null,
    val documentName: String = "",
    val notes: String = ""
)

// ── VOUCHERS ─────────────────────────────────────────────────────────────
data class Voucher(
    val id: Long = 0,
    val emoji: String,
    val groupName: String,
    val name: String,
    val person: String? = null,
    val assetPath: String,
    val dayId: Int? = null,
    val sortOrder: Int = 0,
    val isUsed: Boolean = false
)
