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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Edit
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
import androidx.core.content.FileProvider
import com.rodrigoleao.gramado2026.data.model.BoardingPass
import com.rodrigoleao.gramado2026.notifications.NotificationHelper
import com.rodrigoleao.gramado2026.ui.theme.*
import java.io.File

// ── Helpers de tipo de transporte ─────────────────────────────────────────────

private fun transportEmoji(type: String) = when (type) {
    "FLIGHT" -> "✈️"
    "TRAIN"  -> "🚂"
    "BUS"    -> "🚌"
    "SHIP"   -> "🚢"
    else     -> "🎫"
}

private fun identifierLabel(type: String) = when (type) {
    "FLIGHT" -> "VOO"
    "TRAIN"  -> "TREM"
    "BUS"    -> "LINHA"
    "SHIP"   -> "SERVIÇO"
    else     -> "REF."
}

private fun departureLabel(type: String) = when (type) {
    "FLIGHT" -> "EMBARQUE"
    else     -> "PARTIDA"
}

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

    val savedUrls = remember(passes) {
        mutableStateMapOf<String, String>().apply {
            passes.forEach { pass ->
                prefs.getString(passKey(pass), null)?.let { put(passKey(pass), it) }
            }
        }
    }

    val savedGates = remember(passes) {
        mutableStateMapOf<String, String>().apply {
            passes.distinctBy { gateKey(it) }.forEach { pass ->
                prefs.getString(gateKey(pass), null)?.let { put(gateKey(pass), it) }
            }
        }
    }

    var dialogPass     by remember { mutableStateOf<BoardingPass?>(null) }
    var dialogGatePass by remember { mutableStateOf<BoardingPass?>(null) }

    val reminderPrefs  = remember { context.getSharedPreferences("reminders", Context.MODE_PRIVATE) }
    var reminderActive by remember { mutableStateOf(reminderPrefs.getBoolean("checkin_active", false)) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val ok = NotificationHelper.schedule(context)
            reminderActive = ok
            reminderPrefs.edit().putBoolean("checkin_active", ok).apply()
        }
    }

    if (passes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🎟️", fontSize = 36.sp)
                Text("Nenhuma passagem ainda", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("Toque em + para adicionar", style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(alpha = 0.6f))
            }
        }
        return
    }

    // Agrupa por data — mantém a ordem de inserção
    val grouped = passes.groupBy { it.date }

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top    = contentPadding.calculateTopPadding()    + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
            start  = 16.dp,
            end    = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        grouped.forEach { (date, passesForDate) ->
            item {
                Text(
                    text          = "📅  $date",
                    modifier      = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    fontSize      = 10.sp,
                    color         = GreenMoss,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
            }

            val byGroup = passesForDate.groupBy { "${it.flightNumber}_${it.origin}_${it.destination}" }
            byGroup.forEach { (_, groupPasses) ->
                item {
                    BoardingPassCard(
                        passes          = groupPasses,
                        savedUrls       = savedUrls,
                        gate            = savedGates[gateKey(groupPasses.first())] ?: "",
                        onLinkClick     = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            runCatching { context.startActivity(intent) }
                        },
                        onAddUrlClick      = { pass -> dialogPass = pass },
                        onEditGateClick    = { dialogGatePass = groupPasses.first() },
                        onEditBoardingPass = onEditBoardingPass,
                        onOpenFile         = { path ->
                            val file = File(path)
                            if (!file.exists()) {
                                android.widget.Toast.makeText(context, "Arquivo não encontrado.", android.widget.Toast.LENGTH_SHORT).show()
                                return@BoardingPassCard
                            }
                            runCatching {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }

        // Lembrete de check-in — só exibe se houver voos
        if (passes.any { it.transportType == "FLIGHT" }) {
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
        }

        // Dica de link de passagem
        if (passes.any { it.walletUrl == null && it.documentPath == null }) {
            item {
                Surface(
                    shape    = RoundedCornerShape(10.dp),
                    color    = BadgeBookedBg,
                    border   = BorderStroke(1.dp, BadgeBookedText.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text      = "ℹ️  Toque em ✏️ ao lado do passageiro para adicionar o link da passagem digital após fazer o check-in.",
                        modifier  = Modifier.padding(12.dp),
                        style     = MaterialTheme.typography.bodySmall,
                        color     = BadgeBookedText,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    dialogPass?.let { pass ->
        AddLinkDialog(
            pass       = pass,
            currentUrl = savedUrls[passKey(pass)] ?: "",
            onConfirm  = { url ->
                savedUrls[passKey(pass)] = url
                prefs.edit().putString(passKey(pass), url).apply()
                dialogPass = null
            },
            onDismiss  = { dialogPass = null }
        )
    }

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
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFFF0FFF0) else Color(0xFFFFFDF0)
        ),
        border   = BorderStroke(1.dp, if (isActive) GreenMoss.copy(alpha = 0.4f) else AmberPrimary.copy(alpha = 0.4f))
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector        = if (isActive) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                contentDescription = null,
                tint               = if (isActive) GreenMoss else AmberPrimary,
                modifier           = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "Lembrete de check-in",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (isActive) GreenMoss else TextPrimary
                )
                Text(
                    text      = if (isActive)
                        "✅  Notificação agendada para ${NotificationHelper.reminderDisplay}"
                    else
                        "Notificar 72h antes do voo de volta (${NotificationHelper.reminderDisplay})",
                    style      = MaterialTheme.typography.bodySmall,
                    color      = TextSecondary,
                    lineHeight = 17.sp
                )
            }
            if (isActive) {
                TextButton(
                    onClick = onCancel,
                    colors  = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCC3333))
                ) { Text("Cancelar", fontSize = 12.sp) }
            } else {
                Button(
                    onClick          = onActivate,
                    colors           = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                    contentPadding   = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape            = RoundedCornerShape(10.dp)
                ) { Text("🔔  Ativar", color = GreenMoss, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
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
    onLinkClick: (String) -> Unit,
    onAddUrlClick: (BoardingPass) -> Unit,
    onEditGateClick: () -> Unit,
    onEditBoardingPass: (Long) -> Unit,
    onOpenFile: (String) -> Unit
) {
    val first     = passes.first()
    val isFlight  = first.transportType == "FLIGHT"
    val typeEmoji = transportEmoji(first.transportType)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border    = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // ── Cabeçalho ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(GreenMoss)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Origem
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                        Text(
                            text       = first.origin,
                            fontSize   = if (isFlight) 32.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                            lineHeight = if (isFlight) 34.sp else 22.sp
                        )
                        if (isFlight) {
                            Text(first.originCity, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                        }
                    }
                    // Ícone de transporte + identificador
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(typeEmoji, fontSize = 22.sp, lineHeight = 26.sp)
                        Text(
                            text          = first.flightNumber,
                            fontSize      = 10.sp,
                            color         = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 1.sp,
                            textAlign     = TextAlign.Center
                        )
                    }
                    // Destino
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                        Text(
                            text       = first.destination,
                            fontSize   = if (isFlight) 32.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                            lineHeight = if (isFlight) 34.sp else 22.sp,
                            textAlign  = TextAlign.End
                        )
                        if (isFlight) {
                            Text(
                                text      = first.destinationCity,
                                fontSize  = 11.sp,
                                color     = Color.White.copy(alpha = 0.75f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }

            // ── Linha tracejada ──────────────────────────────────────────
            TearLine()

            // ── Infos ────────────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoBlock("DATA",                      first.date)
                InfoBlock(departureLabel(first.transportType), first.boardingTime)
                InfoBlock(identifierLabel(first.transportType), first.flightNumber)
                if (isFlight) {
                    GateBlock(gate = gate, onClick = onEditGateClick)
                }
            }

            // ── Observações ──────────────────────────────────────────────
            if (first.notes.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = DividerColor)
                Text(
                    text       = first.notes,
                    style      = MaterialTheme.typography.bodySmall,
                    color      = TextSecondary,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    lineHeight = 18.sp
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = DividerColor)

            // ── Passageiros ──────────────────────────────────────────────
            passes.forEachIndexed { index, pass ->
                val effectiveUrl = savedUrls[passKey(pass)] ?: pass.walletUrl
                PassengerRow(
                    pass               = pass,
                    effectiveUrl       = effectiveUrl,
                    onLinkClick        = onLinkClick,
                    onAddUrlClick      = { onAddUrlClick(pass) },
                    onOpenFile         = onOpenFile,
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
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 12.dp, height = 24.dp)
                .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 100.dp, bottomEnd = 100.dp, bottomStart = 0.dp))
                .background(Sand)
        )
        repeat(30) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(DividerColor))
            Spacer(modifier = Modifier.width(2.dp))
        }
        Box(
            modifier = Modifier
                .size(width = 12.dp, height = 24.dp)
                .clip(RoundedCornerShape(topStart = 100.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 100.dp))
                .background(Sand)
        )
    }
}

