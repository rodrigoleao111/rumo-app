@file:OptIn(ExperimentalMaterial3Api::class)

package com.rodrigoleao.pipa.ui.share_trip

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.rodrigoleao.pipa.ui.theme.*
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.rodrigoleao.pipa.R

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
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_title)))
            viewModel.clearReady()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.share_title), color = Color.White, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.common_back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenMoss)
            )
        },
        containerColor = Sand
    ) { padding ->

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
                Text("📤", fontSize = 56.sp)

                Spacer(Modifier.height(20.dp))

                Text(
                    text       = stringResource(R.string.share_title),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    textAlign  = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text      = stringResource(R.string.share_description),
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                InfoRow(emoji = "🗓️", text = stringResource(R.string.share_info_itinerary))
                InfoRow(emoji = "🏨", text = stringResource(R.string.share_info_lodging))
                InfoRow(emoji = "👥", text = stringResource(R.string.share_info_contacts))
                InfoRow(emoji = "📄", text = stringResource(R.string.share_info_documents))
                InfoRow(emoji = "🎟️", text = stringResource(R.string.share_info_vouchers))
                InfoRow(emoji = "✈️", text = stringResource(R.string.share_info_boarding))
            }

            // Rodapé fixo — aviso + botão de compartilhar (sempre visível)
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text      = stringResource(R.string.share_sensitive_warning),
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
                    Icon(ImageVector.vectorResource(R.drawable.ic_share), contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.share_title), color = AmberPrimary, fontWeight = FontWeight.SemiBold)
                }
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
                        text  = stringResource(R.string.share_preparing),
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
            title = { Text(stringResource(R.string.share_error_title)) },
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
                    Text(stringResource(R.string.common_ok), color = AmberPrimary, fontWeight = FontWeight.SemiBold)
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
