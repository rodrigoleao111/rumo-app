package com.rodrigoleao.gramado2026.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tripId")]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val name: String,
    val role: String,
    val phone: String?,
    val contactType: String,    // ContactType.name
    val hasWhatsApp: Boolean,
    val isEmergency: Boolean,
    val customTypeName: String = "",
    val sortOrder: Int = 0,
    val isFavorite: Boolean = false
)
