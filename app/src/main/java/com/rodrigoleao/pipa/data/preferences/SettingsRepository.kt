package com.rodrigoleao.pipa.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
        .map { it[KEY_SORT_BY_PROXIMITY] ?: false }

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

    companion object {
        private val KEY_AUTO_OPEN          = booleanPreferencesKey("auto_open_active_trip")
        private val KEY_EMERGENCY_CONTACTS = booleanPreferencesKey("show_emergency_contacts")
        private val KEY_SORT_BY_PROXIMITY  = booleanPreferencesKey("sort_trips_by_proximity")
        private val KEY_HIDE_COMPLETED     = booleanPreferencesKey("hide_completed_trips")
    }
}
