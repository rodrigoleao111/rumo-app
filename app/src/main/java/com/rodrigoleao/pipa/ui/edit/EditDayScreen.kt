package com.rodrigoleao.pipa.ui.edit

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.pipa.data.model.UiEvent
import com.rodrigoleao.pipa.ui.theme.*
import java.io.File
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.rodrigoleao.pipa.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDayScreen(
    viewModel: EditDayViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val canSave = state.title.isNotBlank()

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val fileName = run {
            var name = "documento"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (col >= 0) name = cursor.getString(col)
                }
            }
            name
        }
        val destDir = File(context.filesDir, "Arquivos").also { it.mkdirs() }
        val destFile = File(destDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        viewModel.updateDocument(destFile.absolutePath, fileName)
    }

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
                title = {
                    Text(stringResource(R.string.editd_edit_day_title), fontWeight = FontWeight.SemiBold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.common_back), tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save() }, enabled = canSave && !state.isSaving) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_check), contentDescription = stringResource(R.string.common_save), tint = Color.White)
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

        if (state.entity == null) {
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
            EditSectionLabel(stringResource(R.string.editd_label_day_title))
            EditTextField(
                value         = state.title,
                onValueChange = viewModel::updateTitle,
                placeholder   = stringResource(R.string.editd_day_title_placeholder)
            )

            EditSectionLabel(stringResource(R.string.editd_label_day_alert))
            EditTextField(
                value         = state.dayAlert,
                onValueChange = viewModel::updateDayAlert,
                placeholder   = stringResource(R.string.editd_day_alert_placeholder),
                singleLine    = false,
                minLines      = 2
            )

            HorizontalDivider(color = CardBorder)

            EditSectionLabel(stringResource(R.string.editd_label_link_or_doc))
            Text(
                text  = stringResource(R.string.editd_link_hint),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            EditTextField(
                value         = state.dayLinkLabel,
                onValueChange = viewModel::updateDayLinkLabel,
                placeholder   = stringResource(R.string.editd_link_label_placeholder)
            )

            OutlinedTextField(
                value         = state.dayLinkUrl,
                onValueChange = viewModel::updateDayLinkUrl,
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text(stringResource(R.string.editd_url_placeholder), color = TextSecondary.copy(alpha = 0.5f)) },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                leadingIcon   = {
                    Icon(ImageVector.vectorResource(R.drawable.ic_link), contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = GreenMoss,
                    unfocusedBorderColor    = CardBorder,
                    focusedContainerColor   = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite,
                    cursorColor             = GreenMoss
                )
            )

            HorizontalDivider(color = CardBorder)

            EditSectionLabel(stringResource(R.string.editd_label_document))
            Text(
                text  = stringResource(R.string.editd_document_hint),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (state.dayDocumentPath.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFFE8F0E8)),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, GreenMoss.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_attach), contentDescription = null, tint = GreenMoss, modifier = Modifier.size(20.dp))
                        Text(
                            text     = state.dayDocumentName.ifBlank { stringResource(R.string.editd_document_fallback) },
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = GreenMoss,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { viewModel.clearDocument() }, modifier = Modifier.size(32.dp)) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_close), contentDescription = stringResource(R.string.common_remove), tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                EditTextField(
                    value         = state.dayDocumentTitle,
                    onValueChange = viewModel::updateDocumentTitle,
                    placeholder   = stringResource(R.string.editd_document_title_placeholder)
                )
            } else {
                OutlinedButton(
                    onClick  = { documentPicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = GreenMoss),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, GreenMoss.copy(alpha = 0.5f))
                ) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_attach), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.editd_import_document))
                }
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
                else Text(stringResource(R.string.editd_save_day), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}
