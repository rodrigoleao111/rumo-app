package com.rodrigoleao.pipa.data.model

/** Modelo de domínio de uma conversa com a IA (mensagens já desserializadas). */
data class AiConversation(
    val id: Long,
    val tripId: Long?,
    val tripName: String,
    val destination: String,
    val startDate: String?,
    val endDate: String?,
    val messages: List<AiChatMessage>,
    val createdAt: Long
)

/** Uma mensagem da conversa. `fromUser = true` → usuário; `false` → IA. */
data class AiChatMessage(
    val fromUser: Boolean,
    val text: String
)
