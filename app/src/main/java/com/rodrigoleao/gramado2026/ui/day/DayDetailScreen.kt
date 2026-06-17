@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.rodrigoleao.gramado2026.ui.day

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rodrigoleao.gramado2026.data.model.*
import com.rodrigoleao.gramado2026.data.weather.LiveWeatherDay
import com.rodrigoleao.gramado2026.data.weather.WeatherRepository
import com.rodrigoleao.gramado2026.ui.components.BadgeChip
import com.rodrigoleao.gramado2026.ui.theme.*

@Composable
fun DayDetailScreen(
    day: TravelDay,
    refreshKey: Long = 0L,
    tripLat: Double? = null,
    tripLon: Double? = null,
    tripStartDate: String? = null,
    tripEndDate: String? = null,
    onBack: () -> Unit = {},
    onBustourMapClick: () -> Unit = {},
    onEditDay: () -> Unit = {},
    onEditActivity: (Long) -> Unit = {},
    onDeleteActivity: (Long) -> Unit = {},
    onAddActivity: () -> Unit = {},
    onMoveActivity: (from: Int, to: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var activityToDelete by remember { mutableStateOf<Long?>(null) }

    var liveWeather    by remember { mutableStateOf<LiveWeatherDay?>(null) }
    var weatherLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0L) snackbarHostState.showSnackbar("Alterações salvas ✓")
    }

    LaunchedEffect(refreshTrigger) {
        // Dias passados não têm previsão disponível — evita chamar a API e receber zeros
        if (day.date < LocalDate.now()) {
            weatherLoading = false
        } else {
            weatherLoading = true
            val all = if (refreshTrigger == 0)
                WeatherRepository.getWeather(context, tripLat, tripLon, tripStartDate, tripEndDate)
            else
                WeatherRepository.refresh(context, tripLat, tripLon, tripStartDate, tripEndDate)
            liveWeather    = all?.get(day.date.toString())
            weatherLoading = false
        }
    }

    val expandedActivities = remember { mutableStateMapOf<Int, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(day.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Text("${day.dayOfWeek}, ${day.date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("pt", "BR")))}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onEditDay) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar dia", tint = GreenMoss)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick           = onAddActivity,
                containerColor    = AmberPrimary,
                contentColor      = Color.White,
                shape             = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar atividade")
            }
        },
        containerColor = GreenLight,
        snackbarHost   = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = AmberPrimary,
                    contentColor   = Color.White
                )
            }
        }
    ) { innerPadding ->

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top    = innerPadding.calculateTopPadding()    + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp
            )
        ) {
            // ── Clima ────────────────────────────────────────────────────
            item {
                WeatherCard(
                    liveWeather = liveWeather,
                    isLoading   = weatherLoading,
                    onRefresh   = { refreshTrigger = refreshTrigger + 1 }
                )
            }

            // ── Alerta do dia ────────────────────────────────────────────
            day.dayAlert?.let { alert ->
                item { DayAlertCard(message = alert) }
            }

            // ── Checklist de vouchers ────────────────────────────────────
            if (day.vouchers.isNotEmpty()) {
                item { VoucherChecklist(vouchers = day.vouchers) }
            }

            // ── Link / documento do dia ──────────────────────────────────
            if (!day.dayLinkUrl.isNullOrBlank()) {
                item {
                    DayLinkCard(
                        label = day.dayLinkLabel.ifBlank { "Ver documento" },
                        url   = day.dayLinkUrl,
                        context = context
                    )
                }
            }

            // ── Documento do dia ─────────────────────────────────────────
            if (!day.dayDocumentPath.isNullOrBlank()) {
                item {
                    DayDocumentCard(
                        name    = day.dayDocumentTitle.ifBlank { day.dayDocumentName.ifBlank { "Documento" } },
                        path    = day.dayDocumentPath,
                        context = context
                    )
                }
            }

            // ── Botão mapa Bustour (somente Dia 3) ──────────────────────
            if (day.id == 3) {
                item { BustourMapButton(onClick = onBustourMapClick) }
            }

            // ── Divisor de início da timeline ────────────────────────────
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = DividerColor
                )
            }

            if (day.activities.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📋", fontSize = 36.sp)
                            Text("Nenhuma atividade ainda", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text("Toque em + para adicionar", style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // ── Atividades (colapsáveis) ─────────────────────────────────
            itemsIndexed(day.activities) { index, activity ->
                ActivityItem(
                    activity   = activity,
                    expanded   = expandedActivities[index] == true,
                    onToggle   = { expandedActivities[index] = !(expandedActivities[index] ?: false) },
                    onEdit     = { onEditActivity(activity.id) },
                    onDelete   = { activityToDelete = activity.id },
                    onMapClick = activity.mapQuery?.let { query ->
                        {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}"))
                            context.startActivity(intent)
                        }
                    },
                    onUberClick = activity.uberDestination?.let { dest ->
                        {
                            val url = "https://m.uber.com/ul/?action=setPickup" +
                                "&pickup=my_location" +
                                "&dropoff[formatted_address]=${Uri.encode(dest)}" +
                                "&dropoff[nickname]=${Uri.encode(activity.name)}"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = DividerColor)
            }
        }
    }

    // Dialog de confirmação de exclusão
    activityToDelete?.let { actId ->
        AlertDialog(
            onDismissRequest = { activityToDelete = null },
            title = { Text("Excluir atividade?") },
            text  = { Text("Esta atividade será removida permanentemente do dia.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteActivity(actId)
                    activityToDelete = null
                }) { Text("Excluir", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { activityToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

// ── DAY LINK CARD ─────────────────────────────────────────────────────────────

@Composable
private fun DayLinkCard(label: String, url: String, context: android.content.Context) {
    Card(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFFE8F0E8)),
        border   = BorderStroke(1.dp, GreenMoss.copy(alpha = 0.4f))
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🔗", fontSize = 24.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = GreenMoss)
                Text(url, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
            }
            Text("›", fontSize = 20.sp, color = GreenMoss, fontWeight = FontWeight.Bold)
        }
    }
}

// ── DAY DOCUMENT CARD ─────────────────────────────────────────────────────────

@Composable
private fun DayDocumentCard(name: String, path: String, context: Context) {
    Card(
        onClick = {
            val file = File(path)
            if (!file.exists()) return@Card
            val ext  = file.extension.lowercase()
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
            val uri  = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Abrir com…"))
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFFE8F0E8)),
        border   = BorderStroke(1.dp, GreenMoss.copy(alpha = 0.4f))
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📎", fontSize = 24.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = GreenMoss, maxLines = 1)
                Text("Toque para abrir", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text("›", fontSize = 20.sp, color = GreenMoss, fontWeight = FontWeight.Bold)
        }
    }
}

// ── BUSTOUR MAP BUTTON ────────────────────────────────────────────────────────

@Composable
private fun BustourMapButton(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0E8)),
        border = BorderStroke(1.dp, GreenMoss.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🗺️", fontSize = 24.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text("Mapa de Rotas — Bustour", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = GreenMoss)
                Text("Linha Vermelha e Amarela com todas as paradas", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text("›", fontSize = 20.sp, color = GreenMoss, fontWeight = FontWeight.Bold)
        }
    }
}

// ── WEATHER CARD ──────────────────────────────────────────────────────────────

@Composable
private fun WeatherCard(liveWeather: LiveWeatherDay?, isLoading: Boolean, onRefresh: () -> Unit) {
    val hasData   = liveWeather != null && !(liveWeather.minTemp == 0 && liveWeather.maxTemp == 0)
    val condition = liveWeather?.condition

    val infiniteTransition = rememberInfiniteTransition(label = "weather_refresh")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(animation = tween(700, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label         = "spin"
    )

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border    = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            when {
                isLoading -> Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp, color = AmberPrimary)
                }
                hasData -> Text(liveWeather!!.emoji, fontSize = 38.sp)
                // sem dados: sem ícone
            }

            Column(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    // nada além do spinner
                } else if (!hasData) {
                    Text(
                        "Temperatura indisponível no momento",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${liveWeather!!.minTemp}°C  ~  ${liveWeather.maxTemp}°C", style = MaterialTheme.typography.titleMedium, color = AmberPrimary, fontWeight = FontWeight.SemiBold)
                        Text("● ao vivo", fontSize = 9.sp, color = GreenMoss, fontWeight = FontWeight.SemiBold)
                    }
                    if (condition != null) Text(condition, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            IconButton(onClick = { if (!isLoading) onRefresh() }, modifier = Modifier.size(36.dp), enabled = !isLoading) {
                Icon(Icons.Default.Refresh, contentDescription = if (isLoading) "Atualizando…" else "Atualizar clima",
                    tint = if (isLoading) AmberPrimary else GreenSage,
                    modifier = Modifier.size(22.dp).rotate(if (isLoading) spinAngle else 0f))
            }
        }
    }
}

