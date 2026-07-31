package com.rodrigoleao.pipa.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Snapshot de uma conversa com a IA no wizard de criação de viagem.
 *
 * É **independente** da viagem (sem ForeignKey): guarda os dados exibidos no card
 * (título, destino, datas), então sobrevive à exclusão da viagem. As mensagens ficam
 * serializadas em `messagesJson` — um array JSON `[{"role":"USER|AI","text":"..."}]`.
 */
@Entity(tableName = "ai_conversations")
data class AiConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long?,          // referência à viagem (pode ser null)
    val tripName: String,
    val destination: String,
    val startDate: String?,     // ISO "yyyy-MM-dd" — período da viagem
    val endDate: String?,
    val messagesJson: String,   // JSON array das mensagens da conversa
    val createdAt: Long         // epoch millis do início da conversa
)
