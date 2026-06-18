@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.rodrigoleao.gramado2026.ui.edit

import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Schedule
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
import com.rodrigoleao.gramado2026.ui.theme.*
import java.io.File
import java.time.LocalDate

private val PT_MONTHS = listOf("Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez")

private fun dateStringToMillis(s: String): Long? {
    val parts = s.trim().split(" ")
    if (parts.size != 3) return null
    val day   = parts[0].toIntOrNull() ?: return null
    val month = PT_MONTHS.indexOf(parts[1]).takeIf { it >= 0 }?.plus(1) ?: return null
    val year  = parts[2].toIntOrNull() ?: return null
    return runCatching { LocalDate.of(year, month, day).toEpochDay() * 86_400_000L }.getOrNull()
}

private fun millisToDateString(millis: Long): String {
    val d = LocalDate.ofEpochDay(millis / 86_400_000L)
    return "%02d %s %d".format(d.dayOfMonth, PT_MONTHS[d.monthValue - 1], d.year)
}

private fun parseTime(s: String): Pair<Int, Int>? {
    val parts = s.split("h")
    if (parts.size != 2) return null
    return Pair(parts[0].toIntOrNull() ?: return null, parts[1].toIntOrNull() ?: return null)
}

private fun formatTime(hour: Int, minute: Int) = "%02dh%02d".format(hour, minute)

private data class TransportOption(val type: String, val emoji: String, val label: String)

private val TRANSPORT_OPTIONS = listOf(
    TransportOption("FLIGHT", "✈️", "Avião"),
    TransportOption("TRAIN",  "🚂", "Trem"),
    TransportOption("BUS",    "🚌", "Ônibus"),
    TransportOption("SHIP",   "🚢", "Navio"),
    TransportOption("OTHER",  "🎫", "Outro")
)

