@file:OptIn(ExperimentalMaterial3Api::class)

package com.rodrigoleao.gramado2026.ui.share_trip

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.gramado2026.ui.theme.*

@Composable
fun ShareTripScreen(
    viewModel: ShareTripViewModel,
    onBack: () -> Unit
) {
    val phase   = viewModel.phase.collectAsStateWithLifecycle().value
    val context = LocalContext.current

    // Dispara o compartilhamento quando o arquivo estiver pronto
    LaunchedEffect(phase) {
        if (phase is SharePhase.Ready) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type    = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, phase.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar viagem"))
            viewModel.clearReady()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Compartilhar viagem", color = Color.White, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenMoss)
            )
        },
        containerColor = GreenLight
    ) { padding ->

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text("📤", fontSize = 56.sp)

            Spacer(Modifier.height(20.dp))

            Text(
                text       = "Compartilhar viagem",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text      = "Gera um arquivo .travel com todo o conteúdo da viagem. Envie para outra pessoa importar no app.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            InfoRow(emoji = "🗓️", text = "Dias, atividades e roteiro completo")
            InfoRow(emoji = "🏨", text = "Dados de hospedagem")
            InfoRow(emoji = "👥", text = "Contatos importantes")
            InfoRow(emoji = "📄", text = "Documentos anexados aos dias")
            InfoRow(emoji = "🎟️", text = "Vouchers e ingressos")
            InfoRow(emoji = "✈️", text = "Cartões de embarque")

            Spacer(Modifier.weight(1f))

            Text(
                text      = "O arquivo pode conter informações sensíveis além do conteúdo do roteiro. Verifique os documentos anexados a viagem antes de compartilhar com alguém.",
                style     = MaterialTheme.typography.bodySmall,
                color     = TextSecondary,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(bottom = 20.dp)
            )

            Button(
                onClick  = { viewModel.export() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenMoss)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Compartilhar viagem", color = AmberPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // ── Dialog de loading ────────────────────────────────────────────────────
    if (phase is SharePhase.Exporting) {
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
                        text  = "Preparando arquivo…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }

    // ── Dialog de erro ───────────────────────────────────────────────────────
    if (phase is SharePhase.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            icon  = { Text("⚠️", fontSize = 28.sp) },
            title = { Text("Não foi possível compartilhar") },
            text  = {
                Text(
                    text  = (phase as SharePhase.Error).message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissError() },
                    colors  = ButtonDefaults.buttonColors(containerColor = GreenMoss)
                ) {
                    Text("OK", color = AmberPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun InfoRow(emoji: String, text: String) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}
