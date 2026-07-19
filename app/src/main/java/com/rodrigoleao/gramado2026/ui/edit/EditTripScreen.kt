package com.rodrigoleao.gramado2026.ui.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.gramado2026.data.model.UiEvent
import com.rodrigoleao.gramado2026.ui.theme.*

private val EMOJI_OPTIONS = listOf(
    "⛰️", "🏖️", "🏙️", "🌊", "🌿", "🗺️", "✈️", "🏕️", "🏰", "🎡",
    "🌎", "🚢", "🎭", "🍽️", "🏔️", "🌅", "🎪", "🚂", "🏝️", "🌄"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTripScreen(
    viewModel: EditTripViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val canSave = state.name.isNotBlank() && state.destination.isNotBlank() && state.coverEmoji.isNotEmpty()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.NavigateBack -> onBack()
                is UiEvent.NavigateAfterDelete -> onDeleted()
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = AmberPrimary, contentColor = Color.White)
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text("Editar viagem", fontWeight = FontWeight.SemiBold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = state.entity != null
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.White.copy(alpha = 0.85f))
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
        containerColor = GreenLight
    ) { innerPadding ->

        if (state.entity == null) {
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Destino com autocomplete ──────────────────────────────────────
            EditSectionLabel("Destino")
            Box {
                OutlinedTextField(
                    value         = state.destination,
                    onValueChange = viewModel::updateDestination,
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("Ex: Gramado & Canela, RS", color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    trailingIcon  = {
                        if (state.latitude != null) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = GreenMoss, modifier = Modifier.size(20.dp))
                        } else if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = GreenMoss)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = GreenMoss,
                        unfocusedBorderColor    = CardBorder,
                        focusedContainerColor   = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        cursorColor             = GreenMoss
                    )
                )
                if (searchResults.isNotEmpty()) {
                    Card(
                        modifier  = Modifier.fillMaxWidth().padding(top = 56.dp),
                        shape     = RoundedCornerShape(12.dp),
                        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        searchResults.forEach { result ->
                            val label = buildString {
                                append(result.name)
                                result.admin1?.let { append(", $it") }
                                append(", ${result.country}")
                            }
                            Row(
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectResult(result) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = GreenMoss, modifier = Modifier.size(18.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            }
                            HorizontalDivider(color = CardBorder)
                        }
                    }
                }
            }
            if (state.latitude != null) {
                Text(
                    text  = "Localização selecionada — clima será atualizado na viagem",
                    style = MaterialTheme.typography.labelSmall,
                    color = GreenMoss
                )
            }

            EditSectionLabel("Nome da viagem")
            EditTextField(value = state.name, onValueChange = viewModel::updateName, placeholder = "Ex: Férias de inverno 2026")

            EditSectionLabel("Ícone")
            EmojiGrid(selected = state.coverEmoji, onSelect = viewModel::updateEmoji)

            HorizontalDivider(color = CardBorder)

            EditSectionLabel("Hotel")
            EditTextField(value = state.hotelName, onValueChange = viewModel::updateHotelName, placeholder = "Nome do hotel")
            EditTextField(value = state.hotelAddress, onValueChange = viewModel::updateHotelAddress, placeholder = "Endereço completo")

            EditSectionLabel("Telefone do hotel")
            EditTextField(
                value         = state.hotelPhone,
                onValueChange = viewModel::updateHotelPhone,
                placeholder   = "Ex: +55 54 3286-0000",
                keyboardType  = KeyboardType.Phone
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
                else Text("Salvar alterações", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir viagem?") },
            text  = { Text("Todos os dias e atividades desta viagem serão removidos permanentemente.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteTrip()
                }) { Text("Excluir", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

// ── Componentes compartilhados pelas telas de edição ──────────────────────────

@Composable
internal fun EditSectionLabel(text: String) {
    Text(text.uppercase(), fontSize = 10.sp, color = GreenMoss, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        modifier        = Modifier.fillMaxWidth(),
        placeholder     = { Text(placeholder, color = TextSecondary.copy(alpha = 0.5f)) },
        singleLine      = singleLine,
        minLines        = minLines,
        shape           = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = GreenMoss,
            unfocusedBorderColor    = CardBorder,
            focusedContainerColor   = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite,
            cursorColor             = GreenMoss
        )
    )
}

@Composable
internal fun EmojiGrid(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EMOJI_OPTIONS.chunked(5).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { emoji ->
                    val isSelected = emoji == selected
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onSelect(emoji) },
                        shape  = RoundedCornerShape(12.dp),
                        color  = if (isSelected) AmberPrimary.copy(alpha = 0.15f) else SurfaceWhite,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) AmberPrimary else CardBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