// ── ALERT CARD ────────────────────────────────────────────────────────────────

@Composable
private fun DayAlertCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape    = RoundedCornerShape(10.dp),
        color    = Color(0xFFFFF8F0),
        border   = BorderStroke(1.dp, Color(0xFFE8A040))
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Text("⚠️", fontSize = 15.sp, modifier = Modifier.padding(top = 1.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A3200), lineHeight = 18.sp)
        }
    }
}

// ── VOUCHER CHECKLIST ─────────────────────────────────────────────────────────

@Composable
private fun VoucherChecklist(vouchers: List<DayVoucher>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("🎫  LEVAR HOJE", fontSize = 10.sp, color = GreenMoss, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            vouchers.forEach { voucher ->
                Surface(shape = RoundedCornerShape(100.dp), color = BadgeBookedBg, border = BorderStroke(0.5.dp, BadgeBookedText.copy(alpha = 0.35f))) {
                    Text("${voucher.emoji}  ${voucher.label}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, color = BadgeBookedText)
                }
            }
        }
    }
}

// ── ACTIVITY ITEM ─────────────────────────────────────────────────────────────

@Composable
private fun ActivityItem(
    activity: TravelActivity,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMapClick: (() -> Unit)?,
    onUberClick: (() -> Unit)?
) {
    SwipeToRevealActivity(onEdit = onEdit, onDelete = onDelete) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .animateContentSize(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing))
                .padding(start = 16.dp, end = 16.dp, top = 3.dp, bottom = 3.dp)
        ) {
            // Barra vertical âmbar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (expanded) AmberPrimary else AmberPrimary.copy(alpha = 0.45f))
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier          = Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(activity.time, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AmberPrimary)
                        Text("${activity.emoji} ${activity.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        if (activity.badges.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 2.dp)) {
                                activity.badges.forEach { badge -> BadgeChip(badge = badge) }
                            }
                        }
                    }

                    Icon(
                        imageVector        = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Recolher" else "Ver detalhes",
                        tint               = GreenSage.copy(alpha = 0.65f),
                        modifier           = Modifier.size(24.dp)
                    )
                }

                if (expanded) {
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(activity.detail, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 22.sp)
                        if (activity.walkStops.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            WalkRouteSection(stops = activity.walkStops)
                        }
                        if (onMapClick != null || onUberClick != null) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                onMapClick?.let { onClick ->
                                    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.height(30.dp), border = BorderStroke(1.dp, GreenMoss)) {
                                        Text("🗺️  Maps", fontSize = 12.sp, color = GreenMoss)
                                    }
                                }
                                onUberClick?.let { onClick ->
                                    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.height(30.dp), border = BorderStroke(1.dp, Color(0xFF444444))) {
                                        Text("🚗  Uber", fontSize = 12.sp, color = Color(0xFF444444))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── SWIPE TO REVEAL ───────────────────────────────────────────────────────────

@Composable
private fun SwipeToRevealActivity(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val density      = LocalDensity.current
    val actionWidth  = 128.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX      = remember { Animatable(0f) }
    val scope        = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Botões de ação revelados ao deslizar
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(actionWidth)
                .fillMaxHeight()
        ) {
            // Editar — fundo dourado, lápis verde
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
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = GreenMoss, modifier = Modifier.size(22.dp))
            }
            // Deletar — fundo vermelho, ícone branco
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
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

// ── WALK ROUTE ────────────────────────────────────────────────────────────────

@Composable
private fun WalkRouteSection(stops: List<WalkStop>) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
        stops.forEachIndexed { index, stop ->
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(14.dp)) {
                    Spacer(Modifier.height(3.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (stop.isLast) AmberPrimary else GreenMoss))
                    if (index < stops.size - 1) Box(modifier = Modifier.width(1.5.dp).height(30.dp).background(GreenMoss.copy(alpha = 0.25f)))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.padding(bottom = 2.dp)) {
                    Text("${stop.emoji}  ${stop.label}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontWeight = if (stop.isLast) FontWeight.SemiBold else FontWeight.Normal)
                    stop.sublabel?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(alpha = 0.6f)) }
                }
            }
        }
    }
}
