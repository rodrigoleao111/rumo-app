package com.rodrigoleao.gramado2026.data.preferences

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

class ContactCategoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("contact_categories", Context.MODE_PRIVATE)

    fun getCustomCategories(): List<String> {
        val json = prefs.getString(KEY, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    fun addCategory(name: String) {
        val current = getCustomCategories().toMutableList()
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !current.contains(trimmed)) {
            current.add(trimmed)
            val arr = JSONArray().apply { current.forEach { put(it) } }
            prefs.edit { putString(KEY, arr.toString()) }
        }
    }

    companion object { private const val KEY = "custom_categories" }
}
