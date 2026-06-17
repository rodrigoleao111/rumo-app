package com.rodrigoleao.gramado2026.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "travel_days",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tripId")]
)
data class TravelDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val dayNumber: Int,         // 1-N (ordem do dia na viagem)
    val date: String,           // ISO: "2026-06-09"
    val dayOfWeek: String,
    val title: String,
    val weatherEmoji: String,
    val minTemp: Int,
    val maxTemp: Int,
    val weatherCondition: String,
    val dayAlert: String?,
    val dayLinkUrl: String? = null,
    val dayLinkLabel: String = "",
    val dayDocumentPath: String? = null,
    val dayDocumentName: String = "",
    val dayDocumentTitle: String = ""
)
