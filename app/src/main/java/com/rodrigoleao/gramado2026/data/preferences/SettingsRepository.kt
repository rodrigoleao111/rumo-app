package com.rodrigoleao.gramado2026.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "rumo_settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val autoOpenActiveTrip: Flow<Boolean> = dataStore.data
        .map { it[KEY_AUTO_OPEN] ?: true }

    val showEmergencyContacts: Flow<Boolean> = dataStore.data
        .map { it[KEY_EMERGENCY_CONTACTS] ?: true }

    suspend fun setAutoOpenActiveTrip(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_OPEN] = enabled }
    }

    suspend fun setShowEmergencyContacts(enabled: Boolean) {
        dataStore.edit { it[KEY_EMERGENCY_CONTACTS] = enabled }
    }

    companion object {
        private val KEY_AUTO_OPEN          = booleanPreferencesKey("auto_open_active_trip")
        private val KEY_EMERGENCY_CONTACTS = booleanPreferencesKey("show_emergency_contacts")
    }
}
