package com.rodrigoleao.gramado2026.data.db.dao

import androidx.room.*
import com.rodrigoleao.gramado2026.data.db.entity.ChecklistItemEntity
import com.rodrigoleao.gramado2026.data.db.entity.NoteBlockEntity
import com.rodrigoleao.gramado2026.data.db.entity.NoteEntity

data class DayNoteCount(val dayId: Int, val count: Int)

@Dao
interface NoteDao {

    // ── Notes ───────────────────────────────────────────────────────────────
    // `dayId IS :dayId` cobre os dois casos: NULL (notas gerais) e = N (notas de dia).
    @Query("SELECT * FROM notes WHERE tripId = :tripId AND dayId IS :dayId ORDER BY sortOrder ASC, id ASC")
    suspend fun getNotes(tripId: Long, dayId: Int?): List<NoteEntity>

    // Todas as notas da viagem (gerais + de dia) — usado no export.
    @Query("SELECT * FROM notes WHERE tripId = :tripId ORDER BY dayId ASC, sortOrder ASC, id ASC")
    suspend fun getAllNotes(tripId: Long): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT COUNT(*) FROM notes WHERE tripId = :tripId AND dayId IS :dayId")
    suspend fun countNotes(tripId: Long, dayId: Int?): Int

    // Contagem de notas por dia (só notas de dia) — para o preview no DayDetailScreen.
    @Query("SELECT dayId AS dayId, COUNT(*) AS count FROM notes WHERE tripId = :tripId AND dayId IS NOT NULL GROUP BY dayId")
    suspend fun getDayNoteCounts(tripId: Long): List<DayNoteCount>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNote(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("UPDATE notes SET title = :title, updatedAt = :ts WHERE id = :id")
    suspend fun updateNoteTitle(id: Long, title: String, ts: Long)

    @Query("UPDATE notes SET sortOrder = :order WHERE id = :id")
    suspend fun updateNoteSortOrder(id: Long, order: Int)

    @Query("UPDATE notes SET updatedAt = :ts WHERE id = :id")
    suspend fun touchNote(id: Long, ts: Long)

    // ── Blocks ──────────────────────────────────────────────────────────────
    @Query("SELECT * FROM note_blocks WHERE noteId IN (:noteIds) ORDER BY noteId ASC, sortOrder ASC")
    suspend fun getBlocksForNotes(noteIds: List<Long>): List<NoteBlockEntity>

    @Query("SELECT * FROM note_blocks WHERE noteId = :noteId ORDER BY sortOrder ASC")
    suspend fun getBlocksForNote(noteId: Long): List<NoteBlockEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBlock(block: NoteBlockEntity): Long

    @Query("UPDATE note_blocks SET content = :content WHERE id = :id")
    suspend fun updateBlockContent(id: Long, content: String)

    @Query("UPDATE note_blocks SET sortOrder = :order WHERE id = :id")
    suspend fun updateBlockSortOrder(id: Long, order: Int)

    @Query("DELETE FROM note_blocks WHERE id = :id")
    suspend fun deleteBlock(id: Long)

    // ── Checklist items ─────────────────────────────────────────────────────
    @Query("SELECT * FROM checklist_items WHERE blockId IN (:blockIds) ORDER BY blockId ASC, sortOrder ASC")
    suspend fun getItemsForBlocks(blockIds: List<Long>): List<ChecklistItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: ChecklistItemEntity): Long

    @Query("UPDATE checklist_items SET text = :text WHERE id = :id")
    suspend fun updateItemText(id: Long, text: String)

    @Query("UPDATE checklist_items SET isChecked = :checked WHERE id = :id")
    suspend fun updateItemChecked(id: Long, checked: Boolean)

    @Query("UPDATE checklist_items SET sortOrder = :order WHERE id = :id")
    suspend fun updateItemSortOrder(id: Long, order: Int)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteItem(id: Long)
}
