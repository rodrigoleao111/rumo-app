package com.rodrigoleao.gramado2026.ui.trips

import androidx.lifecycle.SavedStateHandle
import com.rodrigoleao.gramado2026.data.repository.ActivityRepository
import com.rodrigoleao.gramado2026.data.repository.ContactRepository
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import com.rodrigoleao.gramado2026.data.repository.VoucherRepository
import com.rodrigoleao.gramado2026.util.MainDispatcherRule
import com.rodrigoleao.gramado2026.util.fakeContact
import com.rodrigoleao.gramado2026.util.fakeTripData
import com.rodrigoleao.gramado2026.util.fakeVoucher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testes unitários de TripViewModel.
 *
 * Cobre:
 * - Melhoria #4: deleteContact, reorderContacts, toggleFavoriteContact,
 *               deleteActivity, swapActivityPositions
 * - Melhoria #6: atualização otimista — estado correto ANTES de advanceUntilIdle
 *               para todas as operações de lista
 *
 * Convenção de nomenclatura: método_condiçãoOuEntrada_resultadoEsperado
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TripViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var tripRepo: TripRepository
    private lateinit var voucherRepo: VoucherRepository
    private lateinit var contactRepo: ContactRepository
    private lateinit var activityRepo: ActivityRepository
    private lateinit var noteRepo: com.rodrigoleao.gramado2026.data.repository.NoteRepository
    private val tripId = 1L

    @Before
    fun setup() {
        tripRepo     = mockk()
        voucherRepo  = mockk()
        contactRepo  = mockk()
        activityRepo = mockk()
        noteRepo     = mockk()
        coEvery { tripRepo.touchLastEditedAt(any()) } just Runs   // F1: chamado por toda mutação
        coEvery { noteRepo.getNotes(any(), any()) } returns emptyList()   // F4: notas gerais no load
        coEvery { noteRepo.dayNoteCounts(any()) } returns emptyMap()      // F4: contagem por dia
    }

    private fun buildVm(initialData: com.rodrigoleao.gramado2026.data.repository.TripData = fakeTripData()): TripViewModel {
        coEvery { tripRepo.getTripData(tripId) } returns initialData
        return TripViewModel(tripRepo, voucherRepo, contactRepo, activityRepo, noteRepo, SavedStateHandle(mapOf("tripId" to tripId)))
    }

    // ── Carregamento inicial ───────────────────────────────────────────────────

    @Test
    fun `init carrega tripData do repositorio`() = runTest {
        val data = fakeTripData(contacts = listOf(fakeContact(1L), fakeContact(2L)))
        val vm = buildVm(data)
        advanceUntilIdle()

        assertEquals(2, vm.tripData.value?.contacts?.size)
    }

    // ── deleteContact — Melhoria #6: otimista ─────────────────────────────────

    @Test
    fun `deleteContact remove contato do estado imediatamente sem aguardar banco`() = runTest {
        val contacts = listOf(fakeContact(1L), fakeContact(2L))
        val vm = buildVm(fakeTripData(contacts = contacts))
        advanceUntilIdle()

        coEvery { contactRepo.deleteContact(1L) } just Runs

        vm.deleteContact(1L)

        assertEquals(1, vm.tripData.value?.contacts?.size)
        assertEquals(2L, vm.tripData.value?.contacts?.first()?.id)
    }

    @Test
    fun `deleteContact persiste no repositorio apos atualizar estado`() = runTest {
        val vm = buildVm(fakeTripData(contacts = listOf(fakeContact(99L))))
        advanceUntilIdle()

        coEvery { contactRepo.deleteContact(99L) } just Runs

        vm.deleteContact(99L)
        advanceUntilIdle()

        coVerify(exactly = 1) { contactRepo.deleteContact(99L) }
    }

    @Test
    fun `deleteContact nao recarrega tripData do banco`() = runTest {
        val vm = buildVm(fakeTripData(contacts = listOf(fakeContact(1L))))
        advanceUntilIdle()

        coEvery { contactRepo.deleteContact(any()) } just Runs

        vm.deleteContact(1L)
        advanceUntilIdle()

        coVerify(exactly = 1) { tripRepo.getTripData(tripId) }
    }

    // ── reorderContacts — Melhoria #6: otimista ───────────────────────────────

    @Test
    fun `reorderContacts atualiza estado imediatamente sem aguardar banco`() = runTest {
        val c1 = fakeContact(1L, sortOrder = 0)
        val c2 = fakeContact(2L, sortOrder = 1)
        val vm = buildVm(fakeTripData(contacts = listOf(c1, c2)))
        advanceUntilIdle()

        coEvery { contactRepo.reorderContacts(any()) } just Runs

        val reordered = listOf(c2.copy(sortOrder = 0), c1.copy(sortOrder = 1))
        vm.reorderContacts(reordered)

        assertEquals(2L, vm.tripData.value?.contacts?.first()?.id)
        assertEquals(1L, vm.tripData.value?.contacts?.last()?.id)
    }

    @Test
    fun `reorderContacts persiste no repositorio`() = runTest {
        val vm = buildVm()
        advanceUntilIdle()

        val reordered = listOf(fakeContact(2L), fakeContact(1L))
        coEvery { contactRepo.reorderContacts(reordered) } just Runs

        vm.reorderContacts(reordered)
        advanceUntilIdle()

        coVerify(exactly = 1) { contactRepo.reorderContacts(reordered) }
    }

    @Test
    fun `reorderContacts nao chama getTripData`() = runTest {
        val vm = buildVm()
        advanceUntilIdle()

        coEvery { contactRepo.reorderContacts(any()) } just Runs

        vm.reorderContacts(emptyList())
        advanceUntilIdle()

        coVerify(exactly = 1) { tripRepo.getTripData(tripId) }
    }

    // ── toggleFavoriteContact — Melhoria #6: otimista ─────────────────────────

    @Test
    fun `toggleFavoriteContact atualiza isFavorite imediatamente sem aguardar banco`() = runTest {
        val contact = fakeContact(1L, isFavorite = false)
        val vm = buildVm(fakeTripData(contacts = listOf(contact)))
        advanceUntilIdle()

        coEvery { contactRepo.toggleFavoriteContact(1L, true) } just Runs

        vm.toggleFavoriteContact(1L, true)

        assertTrue(vm.tripData.value?.contacts?.first()?.isFavorite == true)
    }

    @Test
    fun `toggleFavoriteContact persiste no repositorio`() = runTest {
        val vm = buildVm(fakeTripData(contacts = listOf(fakeContact(5L))))
        advanceUntilIdle()

        coEvery { contactRepo.toggleFavoriteContact(5L, true) } just Runs

        vm.toggleFavoriteContact(5L, true)
        advanceUntilIdle()

        coVerify(exactly = 1) { contactRepo.toggleFavoriteContact(5L, true) }
    }

    @Test
    fun `toggleFavoriteContact nao recarrega tripData do banco`() = runTest {
        val vm = buildVm(fakeTripData(contacts = listOf(fakeContact(1L))))
        advanceUntilIdle()

        coEvery { contactRepo.toggleFavoriteContact(any(), any()) } just Runs

        vm.toggleFavoriteContact(1L, true)
        advanceUntilIdle()

        coVerify(exactly = 1) { tripRepo.getTripData(tripId) }
    }

    // ── deleteActivity ─────────────────────────────────────────────────────────

    @Test
    fun `deleteActivity chama repo e recarrega estado`() = runTest {
        val vm = buildVm()
        advanceUntilIdle()

        coEvery { activityRepo.deleteActivity(42L) } just Runs

        vm.deleteActivity(42L)
        advanceUntilIdle()

        coVerify(exactly = 1) { activityRepo.deleteActivity(42L) }
        coVerify(exactly = 2) { tripRepo.getTripData(tripId) }
    }

    @Test
    fun `deleteActivity chama repo antes do reload`() = runTest {
        val vm = buildVm()
        advanceUntilIdle()

        coEvery { activityRepo.deleteActivity(any()) } just Runs

        vm.deleteActivity(5L)
        advanceUntilIdle()

        coVerifyOrder {
            activityRepo.deleteActivity(5L)
            tripRepo.getTripData(tripId)
        }
    }

    // ── swapActivityPositions ──────────────────────────────────────────────────

    @Test
    fun `swapActivityPositions chama repo com os ids e posicoes corretos`() = runTest {
        val vm = buildVm()
        advanceUntilIdle()

        coEvery { activityRepo.swapActivityPositions(any(), any(), any(), any()) } just Runs

        vm.swapActivityPositions(id1 = 10L, pos1 = 0, id2 = 20L, pos2 = 1)
        advanceUntilIdle()

        coVerify(exactly = 1) { activityRepo.swapActivityPositions(10L, 0, 20L, 1) }
    }

    @Test
    fun `swapActivityPositions recarrega estado apos persistir`() = runTest {
        val vm = buildVm()
        advanceUntilIdle()

        coEvery { activityRepo.swapActivityPositions(any(), any(), any(), any()) } just Runs

        vm.swapActivityPositions(10L, 0, 20L, 1)
        advanceUntilIdle()

        coVerifyOrder {
            activityRepo.swapActivityPositions(10L, 0, 20L, 1)
            tripRepo.getTripData(tripId)
        }
    }

    // ── Vouchers — Melhoria #6: otimista ──────────────────────────────────────

    @Test
    fun `toggleVoucherUsed atualiza estado imediatamente sem aguardar banco`() = runTest {
        val voucher = fakeVoucher(id = 7L, isUsed = false)
        val vm = buildVm(fakeTripData(vouchers = listOf(voucher)))
        advanceUntilIdle()

        coEvery { voucherRepo.toggleVoucherUsed(7L, true) } just Runs

        vm.toggleVoucherUsed(7L, true)

        assertTrue(vm.tripData.value?.vouchers?.first()?.isUsed == true)
    }

    @Test
    fun `deleteVoucher atualiza estado imediatamente sem aguardar banco`() = runTest {
        val vouchers = listOf(
            fakeVoucher(1L, sortOrder = 0),
            fakeVoucher(2L, sortOrder = 1),
            fakeVoucher(3L, sortOrder = 2)
        )
        val vm = buildVm(fakeTripData(vouchers = vouchers))
        advanceUntilIdle()

        coEvery { voucherRepo.deleteVoucherAndReindex(2L, tripId) } just Runs

        vm.deleteVoucher(2L)

        val remaining = vm.tripData.value?.vouchers
        assertEquals(2, remaining?.size)
        assertFalse(remaining?.any { it.id == 2L } == true)
    }

    @Test
    fun `deleteVoucher reindexaSortOrder corretamente no estado otimista`() = runTest {
        val vouchers = listOf(
            fakeVoucher(1L, sortOrder = 0),
            fakeVoucher(2L, sortOrder = 1),
            fakeVoucher(3L, sortOrder = 2)
        )
        val vm = buildVm(fakeTripData(vouchers = vouchers))
        advanceUntilIdle()

        coEvery { voucherRepo.deleteVoucherAndReindex(1L, tripId) } just Runs

        vm.deleteVoucher(1L)

        val remaining = vm.tripData.value?.vouchers
        assertEquals(listOf(0, 1), remaining?.map { it.sortOrder })
    }

    @Test
    fun `deleteVoucher chama deleteVoucherAndReindex no repositorio`() = runTest {
        val vouchers = listOf(fakeVoucher(1L, sortOrder = 0), fakeVoucher(2L, sortOrder = 1))
        val vm = buildVm(fakeTripData(vouchers = vouchers))
        advanceUntilIdle()

        coEvery { voucherRepo.deleteVoucherAndReindex(1L, tripId) } just Runs

        vm.deleteVoucher(1L)
        advanceUntilIdle()

        coVerify(exactly = 1) { voucherRepo.deleteVoucherAndReindex(1L, tripId) }
    }

    @Test
    fun `reorderVouchers atualiza estado imediatamente sem aguardar banco`() = runTest {
        val vouchers = listOf(fakeVoucher(1L), fakeVoucher(2L))
        val vm = buildVm(fakeTripData(vouchers = vouchers))
        advanceUntilIdle()

        coEvery { voucherRepo.reorderVouchers(any()) } just Runs

        val reordered = listOf(fakeVoucher(2L), fakeVoucher(1L))
        vm.reorderVouchers(reordered)

        assertEquals(2L, vm.tripData.value?.vouchers?.first()?.id)
    }

    @Test
    fun `reorderVouchers persiste no repositorio`() = runTest {
        val vm = buildVm()
        advanceUntilIdle()

        val reordered = listOf(fakeVoucher(2L), fakeVoucher(1L))
        coEvery { voucherRepo.reorderVouchers(reordered) } just Runs

        vm.reorderVouchers(reordered)
        advanceUntilIdle()

        coVerify(exactly = 1) { voucherRepo.reorderVouchers(reordered) }
    }
}
