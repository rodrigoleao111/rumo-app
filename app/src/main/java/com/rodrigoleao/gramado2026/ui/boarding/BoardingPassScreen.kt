@file:OptIn(ExperimentalMaterial3Api::class)

package com.rodrigoleao.gramado2026.ui.boarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rodrigoleao.gramado2026.data.model.BoardingPass
import com.rodrigoleao.gramado2026.notifications.NotificationHelper
import com.rodrigoleao.gramado2026.ui.theme.*

// Chave única por cartão de embarque (por passageiro)
private fun passKey(pass: BoardingPass): String =
    "${pass.flightNumber}_${pass.origin}_${pass.destination}_${pass.passenger.split(" ").first().lowercase()}"

// Chave do portão (por voo — o portão é o mesmo para todos os passageiros)
private fun gateKey(pass: BoardingPass): String =
    "gate_${pass.flightNumber}_${pass.origin}_${pass.destination}"

@Composable
fun BoardingPassScreen(
    passes: List<BoardingPass>,
    contentPadding: PaddingValues = PaddingValues(),
    onEditBoardingPass: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs   = remember { context.getSharedPreferences("boarding_passes", Context.MODE_PRIVATE) }

    // Mapa mutável de URLs salvas: passKey → url
    val savedUrls = remember {
        mutableStateMapOf<String, String>().apply {
            passes.forEach { pass ->
                prefs.getString(passKey(pass), null)?.let { put(passKey(pass), it) }
            }
        }
    }

    // Portões de embarque salvos: gateKey → portão (ex. "A12")
    val savedGates = remember {
        mutableStateMapOf<String, String>().apply {
            // Carrega portões já salvos para cada voo único
            passes.distinctBy { gateKey(it) }.forEach { pass ->
                prefs.getString(gateKey(pass), null)?.let { put(gateKey(pass), it) }
            }
        }
    }

    // Estado do dialog de adicionar URL
    var dialogPass     by remember { mutableStateOf<BoardingPass?>(null) }
    // Estado do dialog de portão (guarda o representante do voo)
    var dialogGatePass by remember { mutableStateOf<BoardingPass?>(null) }

    // Estado do lembrete (SharedPreferences como source of truth simples)
    val reminderPrefs = remember { context.getSharedPreferences("reminders", Context.MODE_PRIVATE) }
    var reminderActive by remember { mutableStateOf(reminderPrefs.getBoolean("checkin_active", false)) }

    // Launcher de permissão para POST_NOTIFICATIONS (Android 13+)
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val ok = NotificationHelper.schedule(context)
            reminderActive = ok
            reminderPrefs.edit().putBoolean("checkin_active", ok).apply()
        }
    }

    // Agrupamento: IDA (09 Jun) e VOLTA (13 Jun)
    val grouped = passes.groupBy { it.date }

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top    = contentPadding.calculateTopPadding()    + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 12.dp,
            start  = 16.dp,
            end    = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            grouped.forEach { (date, passesForDate) ->
                item {
                    val label = if (date == "09 Jun 2026") "IDA" else "VOLTA"
                    Text(
                        text = "✈️  $label — $date",
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                        fontSize = 10.sp,
                        color = GreenMoss,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp
                    )
                }

                // Agrupar por voo (mesmo origin+destination+flightNumber)
                val byFlight = passesForDate.groupBy { "${it.flightNumber}_${it.origin}_${it.destination}" }
                byFlight.forEach { (_, flightPasses) ->
                    item {
                        BoardingPassCard(
                            passes    = flightPasses,
                            savedUrls = savedUrls,
                            gate      = savedGates[gateKey(flightPasses.first())] ?: "",
                            onWalletClick   = { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            onAddUrlClick      = { pass -> dialogPass = pass },
                            onEditGateClick    = { dialogGatePass = flightPasses.first() },
                            onEditBoardingPass = onEditBoardingPass
                        )
                    }
                }
            }

            // ── Card de lembrete de check-in ─────────────────────────────
            item {
                CheckInReminderCard(
                    isActive   = reminderActive,
                    onActivate = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val ok = NotificationHelper.schedule(context)
                            reminderActive = ok
                            reminderPrefs.edit().putBoolean("checkin_active", ok).apply()
                        }
                    },
                    onCancel = {
                        NotificationHelper.cancel(context)
                        reminderActive = false
                        reminderPrefs.edit().putBoolean("checkin_active", false).apply()
                    }
                )
            }

            // ── Info wallet ───────────────────────────────────────────────
            item {
                Surface(
                    shape    = RoundedCornerShape(10.dp),
                    color    = BadgeBookedBg,
                    border   = BorderStroke(1.dp, BadgeBookedText.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ℹ️  Toque em \"🎫 Wallet\" para salvar o cartão no Google Wallet (funciona offline após salvo). " +
                               "Para cartões pendentes, toque em ✏️ e cole o link após fazer o check-in.",
                        modifier  = Modifier.padding(12.dp),
                        style     = MaterialTheme.typography.bodySmall,
                        color     = BadgeBookedText,
                        lineHeight = 18.sp
                    )
                }
            }
    }

    // Dialog de URL do Google Wallet
    dialogPass?.let { pass ->
        AddWalletUrlDialog(
            pass          = pass,
            currentUrl    = savedUrls[passKey(pass)] ?: "",
            onConfirm     = { url ->
                savedUrls[passKey(pass)] = url
                prefs.edit().putString(passKey(pass), url).apply()
                dialogPass = null
            },
            onDismiss     = { dialogPass = null }
        )
    }

    // Dialog de portão de embarque
    dialogGatePass?.let { pass ->
        EditGateDialog(
            currentGate = savedGates[gateKey(pass)] ?: "",
            flightLabel = "${pass.origin} → ${pass.destination}  ·  ${pass.flightNumber}",
            onConfirm   = { gate ->
                if (gate.isBlank()) {
                    savedGates.remove(gateKey(pass))
                    prefs.edit().remove(gateKey(pass)).apply()
                } else {
                    savedGates[gateKey(pass)] = gate
                    prefs.edit().putString(gateKey(pass), gate).apply()
                }
                dialogGatePass = null
            },
            onDismiss   = { dialogGatePass = null }
        )
    }
}

