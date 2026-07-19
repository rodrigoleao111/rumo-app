package com.rodrigoleao.gramado2026.ui.edit

import androidx.lifecycle.SavedStateHandle
import com.rodrigoleao.gramado2026.data.db.entity.ContactEntity
import com.rodrigoleao.gramado2026.data.model.UiEvent
import com.rodrigoleao.gramado2026.data.preferences.ContactCategoryRepository
import com.rodrigoleao.gramado2026.data.repository.ContactRepository
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import com.rodrigoleao.gramado2026.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Testes unitários de UiEvent nos ViewModels de edição — Melhoria #7.
 *
 * Cobre:
 * - NavigateBack emitido em save() com sucesso
 * - ShowSnackbar emitido em save() com falha
 * - NavigateBack / NavigateAfterDelete emitido em delete() com sucesso
 * - ShowSnackbar emitido em delete() com falha
 * - isSaving resetado para false em caso de falha
 *
 * Convenção de nomenclatura: método_condição_resultado
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditViewModelUiEventTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var contactRepo: ContactRepository
    private lateinit var categoryRepo: ContactCategoryRepository
    private lateinit var tripRepo: TripRepository

    @Before
    fun setup() {
        contactRepo  = mockk()
        categoryRepo = mockk()
        tripRepo     = mockk()
        coEvery { categoryRepo.getCustomCategories() } returns emptyList()
        coEvery { contactRepo.getContactEntity(0L) } returns null
        coEvery { tripRepo.touchLastEditedAt(any()) } just Runs
    }

    // ── EditContactViewModel ──────────────────────────────────────────────────

    private fun buildContactVm(contactId: Long = 0L) =
        EditContactViewModel(contactRepo, categoryRepo, tripRepo, SavedStateHandle(mapOf("tripId" to 1L, "contactId" to contactId)))

    @Test
    fun save_success_emitsNavigateBack() = runTest {
        val vm = buildContactVm()
        vm.updateName("João")
        coEvery { contactRepo.upsertContact(any(), any()) } returns 1L

        val events = mutableListOf<UiEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiEvent.toList(events)
        }

        vm.save()
        advanceUntilIdle()

        assertIs<UiEvent.NavigateBack>(events.firstOrNull())
        job.cancel()
    }

    @Test
    fun save_failure_emitsShowSnackbar() = runTest {
        val vm = buildContactVm()
        vm.updateName("João")
        coEvery { contactRepo.upsertContact(any(), any()) } throws RuntimeException("DB error")

        val events = mutableListOf<UiEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiEvent.toList(events)
        }

        vm.save()
        advanceUntilIdle()

        assertIs<UiEvent.ShowSnackbar>(events.firstOrNull())
        job.cancel()
    }

    @Test
    fun save_failure_resetsisSaving() = runTest {
        val vm = buildContactVm()
        vm.updateName("João")
        coEvery { contactRepo.upsertContact(any(), any()) } throws RuntimeException("DB error")

        vm.save()
        advanceUntilIdle()

        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun delete_success_emitsNavigateBack() = runTest {
        coEvery { contactRepo.getContactEntity(10L) } returns ContactEntity(
            id = 10L, tripId = 1L, name = "X", role = "", phone = null,
            contactType = "CUSTOM", customTypeName = "", hasWhatsApp = false, isEmergency = false
        )
        val vm = buildContactVm(contactId = 10L)
        advanceUntilIdle()
        coEvery { contactRepo.deleteContact(10L) } just Runs

        val events = mutableListOf<UiEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiEvent.toList(events)
        }

        vm.delete()
        advanceUntilIdle()

        assertIs<UiEvent.NavigateBack>(events.firstOrNull())
        job.cancel()
    }

    @Test
    fun delete_failure_emitsShowSnackbar() = runTest {
        coEvery { contactRepo.getContactEntity(10L) } returns ContactEntity(
            id = 10L, tripId = 1L, name = "X", role = "", phone = null,
            contactType = "CUSTOM", customTypeName = "", hasWhatsApp = false, isEmergency = false
        )
        val vm = buildContactVm(contactId = 10L)
        advanceUntilIdle()
        coEvery { contactRepo.deleteContact(10L) } throws RuntimeException("DB error")

        val events = mutableListOf<UiEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiEvent.toList(events)
        }

        vm.delete()
        advanceUntilIdle()

        assertIs<UiEvent.ShowSnackbar>(events.firstOrNull())
        job.cancel()
    }

    // ── EditTripViewModel ─────────────────────────────────────────────────────

    @Test
    fun editTrip_deleteTrip_success_emitsNavigateAfterDelete() = runTest {
        val entity = com.rodrigoleao.gramado2026.data.db.entity.TripEntity(
            id = 1L, name = "Trip", destination = "Dest", coverEmoji = "🌲",
            hotelName = "", hotelAddress = "", hotelPhone = "",
            startDate = null, endDate = null, latitude = null, longitude = null,
            voucherSortMode = "BY_CATEGORY"
        )
        coEvery { tripRepo.getTripEntity(1L) } returns entity
        coEvery { tripRepo.deleteTrip(any()) } just Runs

        val vm = EditTripViewModel(tripRepo, SavedStateHandle(mapOf("tripId" to 1L)))
        advanceUntilIdle()

        val events = mutableListOf<UiEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiEvent.toList(events)
        }

        vm.deleteTrip()
        advanceUntilIdle()

        assertIs<UiEvent.NavigateAfterDelete>(events.firstOrNull())
        job.cancel()
    }

    @Test
    fun editTrip_deleteTrip_failure_resetIsDeleting() = runTest {
        val entity = com.rodrigoleao.gramado2026.data.db.entity.TripEntity(
            id = 1L, name = "Trip", destination = "Dest", coverEmoji = "🌲",
            hotelName = "", hotelAddress = "", hotelPhone = "",
            startDate = null, endDate = null, latitude = null, longitude = null,
            voucherSortMode = "BY_CATEGORY"
        )
        coEvery { tripRepo.getTripEntity(1L) } returns entity
        coEvery { tripRepo.deleteTrip(any()) } throws RuntimeException("DB error")

        val vm = EditTripViewModel(tripRepo, SavedStateHandle(mapOf("tripId" to 1L)))
        advanceUntilIdle()

        vm.deleteTrip()
        advanceUntilIdle()

        assertFalse(vm.state.value.isDeleting)
    }
}
