package com.rodrigoleao.pipa.data.repository

import com.rodrigoleao.pipa.data.db.TravelDatabase
import com.rodrigoleao.pipa.data.db.entity.AiConversationEntity
import com.rodrigoleao.pipa.data.model.AiChatMessage
import com.rodrigoleao.pipa.data.model.AiConversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistência das conversas com a IA. As mensagens são serializadas como JSON
 * numa única coluna (`messagesJson`) — não precisamos de tabela filha só pra isso.
 */
class AiConversationRepository(db: TravelDatabase) {

    private val dao = db.aiConversationDao()

    val conversations: Flow<List<AiConversation>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getById(id: Long): AiConversation? = dao.getById(id)?.toModel()

    /** Insere uma nova conversa e devolve o id gerado. */
    suspend fun insert(
        tripId: Long?,
        tripName: String,
        destination: String,
        startDate: String?,
        endDate: String?,
        messages: List<AiChatMessage>,
        createdAt: Long
    ): Long = dao.insert(
        AiConversationEntity(
            tripId       = tripId,
            tripName     = tripName,
            destination  = destination,
            startDate    = startDate,
            endDate      = endDate,
            messagesJson = messages.toJson(),
            createdAt    = createdAt
        )
    )

    /** Atualiza só as mensagens de uma conversa existente. */
    suspend fun updateMessages(id: Long, messages: List<AiChatMessage>) =
        dao.updateMessages(id, messages.toJson())

    suspend fun delete(id: Long) = dao.delete(id)

    private fun AiConversationEntity.toModel() = AiConversation(
        id          = id,
        tripId      = tripId,
        tripName    = tripName,
        destination = destination,
        startDate   = startDate,
        endDate     = endDate,
        messages    = parseMessages(messagesJson),
        createdAt   = createdAt
    )
}

// ── (De)serialização das mensagens ──────────────────────────────────────────────

private fun List<AiChatMessage>.toJson(): String {
    val arr = JSONArray()
    forEach { m ->
        arr.put(JSONObject().put("role", if (m.fromUser) "USER" else "AI").put("text", m.text))
    }
    return arr.toString()
}

private fun parseMessages(json: String): List<AiChatMessage> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AiChatMessage(fromUser = o.optString("role") == "USER", text = o.optString("text"))
        }
    }.getOrDefault(emptyList())
}
