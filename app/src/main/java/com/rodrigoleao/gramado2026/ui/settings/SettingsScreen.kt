@file:OptIn(ExperimentalMaterial3Api::class)

package com.rodrigoleao.gramado2026.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.gramado2026.ui.theme.*
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.rodrigoleao.gramado2026.R

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val autoOpen          by viewModel.autoOpenActiveTrip.collectAsStateWithLifecycle()
    val showEmergency     by viewModel.showEmergencyContacts.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações", color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_arrow_back), contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenMoss)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text       = "Abrir viagem em curso automaticamente",
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color      = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text  = "Ao abrir o app, vai direto para a viagem ativa (quando houver apenas uma)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Box(modifier = Modifier.scale(0.80f)) {
                    Switch(
                        checked         = autoOpen,
                        onCheckedChange = { viewModel.setAutoOpenActiveTrip(it) },
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor         = GreenMoss,
                            checkedTrackColor         = AmberPrimary,
                            checkedBorderColor        = AmberPrimary,
                            uncheckedThumbColor       = GreenMoss,
                            uncheckedTrackColor       = Color(0xFF9E9E9E),
                            uncheckedBorderColor      = Color(0xFF9E9E9E)
                        )
                    )
                }
            }
            HorizontalDivider(color = Sand)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text       = "Adicionar números de emergência nos contatos das viagens",
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color      = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text  = "Exibe SAMU (192), Bombeiros (193) e Polícia Militar (190) na aba de contatos de todas as viagens",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Box(modifier = Modifier.scale(0.80f)) {
                    Switch(
                        checked         = showEmergency,
                        onCheckedChange = { viewModel.setShowEmergencyContacts(it) },
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor         = GreenMoss,
                            checkedTrackColor         = AmberPrimary,
                            checkedBorderColor        = AmberPrimary,
                            uncheckedThumbColor       = GreenMoss,
                            uncheckedTrackColor       = Color(0xFF9E9E9E),
                            uncheckedBorderColor      = Color(0xFF9E9E9E)
                        )
                    )
                }
            }
            HorizontalDivider(color = Sand)
        }
    }
}
