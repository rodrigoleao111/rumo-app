package com.rodrigoleao.gramado2026.data.repository

import androidx.room.withTransaction
import com.rodrigoleao.gramado2026.data.db.TravelDatabase
import com.rodrigoleao.gramado2026.data.db.contentText
import com.rodrigoleao.gramado2026.data.db.entity.ChecklistItemEntity
import com.rodrigoleao.gramado2026.data.db.entity.NoteBlockEntity
import com.rodrigoleao.gramado2026.data.db.entity.NoteEntity
import com.rodrigoleao.gramado2026.data.db.toDomain
import com.rodrigoleao.gramado2026.data.db.typeName
import com.rodrigoleao.gramado2026.data.model.Note
import com.rodrigoleao.gramado2026.data.model.NoteBlock
import com.rodrigoleao.gramado2026.data.model.NoteBlockType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório de notas (F4). Segue o padrão do app: `suspend` que retorna domain
 * models montados em bloco (não `Flow`) — o ViewModel recarrega via `refresh()`.
 */
@Singleton
class NoteRepository @Inject constructor(private val db: TravelDatabase) {

    private val dao get() = db.noteDao()
    private fun now() = System.currentTimeMillis()

    // ── Leitura ─────────────────────────────────────────────────────────────

    /** Notas (com blocos e itens) de um escopo. `dayId = null` → notas gerais da viagem. */
    suspend fun getNotes(tripId: Long, dayId: Int?): List<Note> {
        val notes = dao.getNotes(tripId, dayId)
        if (notes.isEmpty()) return emptyList()
        return assemble(notes)
    }

    suspend fun getNote(id: Long): Note? {
        val note = dao.getNoteById(id) ?: return null
        return assemble(listOf(note)).firstOrNull()
    }

    /** Todas as notas da viagem (gerais + de dia), montadas — usado no export. */
    suspend fun getAllNotes(tripId: Long): List<Note> {
        val notes = dao.getAllNotes(tripId)
        if (notes.isEmpty()) return emptyList()
        return assemble(notes)
    }

    /** Insere uma nota completa preservando estrutura e timestamps — usado no import. */
    suspend fun insertImportedNote(tripId: Long, note: Note): Long = db.withTransaction {
        val noteId = dao.insertNote(
            NoteEntity(
                tripId = tripId, dayId = note.dayId, title = note.title,
                sortOrder = note.sortOrder, createdAt = note.createdAt, updatedAt = note.updatedAt
            )
        )
        note.blocks.forEach { block ->
            val blockId = dao.insertBlock(
                NoteBlockEntity(noteId = noteId, type = block.typeName(), content = block.contentText(), sortOrder = block.sortOrder)
            )
            if (block is NoteBlock.ChecklistBlock) {
                block.items.forEach { item ->
                    dao.insertItem(ChecklistItemEntity(blockId = blockId, text = item.text, isChecked = item.isChecked, sortOrder = item.sortOrder))
                }
            }
        }
        noteId
    }

    suspend fun countNotes(tripId: Long, dayId: Int?): Int = dao.countNotes(tripId, dayId)

    // Monta Note completo com 3 queries bulk (notes → blocks → items), sem N+1.
    private suspend fun assemble(notes: List<NoteEntity>): List<Note> {
        val blocks = dao.getBlocksForNotes(notes.map { it.id })
        val items  = if (blocks.isEmpty()) emptyList()
                     else dao.getItemsForBlocks(blocks.map { it.id })
        val itemsByBlock = items.groupBy { it.blockId }
        val blocksByNote = blocks.groupBy { it.noteId }
        return notes.map { n ->
            val domainBlocks = (blocksByNote[n.id] ?: emptyList()).map { b ->
                b.toDomain((itemsByBlock[b.id] ?: emptyList()).map { it.toDomain() })
            }
            n.toDomain(domainBlocks)
        }
    }

    // ── Notas ───────────────────────────────────────────────────────────────

    suspend fun createNote(tripId: Long, dayId: Int?): Long {
        val ts = now()
        return dao.insertNote(
            NoteEntity(
                tripId = tripId, dayId = dayId, title = "",
                sortOrder = dao.countNotes(tripId, dayId), createdAt = ts, updatedAt = ts
            )
        )
    }

    suspend fun updateNoteTitle(id: Long, title: String) = dao.updateNoteTitle(id, title, now())

    suspend fun deleteNote(id: Long) = dao.deleteNote(id)   // CASCADE remove blocos + itens

    suspend fun touchNote(id: Long) = dao.touchNote(id, now())

    suspend fun reorderNotes(orderedIds: List<Long>) = db.withTransaction {
        orderedIds.forEachIndexed { index, id -> dao.updateNoteSortOrder(id, index) }
    }

    // ── Blocos ──────────────────────────────────────────────────────────────

    /** Cria um bloco vazio ao fim da nota. Checklist já nasce com 1 item vazio (UC-F4-04). */
    suspend fun addBlock(noteId: Long, type: NoteBlockType): Long = db.withTransaction {
        val order   = dao.getBlocksForNote(noteId).size
        val blockId = dao.insertBlock(NoteBlockEntity(noteId = noteId, type = type.name, content = "", sortOrder = order))
        if (type == NoteBlockType.CHECKLIST) {
            dao.insertItem(ChecklistItemEntity(blockId = blockId, text = "", isChecked = false, sortOrder = 0))
        }
        dao.touchNote(noteId, now())
        blockId
    }

    suspend fun updateBlockContent(id: Long, content: String) = dao.updateBlockContent(id, content)

    suspend fun deleteBlock(id: Long) = dao.deleteBlock(id)  // CASCADE remove itens

    suspend fun reorderBlocks(orderedIds: List<Long>) = db.withTransaction {
        orderedIds.forEachIndexed { index, id -> dao.updateBlockSortOrder(id, index) }
    }

    // ── Itens de checklist ──────────────────────────────────────────────────

    suspend fun addChecklistItem(blockId: Long): Long {
        val order = dao.getItemsForBlocks(listOf(blockId)).size
        return dao.insertItem(ChecklistItemEntity(blockId = blockId, text = "", isChecked = false, sortOrder = order))
    }

    suspend fun updateItemText(id: Long, text: String) = dao.updateItemText(id, text)

    suspend fun toggleChecklistItem(id: Long, checked: Boolean) = dao.updateItemChecked(id, checked)

    suspend fun deleteChecklistItem(id: Long) = dao.deleteItem(id)

    suspend fun reorderChecklistItems(orderedIds: List<Long>) = db.withTransaction {
        orderedIds.forEachIndexed { index, id -> dao.updateItemSortOrder(id, index) }
    }
}
