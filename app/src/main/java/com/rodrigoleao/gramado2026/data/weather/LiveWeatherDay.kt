package com.rodrigoleao.gramado2026.data.weather

data class LiveWeatherDay(
    val date: String,          // "2026-06-09"
    val minTemp: Int,
    val maxTemp: Int,
    val weatherCode: Int,
    val precipitationMm: Double
) {
    val emoji: String get() = weatherCodeEmoji(weatherCode)
    val condition: String get() = weatherCodeCondition(weatherCode, precipitationMm)
}

fun weatherCodeEmoji(code: Int): String = when (code) {
    0             -> "☀️"
    1             -> "🌤️"
    2             -> "⛅"
    3             -> "☁️"
    45, 48        -> "🌫️"
    51, 53, 55    -> "🌦️"
    61, 63, 65    -> "🌧️"
    71, 73, 75    -> "❄️"
    77            -> "🌨️"
    80, 81, 82    -> "🌦️"
    85, 86        -> "🌨️"
    95            -> "⛈️"
    96, 99        -> "⛈️"
    else          -> "🌡️"
}

fun weatherCodeCondition(code: Int, precipMm: Double = 0.0): String {
    val base = when (code) {
        0          -> "Céu aberto"
        1          -> "Principalmente aberto"
        2          -> "Parcialmente nublado"
        3          -> "Nublado"
        45, 48     -> "Neblina"
        51, 53, 55 -> "Garoa"
        61         -> "Chuva fraca"
        63         -> "Chuva moderada"
        65         -> "Chuva forte"
        71, 73, 75 -> "Neve"
        80, 81, 82 -> "Pancadas de chuva"
        95         -> "Tempestade"
        96, 99     -> "Tempestade com granizo"
        else       -> "Variável"
    }
    return if (precipMm > 0) "$base · ${precipMm.toInt()}mm" else base
}
