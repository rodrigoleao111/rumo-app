package com.rodrigoleao.gramado2026.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.rodrigoleao.gramado2026.data.db.dao.TravelActivityDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TravelActivityDaoTest {

    private lateinit var db: TravelDatabase
    private lateinit var dao: TravelActivityDao

    @Before
    fun setup() {
        db = inMemoryDb()
        dao = db.activityDao()
        runBlocking {
            db.tripDao().insert(tripEntity(id = 1))
            db.dayDao().insert(dayEntity(id = 1, tripId = 1, dayNumber = 1))
            db.dayDao().insert(dayEntity(id = 2, tripId = 1, dayNumber = 2))
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun getActivitiesForDay_ordenaPorPosition() = runBlocking {
        dao.insertActivity(activityEntity(dayId = 1, position = 1, name = "Almoço"))
        dao.insertActivity(activityEntity(dayId = 1, position = 0, name = "Café"))

        val result = dao.getActivitiesForDay(1)
        assertThat(result.map { it.name }).containsExactly("Café", "Almoço").inOrder()
    }

    @Test
    fun bulkGetActivitiesForDays_ordenaPorDiaEPosicao() = runBlocking {
        dao.insertActivity(activityEntity(dayId = 2, position = 0, name = "Dia2-A"))
        dao.insertActivity(activityEntity(dayId = 1, position = 1, name = "Dia1-B"))
        dao.insertActivity(activityEntity(dayId = 1, position = 0, name = "Dia1-A"))

        val result = dao.getActivitiesForDays(listOf(1, 2))
        // ORDER BY dayId ASC, position ASC
        assertThat(result.map { it.name }).containsExactly("Dia1-A", "Dia1-B", "Dia2-A").inOrder()
    }

    @Test  // runBlocking<Unit>: containsExactly() retorna Ordered — método @Test precisa ser void
    fun bulkGetBadgesForActivities_retornaDeTodasAsAtividades() = runBlocking<Unit> {
        val a1 = dao.insertActivity(activityEntity(dayId = 1, position = 0))
        val a2 = dao.insertActivity(activityEntity(dayId = 1, position = 1))
        dao.insertBadge(badgeEntity(activityId = a1, label = "GRÁTIS"))
        dao.insertBadge(badgeEntity(activityId = a2, label = "RESERVADO"))

        val result = dao.getBadgesForActivities(listOf(a1, a2))
        assertThat(result.map { it.label }).containsExactly("GRÁTIS", "RESERVADO")
    }

    @Test
    fun bulkGetWalkStopsForActivities_ordenaPorAtividadeEPosicao() = runBlocking {
        val a1 = dao.insertActivity(activityEntity(dayId = 1, position = 0))
        dao.insertWalkStop(walkStopEntity(activityId = a1, position = 1, label = "Parada B"))
        dao.insertWalkStop(walkStopEntity(activityId = a1, position = 0, label = "Parada A"))

        val result = dao.getWalkStopsForActivities(listOf(a1))
        assertThat(result.map { it.label }).containsExactly("Parada A", "Parada B").inOrder()
    }

    @Test
    fun insertBadges_emLote() = runBlocking {
        val a1 = dao.insertActivity(activityEntity(dayId = 1))
        dao.insertBadges(listOf(badgeEntity(activityId = a1, label = "B1"), badgeEntity(activityId = a1, label = "B2")))
        assertThat(dao.getBadgesForActivity(a1)).hasSize(2)
    }

    @Test
    fun deleteBadgesForActivity_limpaSomenteDaAtividade() = runBlocking {
        val a1 = dao.insertActivity(activityEntity(dayId = 1, position = 0))
        val a2 = dao.insertActivity(activityEntity(dayId = 1, position = 1))
        dao.insertBadge(badgeEntity(activityId = a1))
        dao.insertBadge(badgeEntity(activityId = a2))

        dao.deleteBadgesForActivity(a1)

        assertThat(dao.getBadgesForActivity(a1)).isEmpty()
        assertThat(dao.getBadgesForActivity(a2)).hasSize(1)
    }

    @Test
    fun updatePosition_persiste() = runBlocking {
        val id = dao.insertActivity(activityEntity(dayId = 1, position = 0))
        dao.updatePosition(id, 7)
        assertThat(dao.getById(id)!!.position).isEqualTo(7)
    }

    @Test
    fun countForDay_contaSomenteODia() = runBlocking {
        dao.insertActivity(activityEntity(dayId = 1, position = 0))
        dao.insertActivity(activityEntity(dayId = 1, position = 1))
        dao.insertActivity(activityEntity(dayId = 2, position = 0))
        assertThat(dao.countForDay(1)).isEqualTo(2)
    }

    @Test
    fun deletarAtividade_cascateiaBadgesEWalkStops() = runBlocking {
        val a1 = dao.insertActivity(activityEntity(dayId = 1))
        dao.insertBadge(badgeEntity(activityId = a1))
        dao.insertWalkStop(walkStopEntity(activityId = a1))

        dao.deleteById(a1)

        assertThat(dao.getBadgesForActivity(a1)).isEmpty()
        assertThat(dao.getWalkStopsForActivity(a1)).isEmpty()
    }
}
