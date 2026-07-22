@file:OptIn(ExperimentalMaterial3Api::class)

package com.rodrigoleao.gramado2026.ui.notes

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.gramado2026.data.model.NoteBlock
import com.rodrigoleao.gramado2026.data.model.NoteBlockType
import com.rodrigoleao.gramado2026.ui.theme.*
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.rodrigoleao.gramado2026.R

@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Bloco atualmente focado — alvo do botão "Excluir bloco" da toolbar.
    var focusedBlockId by remember { mutableStateOf<Long?>(null) }

    // Estado local dos blocos para o drag; ressincroniza quando o VM emite nova lista.
    var localBlocks by remember(state.blocks) { mutableStateOf(state.blocks) }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // índice 0 da lista é o campo de título — não participa da reordenação
        val fromIdx = from.index - 1
        val toIdx   = to.index - 1
        if (fromIdx !in localBlocks.indices || toIdx !in localBlocks.indices) return@rememberReorderableLazyListState
        localBlocks = localBlocks.toMutableList().apply { add(toIdx, removeAt(fromIdx)) }
    }
    LaunchedEffect(localBlocks) {
        if (localBlocks != state.blocks) viewModel.reorderBlocks(localBlocks.map { it.id })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nota", color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_arrow_back), contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenMoss)
            )
        },
        bottomBar = {
            InsertToolbar(
                onAddText      = { viewModel.addBlock(NoteBlockType.TEXT) },
                onAddChecklist = { viewModel.addBlock(NoteBlockType.CHECKLIST) },
                onAddHeading   = { viewModel.addBlock(NoteBlockType.HEADING) },
                onDeleteBlock  = { focusedBlockId?.let { viewModel.deleteBlock(it); focusedBlockId = null } },
                canDelete      = focusedBlockId != null
            )
        },
        containerColor = Sand
    ) { padding ->
        LazyColumn(
            state          = lazyListState,
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top    = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
                start  = 12.dp, end = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Título da nota (não reordenável)
            item(key = "note_title") {
                TextField(
                    value         = state.title,
                    onValueChange = viewModel::updateTitle,
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("Título da nota", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextSecondary.copy(alpha = 0.5f)) },
                    textStyle     = androidx.compose.ui.text.TextStyle(fontFamily = PlusJakartaSans, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
                    colors        = transparentFieldColors(),
                    singleLine    = true
                )
            }

            items(localBlocks, key = { it.id }) { block ->
                ReorderableItem(reorderState, key = block.id) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "blockDrag")
                    Surface(
                        shadowElevation = elevation,
                        color           = if (isDragging) SurfaceWhite else Color.Transparent,
                        shape           = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            IconButton(
                                modifier = Modifier.longPressDraggableHandle().size(40.dp),
                                onClick  = {}
                            ) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_drag), contentDescription = "Reordenar", tint = TextSecondary.copy(alpha = 0.5f))
                            }
                            Box(Modifier.weight(1f).padding(top = 4.dp)) {
                                BlockEditor(
                                    block          = block,
                                    autoFocus      = state.focusBlockId == block.id,
                                    autoFocusItem  = state.focusItemId,
                                    onFocused      = { focusedBlockId = block.id },
                                    onFocusHandled = viewModel::consumeFocus,
                                    onContent      = { viewModel.updateBlockContent(block.id, it) },
                                    onAddItem      = { viewModel.addChecklistItem(block.id) },
                                    onItemText     = { itemId, text -> viewModel.updateItemText(block.id, itemId, text) },
                                    onItemToggle   = { itemId, checked -> viewModel.toggleChecklistItem(block.id, itemId, checked) },
                                    onItemDelete   = { itemId -> viewModel.deleteChecklistItem(block.id, itemId) },
                                    onReorderItems = { ids -> viewModel.reorderChecklistItems(block.id, ids) }
                                )
                            }
                        }
                    }
                }
            }

            if (localBlocks.isEmpty()) {
                item(key = "empty_hint") {
                    Text(
                        text     = "Toque em Texto, Checklist ou Título abaixo para começar.",
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

// ── Bloco: dispatch por tipo ─────────────────────────────────────────────────

@Composable
private fun BlockEditor(
    block: NoteBlock,
    autoFocus: Boolean,
    autoFocusItem: Long?,
    onFocused: () -> Unit,
    onFocusHandled: () -> Unit,
    onContent: (String) -> Unit,
    onAddItem: () -> Unit,
    onItemText: (Long, String) -> Unit,
    onItemToggle: (Long, Boolean) -> Unit,
    onItemDelete: (Long) -> Unit,
    onReorderItems: (List<Long>) -> Unit
) {
    when (block) {
        is NoteBlock.TextBlock -> BlockTextField(
            initial = block.content, key = block.id, autoFocus = autoFocus,
            onFocused = onFocused, onFocusHandled = onFocusHandled, onValueChange = onContent,
            placeholder = "Escreva algo…", fontSize = 16.sp, bold = false
        )
        is NoteBlock.HeadingBlock -> BlockTextField(
            initial = block.content, key = block.id, autoFocus = autoFocus,
            onFocused = onFocused, onFocusHandled = onFocusHandled, onValueChange = onContent,
            placeholder = "Título da seção", fontSize = 19.sp, bold = true
        )
        is NoteBlock.ChecklistBlock -> Column(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ReorderableColumn(
                list = block.items,
                onSettle = { from, to ->
                    val ids = block.items.toMutableList().apply { add(to, removeAt(from)) }.map { it.id }
                    onReorderItems(ids)
                },
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) { _, item, _ ->
                key(item.id) {
                    ChecklistItemRow(
                        text        = item.text,
                        checked     = item.isChecked,
                        itemKey     = item.id,
                        autoFocus   = autoFocusItem == item.id,
                        onFocused   = onFocused,
                        onFocusHandled = onFocusHandled,
                        onToggle    = { onItemToggle(item.id, it) },
                        onTextChange = { onItemText(item.id, it) },
                        onDelete    = { onItemDelete(item.id) },
                        dragHandleModifier = Modifier.draggableHandle()
                    )
                }
            }
            TextButton(onClick = onAddItem, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Text("+ Adicionar item", color = GreenMoss, fontSize = 14.sp)
            }
        }
    }
}

// TextField com estado local (fonte de verdade do cursor), keyed por id do bloco.
@Composable
private fun BlockTextField(
    initial: String,
    key: Long,
    autoFocus: Boolean,
    onFocused: () -> Unit,
    onFocusHandled: () -> Unit,
    onValueChange: (String) -> Unit,
    placeholder: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    bold: Boolean
) {
    var text by remember(key) { mutableStateOf(initial) }
    val requester = remember(key) { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (autoFocus) { requester.requestFocus(); onFocusHandled() }
    }
    TextField(
        value         = text,
        onValueChange = { text = it; onValueChange(it) },
        modifier      = Modifier
            .fillMaxWidth()
            .focusRequester(requester)
            .onFocusChanged { if (it.isFocused) onFocused() },
        placeholder   = { Text(placeholder, fontSize = fontSize, color = TextSecondary.copy(alpha = 0.5f)) },
        textStyle     = androidx.compose.ui.text.TextStyle(
            fontFamily = PlusJakartaSans,
            fontSize = fontSize,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = TextPrimary
        ),
        colors        = transparentFieldColors()
    )
}

@Composable
private fun ChecklistItemRow(
    text: String,
    checked: Boolean,
    itemKey: Long,
    autoFocus: Boolean,
    onFocused: () -> Unit,
    onFocusHandled: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
    dragHandleModifier: Modifier = Modifier
) {
    var value by remember(itemKey) { mutableStateOf(text) }
    val requester = remember(itemKey) { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (autoFocus) { requester.requestFocus(); onFocusHandled() }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {}, modifier = dragHandleModifier.size(28.dp)) {
            Icon(ImageVector.vectorResource(R.drawable.ic_drag), contentDescription = "Reordenar item", tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = { onToggle(!checked) }, modifier = Modifier.size(36.dp)) {
            Icon(
                if (checked) ImageVector.vectorResource(R.drawable.ic_checkbox_checked) else ImageVector.vectorResource(R.drawable.ic_checkbox_blank),
                contentDescription = if (checked) "Desmarcar" else "Marcar",
                tint = if (checked) GreenMoss else TextSecondary
            )
        }
        TextField(
            value         = value,
            onValueChange = { value = it; onTextChange(it) },
            modifier      = Modifier
                .weight(1f)
                .focusRequester(requester)
                .onFocusChanged { if (it.isFocused) onFocused() },
            placeholder   = { Text("Item", color = TextSecondary.copy(alpha = 0.5f)) },
            textStyle     = androidx.compose.ui.text.TextStyle(
                fontFamily = PlusJakartaSans,
                fontSize = 15.sp,
                color = if (checked) TextSecondary else TextPrimary,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
            ),
            colors        = transparentFieldColors(),
            singleLine    = true
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(ImageVector.vectorResource(R.drawable.ic_delete), contentDescription = "Remover item", tint = TextSecondary.copy(alpha = 0.4f))
        }
    }
}

// ── Toolbar de inserção (fixada acima do teclado) ────────────────────────────

@Composable
private fun InsertToolbar(
    onAddText: () -> Unit,
    onAddChecklist: () -> Unit,
    onAddHeading: () -> Unit,
    onDeleteBlock: () -> Unit,
    canDelete: Boolean
) {
    Surface(color = SurfaceWhite, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            ToolbarButton(ImageVector.vectorResource(R.drawable.ic_note_text), "Texto", GreenMoss, onAddText)
            ToolbarButton(ImageVector.vectorResource(R.drawable.ic_checkbox_checked), "Checklist", GreenMoss, onAddChecklist)
            ToolbarButton(ImageVector.vectorResource(R.drawable.ic_title), "Título", GreenMoss, onAddHeading)
            ToolbarButton(
                ImageVector.vectorResource(R.drawable.ic_delete), "Excluir",
                if (canDelete) MaterialTheme.colorScheme.error else TextSecondary.copy(alpha = 0.3f),
                onDeleteBlock, enabled = canDelete
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(icon, contentDescription = label, tint = tint)
        }
        Text(label, fontSize = 10.sp, color = tint)
    }
}

@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor   = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor  = Color.Transparent,
    focusedIndicatorColor   = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor  = Color.Transparent,
    cursorColor             = GreenMoss
)
