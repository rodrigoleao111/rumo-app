package com.rodrigoleao.gramado2026.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testes unitários de SettingsRepository — Melhoria #9.
 *
 * DataStore é testável em JVM puro com PreferenceDataStoreFactory + TemporaryFolder.
 *
 * IMPORTANTE: o DataStore mantém uma coroutine interna de longa duração para leitura do arquivo.
 * Para evitar `UncompletedCoroutinesError`, o scope passado ao DataStore deve ser
 * `testScope.backgroundScope` — que o `runTest` não aguarda ao encerrar.
 *
 * Cada teste usa um arquivo separado dentro da pasta temporária para garantir isolamento.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun buildRepo(testScope: TestScope, fileName: String): SettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { tmpFolder.newFile("$fileName.preferences_pb") }
        )
        return SettingsRepository(dataStore)
    }

    @Test
    fun autoOpenActiveTrip_default_isTrue() = runTest {
        val repo = buildRepo(this, "defaults_auto")
        assertTrue(repo.autoOpenActiveTrip.first())
    }

    @Test
    fun showEmergencyContacts_default_isTrue() = runTest {
        val repo = buildRepo(this, "defaults_emergency")
        assertTrue(repo.showEmergencyContacts.first())
    }

    @Test
    fun setAutoOpenActiveTrip_false_persistsAndEmits() = runTest {
        val repo = buildRepo(this, "auto_false")
        repo.setAutoOpenActiveTrip(false)
        assertFalse(repo.autoOpenActiveTrip.first())
    }

    @Test
    fun setAutoOpenActiveTrip_trueAfterFalse_persistsAndEmits() = runTest {
        val repo = buildRepo(this, "auto_toggle")
        repo.setAutoOpenActiveTrip(false)
        repo.setAutoOpenActiveTrip(true)
        assertTrue(repo.autoOpenActiveTrip.first())
    }

    @Test
    fun setShowEmergencyContacts_false_persistsAndEmits() = runTest {
        val repo = buildRepo(this, "emergency_false")
        repo.setShowEmergencyContacts(false)
        assertFalse(repo.showEmergencyContacts.first())
    }

    @Test
    fun setShowEmergencyContacts_doesNotAffectAutoOpen() = runTest {
        val repo = buildRepo(this, "independent_a")
        repo.setShowEmergencyContacts(false)
        assertTrue(repo.autoOpenActiveTrip.first())
    }

    @Test
    fun setAutoOpenActiveTrip_doesNotAffectShowEmergency() = runTest {
        val repo = buildRepo(this, "independent_b")
        repo.setAutoOpenActiveTrip(false)
        assertTrue(repo.showEmergencyContacts.first())
    }
}
