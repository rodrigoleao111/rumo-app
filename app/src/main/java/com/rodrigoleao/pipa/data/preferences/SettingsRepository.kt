package com.rodrigoleao.pipa.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "pipa_settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val autoOpenActiveTrip: Flow<Boolean> = dataStore.data
        .map { it[KEY_AUTO_OPEN] ?: true }

    val showEmergencyContacts: Flow<Boolean> = dataStore.data
        .map { it[KEY_EMERGENCY_CONTACTS] ?: true }

    val sortTripsByProximity: Flow<Boolean> = dataStore.data
        .map { it[KEY_SORT_BY_PROXIMITY] ?: true }

    val hideCompletedTrips: Flow<Boolean> = dataStore.data
        .map { it[KEY_HIDE_COMPLETED] ?: false }

    suspend fun setAutoOpenActiveTrip(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_OPEN] = enabled }
    }

    suspend fun setShowEmergencyContacts(enabled: Boolean) {
        dataStore.edit { it[KEY_EMERGENCY_CONTACTS] = enabled }
    }

    suspend fun setSortTripsByProximity(enabled: Boolean) {
        dataStore.edit { it[KEY_SORT_BY_PROXIMITY] = enabled }
    }

    suspend fun setHideCompletedTrips(enabled: Boolean) {
        dataStore.edit { it[KEY_HIDE_COMPLETED] = enabled }
    }

    // ── Cap diário de conversas com IA (por dispositivo) ────────────────────────
    // Armazena a data (yyyy-MM-dd) e a contagem do dia. A contagem "zera" sozinha
    // quando a data guardada não é mais o `today` informado pelo chamador.

    /** Conversas com IA já iniciadas hoje (0 se a data guardada não for `today`). */
    fun aiConversationsToday(today: String): Flow<Int> = dataStore.data
        .map { prefs -> if (prefs[KEY_AI_CONV_DATE] == today) (prefs[KEY_AI_CONV_COUNT] ?: 0) else 0 }

    /** Incrementa o contador do dia, reiniciando se virou o dia. */
    suspend fun incrementAiConversations(today: String) {
        dataStore.edit { prefs ->
            val current = if (prefs[KEY_AI_CONV_DATE] == today) (prefs[KEY_AI_CONV_COUNT] ?: 0) else 0
            prefs[KEY_AI_CONV_DATE]  = today
            prefs[KEY_AI_CONV_COUNT] = current + 1
        }
    }

    companion object {
        private val KEY_AUTO_OPEN          = booleanPreferencesKey("auto_open_active_trip")
        private val KEY_EMERGENCY_CONTACTS = booleanPreferencesKey("show_emergency_contacts")
        private val KEY_SORT_BY_PROXIMITY  = booleanPreferencesKey("sort_trips_by_proximity")
        private val KEY_HIDE_COMPLETED     = booleanPreferencesKey("hide_completed_trips")
        private val KEY_AI_CONV_DATE       = stringPreferencesKey("ai_conversations_date")
        private val KEY_AI_CONV_COUNT      = intPreferencesKey("ai_conversations_count")
    }
}
