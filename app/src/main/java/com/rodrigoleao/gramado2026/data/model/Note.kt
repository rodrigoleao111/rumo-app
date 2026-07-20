package com.rodrigoleao.gramado2026.data.model

/** Tipos de bloco de uma nota (F4). Persistido como `NoteBlockType.name`. */
enum class NoteBlockType { TEXT, CHECKLIST, HEADING }

/** Nota de uma viagem (geral) ou de um dia específico (`dayId` = dayNumber). */
data class Note(
    val id: Long = 0,
    val tripId: Long,
    val dayId: Int? = null,        // null = nota geral da viagem
    val title: String = "",
    val blocks: List<NoteBlock> = emptyList(),
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

sealed class NoteBlock {
    abstract val id: Long
    abstract val sortOrder: Int

    data class TextBlock(
        override val id: Long = 0,
        val content: String = "",
        override val sortOrder: Int = 0
    ) : NoteBlock()

    data class ChecklistBlock(
        override val id: Long = 0,
        val items: List<ChecklistItem> = emptyList(),
        override val sortOrder: Int = 0
    ) : NoteBlock()

    data class HeadingBlock(
        override val id: Long = 0,
        val content: String = "",
        override val sortOrder: Int = 0
    ) : NoteBlock()
}

data class ChecklistItem(
    val id: Long = 0,
    val text: String = "",
    val isChecked: Boolean = false,
    val sortOrder: Int = 0
)
