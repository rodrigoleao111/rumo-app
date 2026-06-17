package com.rodrigoleao.gramado2026.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "walk_stops",
    foreignKeys = [ForeignKey(
        entity = TravelActivityEntity::class,
        parentColumns = ["id"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("activityId")]
)
data class WalkStopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: Long,
    val position: Int,
    val emoji: String,
    val label: String,
    val sublabel: String?,
    val isLast: Boolean
)
