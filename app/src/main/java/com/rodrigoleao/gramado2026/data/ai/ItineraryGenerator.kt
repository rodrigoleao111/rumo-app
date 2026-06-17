package com.rodrigoleao.gramado2026.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ItineraryGenerator(
    apiKey: String,
    val ctx: TripContext
) {
    data class TripContext(
        val destination: String,
        val startDate: String,
        val endDate: String,
        val dayCount: Int,
        val hotelName: String,
        val hotelAddress: String
    )

    data class GeneratedDay(
        val dayNumber: Int,
        val title: String,
        val dayAlert: String?,
        val activities: List<GeneratedActivity>
    )

    data class GeneratedActivity(
        val time: String,
        val emoji: String,
        val name: String,
        val detail: String,
        val address: String?,
        val badges: List<String>
    )

    companion object {
        private const val TAG = "ItineraryGenerator"

        fun parseJson(text: String): List<GeneratedDay> {
            val t = text.trim()
            val json = when {
                t.startsWith("```json") -> t.removePrefix("```json").removeSuffix("```").trim()
                t.startsWith("```")     -> t.removePrefix("```").removeSuffix("```").trim()
                else                    -> t
            }
            val root      = org.json.JSONObject(json)
            val daysArray = root.getJSONArray("days")
            return (0 until daysArray.length()).map { i ->
                val day  = daysArray.getJSONObject(i)
                val acts = day.getJSONArray("activities")
                GeneratedDay(
                    dayNumber  = day.getInt("dayNumber"),
                    title      = day.getString("title"),
                    dayAlert   = day.optString("dayAlert").takeIf { it.isNotBlank() && it != "null" },
                    activities = (0 until acts.length()).map { j ->
                        val act         = acts.getJSONObject(j)
                        val badgesArray = act.optJSONArray("badges")
                        GeneratedActivity(
                            time    = act.optString("time", ""),
                            emoji   = act.optString("emoji", "📍"),
                            name    = act.getString("name"),
                            detail  = act.optString("detail", ""),
                            address = act.optString("address").takeIf { it.isNotBlank() && it != "null" },
                            badges  = if (badgesArray != null)
                                (0 until badgesArray.length()).map { badgesArray.getString(it) }
                            else emptyList()
                        )
                    }
                )
            }
        }
    }

    private val initialGreeting: String = buildInitialGreeting()

    private val model = GenerativeModel(
        modelName         = "gemini-2.0-flash",
        apiKey            = apiKey,
        systemInstruction = content { text(buildSystemPrompt()) }
    )

    private val chat = model.startChat(
        history = listOf(
            content(role = "model") { text(initialGreeting) }
        )
    )

    fun getInitialGreeting(): String = initialGreeting

    suspend fun sendMessage(userText: String): String {
        Log.d(TAG, ">>> USER: $userText")
        return try {
            val response = chat.sendMessage(userText).text
                ?: "Não consegui responder. Pode tentar de novo?"
            Log.d(TAG, "<<< AI: $response")
            response
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage error", e)
            "Erro de conexão. Verifique sua internet e tente novamente."
        }
    }

    suspend fun generateItinerary(): List<GeneratedDay> {
        Log.d(TAG, ">>> GENERATE: sending generation prompt")
        val raw = chat.sendMessage(buildGenerationPrompt()).text
            ?: throw Exception("Resposta vazia da IA")
        Log.d(TAG, "<<< GENERATE RAW:\n$raw")
        val json = extractJson(raw)
        Log.d(TAG, "<<< GENERATE JSON:\n$json")
        return parseItinerary(json)
    }

    // ── Prompts ───────────────────────────────────────────────────────────────

    private fun buildSystemPrompt(): String {
        val hotel = if (ctx.hotelName.isNotBlank())
            "Hospedagem: ${ctx.hotelName}, ${ctx.hotelAddress}" else ""
        return """
            Você é um assistente especialista em roteiros de viagem no Brasil.

            Viagem: ${ctx.destination} · ${ctx.startDate} a ${ctx.endDate} · ${ctx.dayCount} dias
            $hotel

            Regras de conversa:
            - Faça no máximo 2 perguntas por mensagem, de forma natural e direta
            - Use português brasileiro informal mas profissional
            - Após 2-3 trocas, você pode oferecer gerar o roteiro
            - Leve em conta horários realistas, distâncias e ritmo de viagem
            - Ao gerar roteiro, retorne APENAS JSON válido conforme o schema fornecido
        """.trimIndent()
    }

    private fun buildInitialGreeting(): String {
        val fmt   = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("pt", "BR"))
        val start = LocalDate.parse(ctx.startDate).format(fmt)
        val end   = LocalDate.parse(ctx.endDate).format(fmt)
        val n     = ctx.dayCount
        val hotel = if (ctx.hotelName.isNotBlank()) " Hotel: ${ctx.hotelName}." else ""

        return "Olá! Vou ajudar a montar o roteiro para ${ctx.destination}, " +
            "de $start a $end ($n ${if (n == 1) "dia" else "dias"}).$hotel\n\n" +
            "Para começar: qual é o perfil da viagem? Por exemplo — família com crianças, " +
            "casal, grupo de amigos, viagem solo... E o que mais curtem fazer: " +
            "gastronomia, aventura, cultura, compras, natureza?"
    }

    private fun buildGenerationPrompt(): String = """
        Com base em tudo que conversamos, gere o roteiro completo.
        Retorne APENAS o JSON abaixo — sem texto adicional, sem markdown, sem ```.

        {
          "days": [
            {
              "dayNumber": 1,
              "title": "Título curto e descritivo",
              "dayAlert": "Alerta útil ou null",
              "activities": [
                {
                  "time": "09h00",
                  "emoji": "🎯",
                  "name": "Nome da atividade",
                  "detail": "Descrição com dicas práticas",
                  "address": "Endereço completo ou null",
                  "badges": ["FREE"]
                }
              ]
            }
          ]
        }

        Regras:
        - Gere exatamente ${ctx.dayCount} dias (dayNumber 1 a ${ctx.dayCount})
        - Cada dia: 3 a 6 atividades com horários realistas
        - badges válidos: FREE, PAID, BOOKED, INCLUDED, WALKING, UBER
        - Atividades específicas e reais para ${ctx.destination}
    """.trimIndent()

    // ── Parse ─────────────────────────────────────────────────────────────────

    private fun extractJson(text: String): String {
        val t = text.trim()
        return when {
            t.startsWith("```json") -> t.removePrefix("```json").removeSuffix("```").trim()
            t.startsWith("```")     -> t.removePrefix("```").removeSuffix("```").trim()
            else                    -> t
        }
    }

    private fun parseItinerary(json: String): List<GeneratedDay> {
        val root      = JSONObject(json)
        val daysArray = root.getJSONArray("days")
        return (0 until daysArray.length()).map { i ->
            val day   = daysArray.getJSONObject(i)
            val acts  = day.getJSONArray("activities")
            GeneratedDay(
                dayNumber  = day.getInt("dayNumber"),
                title      = day.getString("title"),
                dayAlert   = day.optString("dayAlert").takeIf { it.isNotBlank() && it != "null" },
                activities = (0 until acts.length()).map { j ->
                    val act          = acts.getJSONObject(j)
                    val badgesArray  = act.optJSONArray("badges")
                    GeneratedActivity(
                        time    = act.optString("time", ""),
                        emoji   = act.optString("emoji", "📍"),
                        name    = act.getString("name"),
                        detail  = act.optString("detail", ""),
                        address = act.optString("address").takeIf { it.isNotBlank() && it != "null" },
                        badges  = if (badgesArray != null)
                            (0 until badgesArray.length()).map { badgesArray.getString(it) }
                        else emptyList()
                    )
                }
            )
        }
    }
}
