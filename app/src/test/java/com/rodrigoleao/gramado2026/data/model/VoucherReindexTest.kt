package com.rodrigoleao.gramado2026.data.model

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Testes unitários da função de extensão List<Voucher>.reindexedByGroup().
 * Cobre a melhoria #8: lógica de reindexação centralizada como função pura.
 */
class VoucherReindexTest {

    private fun voucher(id: Long, group: String, sortOrder: Int = 0) = Voucher(
        id        = id,
        emoji     = "🎟️",
        groupName = group,
        name      = "Voucher $id",
        assetPath = "test/$id.pdf",
        sortOrder = sortOrder
    )

    @Test
    fun `lista vazia retorna lista vazia`() {
        val result = emptyList<Voucher>().reindexedByGroup()
        assertEquals(emptyList(), result)
    }

    @Test
    fun `grupo unico reindexado a partir de zero`() {
        val vouchers = listOf(
            voucher(1L, "Atrações", sortOrder = 5),
            voucher(2L, "Atrações", sortOrder = 8),
            voucher(3L, "Atrações", sortOrder = 12)
        )
        val result = vouchers.reindexedByGroup()
        assertEquals(listOf(0, 1, 2), result.map { it.sortOrder })
    }

    @Test
    fun `ids e campos nao relacionados a sortOrder sao preservados`() {
        val vouchers = listOf(
            voucher(10L, "A", sortOrder = 99),
            voucher(20L, "A", sortOrder = 100)
        )
        val result = vouchers.reindexedByGroup()
        assertEquals(listOf(10L, 20L), result.map { it.id })
        assertEquals(listOf("A", "A"), result.map { it.groupName })
    }

    @Test
    fun `grupos diferentes reindexados independentemente`() {
        val vouchers = listOf(
            voucher(1L, "Atrações", sortOrder = 0),
            voucher(2L, "Atrações", sortOrder = 1),
            voucher(3L, "Hotel", sortOrder = 0),
            voucher(4L, "Hotel", sortOrder = 1),
            voucher(5L, "Hotel", sortOrder = 2)
        )
        val result = vouchers.reindexedByGroup()

        val atracoes = result.filter { it.groupName == "Atrações" }
        val hotel    = result.filter { it.groupName == "Hotel" }

        assertEquals(listOf(0, 1), atracoes.map { it.sortOrder })
        assertEquals(listOf(0, 1, 2), hotel.map { it.sortOrder })
    }

    @Test
    fun `reindexacao apos remocao fecha o gap de sortOrder`() {
        // Simula o que acontece no VM após filtrar o voucher deletado
        val afterDelete = listOf(
            voucher(1L, "Atrações", sortOrder = 0),
            // voucher 2L foi removido
            voucher(3L, "Atrações", sortOrder = 2)  // sortOrder 2 deve virar 1
        )
        val result = afterDelete.reindexedByGroup()
        assertEquals(listOf(0, 1), result.map { it.sortOrder })
    }

    @Test
    fun `voucher unico no grupo tem sortOrder zero`() {
        val result = listOf(voucher(1L, "Solo", sortOrder = 7)).reindexedByGroup()
        assertEquals(0, result.single().sortOrder)
    }

    @Test
    fun `tres grupos distintos reindexados corretamente`() {
        val vouchers = listOf(
            voucher(1L, "A", sortOrder = 99),
            voucher(2L, "B", sortOrder = 50),
            voucher(3L, "C", sortOrder = 77),
            voucher(4L, "A", sortOrder = 100),
            voucher(5L, "B", sortOrder = 51)
        )
        val result = vouchers.reindexedByGroup()

        assertEquals(listOf(0, 1), result.filter { it.groupName == "A" }.map { it.sortOrder })
        assertEquals(listOf(0, 1), result.filter { it.groupName == "B" }.map { it.sortOrder })
        assertEquals(listOf(0),    result.filter { it.groupName == "C" }.map { it.sortOrder })
    }
}
