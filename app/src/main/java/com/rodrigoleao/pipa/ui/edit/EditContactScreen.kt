package com.rodrigoleao.pipa.ui.edit

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
import com.rodrigoleao.pipa.data.model.UiEvent
import com.rodrigoleao.pipa.ui.theme.*
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.rodrigoleao.pipa.R

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
                title = { Text(if (isEditing) stringResource(R.string.contact_edit_title) else stringResource(R.string.contact_new_title), fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { if (isDirty) showDiscardDialog = true else onBack() }) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.common_back), tint = Color.White)
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.common_delete), tint = Color(0xFFFFAA99))
                        }
                    }
                    IconButton(onClick = { viewModel.save() }, enabled = canSave && !state.isSaving) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_check), contentDescription = stringResource(R.string.common_save), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenMoss)
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditSectionLabel(stringResource(R.string.contact_field_name))
            EditTextField(value = state.name, onValueChange = viewModel::updateName, placeholder = stringResource(R.string.contact_name_placeholder))

            EditSectionLabel(stringResource(R.string.contact_field_role))
            EditTextField(value = state.role, onValueChange = viewModel::updateRole, placeholder = stringResource(R.string.contact_role_placeholder))

            EditSectionLabel(stringResource(R.string.contact_field_phone))
            EditTextField(value = state.phone, onValueChange = viewModel::updatePhone, placeholder = stringResource(R.string.contact_phone_placeholder))

            EditSectionLabel(stringResource(R.string.contact_field_category))
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
                Text(stringResource(R.string.contact_has_whatsapp), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
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
                else Text(if (isEditing) stringResource(R.string.contact_save_button) else stringResource(R.string.contact_add_button), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.contact_delete_title)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.delete() }) {
                    Text(stringResource(R.string.common_delete), color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.contact_discard_title)) },
            text  = { Text(stringResource(R.string.contact_discard_message)) },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; onBack() }) {
                    Text(stringResource(R.string.contact_discard_confirm), color = Color(0xFFD32F2F))
                }
            },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text(stringResource(R.string.contact_discard_dismiss)) } }
        )
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false; newCategoryName = "" },
            title = { Text(stringResource(R.string.contact_new_category_title)) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text(stringResource(R.string.contact_category_name_label)) },
                    placeholder = { Text(stringResource(R.string.contact_category_name_placeholder)) },
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
                ) { Text(stringResource(R.string.common_add), color = GreenMoss) }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false; newCategoryName = "" }) { Text(stringResource(R.string.common_cancel)) }
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
        "AGENCY"     to stringResource(R.string.contact_category_agency),
        "HOTEL"      to stringResource(R.string.contact_category_hotel),
        "ATTRACTION" to stringResource(R.string.contact_category_attraction),
        "EMERGENCY"  to stringResource(R.string.contact_category_emergency),
        "FAMILY"     to stringResource(R.string.contact_category_family)
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
                Icon(ImageVector.vectorResource(R.drawable.ic_add), contentDescription = stringResource(R.string.contact_new_category_title), tint = TextSecondary, modifier = Modifier.size(14.dp))
                Text(stringResource(R.string.contact_new_category_chip), fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}
