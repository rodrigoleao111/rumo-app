package com.rodrigoleao.gramado2026.data.ai

import com.google.common.truth.Truth.assertThat
import org.json.JSONException
import org.junit.Test

/**
 * Testa `ItineraryGenerator.parseJson()` — o parser do JSON de roteiro gerado por IA
 * (modo chat Gemini e modo importação externa).
 *
 * Roda na JVM: a função é pura e o `org.json` real está no classpath de teste
 * (`testImplementation("org.json:json")` — o do SDK Android é stub na JVM).
 *
 * Contrato do parser (ver docs/ai-itinerary-schema.md):
 *  - Obrigatórios: days[], dayNumber, title, activities[], name — ausência lança JSONException
 *  - Opcionais com default: time "", emoji "📍", detail "", badges []
 *  - dayAlert/address: ausente, em branco ou a string "null" → null
 *  - Cerca de markdown (```json … ```) no início é removida
 */
class ItineraryParserTest {

    @Test
    fun `dia completo com atividade e badges e parseado integralmente`() {
        val json = """
        {
          "days": [{
            "dayNumber": 1,
            "title": "Chegada em Gramado",
            "dayAlert": "Check-in a partir das 14h",
            "activities": [{
              "time": "14h00",
              "emoji": "🏨",
              "name": "Check-in no hotel",
              "detail": "Hotel Serra Azul",
              "address": "Rua Garibaldi, 152",
              "badges": ["BOOKED", "UBER"]
            }]
          }]
        }
        """
        val result = ItineraryGenerator.parseJson(json)

        assertThat(result).hasSize(1)
        with(result[0]) {
            assertThat(dayNumber).isEqualTo(1)
            assertThat(title).isEqualTo("Chegada em Gramado")
            assertThat(dayAlert).isEqualTo("Check-in a partir das 14h")
            assertThat(activities).hasSize(1)
        }
        with(result[0].activities[0]) {
            assertThat(time).isEqualTo("14h00")
            assertThat(emoji).isEqualTo("🏨")
            assertThat(name).isEqualTo("Check-in no hotel")
            assertThat(detail).isEqualTo("Hotel Serra Azul")
            assertThat(address).isEqualTo("Rua Garibaldi, 152")
            assertThat(badges).containsExactly("BOOKED", "UBER").inOrder()
        }
    }

    @Test
    fun `campos opcionais ausentes recebem defaults`() {
        val json = """
        {"days": [{"dayNumber": 1, "title": "Dia livre",
                   "activities": [{"name": "Passeio livre"}]}]}
        """
        val act = ItineraryGenerator.parseJson(json)[0].activities[0]

        assertThat(act.name).isEqualTo("Passeio livre")
        assertThat(act.time).isEmpty()
        assertThat(act.emoji).isEqualTo("📍")     // default do parser
        assertThat(act.detail).isEmpty()
        assertThat(act.address).isNull()
        assertThat(act.badges).isEmpty()
    }

    @Test
    fun `dayAlert ausente ou string null vira null`() {
        val semAlert = """{"days": [{"dayNumber": 1, "title": "T", "activities": []}]}"""
        assertThat(ItineraryGenerator.parseJson(semAlert)[0].dayAlert).isNull()

        // IAs às vezes serializam null como a string "null"
        val alertStringNull = """{"days": [{"dayNumber": 1, "title": "T", "dayAlert": "null", "activities": []}]}"""
        assertThat(ItineraryGenerator.parseJson(alertStringNull)[0].dayAlert).isNull()
    }

    @Test
    fun `address string null vira null`() {
        val json = """
        {"days": [{"dayNumber": 1, "title": "T",
                   "activities": [{"name": "A", "address": "null"}]}]}
        """
        assertThat(ItineraryGenerator.parseJson(json)[0].activities[0].address).isNull()
    }

    @Test
    fun `cerca de markdown json e removida`() {
        val json = "```json\n{\"days\": [{\"dayNumber\": 1, \"title\": \"T\", \"activities\": []}]}\n```"
        val result = ItineraryGenerator.parseJson(json)
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("T")
    }

    @Test
    fun `cerca de markdown generica e removida`() {
        val json = "```\n{\"days\": [{\"dayNumber\": 1, \"title\": \"T\", \"activities\": []}]}\n```"
        assertThat(ItineraryGenerator.parseJson(json)).hasSize(1)
    }

    @Test
    fun `multiplos dias e atividades preservam a ordem`() {
        val json = """
        {"days": [
          {"dayNumber": 1, "title": "Dia 1", "activities": [{"name": "A1"}, {"name": "A2"}]},
          {"dayNumber": 2, "title": "Dia 2", "activities": [{"name": "B1"}]}
        ]}
        """
        val result = ItineraryGenerator.parseJson(json)

        assertThat(result.map { it.dayNumber }).containsExactly(1, 2).inOrder()
        assertThat(result[0].activities.map { it.name }).containsExactly("A1", "A2").inOrder()
        assertThat(result[1].activities.map { it.name }).containsExactly("B1")
    }

    @Test
    fun `lista de dias vazia retorna lista vazia`() {
        assertThat(ItineraryGenerator.parseJson("""{"days": []}""")).isEmpty()
    }

    // ── Contrato de erro: o parser NÃO é tolerante — quem chama trata a exceção ──

    @Test(expected = JSONException::class)
    fun `json invalido lanca JSONException`() {
        // Texto de prosa antes do JSON (sem cerca de markdown) não é suportado
        ItineraryGenerator.parseJson("Claro! Aqui está o roteiro: {\"days\": []}")
    }

    @Test(expected = JSONException::class)
    fun `dia sem title obrigatorio lanca JSONException`() {
        ItineraryGenerator.parseJson("""{"days": [{"dayNumber": 1, "activities": []}]}""")
    }

    @Test(expected = JSONException::class)
    fun `atividade sem name obrigatorio lanca JSONException`() {
        ItineraryGenerator.parseJson(
            """{"days": [{"dayNumber": 1, "title": "T", "activities": [{"time": "09h00"}]}]}"""
        )
    }

    @Test(expected = JSONException::class)
    fun `raiz sem days lanca JSONException`() {
        ItineraryGenerator.parseJson("""{"itinerary": []}""")
    }
}
