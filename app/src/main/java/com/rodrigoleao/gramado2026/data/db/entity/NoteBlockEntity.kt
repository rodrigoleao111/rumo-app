package com.rodrigoleao.gramado2026.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_blocks",
    foreignKeys = [ForeignKey(
        entity = NoteEntity::class,
        parentColumns = ["id"],
        childColumns = ["noteId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("noteId")]
)
data class NoteBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val type: String,              // NoteBlockType.name — TEXT | CHECKLIST | HEADING
    val content: String = "",      // texto do TEXT/HEADING; vazio para CHECKLIST
    val sortOrder: Int = 0
)
