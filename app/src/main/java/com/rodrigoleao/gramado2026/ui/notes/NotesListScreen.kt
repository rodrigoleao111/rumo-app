@file:OptIn(ExperimentalMaterial3Api::class)

package com.rodrigoleao.gramado2026.ui.notes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.gramado2026.data.model.Note
import com.rodrigoleao.gramado2026.data.model.NoteBlock
import com.rodrigoleao.gramado2026.ui.theme.*
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Tela de notas de um dia específico (rota própria). A aba geral usa NotesListContent direto. */
@Composable
fun DayNotesScreen(
    viewModel: NotesListViewModel,
    dayLabel: String,
    onOpenNote: (Long) -> Unit,
    onBack: () -> Unit
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notas — $dayLabel", color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenMoss)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { viewModel.createNote(onOpenNote) },
                containerColor = AmberPrimary,
                contentColor   = GreenMoss,
                shape          = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Nova nota") }
        },
        containerColor = Sand
    ) { padding ->
        NotesListContent(
            notes          = notes,
            contentPadding = padding,
            onOpenNote     = onOpenNote,
            onDeleteNote   = viewModel::deleteNote,
            onReorderNotes = viewModel::reorderNotes
        )
    }
}

/** Lista de notas reutilizável (aba geral e notas de dia). Drag-to-reorder + swipe-delete. */
@Composable
fun NotesListContent(
    notes: List<Note>,
    contentPadding: PaddingValues,
    onOpenNote: (Long) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onReorderNotes: (List<Note>) -> Unit,
    onAddNote: (() -> Unit)? = null
) {
    if (notes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding).padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📝", fontSize = 40.sp)
                Text("Nenhuma anotação ainda", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("Toque em + para criar sua primeira nota", style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(alpha = 0.6f))
            }
        }
        return
    }

    var localNotes  by remember(notes) { mutableStateOf(notes) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (from.index !in localNotes.indices || to.index !in localNotes.indices) return@rememberReorderableLazyListState
        localNotes = localNotes.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }
    LaunchedEffect(localNotes) {
        if (localNotes != notes) onReorderNotes(localNotes)
    }

    LazyColumn(
        state          = lazyListState,
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top    = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
            start  = 16.dp, end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(localNotes, key = { it.id }) { note ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart && noteToDelete == null) {
                        noteToDelete = note
                    }
                    false
                }
            )
            LaunchedEffect(noteToDelete) { if (noteToDelete == null) dismissState.reset() }

            ReorderableItem(reorderState, key = note.id) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 10.dp else 0.dp, label = "noteDrag")
                Box(Modifier.shadow(elevation, RoundedCornerShape(12.dp), clip = false)) {
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val color by animateColorAsState(
                                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) Color(0xFFE53935) else Color.Transparent,
                                label = "swipe_bg"
                            )
                            Box(
                                modifier = Modifier.fillMaxSize().background(color, RoundedCornerShape(12.dp)).padding(end = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    ) {
                        NoteCard(
                            note       = note,
                            onClick    = { onOpenNote(note.id) },
                            dragHandle = {
                                IconButton(modifier = Modifier.size(36.dp).longPressDraggableHandle(), onClick = {}) {
                                    Icon(Icons.Default.DragHandle, contentDescription = "Reordenar", tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    noteToDelete?.let { n ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Remover nota?") },
            text  = { Text("\"${n.title.ifBlank { "Sem título" }}\" e todo o seu conteúdo serão removidos permanentemente.") },
            confirmButton = {
                TextButton(onClick = { onDeleteNote(n.id); noteToDelete = null }) {
                    Text("Remover", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { noteToDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    dragHandle: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            dragHandle()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = note.title.ifBlank { "Sem título" },
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (note.title.isBlank()) TextSecondary else TextPrimary,
                    maxLines   = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    text     = note.previewText(),
                    style    = MaterialTheme.typography.bodySmall,
                    color    = TextSecondary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text     = "Editada em ${formatNoteTimestamp(note.updatedAt)}",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = TextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// Resumo dos 2 primeiros blocos (UC-F4: texto truncado / "X itens, Y marcados").
private fun Note.previewText(): String {
    if (blocks.isEmpty()) return "Nota vazia"
    return blocks.take(2).joinToString("  ·  ") { b ->
        when (b) {
            is NoteBlock.TextBlock      -> b.content.trim().take(50).ifBlank { "Texto" }
            is NoteBlock.HeadingBlock   -> b.content.trim().take(50).ifBlank { "Título" }
            is NoteBlock.ChecklistBlock -> {
                val total = b.items.size
                val done  = b.items.count { it.isChecked }
                "$total ${if (total == 1) "item" else "itens"}, $done ✓"
            }
        }
    }
}

private fun formatNoteTimestamp(ms: Long): String =
    if (ms <= 0L) "—"
    else java.time.Instant.ofEpochMilli(ms)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR")))
