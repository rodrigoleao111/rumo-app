package com.rodrigoleao.gramado2026.data.preferences

import android.content.Context
import androidx.core.content.edit

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("rumo_settings", Context.MODE_PRIVATE)

    var autoOpenActiveTrip: Boolean
        get()      = prefs.getBoolean(KEY_AUTO_OPEN, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_OPEN, value) }

    companion object {
        private const val KEY_AUTO_OPEN = "auto_open_active_trip"
    }
}
