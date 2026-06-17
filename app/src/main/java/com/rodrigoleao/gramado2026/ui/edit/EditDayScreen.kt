package com.rodrigoleao.gramado2026.ui.edit

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.rodrigoleao.gramado2026.ui.theme.*
import java.io.File

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Editar dia", fontWeight = FontWeight.SemiBold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save(onBack) }, enabled = canSave && !state.isSaving) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditSectionLabel("Título do dia")
            EditTextField(
                value         = state.title,
                onValueChange = viewModel::updateTitle,
                placeholder   = "Ex: Chegada em Gramado"
            )

            EditSectionLabel("Alerta do dia (opcional)")
            EditTextField(
                value         = state.dayAlert,
                onValueChange = viewModel::updateDayAlert,
                placeholder   = "Ex: Confirmar reserva de jantar",
                singleLine    = false,
                minLines      = 2
            )

            HorizontalDivider(color = CardBorder)

            EditSectionLabel("Link ou documento (opcional)")
            Text(
                text  = "Adicione um link útil para o dia — mapa, cardápio, ingresso online, etc.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            EditTextField(
                value         = state.dayLinkLabel,
                onValueChange = viewModel::updateDayLinkLabel,
                placeholder   = "Rótulo  (ex: Mapa de Rotas, Cardápio)"
            )

            OutlinedTextField(
                value         = state.dayLinkUrl,
                onValueChange = viewModel::updateDayLinkUrl,
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("https://...", color = TextSecondary.copy(alpha = 0.5f)) },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                leadingIcon   = {
                    Icon(Icons.Default.Link, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
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

            EditSectionLabel("Documento (opcional)")
            Text(
                text  = "Importe um PDF, imagem ou outro arquivo salvo no dispositivo.",
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
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = GreenMoss, modifier = Modifier.size(20.dp))
                        Text(
                            text     = state.dayDocumentName.ifBlank { "Documento" },
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = GreenMoss,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { viewModel.clearDocument() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remover", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                EditTextField(
                    value         = state.dayDocumentTitle,
                    onValueChange = viewModel::updateDocumentTitle,
                    placeholder   = "Título do documento (ex: Ingresso Parque do Caracol)"
                )
            } else {
                OutlinedButton(
                    onClick  = { documentPicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = GreenMoss),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, GreenMoss.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Importar documento")
                }
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
                else Text("Salvar dia", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}
