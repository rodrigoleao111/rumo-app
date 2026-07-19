package com.rodrigoleao.gramado2026.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.rodrigoleao.gramado2026.data.db.dao.VoucherDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VoucherDaoTest {

    private lateinit var db: TravelDatabase
    private lateinit var dao: VoucherDao

    @Before
    fun setup() {
        db = inMemoryDb()
        dao = db.voucherDao()
        runBlocking { db.tripDao().insert(tripEntity(id = 1)) }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertEGetParaViagem() = runBlocking {
        dao.insert(voucherEntity(tripId = 1, name = "Bondinho"))
        val result = dao.getVouchersForTrip(1)
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Bondinho")
        assertThat(result[0].isUsed).isFalse()
    }

    @Test
    fun ordenacao_grupoDepoisSortOrderDepoisId() = runBlocking {
        // Inseridos fora de ordem de propósito
        dao.insert(voucherEntity(tripId = 1, name = "Zoo",      groupName = "Passeios",     sortOrder = 1))
        dao.insert(voucherEntity(tripId = 1, name = "Jantar",   groupName = "Alimentação",  sortOrder = 0))
        dao.insert(voucherEntity(tripId = 1, name = "Bondinho", groupName = "Passeios",     sortOrder = 0))

        val result = dao.getVouchersForTrip(1)
        // ORDER BY groupName ASC, sort_order ASC, id ASC
        assertThat(result.map { it.name }).containsExactly("Jantar", "Bondinho", "Zoo").inOrder()
    }

    @Test
    fun updateSortOrder_persiste() = runBlocking {
        val id = dao.insert(voucherEntity(tripId = 1, sortOrder = 0))
        dao.updateSortOrder(id, 5)
        assertThat(dao.getById(id)!!.sortOrder).isEqualTo(5)
    }

    @Test
    fun updateIsUsed_persisteIdaEVolta() = runBlocking {
        val id = dao.insert(voucherEntity(tripId = 1))
        dao.updateIsUsed(id, true)
        assertThat(dao.getById(id)!!.isUsed).isTrue()
        dao.updateIsUsed(id, false)
        assertThat(dao.getById(id)!!.isUsed).isFalse()
    }

    @Test
    fun maxSortOrderNoGrupo_menosUmParaGrupoVazio() = runBlocking {
        dao.insert(voucherEntity(tripId = 1, groupName = "Passeios", sortOrder = 3))
        assertThat(dao.getMaxSortOrderInGroup(1, "Passeios")).isEqualTo(3)
        assertThat(dao.getMaxSortOrderInGroup(1, "GrupoInexistente")).isEqualTo(-1)
    }

    @Test
    fun deleteById_removeApenasUm() = runBlocking {
        val id1 = dao.insert(voucherEntity(tripId = 1, name = "Bondinho", sortOrder = 0))
        dao.insert(voucherEntity(tripId = 1, name = "Dreamland", sortOrder = 1))

        dao.deleteById(id1)

        val result = dao.getVouchersForTrip(1)
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Dreamland")
    }

    @Test
    fun deletarViagem_cascateiaVouchers() = runBlocking {
        dao.insert(voucherEntity(tripId = 1))
        db.tripDao().delete(db.tripDao().getById(1)!!)
        assertThat(dao.getVouchersForTrip(1)).isEmpty()
    }
}
