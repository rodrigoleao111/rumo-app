package com.rodrigoleao.gramado2026.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "voucher_groups",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tripId")]
)
data class VoucherGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val name: String
)