// ── REMINDER CARD ────────────────────────────────────────────────────────────

@Composable
private fun CheckInReminderCard(
    isActive: Boolean,
    onActivate: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFFF0FFF0) else Color(0xFFFFFDF0)
        ),
        border = BorderStroke(
            1.dp,
            if (isActive) GreenMoss.copy(alpha = 0.4f) else AmberPrimary.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                contentDescription = null,
                tint = if (isActive) GreenMoss else AmberPrimary,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lembrete de check-in",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) GreenMoss else TextPrimary
                )
                Text(
                    text = if (isActive)
                        "✅  Notificação agendada para ${NotificationHelper.reminderDisplay}"
                    else
                        "Notificar 72h antes do voo de volta (${NotificationHelper.reminderDisplay})",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
            }
            if (isActive) {
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCC3333))
                ) {
                    Text("Cancelar", fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = onActivate,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🔔  Ativar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── BOARDING PASS CARD ───────────────────────────────────────────────────────

@Composable
private fun BoardingPassCard(
    passes: List<BoardingPass>,
    savedUrls: Map<String, String>,
    gate: String,
    onWalletClick: (String) -> Unit,
    onAddUrlClick: (BoardingPass) -> Unit,
    onEditGateClick: () -> Unit,
    onEditBoardingPass: (Long) -> Unit
) {
    val first = passes.first()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // ── Cabeçalho verde ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(GreenMoss)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Origem
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(first.origin, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 34.sp)
                        Text(first.originCity, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                    // Avião + número de voo
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FlightTakeoff, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(28.dp))
                        Text(first.flightNumber, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), letterSpacing = 1.sp)
                    }
                    // Destino
                    Column(horizontalAlignment = Alignment.End) {
                        Text(first.destination, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 34.sp)
                        Text(first.destinationCity, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                }
            }

            // ── Linha tracejada (tear line) ──────────────────────────────
            TearLine()

            // ── Infos do voo ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoBlock("DATA",     first.date)
                InfoBlock("EMBARQUE", first.boardingTime)
                InfoBlock("VOO",      first.flightNumber)
                GateBlock(gate = gate, onClick = onEditGateClick)
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = DividerColor)

            // ── Passageiros ──────────────────────────────────────────────
            passes.forEachIndexed { index, pass ->
                val effectiveUrl = savedUrls[passKey(pass)] ?: pass.walletUrl
                PassengerRow(
                    pass               = pass,
                    effectiveUrl       = effectiveUrl,
                    onWalletClick      = onWalletClick,
                    onAddUrlClick      = { onAddUrlClick(pass) },
                    onEditBoardingPass = { onEditBoardingPass(pass.id) }
                )
                if (index < passes.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = DividerColor)
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

// ── TEAR LINE ────────────────────────────────────────────────────────────────

@Composable
private fun TearLine() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 12.dp, height = 24.dp)
                .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 100.dp, bottomEnd = 100.dp, bottomStart = 0.dp))
                .background(GreenLight)
        )
        repeat(30) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(DividerColor))
            Spacer(modifier = Modifier.width(2.dp))
        }
        Box(
            modifier = Modifier
                .size(width = 12.dp, height = 24.dp)
                .clip(RoundedCornerShape(topStart = 100.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 100.dp))
                .background(GreenLight)
        )
    }
}

