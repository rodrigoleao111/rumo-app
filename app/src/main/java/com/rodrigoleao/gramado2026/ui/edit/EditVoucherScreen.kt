package com.rodrigoleao.gramado2026.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.gramado2026.ui.theme.*

private val VOUCHER_EMOJIS = listOf("🎫", "🏨", "🎡", "🚠", "🌿", "🏔️", "🎭", "🍽️", "🎪", "🚢", "✈️", "🏛️", "🛍️", "🎢", "⛪")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVoucherScreen(
    viewModel: EditVoucherViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDirty by viewModel.isDirty.collectAsStateWithLifecycle()
    var showDeleteDialog  by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isEditing = state.entity != null
    val canSave   = state.name.isNotBlank() && state.groupName.isNotBlank()

    BackHandler(enabled = isDirty) { showDiscardDialog = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar voucher" else "Novo voucher", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { if (isDirty) showDiscardDialog = true else onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFD32F2F))
                        }
                    }
                    IconButton(onClick = { viewModel.save(onBack) }, enabled = canSave && !state.isSaving) {
                        Icon(Icons.Default.Check, contentDescription = "Salvar", tint = GreenMoss)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        containerColor = GreenLight
    ) { innerPadding ->

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GreenSage) }
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
            EditSectionLabel("Ícone")
            EmojiGrid(selected = state.emoji, onSelect = viewModel::updateEmoji)

            EditSectionLabel("Grupo / categoria")
            EditTextField(value = state.groupName, onValueChange = viewModel::updateGroupName, placeholder = "Ex: Parques, Passeios")

            EditSectionLabel("Nome do voucher")
            EditTextField(value = state.name, onValueChange = viewModel::updateName, placeholder = "Ex: Parque do Caracol — Adulto")

            EditSectionLabel("Passageiro / pessoa (opcional)")
            EditTextField(value = state.person, onValueChange = viewModel::updatePerson, placeholder = "Ex: Rodrigo")

            EditSectionLabel("Link ou caminho do arquivo")
            EditTextField(
                value = state.assetPath,
                onValueChange = viewModel::updateAssetPath,
                placeholder = "https://... ou vouchers/arquivo.pdf"
            )
            Text(
                text  = "Cole um link web (confirmação de reserva, e-ticket) ou um caminho de asset.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.7f),
                lineHeight = 15.sp
            )

            EditSectionLabel("Dia da viagem (opcional)")
            EditTextField(value = state.dayNumber, onValueChange = viewModel::updateDayNumber, placeholder = "Ex: 1, 2, 3…")

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = { viewModel.save(onBack) },
                enabled  = canSave && !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenMoss)
            ) {
                if (state.isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text(if (isEditing) "Salvar voucher" else "Adicionar voucher", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir voucher?") },
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
                TextButton(onClick = { showDiscardDialog = false; onBack() }) { Text("Descartar", color = Color(0xFFD32F2F)) }
            },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Continuar editando") } }
        )
    }
}
