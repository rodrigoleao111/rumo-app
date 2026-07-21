package com.rodrigoleao.gramado2026.ui.edit

import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.gramado2026.data.model.UiEvent
import com.rodrigoleao.gramado2026.ui.theme.*
import java.io.File

private val VOUCHER_QUICK_EMOJIS = listOf("🎫", "🏨", "🎡", "🚠", "🌿")

// Grupos predefinidos ficam no ViewModel (EditVoucherViewModel.DEFAULT_GROUPS)

private val VOUCHER_ALL_EMOJIS = listOf(
    "🎫", "🏨", "🎡", "🚠", "🌿", "🏔️", "🎭", "🍽️",
    "🎪", "🚢", "✈️", "🏛️", "🛍️", "🎢", "⛪", "🎠",
    "🎟️", "🏕️", "🌅", "🌊", "🌲", "🌋", "🚌", "🚂",
    "📸", "🗺️", "🧭", "📍", "☕", "🍷", "🧁", "🍦"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVoucherScreen(
    viewModel: EditVoucherViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state   by viewModel.state.collectAsStateWithLifecycle()
    val isDirty by viewModel.isDirty.collectAsStateWithLifecycle()

    var showDeleteDialog   by remember { mutableStateOf(false) }
    var showDiscardDialog  by remember { mutableStateOf(false) }
    var showEmojiDialog    by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var newGroupInput      by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        var fileName = "arquivo"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (col >= 0) fileName = cursor.getString(col)
            }
        }
        val destDir  = File(context.filesDir, "Vouchers").also { it.mkdirs() }
        val uniqueName = "${System.currentTimeMillis()}_$fileName"
        val destFile = File(destDir, uniqueName)
        fileName = uniqueName
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        viewModel.updateFile(destFile.absolutePath, fileName)
    }

    val isEditing = state.entity != null
    // Apenas o nome é obrigatório — categoria tem fallback "Geral"
    val canSave   = state.name.isNotBlank()

    BackHandler(enabled = isDirty) { showDiscardDialog = true }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.NavigateBack -> onBack()
                is UiEvent.NavigateAfterDelete -> onBack()
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = AmberPrimary, contentColor = GreenMoss)
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar voucher" else "Novo voucher", fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { if (isDirty) showDiscardDialog = true else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.White)
                        }
                    }
                    IconButton(onClick = { viewModel.save() }, enabled = canSave && !state.isSaving) {
                        Icon(Icons.Default.Check, contentDescription = "Salvar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = GreenMoss,
                    scrolledContainerColor = GreenMoss
                )
            )
        },
        containerColor = Sand
    ) { innerPadding ->

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenSage)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Nome — campo mais importante, vem primeiro
            EditSectionLabel("Nome do voucher")
            EditTextField(
                value         = state.name,
                onValueChange = viewModel::updateName,
                placeholder   = "Ex: Parque do Caracol — Adulto"
            )

            // 2. Categoria
            EditSectionLabel("Categoria")
            VoucherGroupSelector(
                selected      = state.groupName,
                groups        = state.availableGroups,
                onSelect      = viewModel::updateGroupName,
                onCreateClick = { newGroupInput = ""; showNewGroupDialog = true }
            )

            // 3. Ícone
            EditSectionLabel("Ícone")
            VoucherEmojiRow(
                selected    = state.emoji,
                onSelect    = viewModel::updateEmoji,
                onMoreClick = { showEmojiDialog = true }
            )

            // 4. Para quem (opcional)
            EditSectionLabel("Para quem (opcional)")
            EditTextField(
                value         = state.person,
                onValueChange = viewModel::updatePerson,
                placeholder   = "Ex: adulto, criança, Rodrigo"
            )

            // 5. Arquivo ou link (opcional)
            EditSectionLabel("Arquivo ou link (opcional)")
            VoucherSourceSelector(
                mode         = state.inputMode,
                linkUrl      = state.linkUrl,
                fileName     = state.fileName,
                onModeChange = viewModel::setInputMode,
                onLinkChange = viewModel::updateLinkUrl,
                onPickFile   = { filePicker.launch(arrayOf("*/*")) },
                onClearFile  = viewModel::clearFile
            )

            // 6. Dia da viagem (opcional) — chips em vez de texto livre
            if (state.availableDays.isNotEmpty()) {
                EditSectionLabel("Dia da viagem (opcional)")
                VoucherDaySelector(
                    selected  = state.dayNumber,
                    days      = state.availableDays,
                    onSelect  = viewModel::selectDay
                )
            }

            if (state.name.isBlank() && !state.isLoading) {
                Text(
                    text  = "Preencha o nome do voucher para continuar.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = { viewModel.save() },
                enabled  = canSave && !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenMoss)
            ) {
                if (state.isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text(
                    if (isEditing) "Salvar voucher" else "Adicionar voucher",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = AmberPrimary
                )
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir voucher?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.delete() }) {
                    Text("Excluir", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Descartar alterações?") },
            text  = { Text("As informações preenchidas serão perdidas.") },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; onBack() }) {
                    Text("Descartar", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Continuar editando") } }
        )
    }

    if (showEmojiDialog) {
        VoucherEmojiPickerDialog(
            selected  = state.emoji,
            onDismiss = { showEmojiDialog = false },
            onSelect  = { viewModel.updateEmoji(it); showEmojiDialog = false }
        )
    }

    if (showNewGroupDialog) {
        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            title = { Text("Nova categoria") },
            text = {
                OutlinedTextField(
                    value         = newGroupInput,
                    onValueChange = { newGroupInput = it },
                    placeholder   = { Text("Ex: Parques, Passeios…") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.createGroup(newGroupInput); showNewGroupDialog = false },
                    enabled = newGroupInput.isNotBlank()
                ) { Text("Criar", color = GreenMoss, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showNewGroupDialog = false }) { Text("Cancelar") } }
        )
    }
}

