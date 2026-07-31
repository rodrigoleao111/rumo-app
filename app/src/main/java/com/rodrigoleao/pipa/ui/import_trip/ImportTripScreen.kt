@file:OptIn(ExperimentalMaterial3Api::class)

package com.rodrigoleao.pipa.ui.import_trip

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.rodrigoleao.pipa.ui.theme.*
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.rodrigoleao.pipa.R

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
                title  = { Text(stringResource(R.string.import_title), color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.common_back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenMoss)
            )
        }
    ) { padding ->

        // ── Tela principal — explicação (rolável) + botão fixo no rodapé ─────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Conteúdo rolável
            Column(
                modifier            = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📥", fontSize = 56.sp)

                Spacer(Modifier.height(20.dp))

                Text(
                    text       = stringResource(R.string.import_title),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    textAlign  = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text      = stringResource(R.string.import_description),
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                // Detalhes do que é importado
                InfoRow(emoji = "🗓️", text = stringResource(R.string.share_info_itinerary))
                InfoRow(emoji = "🏨", text = stringResource(R.string.share_info_lodging))
                InfoRow(emoji = "👥", text = stringResource(R.string.share_info_contacts))
                InfoRow(emoji = "📄", text = stringResource(R.string.share_info_documents))
                InfoRow(emoji = "🎟️", text = stringResource(R.string.share_info_vouchers))
                InfoRow(emoji = "✈️", text = stringResource(R.string.share_info_boarding))
            }

            // Rodapé fixo — nota + botão de importar (sempre visível)
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text      = stringResource(R.string.import_new_trip_note),
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
                    Text(stringResource(R.string.import_title), color = AmberPrimary, fontWeight = FontWeight.SemiBold)
                }
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
                        text  = stringResource(R.string.import_importing),
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
            title = { Text(stringResource(R.string.import_duplicate_title)) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text  = stringResource(R.string.import_duplicate_exists, d.existingTripName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        DuplicateDateRow(stringResource(R.string.import_local_version), formatEditTimestamp(d.existingLastEditedAt))
                        DuplicateDateRow(stringResource(R.string.import_imported_version), formatEditTimestamp(d.incomingLastEditedAt))
                    }
                    Text(
                        text = when {
                            identical     -> stringResource(R.string.import_versions_identical)
                            incomingNewer -> stringResource(R.string.import_incoming_newer)
                            else          -> stringResource(R.string.import_local_newer)
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
                        stringResource(R.string.import_action),
                        color      = if (localNewer) Color.White else AmberPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDuplicate() }) {
                    Text(stringResource(R.string.import_keep_local), color = GreenMoss, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // ── Dialog de erro ───────────────────────────────────────────────────────
    if (phase is ImportPhase.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            icon  = { Text("⚠️", fontSize = 28.sp) },
            title = { Text(stringResource(R.string.import_error_title)) },
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
                    Text(stringResource(R.string.import_retry), color = AmberPrimary, fontWeight = FontWeight.SemiBold)
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
