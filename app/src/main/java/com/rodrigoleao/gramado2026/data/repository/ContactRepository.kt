package com.rodrigoleao.gramado2026.data.repository

import androidx.room.withTransaction
import com.rodrigoleao.gramado2026.data.db.TravelDatabase
import com.rodrigoleao.gramado2026.data.db.entity.ContactEntity
import com.rodrigoleao.gramado2026.data.model.Contact
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(private val db: TravelDatabase) {

    suspend fun getContactEntity(id: Long): ContactEntity? = db.contactDao().getById(id)

    suspend fun upsertContact(tripId: Long, entity: ContactEntity): Long =
        if (entity.id == 0L) db.contactDao().insert(entity.copy(tripId = tripId))
        else { db.contactDao().update(entity); entity.id }

    suspend fun deleteContact(id: Long) = db.contactDao().deleteById(id)

    suspend fun reorderContacts(contacts: List<Contact>) = db.withTransaction {
        contacts.forEachIndexed { index, contact ->
            if (contact.id > 0) db.contactDao().updateSortOrder(contact.id, index)
        }
    }

    suspend fun toggleFavoriteContact(id: Long, isFavorite: Boolean) =
        db.contactDao().updateIsFavorite(id, isFavorite)
}
