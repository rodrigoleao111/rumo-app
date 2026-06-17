package com.rodrigoleao.gramado2026.data.weather

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import kotlin.math.roundToInt

private const val TAG = "WeatherRepository"

object WeatherRepository {

    private const val CACHE_PREFS    = "weather_cache"
    private const val CACHE_DURATION = 3 * 60 * 60 * 1000L   // 3h

    // Cache em memória por chave "{lat}_{lon}_{start}_{end}"
    private val memoryCacheMap = mutableMapOf<String, Pair<Map<String, LiveWeatherDay>, Long>>()

    // ── API pública ───────────────────────────────────────────────────────────

    suspend fun searchLocations(query: String, count: Int = 6): List<GeocodingResult> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=$count&language=pt&format=json"
                Log.d(TAG, "searchLocations → $url")
                val json = httpGet(url)
                Log.d(TAG, "searchLocations ← ${json.take(300)}")
                val root = JSONObject(json)
                val results = root.optJSONArray("results") ?: run {
                    Log.w(TAG, "searchLocations: campo 'results' ausente ou nulo")
                    return@withContext emptyList()
                }
                (0 until results.length()).map { i ->
                    val obj = results.getJSONObject(i)
                    GeocodingResult(
                        name      = obj.getString("name"),
                        admin1    = obj.optString("admin1").ifBlank { null },
                        country   = obj.optString("country").ifBlank { "?" },
                        latitude  = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude")
                    )
                }.also { Log.d(TAG, "searchLocations: ${it.size} resultado(s)") }
            } catch (e: Exception) {
                Log.e(TAG, "searchLocations erro", e)
                emptyList()
            }
        }

    suspend fun geocode(location: String): Pair<Double, Double>? =
        searchLocations(location, count = 1).firstOrNull()?.let {
            Log.d(TAG, "geocode '$location' → lat=${it.latitude}, lon=${it.longitude}")
            Pair(it.latitude, it.longitude)
        } ?: run {
            Log.w(TAG, "geocode '$location' → sem resultado")
            null
        }

    suspend fun getWeather(
        context: Context,
        lat: Double? = null,
        lon: Double? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Map<String, LiveWeatherDay>? {
        Log.d(TAG, "getWeather chamado: lat=$lat, lon=$lon, start=$startDate, end=$endDate")
        if (lat == null || lon == null || startDate == null || endDate == null) {
            Log.w(TAG, "getWeather: parâmetros incompletos — abortando")
            return null
        }
        return fetchWithCache(context, lat, lon, startDate, endDate)
    }

    suspend fun refresh(
        context: Context,
        lat: Double? = null,
        lon: Double? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Map<String, LiveWeatherDay>? {
        Log.d(TAG, "refresh chamado: lat=$lat, lon=$lon, start=$startDate, end=$endDate")
        if (lat == null || lon == null || startDate == null || endDate == null) {
            Log.w(TAG, "refresh: parâmetros incompletos — abortando")
            return null
        }
        val key = cacheKey(lat, lon, startDate, endDate)
        memoryCacheMap.remove(key)
        clearDiskCache(context, key)
        return fetchWithCache(context, lat, lon, startDate, endDate)
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private suspend fun fetchWithCache(
        context: Context,
        lat: Double,
        lon: Double,
        startDate: String,
        endDate: String
    ): Map<String, LiveWeatherDay>? {
        val key = cacheKey(lat, lon, startDate, endDate)
        val now = System.currentTimeMillis()

        // 1. Cache em memória
        memoryCacheMap[key]?.let { (data, time) ->
            if (now - time < CACHE_DURATION) {
                Log.d(TAG, "fetchWithCache: hit memória (key=$key, ${data.size} dias)")
                return data
            }
        }

        val prefs = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)

        // 2. Cache no disco
        val cachedJson = prefs.getString("json_$key", null)
        val cachedTime = prefs.getLong("ts_$key", 0L)
        if (cachedJson != null && now - cachedTime < CACHE_DURATION) {
            Log.d(TAG, "fetchWithCache: hit disco (key=$key)")
            return parseThenCache(key, cachedJson, cachedTime)
        }

        // 3. Buscar da API
        return try {
            Log.d(TAG, "fetchWithCache: buscando da API (key=$key)")
            val json = fetchWeatherJson(lat, lon, startDate, endDate)
            prefs.edit()
                .putString("json_$key", json)
                .putLong("ts_$key", now)
                .apply()
            parseThenCache(key, json, now)
        } catch (e: Exception) {
            Log.e(TAG, "fetchWithCache: erro na API", e)
            if (cachedJson != null) {
                Log.w(TAG, "fetchWithCache: usando cache expirado como fallback")
                parseThenCache(key, cachedJson, cachedTime)
            } else null
        }
    }

    private suspend fun fetchWeatherJson(lat: Double, lon: Double, startDate: String, endDate: String): String =
        withContext(Dispatchers.IO) {
            // O endpoint /v1/forecast suporta até 16 dias a partir de hoje.
            // Limita o end_date para não extrapolar o limite da API.
            val maxEnd = LocalDate.now().plusDays(15).toString()
            val clampedEnd = if (endDate > maxEnd) {
                Log.w(TAG, "fetchWeatherJson: end_date $endDate excede limite da API — limitando a $maxEnd")
                maxEnd
            } else {
                endDate
            }

            // Se o start_date já passou do limite, não há dados de previsão disponíveis
            val clampedStart = if (startDate > maxEnd) {
                Log.w(TAG, "fetchWeatherJson: start_date $startDate além do alcance de previsão — sem dados")
                null
            } else {
                startDate
            }

            if (clampedStart == null) throw Exception("Período da viagem além do alcance de previsão (16 dias)")

            val url = buildString {
                append("https://api.open-meteo.com/v1/forecast")
                append("?latitude=$lat&longitude=$lon")
                append("&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_sum")
                append("&timezone=auto")
                append("&start_date=$clampedStart&end_date=$clampedEnd")
            }
            Log.d(TAG, "fetchWeatherJson → $url")
            val response = httpGet(url)
            Log.d(TAG, "fetchWeatherJson ← ${response.take(500)}")
            response
        }

    private fun parseThenCache(key: String, json: String, timestamp: Long): Map<String, LiveWeatherDay>? {
        val result = parseJson(json)
        if (result == null) {
            Log.e(TAG, "parseThenCache: parseJson retornou null para key=$key")
        } else {
            Log.d(TAG, "parseThenCache: ${result.size} dias parseados (key=$key)")
            result.forEach { (date, day) ->
                Log.d(TAG, "  $date → min=${day.minTemp}°C max=${day.maxTemp}°C code=${day.weatherCode}")
            }
        }
        if (result == null) return null
        memoryCacheMap[key] = Pair(result, timestamp)
        return result
    }

    private fun clearDiskCache(context: Context, key: String) {
        context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove("json_$key")
            .remove("ts_$key")
            .apply()
    }

    private fun cacheKey(lat: Double, lon: Double, startDate: String, endDate: String): String =
        "${lat.toBigDecimal().toPlainString()}_${lon.toBigDecimal().toPlainString()}_${startDate}_${endDate}"

    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout    = 10_000
        return try {
            val code = connection.responseCode
            if (code in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                val body = connection.errorStream?.bufferedReader()?.readText() ?: "(sem corpo)"
                Log.e(TAG, "httpGet: HTTP $code para $url — body: ${body.take(500)}")
                throw Exception("HTTP $code: ${body.take(200)}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseJson(json: String): Map<String, LiveWeatherDay>? {
        return try {
            val root = JSONObject(json)

            // A API Open-Meteo retornou um erro?
            if (root.has("error") && root.optBoolean("error")) {
                Log.e(TAG, "parseJson: API retornou erro → reason=${root.optString("reason")}")
                return null
            }

            val daily = root.optJSONObject("daily") ?: run {
                Log.e(TAG, "parseJson: campo 'daily' ausente. JSON: ${json.take(300)}")
                return null
            }

            val times = daily.optJSONArray("time") ?: run {
                Log.e(TAG, "parseJson: campo 'daily.time' ausente")
                return null
            }

            // Suporta tanto 'weather_code' (novo) quanto 'weathercode' (legado)
            val codes = daily.optJSONArray("weather_code")
                ?: daily.optJSONArray("weathercode")
                ?: run {
                    Log.w(TAG, "parseJson: campo weather_code/weathercode ausente — usando 0")
                    null
                }

            val maxTemps = daily.optJSONArray("temperature_2m_max")
            val minTemps = daily.optJSONArray("temperature_2m_min")
            val precip   = daily.optJSONArray("precipitation_sum")

            Log.d(TAG, "parseJson: ${times.length()} datas recebidas")

            buildMap {
                for (i in 0 until times.length()) {
                    val date = times.getString(i)

                    // Se a temperatura for null na API (fora do alcance de previsão), pula o dia
                    val rawMax = maxTemps?.get(i)
                    val rawMin = minTemps?.get(i)
                    if (rawMax == null || rawMax == JSONObject.NULL ||
                        rawMin == null || rawMin == JSONObject.NULL) {
                        Log.w(TAG, "parseJson: temperatura nula para $date — dia ignorado")
                        continue
                    }

                    put(date, LiveWeatherDay(
                        date            = date,
                        minTemp         = (rawMin as Number).toDouble().roundToInt(),
                        maxTemp         = (rawMax as Number).toDouble().roundToInt(),
                        weatherCode     = codes?.optInt(i, 0) ?: 0,
                        precipitationMm = precip?.optDouble(i, 0.0) ?: 0.0
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseJson: exceção ao parsear", e)
            null
        }
    }
}
