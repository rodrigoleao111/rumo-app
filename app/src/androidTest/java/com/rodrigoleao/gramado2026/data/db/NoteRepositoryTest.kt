package com.rodrigoleao.gramado2026.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.rodrigoleao.gramado2026.data.model.NoteBlock
import com.rodrigoleao.gramado2026.data.model.NoteBlockType
import com.rodrigoleao.gramado2026.data.repository.NoteRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteRepositoryTest {

    private lateinit var db: TravelDatabase
    private lateinit var repo: NoteRepository

    @Before
    fun setup() {
        db = inMemoryDb()
        repo = NoteRepository(db)
        runBlocking { db.tripDao().insert(tripEntity(id = 1)) }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun createNote_retornaIdEPersiste() = runBlocking {
        val id = repo.createNote(tripId = 1, dayId = null)
        assertThat(id).isGreaterThan(0L)
        val notes = repo.getNotes(1, null)
        assertThat(notes).hasSize(1)
        assertThat(notes[0].id).isEqualTo(id)
    }

    @Test
    fun getNotes_filtraPorDayId() = runBlocking {
        repo.createNote(1, null)   // geral
        repo.createNote(1, 3)      // dia 3
        repo.createNote(1, 3)      // dia 3

        assertThat(repo.getNotes(1, null)).hasSize(1)
        assertThat(repo.getNotes(1, 3)).hasSize(2)
        assertThat(repo.getNotes(1, 5)).isEmpty()
    }

    @Test
    fun getNotes_geralNaoIncluiNotasDeDia() = runBlocking {
        repo.createNote(1, 2)
        assertThat(repo.getNotes(1, null)).isEmpty()   // dayId IS NULL não casa com dayId = 2
    }

    @Test
    fun updateNoteTitle_persisteEAtualizaTimestamp() = runBlocking {
        val id = repo.createNote(1, null)
        val before = repo.getNote(id)!!.updatedAt
        repo.updateNoteTitle(id, "Packing list")
        val after = repo.getNote(id)!!
        assertThat(after.title).isEqualTo("Packing list")
        assertThat(after.updatedAt).isAtLeast(before)
    }

    @Test
    fun addBlock_texto_apareceNaNota() = runBlocking {
        val noteId = repo.createNote(1, null)
        val blockId = repo.addBlock(noteId, NoteBlockType.TEXT)
        repo.updateBlockContent(blockId, "Lembrar do carregador")

        val note = repo.getNote(noteId)!!
        assertThat(note.blocks).hasSize(1)
        val block = note.blocks[0]
        assertThat(block).isInstanceOf(NoteBlock.TextBlock::class.java)
        assertThat((block as NoteBlock.TextBlock).content).isEqualTo("Lembrar do carregador")
    }

    @Test
    fun addBlock_checklist_nasceComUmItemVazio() = runBlocking {
        val noteId = repo.createNote(1, null)
        repo.addBlock(noteId, NoteBlockType.CHECKLIST)

        val block = repo.getNote(noteId)!!.blocks.single()
        assertThat(block).isInstanceOf(NoteBlock.ChecklistBlock::class.java)
        assertThat((block as NoteBlock.ChecklistBlock).items).hasSize(1)
    }

    @Test
    fun toggleChecklistItem_persiste() = runBlocking {
        val noteId = repo.createNote(1, null)
        val blockId = repo.addBlock(noteId, NoteBlockType.CHECKLIST)
        val itemId = (repo.getNote(noteId)!!.blocks.single() as NoteBlock.ChecklistBlock).items.single().id

        repo.toggleChecklistItem(itemId, true)

        val item = (repo.getNote(noteId)!!.blocks.single() as NoteBlock.ChecklistBlock).items.single()
        assertThat(item.isChecked).isTrue()
    }

    @Test
    fun blocosEItens_ordenadosPorSortOrder() = runBlocking {
        val noteId = repo.createNote(1, null)
        val heading = repo.addBlock(noteId, NoteBlockType.HEADING)  // sortOrder 0
        val text    = repo.addBlock(noteId, NoteBlockType.TEXT)     // sortOrder 1
        repo.updateBlockContent(heading, "Documentos")
        repo.updateBlockContent(text, "Passaporte na mochila")

        val blocks = repo.getNote(noteId)!!.blocks
        assertThat(blocks.map { it.sortOrder }).containsExactly(0, 1).inOrder()
        assertThat(blocks[0]).isInstanceOf(NoteBlock.HeadingBlock::class.java)
        assertThat(blocks[1]).isInstanceOf(NoteBlock.TextBlock::class.java)
    }

    @Test
    fun reorderNotes_atualizaSortOrder() = runBlocking {
        val a = repo.createNote(1, null)
        val b = repo.createNote(1, null)
        val c = repo.createNote(1, null)

        repo.reorderNotes(listOf(c, a, b))

        assertThat(repo.getNotes(1, null).map { it.id }).containsExactly(c, a, b).inOrder()
    }

    @Test
    fun deleteNote_cascateiaBlocosEItens() = runBlocking {
        val noteId = repo.createNote(1, null)
        repo.addBlock(noteId, NoteBlockType.CHECKLIST)
        repo.addBlock(noteId, NoteBlockType.TEXT)

        repo.deleteNote(noteId)

        assertThat(repo.getNote(noteId)).isNull()
        // sem órfãos: uma nova nota não herda blocos
        val fresh = repo.createNote(1, null)
        assertThat(repo.getNote(fresh)!!.blocks).isEmpty()
    }

    @Test
    fun deletarViagem_cascateiaNotas() = runBlocking {
        repo.createNote(1, null)
        db.tripDao().delete(db.tripDao().getById(1)!!)
        assertThat(repo.getNotes(1, null)).isEmpty()
    }

    @Test
    fun countNotes_porEscopo() = runBlocking {
        repo.createNote(1, null)
        repo.createNote(1, 4)
        assertThat(repo.countNotes(1, null)).isEqualTo(1)
        assertThat(repo.countNotes(1, 4)).isEqualTo(1)
    }

    @Test
    fun dayNoteCounts_agrupaPorDiaEIgnoraGerais() = runBlocking {
        repo.createNote(1, null)   // geral — não entra na contagem por dia
        repo.createNote(1, 2)
        repo.createNote(1, 2)
        repo.createNote(1, 5)

        val counts = repo.dayNoteCounts(1)

        assertThat(counts.keys).containsExactly(2, 5)
        assertThat(counts[2]).isEqualTo(2)
        assertThat(counts[5]).isEqualTo(1)
    }
}
