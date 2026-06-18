package com.rodrigoleao.gramado2026.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditContactScreen(
    viewModel: EditContactViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDirty by viewModel.isDirty.collectAsStateWithLifecycle()
    var showDeleteDialog       by remember { mutableStateOf(false) }
    var showDiscardDialog      by remember { mutableStateOf(false) }
    var showAddCategoryDialog  by remember { mutableStateOf(false) }
    var newCategoryName        by remember { mutableStateOf("") }

    val isEditing = state.entity != null
    val canSave   = state.name.isNotBlank()

    BackHandler(enabled = isDirty) { showDiscardDialog = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar contato" else "Novo contato", fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { if (isDirty) showDiscardDialog = true else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFFFAA99))
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
            EditSectionLabel("Nome")
            EditTextField(value = state.name, onValueChange = viewModel::updateName, placeholder = "Ex: Hotel San Lucas")

            EditSectionLabel("Função / descrição")
            EditTextField(value = state.role, onValueChange = viewModel::updateRole, placeholder = "Ex: Recepção 24h")

            EditSectionLabel("Telefone")
            EditTextField(value = state.phone, onValueChange = viewModel::updatePhone, placeholder = "Ex: 54999001122")

            EditSectionLabel("Categoria")
            CategorySelector(
                selected         = state.selectedCategory,
                customCategories = state.customCategories,
                onSelect         = viewModel::updateCategory,
                onAddCategory    = { showAddCategoryDialog = true }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Checkbox(checked = state.hasWhatsApp, onCheckedChange = { viewModel.toggleWhatsApp() }, colors = CheckboxDefaults.colors(checkedColor = GreenMoss))
                Text("Tem WhatsApp", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = { viewModel.save(onBack) },
                enabled  = canSave && !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenMoss)
            ) {
                if (state.isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text(if (isEditing) "Salvar contato" else "Adicionar contato", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir contato?") },
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

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false; newCategoryName = "" },
            title = { Text("Nova categoria") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Nome da categoria") },
                    placeholder = { Text("Ex: Médico, Guia, Amigos...") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCustomCategory(newCategoryName)
                            viewModel.updateCategory(newCategoryName.trim())
                        }
                        showAddCategoryDialog = false
                        newCategoryName = ""
                    },
                    enabled = newCategoryName.isNotBlank()
                ) { Text("Adicionar", color = GreenMoss) }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false; newCategoryName = "" }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelector(
    selected: String,
    customCategories: List<String>,
    onSelect: (String) -> Unit,
    onAddCategory: () -> Unit
) {
    val builtinOptions = listOf(
        "AGENCY"     to "Agência",
        "HOTEL"      to "Hotel",
        "ATTRACTION" to "Atração",
        "EMERGENCY"  to "Emergência",
        "FAMILY"     to "Família"
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp)
    ) {
        builtinOptions.forEach { (key, label) ->
            val sel = key == selected
            Surface(
                modifier = Modifier.clickable { onSelect(key) },
                shape    = RoundedCornerShape(10.dp),
                color    = if (sel) AmberPrimary.copy(alpha = 0.15f) else SurfaceWhite,
                border   = BorderStroke(1.dp, if (sel) AmberPrimary else CardBorder)
            ) {
                Text(
                    text       = label,
                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    fontSize   = 12.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (sel) GreenMoss else TextSecondary
                )
            }
        }

        customCategories.forEach { categoryName ->
            val sel = categoryName == selected
            Surface(
                modifier = Modifier.clickable { onSelect(categoryName) },
                shape    = RoundedCornerShape(10.dp),
                color    = if (sel) AmberPrimary.copy(alpha = 0.15f) else SurfaceWhite,
                border   = BorderStroke(1.dp, if (sel) AmberPrimary else CardBorder)
            ) {
                Text(
                    text       = categoryName,
                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    fontSize   = 12.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (sel) GreenMoss else TextSecondary
                )
            }
        }

        Surface(
            modifier = Modifier.clickable { onAddCategory() },
            shape    = RoundedCornerShape(10.dp),
            color    = SurfaceWhite,
            border   = BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova categoria", tint = TextSecondary, modifier = Modifier.size(14.dp))
                Text("Nova", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}
