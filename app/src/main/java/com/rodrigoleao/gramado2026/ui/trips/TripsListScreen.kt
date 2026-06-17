@file:OptIn(ExperimentalMaterial3Api::class)

package com.rodrigoleao.gramado2026.ui.trips

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.gramado2026.data.db.entity.TripEntity
import com.rodrigoleao.gramado2026.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TripsListScreen(
    viewModel: TripsListViewModel,
    onTripClick: (Long) -> Unit,
    onNewTripClick: () -> Unit,
    onTripEdit: (Long) -> Unit = {},
    onTripShare: (Long) -> Unit = {},
    onImportTrip: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val trips        by viewModel.trips.collectAsStateWithLifecycle()
    val snackbarHost  = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<TripEntity?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    BackHandler { showExitDialog = true }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(
                    snackbarData     = data,
                    containerColor   = AmberPrimary,
                    contentColor     = Color.White
                )
            }
        }
    ) { innerPadding ->

    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

        // ── Header ───────────────────────────────────────────────────────────
        Surface(color = GreenMoss) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 4.dp, top = 16.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = "✈️  Minhas viagens",
                    style      = MaterialTheme.typography.headlineSmall,
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector        = Icons.Default.Settings,
                        contentDescription = "Configurações",
                        tint               = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // ── Lista ─────────────────────────────────────────────────────────────
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                // Carregando
                trips == null -> item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GreenSage, modifier = Modifier.size(32.dp))
                    }
                }

                // Sem viagens
                trips!!.isEmpty() -> item {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🗺️", fontSize = 56.sp)
                        Text(
                            text       = "Nenhuma viagem ainda",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary
                        )
                        Text(
                            text      = "Crie sua primeira viagem\nno botão abaixo",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Lista de viagens
                else -> items(trips!!, key = { it.id }) { trip ->
                    SwipeToRevealTrip(
                        trip     = trip,
                        onShare  = { onTripShare(trip.id) },
                        onEdit   = { onTripEdit(trip.id) },
                        onDelete = { pendingDelete = trip }
                    ) {
                        TripCard(trip = trip, onClick = { onTripClick(trip.id) })
                    }
                }
            }

            item {
                Row(
                    modifier              = Modifier.padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImportTripCard(
                        modifier = Modifier.weight(1f),
                        onClick  = onImportTrip
                    )
                    NewTripCard(
                        modifier = Modifier.weight(1f),
                        onClick  = onNewTripClick
                    )
                }
            }
        }
    } // end Scaffold content

    } // end Scaffold

    // ── Dialog de confirmação ─────────────────────────────────────────────────
    pendingDelete?.let { trip ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon  = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F)) },
            title = { Text("Excluir viagem?") },
            text  = {
                Text(
                    text  = "\"${trip.name}\" e todos os dias, atividades, contatos e vouchers serão apagados permanentemente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteTrip(trip); pendingDelete = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Sair do app?") },
            text  = { Text("Deseja fechar o Rumo?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { activity?.finish() },
                    colors  = ButtonDefaults.buttonColors(containerColor = GreenMoss)
                ) {
                    Text("Sair", color = AmberPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

// ── SWIPE TO REVEAL ───────────────────────────────────────────────────────────

@Composable
private fun SwipeToRevealTrip(
    trip: TripEntity,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val density       = LocalDensity.current
    val actionWidth   = 162.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX       = remember { Animatable(0f) }
    val scope         = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .height(IntrinsicSize.Min)
    ) {
        // Botões revelados ao deslizar
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(actionWidth)
                .fillMaxHeight()
        ) {
            // Compartilhar — fundo azul, ícone branco, canto esquerdo arredondado
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(GreenSage)
                    .clickable {
                        scope.launch { offsetX.animateTo(0f) }
                        onShare()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            // Editar — fundo âmbar, ícone verde
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(AmberPrimary)
                    .clickable {
                        scope.launch { offsetX.animateTo(0f) }
                        onEdit()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            // Excluir — fundo vermelho, ícone branco, canto direito arredondado
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                    .background(Color(0xFFD32F2F))
                    .clickable {
                        scope.launch { offsetX.animateTo(0f) }
                        onDelete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        // Conteúdo deslizável
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .background(GreenLight)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offsetX.snapTo((offsetX.value + delta).coerceIn(-actionWidthPx, 0f))
                        }
                    },
                    onDragStopped = { velocity ->
                        scope.launch {
                            if (offsetX.value < -actionWidthPx / 2f || velocity < -600f) {
                                offsetX.animateTo(-actionWidthPx)
                            } else {
                                offsetX.animateTo(0f)
                            }
                        }
                    }
                )
        ) {
            content()
        }
    }
}

// ── TRIP CARD ─────────────────────────────────────────────────────────────────

@Composable
private fun TripCard(trip: TripEntity, onClick: () -> Unit) {
    val status = tripStatus(trip.startDate, trip.endDate)

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border    = BorderStroke(
            width = if (status == TripStatus.ACTIVE) 2.dp else 1.dp,
            color = if (status == TripStatus.ACTIVE) GreenMoss else CardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = GreenMoss) {
                Text(text = trip.coverEmoji, fontSize = 28.sp, modifier = Modifier.padding(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = trip.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary
                )
                Text(text = trip.destination, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                if (trip.startDate != null && trip.endDate != null) {
                    Text(
                        text     = formatDateRange(trip.startDate, trip.endDate),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = GreenSage,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            StatusBadge(status, trip.startDate, trip.endDate)
        }
    }
}

// ── BOTTOM ACTION CARDS ───────────────────────────────────────────────────────

@Composable
private fun ImportTripCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    ActionCard(
        modifier    = modifier,
        icon        = Icons.Default.FileOpen,
        iconTint    = AmberPrimary,
        iconBg      = AmberPrimary.copy(alpha = 0.10f),
        title       = "Importar viagem",
        description = "Abrir arquivo .travel",
        onClick     = onClick
    )
}

@Composable
private fun NewTripCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    ActionCard(
        modifier    = modifier,
        icon        = Icons.Default.Add,
        iconTint    = GreenMoss,
        iconBg      = GreenMoss.copy(alpha = 0.10f),
        title       = "Nova viagem",
        description = "Criar do zero",
        onClick     = onClick
    )
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    iconBg: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier  = modifier,
        onClick   = onClick,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border    = BorderStroke(0.5.dp, CardBorder)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title,       fontWeight = FontWeight.SemiBold, color = TextPrimary,  fontSize = 13.sp, lineHeight = 17.sp)
                Text(description, color      = TextSecondary,       fontSize = 11.sp,     lineHeight = 15.sp)
            }
        }
    }
}