// ── VoucherSourceSelector ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoucherSourceSelector(
    mode: VoucherInputMode,
    linkUrl: String,
    fileName: String,
    onModeChange: (VoucherInputMode) -> Unit,
    onLinkChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onClearFile: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val linkSel = mode == VoucherInputMode.LINK
            val fileSel = mode == VoucherInputMode.FILE
            SegmentedButton(
                selected = linkSel,
                onClick  = { onModeChange(VoucherInputMode.LINK) },
                shape    = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon     = {},
                colors   = SegmentedButtonDefaults.colors(
                    activeContainerColor = AmberPrimary.copy(alpha = 0.18f),
                    activeContentColor   = GreenMoss,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Link", fontWeight = if (linkSel) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            SegmentedButton(
                selected = fileSel,
                onClick  = { onModeChange(VoucherInputMode.FILE) },
                shape    = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon     = {},
                colors   = SegmentedButtonDefaults.colors(
                    activeContainerColor = AmberPrimary.copy(alpha = 0.18f),
                    activeContentColor   = GreenMoss,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Arquivo", fontWeight = if (fileSel) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }

        if (mode == VoucherInputMode.LINK) {
            EditTextField(
                value         = linkUrl,
                onValueChange = onLinkChange,
                placeholder   = "https://..."
            )
        } else {
            if (fileName.isNotBlank()) {
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = AmberPrimary.copy(alpha = 0.08f),
                    border   = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
                        Text(
                            text     = fileName,
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = TextPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = onClearFile, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remover arquivo", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            OutlinedButton(
                onClick  = onPickFile,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (fileName.isBlank()) "Selecionar arquivo" else "Trocar arquivo")
            }
        }
    }
}

// ── VoucherDaySelector ────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoucherDaySelector(
    selected: Int?,
    days: List<DayOption>,
    onSelect: (Int?) -> Unit
) {
    FlowRow(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { day ->
            val sel = day.number == selected
            FilterChip(
                selected = sel,
                onClick  = { onSelect(if (sel) null else day.number) },
                label    = { Text("Dia ${day.number} · ${day.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor   = AmberPrimary.copy(alpha = 0.15f),
                    selectedLabelColor       = AmberPrimary,
                    selectedLeadingIconColor = AmberPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled             = true,
                    selected            = sel,
                    selectedBorderColor = AmberPrimary,
                    selectedBorderWidth = 1.5.dp
                )
            )
        }
    }
}

// ── VoucherGroupSelector ──────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoucherGroupSelector(
    selected: String,
    groups: List<String>,
    onSelect: (String) -> Unit,
    onCreateClick: () -> Unit
) {
    FlowRow(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp)
    ) {
        groups.forEach { group ->
            val sel = group == selected
            FilterChip(
                selected = sel,
                onClick  = { onSelect(group) },
                label    = { Text(group) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor   = AmberPrimary.copy(alpha = 0.15f),
                    selectedLabelColor       = AmberPrimary,
                    selectedLeadingIconColor = AmberPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled             = true,
                    selected            = sel,
                    selectedBorderColor = AmberPrimary,
                    selectedBorderWidth = 1.5.dp
                )
            )
        }
        AssistChip(
            onClick = onCreateClick,
            label   = { Text("Nova categoria") },
            leadingIcon = {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        )
    }
    // Categoria legada não presente na lista
    if (selected.isNotBlank() && !groups.contains(selected)) {
        Text(
            text  = "Categoria atual: $selected",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

// ── VoucherEmojiRow ───────────────────────────────────────────────────────────

@Composable
private fun VoucherEmojiRow(selected: String, onSelect: (String) -> Unit, onMoreClick: () -> Unit) {
    val displayed = buildList {
        add(selected)
        VOUCHER_QUICK_EMOJIS.filterNot { it == selected }.take(4).forEach { add(it) }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        displayed.forEach { emoji ->
            val sel = emoji == selected
            Surface(
                modifier = Modifier.weight(1f).aspectRatio(1f).clickable { onSelect(emoji) },
                shape    = RoundedCornerShape(12.dp),
                color    = if (sel) AmberPrimary.copy(alpha = 0.15f) else SurfaceWhite,
                border   = BorderStroke(if (sel) 2.dp else 1.dp, if (sel) AmberPrimary else CardBorder)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 22.sp)
                }
            }
        }
        Surface(
            modifier = Modifier.weight(1f).aspectRatio(1f).clickable { onMoreClick() },
            shape    = RoundedCornerShape(12.dp),
            color    = SurfaceWhite,
            border   = BorderStroke(1.dp, CardBorder)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, contentDescription = "Mais ícones", tint = TextSecondary)
            }
        }
    }
}

// ── VoucherEmojiPickerDialog ──────────────────────────────────────────────────

@Composable
private fun VoucherEmojiPickerDialog(selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Escolha um ícone", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.heightIn(max = 340.dp)
                ) {
                    items(VOUCHER_ALL_EMOJIS) { emoji ->
                        val sel = emoji == selected
                        Surface(
                            modifier = Modifier.aspectRatio(1f).clickable { onSelect(emoji) },
                            shape    = RoundedCornerShape(10.dp),
                            color    = if (sel) AmberPrimary.copy(alpha = 0.15f) else Color.Transparent,
                            border   = if (sel) BorderStroke(2.dp, AmberPrimary) else BorderStroke(0.dp, Color.Transparent)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Fechar", color = GreenMoss)
                }
            }
        }
    }
}
