package com.rodrigoleao.gramado2026.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "travel_activities",
    foreignKeys = [ForeignKey(
        entity = TravelDayEntity::class,
        parentColumns = ["id"],
        childColumns = ["dayId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("dayId")]
)
data class TravelActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val position: Int,
    val time: String,
    val emoji: String,
    val name: String,
    val detail: String,
    val mapQuery: String?,
    val uberDestination: String?
)