// ── INFO BLOCK ───────────────────────────────────────────────────────────────

@Composable
private fun InfoBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = TextSecondary, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary, textAlign = TextAlign.Center)
    }
}

// ── GATE BLOCK ───────────────────────────────────────────────────────────────

@Composable
private fun GateBlock(gate: String, onClick: () -> Unit) {
    val hasGate = gate.isNotBlank()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Text(
            text          = "PORTÃO",
            fontSize      = 9.sp,
            color         = TextSecondary,
            letterSpacing = 1.5.sp,
            fontWeight    = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text       = if (hasGate) gate else "—",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color      = if (hasGate) GreenMoss else TextSecondary.copy(alpha = 0.5f),
                textAlign  = TextAlign.Center
            )
            Icon(
                imageVector        = Icons.Default.Edit,
                contentDescription = "Editar portão",
                tint               = if (hasGate) GreenSage else TextSecondary.copy(alpha = 0.35f),
                modifier           = Modifier.size(11.dp)
            )
        }
    }
}

// ── EDIT GATE DIALOG ─────────────────────────────────────────────────────────

@Composable
private fun EditGateDialog(
    currentGate: String,
    flightLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var gate by remember { mutableStateOf(currentGate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceWhite,
        title = {
            Text(
                text  = "Portão de embarque",
                style = MaterialTheme.typography.titleMedium,
                color = GreenMoss
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text  = flightLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value         = gate,
                    onValueChange = { gate = it.uppercase().take(6) },
                    label         = { Text("Portão") },
                    placeholder   = { Text("Ex: A12, B3, C05") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenMoss,
                        focusedLabelColor  = GreenMoss
                    )
                )
                Text(
                    text  = "💡 Consulte o app da Azul ou o painel do aeroporto no dia do voo.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { onConfirm(gate.trim()) },
                enabled  = gate.isNotBlank(),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenMoss)
            ) {
                Text("Salvar", color = Color.White)
            }
        },
        dismissButton = {
            if (currentGate.isNotBlank()) {
                // Portão já salvo: opção de limpar
                TextButton(onClick = { onConfirm("") }) {
                    Text("Limpar", color = AmberPrimary)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        }
    )
}

// ── PASSENGER ROW ────────────────────────────────────────────────────────────

@Composable
private fun PassengerRow(
    pass: BoardingPass,
    effectiveUrl: String?,
    onWalletClick: (String) -> Unit,
    onAddUrlClick: () -> Unit,
    onEditBoardingPass: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Nome do passageiro
        Column(modifier = Modifier.weight(1f)) {
            Text("PASSAGEIRO", fontSize = 9.sp, color = TextSecondary, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(pass.passenger, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                IconButton(onClick = onEditBoardingPass, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.Edit, "Editar passagem", tint = GreenSage.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                }
            }
        }

        // Ação
        if (effectiveUrl != null) {
            // Cartão disponível → botão Wallet + editar
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { onWalletClick(effectiveUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenMoss),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🎫  Wallet", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                // Editar URL (caso queira trocar)
                IconButton(
                    onClick = onAddUrlClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Edit, "Editar link", tint = GreenSage, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            // Check-in pendente → botão para adicionar link
            OutlinedButton(
                onClick = onAddUrlClick,
                border = BorderStroke(1.dp, AmberPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Edit, null, tint = AmberPrimary, modifier = Modifier.size(14.dp).padding(end = 2.dp))
                Spacer(Modifier.width(4.dp))
                Text("Adicionar link", fontSize = 12.sp, color = AmberPrimary)
            }
        }
    }
}

// ── DIALOG PARA ADICIONAR / EDITAR URL ──────────────────────────────────────

@Composable
private fun AddWalletUrlDialog(
    pass: BoardingPass,
    currentUrl: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Text(
                text = "${pass.origin} → ${pass.destination}  ·  ${pass.flightNumber}",
                style = MaterialTheme.typography.titleMedium,
                color = GreenMoss
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = pass.passenger,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Link do Google Wallet") },
                    placeholder = { Text("https://pay.google.com/gp/v/save/...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenMoss,
                        focusedLabelColor = GreenMoss
                    )
                )
                if (url.isNotBlank() && !url.startsWith("https://pay.google.com")) {
                    Text(
                        text = "⚠️ O link deve começar com https://pay.google.com",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberPrimary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(url.trim()) },
                enabled = url.isNotBlank() && url.startsWith("https://"),
                colors = ButtonDefaults.buttonColors(containerColor = GreenMoss)
            ) {
                Text("Salvar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
