package com.rodrigoleao.gramado2026.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.rodrigoleao.gramado2026.data.db.dao.TripDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripDaoTest {

    private lateinit var db: TravelDatabase
    private lateinit var dao: TripDao

    @Before
    fun setup() {
        db = inMemoryDb()
        dao = db.tripDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun flowGetAllTrips_emiteOrdenadoPorCreatedAt() = runBlocking {
        dao.insert(tripEntity(name = "Recente",  createdAt = 3_000))
        dao.insert(tripEntity(name = "Antiga",   createdAt = 1_000))
        dao.insert(tripEntity(name = "Do meio",  createdAt = 2_000))

        val trips = dao.getAllTrips().first()
        assertThat(trips.map { it.name }).containsExactly("Antiga", "Do meio", "Recente").inOrder()
    }

    @Test
    fun count_refleteInsercoes() = runBlocking {
        assertThat(dao.count()).isEqualTo(0)
        dao.insert(tripEntity())
        assertThat(dao.count()).isEqualTo(1)
    }

    @Test
    fun getById_nuloParaInexistente() = runBlocking {
        assertThat(dao.getById(99)).isNull()
    }

    @Test
    fun updateCoordinates_persiste() = runBlocking {
        val id = dao.insert(tripEntity())
        dao.updateCoordinates(id, -29.37, -50.87)
        val trip = dao.getById(id)!!
        assertThat(trip.latitude).isEqualTo(-29.37)
        assertThat(trip.longitude).isEqualTo(-50.87)
    }

    @Test
    fun updateVoucherSortMode_persiste() = runBlocking {
        val id = dao.insert(tripEntity())
        dao.updateVoucherSortMode(id, "BY_DAY")
        assertThat(dao.getById(id)!!.voucherSortMode).isEqualTo("BY_DAY")
    }

    @Test
    fun deletarViagem_cascateiaDiasEAtividades() = runBlocking {
        val tripId = dao.insert(tripEntity())
        val dayId = db.dayDao().insert(dayEntity(tripId = tripId))
        db.activityDao().insertActivity(activityEntity(dayId = dayId))

        dao.delete(dao.getById(tripId)!!)

        assertThat(db.dayDao().getDaysForTrip(tripId)).isEmpty()
        assertThat(db.activityDao().getActivitiesForDay(dayId)).isEmpty()
    }
}
