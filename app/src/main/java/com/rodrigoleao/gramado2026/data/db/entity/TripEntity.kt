package com.rodrigoleao.gramado2026.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val destination: String,
    val coverEmoji: String,
    val hotelName: String,
    val hotelAddress: String,
    val hotelPhone: String = "",
    val startDate: String? = null,   // ISO: "2026-06-09"
    val endDate: String? = null,     // ISO: "2026-06-13"
    val createdAt: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val voucherSortMode: String = "BY_CATEGORY",
    val tripUuid: String = "",         // UUID estável, preservado em export/import (F1)
    val lastEditedAt: Long = 0L        // unix ms da última edição de conteúdo (F1)
)
