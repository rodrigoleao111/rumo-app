package com.rodrigoleao.pipa.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rodrigoleao.pipa.data.db.entity.AiConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiConversationDao {

    @Query("SELECT * FROM ai_conversations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AiConversationEntity>>

    @Query("SELECT * FROM ai_conversations WHERE id = :id")
    suspend fun getById(id: Long): AiConversationEntity?

    @Insert
    suspend fun insert(conversation: AiConversationEntity): Long

    @Query("UPDATE ai_conversations SET messagesJson = :json WHERE id = :id")
    suspend fun updateMessages(id: Long, json: String)

    @Query("DELETE FROM ai_conversations WHERE id = :id")
    suspend fun delete(id: Long)
}
