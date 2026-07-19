package com.rodrigoleao.gramado2026.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.rodrigoleao.gramado2026.data.db.dao.ContactDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactDaoTest {

    private lateinit var db: TravelDatabase
    private lateinit var dao: ContactDao

    @Before
    fun setup() {
        db = inMemoryDb()
        dao = db.contactDao()
        runBlocking { db.tripDao().insert(tripEntity(id = 1)) }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertEGetPorId_preservaCampos() = runBlocking {
        val id = dao.insert(contactEntity(tripId = 1, name = "Guia Ana"))
        val contact = dao.getById(id)!!
        assertThat(contact.name).isEqualTo("Guia Ana")
        assertThat(contact.phone).isEqualTo("54999990000")
        assertThat(contact.isFavorite).isFalse()
        assertThat(contact.customTypeName).isEmpty()
    }

    @Test
    fun telefoneNulo_sobreviveAoRoundTrip() = runBlocking {
        val id = dao.insert(contactEntity(tripId = 1).copy(phone = null))
        assertThat(dao.getById(id)!!.phone).isNull()
    }

    @Test
    fun ordenacao_porSortOrder() = runBlocking {
        dao.insert(contactEntity(tripId = 1, name = "Carlos", sortOrder = 2))
        dao.insert(contactEntity(tripId = 1, name = "Ana",    sortOrder = 0))
        dao.insert(contactEntity(tripId = 1, name = "Bruno",  sortOrder = 1))

        val result = dao.getContactsForTrip(1)
        assertThat(result.map { it.name }).containsExactly("Ana", "Bruno", "Carlos").inOrder()
    }

    @Test
    fun updateSortOrder_reordenaAConsulta() = runBlocking {
        val idAna = dao.insert(contactEntity(tripId = 1, name = "Ana", sortOrder = 0))
        dao.insert(contactEntity(tripId = 1, name = "Bruno", sortOrder = 1))

        dao.updateSortOrder(idAna, 9)

        assertThat(dao.getContactsForTrip(1).map { it.name })
            .containsExactly("Bruno", "Ana").inOrder()
    }

    @Test
    fun updateIsFavorite_persiste() = runBlocking {
        val id = dao.insert(contactEntity(tripId = 1))
        dao.updateIsFavorite(id, true)
        assertThat(dao.getById(id)!!.isFavorite).isTrue()
    }

    @Test
    fun deleteById_removeApenasUm() = runBlocking {
        val id = dao.insert(contactEntity(tripId = 1, name = "Ana"))
        dao.insert(contactEntity(tripId = 1, name = "Bruno", sortOrder = 1))

        dao.deleteById(id)

        val result = dao.getContactsForTrip(1)
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Bruno")
    }

    @Test
    fun deletarViagem_cascateiaContatos() = runBlocking {
        dao.insert(contactEntity(tripId = 1))
        db.tripDao().delete(db.tripDao().getById(1)!!)
        assertThat(dao.getContactsForTrip(1)).isEmpty()
    }
}
