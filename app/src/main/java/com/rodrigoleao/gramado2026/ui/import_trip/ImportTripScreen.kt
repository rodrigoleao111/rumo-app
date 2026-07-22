@file:OptIn(ExperimentalMaterial3Api::class)

package com.rodrigoleao.gramado2026.ui.import_trip

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.gramado2026.ui.theme.*
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.rodrigoleao.gramado2026.R

@Composable
fun ImportTripScreen(
    viewModel: ImportTripViewModel,
    initialUri: Uri? = null,
    onImported: (Long) -> Unit,
    onBack: () -> Unit
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()

    // Se vier de um intent externo, começa a importar direto
    LaunchedEffect(initialUri) {
        if (initialUri != null) viewModel.startImport(initialUri)
    }

    // Navega ao concluir
    LaunchedEffect(phase) {
        if (phase is ImportPhase.Done) onImported((phase as ImportPhase.Done).tripId)
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.startImport(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Importar viagem", color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_arrow_back), contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenMoss)
            )
        }
    ) { padding ->

        // ── Tela principal — explicação + botão ──────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text("📥", fontSize = 56.sp)

            Spacer(Modifier.height(20.dp))

            Text(
                text       = "Importar viagem",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text      = "Recebeu um arquivo .travel de outra pessoa? Importe aqui para ter o mesmo roteiro no seu dispositivo.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Detalhes do que é importado
            InfoRow(emoji = "🗓️", text = "Dias, atividades e roteiro completo")
            InfoRow(emoji = "🏨", text = "Dados de hospedagem")
            InfoRow(emoji = "👥", text = "Contatos importantes")
            InfoRow(emoji = "📄", text = "Documentos anexados aos dias")
            InfoRow(emoji = "🎟️", text = "Vouchers e ingressos")
            InfoRow(emoji = "✈️", text = "Cartões de embarque")

            Spacer(Modifier.weight(1f))

            Text(
                text      = "Uma nova viagem será criada. Suas viagens atuais não serão alteradas.",
                style     = MaterialTheme.typography.bodySmall,
                color     = TextSecondary,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(bottom = 20.dp)
            )

            Button(
                onClick  = { fileLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenMoss)
            ) {
                Icon(ImageVector.vectorResource(R.drawable.ic_download), contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Importar viagem", color = AmberPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // ── Dialog de loading ────────────────────────────────────────────────────
    if (phase is ImportPhase.Importing) {
        Dialog(
            onDismissRequest = {},
            properties       = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite
            ) {
                Column(
                    modifier            = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = GreenMoss, modifier = Modifier.size(40.dp))
                    Text(
                        text  = "Importando viagem…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }

    // ── Dialog de conflito de duplicata (F1) ─────────────────────────────────
    if (phase is ImportPhase.Duplicate) {
        val d = phase as ImportPhase.Duplicate
        val identical     = d.incomingLastEditedAt == d.existingLastEditedAt
        val incomingNewer = d.incomingLastEditedAt > d.existingLastEditedAt
        val localNewer    = !identical && !incomingNewer

        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicate() },
            icon  = { Text("⚠️", fontSize = 28.sp) },
            title = { Text("Viagem já importada") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text  = "\"${d.existingTripName}\" já existe no app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        DuplicateDateRow("Versão local:", formatEditTimestamp(d.existingLastEditedAt))
                        DuplicateDateRow("Versão importada:", formatEditTimestamp(d.incomingLastEditedAt))
                    }
                    Text(
                        text = when {
                            identical     -> "As versões são idênticas."
                            incomingNewer -> "A versão importada é mais recente. Deseja substituir a versão local?"
                            else          -> "⚠ Atenção: a versão local é mais recente. Importar substituirá dados mais novos."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (localNewer) MaterialTheme.colorScheme.error else TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.overwriteImport(d.pendingUri, d.existingTripId) },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = if (localNewer) MaterialTheme.colorScheme.error else GreenMoss
                    )
                ) {
                    Text(
                        "Importar",
                        color      = if (localNewer) Color.White else AmberPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDuplicate() }) {
                    Text("Manter local", color = GreenMoss, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // ── Dialog de erro ───────────────────────────────────────────────────────
    if (phase is ImportPhase.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            icon  = { Text("⚠️", fontSize = 28.sp) },
            title = { Text("Não foi possível importar") },
            text  = {
                Text(
                    text  = (phase as ImportPhase.Error).message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissError() },
                    colors  = ButtonDefaults.buttonColors(containerColor = GreenMoss)
                ) {
                    Text("Tentar novamente", color = AmberPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun DuplicateDateRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}

/** Formata um unix-ms como "dd/MM/yyyy HH:mm". 0 (viagem pré-F1) vira "—". */
private fun formatEditTimestamp(ms: Long): String =
    if (ms <= 0L) "—"
    else java.time.Instant.ofEpochMilli(ms)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR")))

@Composable
private fun InfoRow(emoji: String, text: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}
