@file:OptIn(ExperimentalLayoutApi::class)

package com.rodrigoleao.gramado2026.ui.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.gramado2026.data.model.BadgeType
import com.rodrigoleao.gramado2026.data.model.UiEvent
import com.rodrigoleao.gramado2026.ui.theme.*

// 5 ícones exibidos na linha rápida (excluindo o selecionado atual, que ocupa o slot 0)
private val QUICK_EMOJIS = listOf("🏨", "🍽️", "🎡", "🏔️", "☕")

// Lista completa exibida no dialog
private val ALL_ACTIVITY_EMOJIS = listOf(
    "🏨", "🛎️", "🍽️", "☕", "🍷", "🍺", "🧁", "🍦",
    "🌿", "🏔️", "🌅", "🏞️", "🌲", "🌊", "🌋", "🏕️",
    "🎡", "🚠", "🎢", "🎭", "🎪", "🎠", "🎟️", "🏛️",
    "🚗", "🚶", "✈️", "🚌", "🚂", "⛵", "🚁", "🛻",
    "🛍️", "📸", "⛪", "🏰", "🗺️", "🧭", "📍", "🎵"
)

private val ALL_BADGES = listOf(
    BadgeType.FREE      to "Grátis",
    BadgeType.PAID      to "Pago",
    BadgeType.BOOKED    to "Reservado",
    BadgeType.INCLUDED  to "Incluído",
    BadgeType.UBER      to "Uber",
    BadgeType.WALKING   to "A pé"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditActivityScreen(
    viewModel: EditActivityViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteDialog   by remember { mutableStateOf(false) }
    var showNewBadgeDialog by remember { mutableStateOf(false) }
    var showEmojiDialog    by remember { mutableStateOf(false) }
    var showTimePicker     by remember { mutableStateOf(false) }

    val isEditing = state.activityId != 0L
    val canSave   = state.name.isNotBlank() && !state.isLoading

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
                title = { Text(if (isEditing) "Editar atividade" else "Nova atividade", fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.White)
                        }
                    }
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = canSave && !state.isSaving
                    ) {
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
            // Horário
            EditSectionLabel("Horário")
            TimePickerField(
                time    = state.time,
                onClick = { showTimePicker = true }
            )

            // Emoji
            EditSectionLabel("Ícone")
            ActivityEmojiRow(
                selected    = state.emoji,
                onSelect    = viewModel::updateEmoji,
                onMoreClick = { showEmojiDialog = true }
            )

            // Nome
            EditSectionLabel("Nome da atividade")
            EditTextField(value = state.name, onValueChange = viewModel::updateName, placeholder = "Ex: Café da manhã no hotel")

            // Detalhe
            EditSectionLabel("Descrição")
            EditTextField(
                value         = state.detail,
                onValueChange = viewModel::updateDetail,
                placeholder   = "Detalhes, dicas, observações…",
                singleLine    = false,
                minLines      = 3
            )

            // Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditSectionLabel("Tags")
                TextButton(
                    onClick = { showNewBadgeDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = GreenMoss)
                    Spacer(Modifier.width(4.dp))
                    Text("Nova", fontSize = 13.sp, color = GreenMoss)
                }
            }
            BadgeSelector(
                selected     = state.selectedBadges,
                customBadges = state.customBadges,
                onToggle     = viewModel::toggleBadge,
                onRemoveCustom = viewModel::removeCustomBadge
            )

            // Endereço
            HorizontalDivider(color = CardBorder)
            EditSectionLabel("Endereço (opcional)")
            Text(
                text  = "Usado para abrir no Maps e chamar Uber.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            EditTextField(
                value         = state.address,
                onValueChange = viewModel::updateAddress,
                placeholder   = "Ex: Parque do Caracol, Canela RS"
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = { viewModel.save() },
                enabled  = canSave && !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenMoss)
            ) {
                if (state.isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text(if (isEditing) "Salvar atividade" else "Adicionar atividade", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }

    if (showTimePicker) {
        val (initHour, initMin) = parseTimeString(state.time)
        TimePickerDialog(
            initialHour   = initHour,
            initialMinute = initMin,
            onDismiss     = { showTimePicker = false },
            onConfirm     = { h, m ->
                viewModel.updateTime("%02dh%02d".format(h, m))
                showTimePicker = false
            }
        )
    }

    if (showEmojiDialog) {
        EmojiPickerDialog(
            selected  = state.emoji,
            onDismiss = { showEmojiDialog = false },
            onSelect  = { emoji ->
                viewModel.updateEmoji(emoji)
                showEmojiDialog = false
            }
        )
    }

    if (showNewBadgeDialog) {
        NewBadgeDialog(
            onDismiss = { showNewBadgeDialog = false },
            onConfirm = { name, color ->
                viewModel.addCustomBadge(name, color)
                showNewBadgeDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir atividade?") },
            text  = { Text("Esta atividade será removida permanentemente do dia.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteActivity()
                }) { Text("Excluir", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

// ── Campo de horário clicável ─────────────────────────────────────────────────

@Composable
private fun TimePickerField(time: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape    = RoundedCornerShape(12.dp),
        color    = SurfaceWhite,
        border   = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Schedule,
                contentDescription = null,
                tint               = TextSecondary.copy(alpha = 0.5f),
                modifier           = Modifier.size(20.dp)
            )
            Text(
                text  = time.ifBlank { "Selecionar horário" },
                color = if (time.isBlank()) TextSecondary.copy(alpha = 0.5f) else TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun parseTimeString(time: String): Pair<Int, Int> {
    return try {
        val cleaned = time.replace("h", ":").replace("H", ":")
        val parts   = cleaned.split(":")
        Pair(parts[0].trim().toInt().coerceIn(0, 23), parts[1].trim().toInt().coerceIn(0, 59))
    } catch (e: Exception) { Pair(9, 0) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(
                modifier          = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Selecionar horário", fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(16.dp))
                TimePicker(
                    state  = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor         = Sand,
                        selectorColor          = GreenMoss,
                        timeSelectorSelectedContainerColor   = GreenMoss,
                        timeSelectorUnselectedContainerColor = Sand,
                        timeSelectorSelectedContentColor     = Color.White,
                        timeSelectorUnselectedContentColor   = TextSecondary
                    )
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                        Text("OK", color = GreenMoss, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Linha rápida de emoji ─────────────────────────────────────────────────────

@Composable
private fun ActivityEmojiRow(selected: String, onSelect: (String) -> Unit, onMoreClick: () -> Unit) {
    // Slot 0 = emoji selecionado; slots 1-4 = quick picks (excluindo o selecionado)
    val displayed = buildList {
        add(selected)
        QUICK_EMOJIS.filterNot { it == selected }.take(4).forEach { add(it) }
    }

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        displayed.forEach { emoji ->
            val sel = emoji == selected
            Surface(
                modifier = Modifier.weight(1f).aspectRatio(1f).clickable { onSelect(emoji) },
                shape    = RoundedCornerShape(12.dp),
                color    = if (sel) AmberPrimary.copy(alpha = 0.15f) else SurfaceWhite,
                border   = BorderStroke(if (sel) 2.dp else 1.dp, if (sel) AmberPrimary else CardBorder)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(emoji, fontSize = 22.sp)
                }
            }
        }
        // Botão "mais opções"
        Surface(
            modifier = Modifier.weight(1f).aspectRatio(1f).clickable { onMoreClick() },
            shape    = RoundedCornerShape(12.dp),
            color    = SurfaceWhite,
            border   = BorderStroke(1.dp, CardBorder)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Default.Add, contentDescription = "Mais ícones", tint = TextSecondary)
            }
        }
    }
}

// ── Dialog de seleção de emoji ────────────────────────────────────────────────

@Composable
private fun EmojiPickerDialog(selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Escolha um ícone", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns                = GridCells.Fixed(6),
                    horizontalArrangement  = Arrangement.spacedBy(8.dp),
                    verticalArrangement    = Arrangement.spacedBy(8.dp),
                    modifier               = Modifier.heightIn(max = 340.dp)
                ) {
                    items(ALL_ACTIVITY_EMOJIS) { emoji ->
                        val sel = emoji == selected
                        Surface(
                            modifier = Modifier.aspectRatio(1f).clickable { onSelect(emoji) },
                            shape    = RoundedCornerShape(10.dp),
                            color    = if (sel) AmberPrimary.copy(alpha = 0.15f) else Color.Transparent,
                            border   = if (sel) BorderStroke(2.dp, AmberPrimary) else BorderStroke(0.dp, Color.Transparent)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Fechar", color = GreenMoss)
                }
            }
        }
    }
}

// ── Seletor de badges ─────────────────────────────────────────────────────────

private val BADGE_PALETTE = listOf(
    "#E53935", "#8E24AA", "#1E88E5", "#00897B",
    "#43A047", "#F4511E", "#6D4C41", "#546E7A"
)

@Composable
private fun BadgeSelector(
    selected: Set<BadgeType>,
    customBadges: List<CustomBadge>,
    onToggle: (BadgeType) -> Unit,
    onRemoveCustom: (CustomBadge) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp)
    ) {
        ALL_BADGES.forEach { (type, label) ->
            val isSelected = type in selected
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .clickable { onToggle(type) },
                shape = RoundedCornerShape(100.dp),
                color = if (isSelected) AmberPrimary else SurfaceWhite,
                border = BorderStroke(1.dp, if (isSelected) AmberPrimary else CardBorder)
            ) {
                Text(
                    text       = label,
                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    fontSize   = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isSelected) Color.White else TextSecondary
                )
            }
        }

        customBadges.forEach { cb ->
            val base = try {
                Color(android.graphics.Color.parseColor(cb.colorHex))
            } catch (e: Exception) { Color(0xFF607D8B) }
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = base.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, base.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(cb.name, fontSize = 13.sp, color = base, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { onRemoveCustom(cb) }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remover", tint = base, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// ── Dialog — nova categoria ───────────────────────────────────────────────────

@Composable
fun NewBadgeDialog(onDismiss: () -> Unit, onConfirm: (name: String, colorHex: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(BADGE_PALETTE.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova categoria", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Nome da categoria") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = GreenMoss,
                        cursorColor          = GreenMoss,
                        focusedLabelColor    = GreenMoss
                    )
                )

                Text("Cor da tag", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp)
                ) {
                    BADGE_PALETTE.forEach { hex ->
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                        val isSelected = hex == selectedColor
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { if (name.isNotBlank()) onConfirm(name, selectedColor) },
                enabled  = name.isNotBlank()
            ) {
                Text("Adicionar", color = GreenMoss, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
