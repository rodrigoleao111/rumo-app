@file:OptIn(ExperimentalMaterial3Api::class)

package com.rodrigoleao.gramado2026.ui.trips

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.gramado2026.data.db.entity.TripEntity
import com.rodrigoleao.gramado2026.data.model.UiEvent
import com.rodrigoleao.gramado2026.ui.components.TripCovers
import com.rodrigoleao.gramado2026.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import com.rodrigoleao.gramado2026.R

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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()

    // Cabeçalho removido — reservamos apenas o espaço do status bar + botão de menu flutuante.
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    BackHandler(enabled = drawerState.isClosed) { showExitDialog = true }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHost.showSnackbar(event.message)
                else -> Unit
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            TripsDrawerContent(
                onNewTrip    = { scope.launch { drawerState.close() }; onNewTripClick() },
                onImportTrip = { scope.launch { drawerState.close() }; onImportTrip() },
                onSettings   = { scope.launch { drawerState.close() }; onSettingsClick() }
            )
        }
    ) {

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(
                    snackbarData     = data,
                    containerColor   = AmberPrimary,
                    contentColor     = GreenMoss
                )
            }
        }
    ) { innerPadding ->

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // ── Lista ─────────────────────────────────────────────────────────────
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(
                start  = 16.dp,
                end    = 16.dp,
                top    = statusBarTop + 66.dp,   // status bar + faixa do botão de menu
                bottom = innerPadding.calculateBottomPadding() + 32.dp
            ),
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
                else -> {
                    // Capa encolhe conforme a lista cresce: >9 → 2/3 da altura, >18 → 1/2
                    val coverScale = when {
                        trips!!.size > 18 -> 0.5f
                        trips!!.size > 9  -> 2f / 3f
                        else              -> 1f
                    }
                    items(trips!!, key = { it.id }) { trip ->
                        TripCard(
                            trip       = trip,
                            coverScale = coverScale,
                            onClick    = { onTripClick(trip.id) },
                            onShare    = { onTripShare(trip.id) },
                            onEdit     = { onTripEdit(trip.id) },
                            onDelete   = { pendingDelete = trip }
                        )
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

        // ── Botão de menu (gaveta) flutuante — cabeçalho removido ──────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp)
        ) {
            HeaderIconButton(
                icon               = Icons.Filled.Menu,
                contentDescription = "Menu",
                onClick            = { scope.launch { drawerState.open() } }
            )
        }
    } // end Scaffold content

    } // end Scaffold

    } // end ModalNavigationDrawer

    // ── Dialog de confirmação ─────────────────────────────────────────────────
    pendingDelete?.let { trip ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon  = { Icon(ImageVector.vectorResource(R.drawable.ic_delete), contentDescription = null, tint = Color(0xFFD32F2F)) },
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

// ── TRIP CARD ─────────────────────────────────────────────────────────────────

@Composable
private fun TripCard(
    trip: TripEntity,
    coverScale: Float = 1f,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val status   = tripStatus(trip.startDate, trip.endDate)
    val coverRes = TripCovers.resFor(trip.coverImage)
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        onClick   = onClick,
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border    = BorderStroke(
            width = if (status == TripStatus.ACTIVE) 2.dp else 1.dp,
            color = if (status == TripStatus.ACTIVE) GreenMoss else CardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // ── Parte superior (≈3/4): imagem de capa + título sobreposto ─────────────
        // A altura da capa escala por coverScale (ratio = largura/altura, então
        // dividir por coverScale reduz apenas a altura, mantendo a largura cheia).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio((16f / 9f) / coverScale)
        ) {
            if (coverRes != null) {
                Image(
                    painter            = painterResource(coverRes),
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                // Viagens antigas sem capa: fundo da marca com o emoji
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .background(GreenMoss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(trip.coverEmoji, fontSize = 44.sp)
                }
            }

            // Scrim para legibilidade do título
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f    to Color.Black.copy(alpha = 0.60f)
                        )
                    )
            )

            Text(
                text       = trip.name,
                style      = MaterialTheme.typography.titleLarge.copy(
                    fontSize = (26f * coverScale).sp,   // fonte escala junto com a capa
                    shadow   = Shadow(
                        color      = Color.Black.copy(alpha = 0.65f),
                        offset     = Offset(0f, 2f),
                        blurRadius = 8f
                    )
                ),
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                maxLines   = 2,
                overflow   = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier   = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )

            // Menu de ações (⋮) no canto superior direito da imagem
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { menuOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Filled.MoreVert,
                        contentDescription = "Mais opções",
                        tint               = GreenMoss,
                        modifier           = Modifier.size(22.dp)
                    )
                }

                DropdownMenu(
                    expanded         = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor   = SurfaceWhite
                ) {
                    DropdownMenuItem(
                        text        = { Text("Compartilhar", color = TextPrimary) },
                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_share), contentDescription = null, tint = GreenSage, modifier = Modifier.size(20.dp)) },
                        onClick     = { menuOpen = false; onShare() }
                    )
                    DropdownMenuItem(
                        text        = { Text("Editar", color = TextPrimary) },
                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_edit), contentDescription = null, tint = GreenMoss, modifier = Modifier.size(20.dp)) },
                        onClick     = { menuOpen = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text        = { Text("Excluir", color = Color(0xFFD32F2F)) },
                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_delete), contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp)) },
                        onClick     = { menuOpen = false; onDelete() }
                    )
                }
            }
        }

        // ── Parte inferior (≈1/4): local, data e status ───────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text     = trip.destination,
                    style    = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color    = TextPrimary
                )
                if (trip.startDate != null && trip.endDate != null) {
                    Text(
                        text  = formatDateRange(trip.startDate, trip.endDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = GreenSage
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
        icon        = ImageVector.vectorResource(R.drawable.ic_import),
        iconTint    = GreenMoss,
        iconBg      = GreenWarm,
        cardBg      = GreenForest,
        title       = "Importar viagem",
        description = "Abrir arquivo .travel",
        onClick     = onClick
    )
}

@Composable
private fun NewTripCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    ActionCard(
        modifier    = modifier,
        icon        = ImageVector.vectorResource(R.drawable.ic_add),
        iconTint    = AmberPrimary,
        iconBg      = AmberPrimary.copy(alpha = 0.22f),
        cardBg      = AmberLight,
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
    cardBg: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier  = modifier,
        onClick   = onClick,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
            Triple(countdown, AmberPrimary, GreenMoss)
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

// ── HEADER / DRAWER ─────────────────────────────────────────────────────────

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick         = onClick,
        shape           = RoundedCornerShape(12.dp),
        color           = SurfaceWhite,
        shadowElevation = 3.dp,
        modifier        = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector        = icon,
                contentDescription = contentDescription,
                tint               = GreenMoss,
                modifier           = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun TripsDrawerContent(
    onNewTrip: () -> Unit,
    onImportTrip: () -> Unit,
    onSettings: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = SurfaceWhite,
        modifier             = Modifier.width(300.dp)
    ) {
        // Cabeçalho do drawer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenMoss)
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp)
        ) {
            Text(
                text       = "Rumo",
                style      = MaterialTheme.typography.headlineSmall,
                color      = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = "Suas viagens em um só lugar",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }

        Spacer(Modifier.height(12.dp))

        NavigationDrawerItem(
            label    = { Text("Nova viagem") },
            icon     = { Icon(ImageVector.vectorResource(R.drawable.ic_add), contentDescription = null, tint = GreenMoss, modifier = Modifier.size(22.dp)) },
            selected = false,
            onClick  = onNewTrip,
            colors   = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = TextPrimary),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        NavigationDrawerItem(
            label    = { Text("Importar viagem") },
            icon     = { Icon(ImageVector.vectorResource(R.drawable.ic_import), contentDescription = null, tint = GreenMoss, modifier = Modifier.size(22.dp)) },
            selected = false,
            onClick  = onImportTrip,
            colors   = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = TextPrimary),
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = DividerColor)

        NavigationDrawerItem(
            label    = { Text("Configurações") },
            icon     = { Icon(ImageVector.vectorResource(R.drawable.ic_settings), contentDescription = null, tint = GreenMoss, modifier = Modifier.size(22.dp)) },
            selected = false,
            onClick  = onSettings,
            colors   = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = TextPrimary),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
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