@Composable
fun EditBoardingPassScreen(
    viewModel: EditBoardingPassViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state   by viewModel.state.collectAsStateWithLifecycle()
    val isDirty by viewModel.isDirty.collectAsStateWithLifecycle()
    var showDeleteDialog  by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDatePicker    by remember { mutableStateOf(false) }
    var showTimePicker    by remember { mutableStateOf(false) }



    val isEditing = state.entity != null
    val canSave   = state.origin.isNotBlank() && state.destination.isNotBlank() && state.passenger.isNotBlank()
    val isFlight  = state.transportType == "FLIGHT"

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
        val destDir  = File(context.filesDir, "Passagens").also { it.mkdirs() }
        val destFile = File(destDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        viewModel.updateFile(destFile.absolutePath, fileName)
    }

    BackHandler(enabled = isDirty) { showDiscardDialog = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Editar passagem" else "Nova passagem",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
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
                    IconButton(onClick = { viewModel.save(onBack) }, enabled = canSave && !state.isSaving) {
                        Icon(Icons.Default.Check, contentDescription = "Salvar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenMoss)
            )
        },
        containerColor = GreenLight
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

            // ── Tipo de transporte ────────────────────────────────────────────
            EditSectionLabel("Tipo de transporte")
            FlowRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                TRANSPORT_OPTIONS.forEach { opt ->
                    val sel = state.transportType == opt.type
                    FilterChip(
                        selected = sel,
                        onClick  = { viewModel.updateTransportType(opt.type) },
                        label    = { Text("${opt.emoji} ${opt.label}") },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor   = AmberPrimary.copy(alpha = 0.15f),
                            selectedLabelColor       = GreenMoss,
                            selectedLeadingIconColor = GreenMoss
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

            // ── Rota ─────────────────────────────────────────────────────────
            if (isFlight) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EditSectionLabel("Origem (sigla)")
                        EditTextField(value = state.origin, onValueChange = viewModel::updateOrigin, placeholder = "REC")
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EditSectionLabel("Destino (sigla)")
                        EditTextField(value = state.destination, onValueChange = viewModel::updateDestination, placeholder = "GRU")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EditSectionLabel("Cidade de origem")
                        EditTextField(value = state.originCity, onValueChange = viewModel::updateOriginCity, placeholder = "Recife")
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EditSectionLabel("Cidade de destino")
                        EditTextField(value = state.destinationCity, onValueChange = viewModel::updateDestinationCity, placeholder = "Gramado")
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EditSectionLabel("Origem")
                        EditTextField(
                            value         = state.originCity,
                            onValueChange = viewModel::updateOriginSingle,
                            placeholder   = "Ex: São Paulo"
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EditSectionLabel("Destino")
                        EditTextField(
                            value         = state.destinationCity,
                            onValueChange = viewModel::updateDestinationSingle,
                            placeholder   = "Ex: Gramado"
                        )
                    }
                }
            }

            // ── Número / linha ────────────────────────────────────────────────
            EditSectionLabel(
                when (state.transportType) {
                    "FLIGHT" -> "Número do voo"
                    "TRAIN"  -> "Número do trem / linha"
                    "BUS"    -> "Linha / serviço"
                    "SHIP"   -> "Nome da embarcação"
                    else     -> "Número / identificador"
                }
            )
            EditTextField(
                value         = state.flightNumber,
                onValueChange = viewModel::updateFlightNumber,
                placeholder   = when (state.transportType) {
                    "FLIGHT" -> "AD 4153"
                    "TRAIN"  -> "Ex: IC 732"
                    "BUS"    -> "Ex: Linha 301"
                    "SHIP"   -> "Ex: Azul Mar"
                    else     -> "Ex: 001"
                }
            )

            // ── Data e horário ────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EditSectionLabel("Data")
                    Box {
                        OutlinedTextField(
                            value         = state.date,
                            onValueChange = {},
                            readOnly      = true,
                            placeholder   = { Text("09 Jun 2026", color = TextSecondary) },
                            trailingIcon  = { Icon(Icons.Default.DateRange, contentDescription = null, tint = GreenMoss) },
                            modifier      = Modifier.fillMaxWidth(),
                            shape         = RoundedCornerShape(12.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = GreenMoss,
                                unfocusedBorderColor    = CardBorder,
                                focusedContainerColor   = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite
                            )
                        )
                        Box(Modifier.matchParentSize().clickable { showDatePicker = true })
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EditSectionLabel("Horário de partida")
                    Box {
                        OutlinedTextField(
                            value         = state.boardingTime,
                            onValueChange = {},
                            readOnly      = true,
                            placeholder   = { Text("06h30", color = TextSecondary) },
                            trailingIcon  = { Icon(Icons.Default.Schedule, contentDescription = null, tint = GreenMoss) },
                            modifier      = Modifier.fillMaxWidth(),
                            shape         = RoundedCornerShape(12.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = GreenMoss,
                                unfocusedBorderColor    = CardBorder,
                                focusedContainerColor   = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite
                            )
                        )
                        Box(Modifier.matchParentSize().clickable { showTimePicker = true })
                    }
                }
            }

            // ── Passageiro ────────────────────────────────────────────────────
            EditSectionLabel("Passageiro")
            EditTextField(value = state.passenger, onValueChange = viewModel::updatePassenger, placeholder = "Nome completo")

            // ── Link ou arquivo da passagem ───────────────────────────────────
            EditSectionLabel("Passagem (link ou arquivo)")
            PassSourceSelector(
                mode         = state.inputMode,
                linkUrl      = state.walletUrl,
                fileName     = state.documentName,
                onModeChange = viewModel::setInputMode,
                onLinkChange = viewModel::updateWalletUrl,
                onPickFile   = { filePicker.launch(arrayOf("*/*")) },
                onClearFile  = viewModel::clearFile
            )

            // ── Observações ───────────────────────────────────────────────────
            EditSectionLabel("Observações (opcional)")
            OutlinedTextField(
                value         = state.notes,
                onValueChange = viewModel::updateNotes,
                placeholder   = { Text("Ex: Correr para o terminal B para trocar de veículo...", color = TextSecondary) },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 3,
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = GreenMoss,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor   = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                )
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = { viewModel.save(onBack) },
                enabled  = canSave && !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenMoss)
            ) {
                if (state.isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text(
                    if (isEditing) "Salvar passagem" else "Adicionar passagem",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = AmberPrimary
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateStringToMillis(state.date)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.updateDate(millisToDateString(millis))
                    }
                }) { Text("OK", color = GreenMoss) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(
                state  = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = GreenMoss,
                    todayDateBorderColor      = GreenMoss
                )
            )
        }
    }

    if (showTimePicker) {
        val parsedTime = parseTime(state.boardingTime)
        val timePickerState = rememberTimePickerState(
            initialHour   = parsedTime?.first  ?: 6,
            initialMinute = parsedTime?.second ?: 0,
            is24Hour      = true
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier              = Modifier.padding(24.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Horário de partida", style = MaterialTheme.typography.titleMedium)
                    TimePicker(
                        state  = timePickerState,
                        colors = TimePickerDefaults.colors(
                            selectorColor          = GreenMoss,
                            clockDialSelectedContentColor = Color.White,
                            timeSelectorSelectedContainerColor = GreenMoss.copy(alpha = 0.15f),
                            timeSelectorSelectedContentColor  = GreenMoss
                        )
                    )
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            showTimePicker = false
                            viewModel.updateBoardingTime(formatTime(timePickerState.hour, timePickerState.minute))
                        }) { Text("OK", color = GreenMoss) }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir passagem?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.delete(onBack) }) {
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
}

// ── PassSourceSelector ────────────────────────────────────────────────────────

@Composable
private fun PassSourceSelector(
    mode: PassInputMode,
    linkUrl: String,
    fileName: String,
    onModeChange: (PassInputMode) -> Unit,
    onLinkChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onClearFile: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val linkSel = mode == PassInputMode.LINK
            val fileSel = mode == PassInputMode.FILE
            SegmentedButton(
                selected = linkSel,
                onClick  = { onModeChange(PassInputMode.LINK) },
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
                onClick  = { onModeChange(PassInputMode.FILE) },
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

        if (mode == PassInputMode.LINK) {
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
