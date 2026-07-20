package com.rodrigoleao.gramado2026.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tripId")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val dayId: Int? = null,        // null = nota geral da viagem; senão = dayNumber (1-N)
    val title: String = "",
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