// ── INFO BLOCK ───────────────────────────────────────────────────────────────

@Composable
private fun InfoBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = TextSecondary, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary, textAlign = TextAlign.Center)
    }
}

// ── GATE BLOCK ───────────────────────────────────────────────────────────────

@Composable
private fun GateBlock(gate: String, onClick: () -> Unit) {
    val hasGate = gate.isNotBlank()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.clickable { onClick() }.padding(4.dp)
    ) {
        Text("PORTÃO", fontSize = 9.sp, color = TextSecondary, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
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

// ── PASSENGER ROW ────────────────────────────────────────────────────────────

@Composable
private fun PassengerRow(
    pass: BoardingPass,
    effectiveUrl: String?,
    onLinkClick: (String) -> Unit,
    onAddUrlClick: () -> Unit,
    onOpenFile: (String) -> Unit,
    onEditBoardingPass: () -> Unit = {}
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("PASSAGEIRO", fontSize = 9.sp, color = TextSecondary, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(pass.passenger, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                IconButton(onClick = onEditBoardingPass, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.Edit, "Editar passagem", tint = GreenSage.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                }
            }
        }

        // Ação: arquivo anexado tem prioridade sobre link
        val hasFile = !pass.documentPath.isNullOrBlank()
        when {
            hasFile -> {
                Button(
                    onClick        = { onOpenFile(pass.documentPath!!) },
                    colors         = ButtonDefaults.buttonColors(containerColor = GreenMoss),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape          = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AttachFile, null, tint = AmberPrimary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Passagem", color = AmberPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            effectiveUrl != null -> {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick        = { onLinkClick(effectiveUrl) },
                        colors         = ButtonDefaults.buttonColors(containerColor = GreenMoss),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        shape          = RoundedCornerShape(10.dp)
                    ) {
                        Text("🎫  Abrir", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = onAddUrlClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, "Editar link", tint = GreenSage, modifier = Modifier.size(18.dp))
                    }
                }
            }
            else -> {
                OutlinedButton(
                    onClick        = onAddUrlClick,
                    border         = BorderStroke(1.dp, AmberPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    shape          = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Edit, null, tint = AmberPrimary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Adicionar link", fontSize = 12.sp, color = AmberPrimary)
                }
            }
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
        title = { Text("Portão de embarque", style = MaterialTheme.typography.titleMedium, color = GreenMoss) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(flightLabel, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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
                    "💡 Consulte o app da companhia ou o painel do terminal no dia do embarque.",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = TextSecondary.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(gate.trim()) },
                enabled = gate.isNotBlank(),
                colors  = ButtonDefaults.buttonColors(containerColor = GreenMoss)
            ) { Text("Salvar", color = Color.White) }
        },
        dismissButton = {
            if (currentGate.isNotBlank()) {
                TextButton(onClick = { onConfirm("") }) { Text("Limpar", color = AmberPrimary) }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }
            }
        }
    )
}

// ── DIALOG PARA ADICIONAR / EDITAR LINK ──────────────────────────────────────

@Composable
private fun AddLinkDialog(
    pass: BoardingPass,
    currentUrl: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceWhite,
        title = {
            Text(
                "${pass.origin} → ${pass.destination}  ·  ${pass.flightNumber}",
                style = MaterialTheme.typography.titleMedium,
                color = GreenMoss
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(pass.passenger, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                OutlinedTextField(
                    value           = url,
                    onValueChange   = { url = it },
                    label           = { Text("Link da passagem") },
                    placeholder     = { Text("https://...") },
                    modifier        = Modifier.fillMaxWidth(),
                    minLines        = 3,
                    maxLines        = 5,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenMoss,
                        focusedLabelColor  = GreenMoss
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(url.trim()) },
                enabled = url.isNotBlank() && url.startsWith("http"),
                colors  = ButtonDefaults.buttonColors(containerColor = GreenMoss)
            ) { Text("Salvar", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }
        }
    )
}