// ── STATUS ────────────────────────────────────────────────────────────────────

private enum class TripStatus { PLANNING, ACTIVE, COMPLETED }

private fun tripStatus(startDate: String?, endDate: String?): TripStatus {
    if (startDate == null || endDate == null) return TripStatus.PLANNING
    val today = LocalDate.now()
    return when {
        today < LocalDate.parse(startDate) -> TripStatus.PLANNING
        today > LocalDate.parse(endDate)   -> TripStatus.COMPLETED
        else                               -> TripStatus.ACTIVE
    }
}

@Composable
private fun StatusBadge(status: TripStatus, startDate: String?, endDate: String?) {
    val (label, bg, textColor) = when (status) {
        TripStatus.PLANNING -> {
            val countdown = countdownLabel(startDate)
            Triple(countdown, AmberPrimary, Color.White)
        }
        TripStatus.ACTIVE    -> Triple("Em curso", GreenMoss, Color.White)
        TripStatus.COMPLETED -> Triple("Concluída", GreenForest, TextSecondary)
    }
    Surface(shape = RoundedCornerShape(100.dp), color = bg) {
        Text(
            text       = label,
            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color      = textColor
        )
    }
}

private fun countdownLabel(startDate: String?): String {
    if (startDate == null) return "Planejando"
    val days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(startDate))
    return when {
        days <= 0L  -> "Planejando"
        days == 1L  -> "amanhã"
        days < 31L  -> "em $days dias"
        days < 365L -> {
            val months = days / 30
            if (months == 1L) "em 1 mês" else "em $months meses"
        }
        else -> {
            val years = days / 365
            if (years == 1L) "em 1 ano" else "em $years anos"
        }
    }
}

// ── HELPERS ───────────────────────────────────────────────────────────────────

private fun formatDateRange(startDate: String, endDate: String): String {
    val fmt   = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("pt", "BR"))
    val start = LocalDate.parse(startDate)
    val end   = LocalDate.parse(endDate)
    return if (start.month == end.month && start.year == end.year) {
        "${start.dayOfMonth}–${end.dayOfMonth} ${DateTimeFormatter.ofPattern("MMM yyyy", Locale("pt", "BR")).format(start)}"
    } else {
        "${DateTimeFormatter.ofPattern("d MMM", Locale("pt", "BR")).format(start)} – ${fmt.format(end)}"
    }
}


