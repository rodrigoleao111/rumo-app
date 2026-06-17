package com.rodrigoleao.gramado2026.data.weather

data class GeocodingResult(
    val name: String,
    val admin1: String?,
    val country: String,
    val latitude: Double,
    val longitude: Double
) {
    val displayName: String
        get() = listOfNotNull(name, admin1, country).joinToString(", ")
}
