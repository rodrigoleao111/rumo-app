package com.rodrigoleao.gramado2026.data.db.dao

import androidx.room.*
import com.rodrigoleao.gramado2026.data.db.entity.ContactEntity

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE tripId = :tripId ORDER BY sortOrder ASC")
    suspend fun getContactsForTrip(tripId: Long): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: Long): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(contact: ContactEntity): Long

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE contacts SET sortOrder = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)

    @Query("UPDATE contacts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateIsFavorite(id: Long, isFavorite: Boolean)
}
