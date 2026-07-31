package com.rodrigoleao.pipa.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rodrigoleao.pipa.data.model.TravelDay
import com.rodrigoleao.pipa.data.weather.LiveWeatherDay
import com.rodrigoleao.pipa.data.weather.WeatherRepository
import com.rodrigoleao.pipa.ui.components.TripCovers
import com.rodrigoleao.pipa.ui.components.WeatherIcon
import com.rodrigoleao.pipa.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.rodrigoleao.pipa.R

private val COVER_HEADER_MAX = 220.dp           // altura no topo da página
private val COVER_HEADER_MIN_CONTENT = 58.dp    // faixa dos botões (abaixo do status bar) quando colapsado

@Composable
fun HomeScreen(
    days: List<TravelDay>,
    hotelName: String = "",
    hotelAddress: String = "",
    hotelPhone: String = "",
    tripLat: Double? = null,
    tripLon: Double? = null,
    tripStartDate: String? = null,
    tripEndDate: String? = null,
    coverImage: String = "",
    coverEmoji: String = "",
    tripName: String = "",
    onBack: () -> Unit = {},
    onShareTrip: () -> Unit = {},
    onEditTrip: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    onDayClick: (Int) -> Unit
) {
    val context   = LocalContext.current
    val listState = rememberLazyListState()
    val todayIdx  = remember(days) { days.indexOfFirst { it.isToday } }

    var liveWeather    by remember { mutableStateOf<Map<String, LiveWeatherDay>?>(null) }
    var weatherLoading by remember { mutableStateOf(true) }

    // Rola para o dia atual apenas na primeira vez que days carrega com um "hoje" válido
    var scrolledToToday by remember { mutableStateOf(false) }
    LaunchedEffect(todayIdx) {
        if (todayIdx >= 0 && !scrolledToToday) {
            listState.animateScrollToItem(index = todayIdx)
            scrolledToToday = true
        }
    }

    // Re-executa sempre que as coordenadas ou datas mudam (ex: tripData carregou do DB após primeira composição)
    LaunchedEffect(tripLat, tripLon, tripStartDate, tripEndDate) {
        weatherLoading = true
        try {
            liveWeather = WeatherRepository.getWeather(context, tripLat, tripLon, tripStartDate, tripEndDate)
        } finally {
            weatherLoading = false
        }
    }

    // ── Cabeçalho colapsável ──────────────────────────────────────────────────
    val density  = LocalDensity.current
    val maxPx    = with(density) { COVER_HEADER_MAX.toPx() }
    val statusPx = WindowInsets.statusBars.getTop(density).toFloat()
    val minPx    = statusPx + with(density) { COVER_HEADER_MIN_CONTENT.toPx() }
    var headerPx by remember { mutableStateOf(maxPx) }

    val headerNestedScroll = remember(minPx, maxPx) {
        object : NestedScrollConnection {
            // Rolar para baixo: encolhe o cabeçalho ANTES de a lista rolar.
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta >= 0f) return Offset.Zero
                val newPx    = (headerPx + delta).coerceIn(minPx, maxPx)
                val consumed = newPx - headerPx
                headerPx = newPx
                return Offset(0f, consumed)
            }
            // Rolar para cima: a lista volta ao topo primeiro; o excedente volta a crescer o cabeçalho.
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta <= 0f) return Offset.Zero
                val newPx = (headerPx + delta).coerceIn(minPx, maxPx)
                val used  = newPx - headerPx
                headerPx = newPx
                return Offset(0f, used)
            }
        }
    }

    val headerDp         = with(density) { headerPx.toDp() }
    val collapseFraction = ((headerPx - minPx) / (maxPx - minPx)).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(headerNestedScroll)
    ) {
        LazyColumn(
            state          = listState,
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top    = headerDp + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 12.dp,
                start  = 16.dp,
                end    = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(days) { day ->
                DayCard(
                    day         = day,
                    liveWeather = liveWeather?.get(day.date.toString()),
                    isLoading   = weatherLoading,
                    onClick     = { onDayClick(day.id) }
                )
            }

            if (hotelName.isNotBlank()) {
                item { HotelCard(hotelName = hotelName, hotelAddress = hotelAddress, hotelPhone = hotelPhone) }
            }
        }

        // Cabeçalho de capa de altura variável (colapsa ao rolar; título esmaece)
        TripCoverHeader(
            coverImage = coverImage,
            coverEmoji = coverEmoji,
            title      = tripName,
            titleAlpha = collapseFraction,
            onBack     = onBack,
            onShare    = onShareTrip,
            onEdit     = onEditTrip,
            modifier   = Modifier
                .fillMaxWidth()
                .height(headerDp)
                .align(Alignment.TopCenter)
        )
    }
}

// ── CABEÇALHO DE CAPA ─────────────────────────────────────────────────────────

@Composable
private fun TripCoverHeader(
    coverImage: String,
    coverEmoji: String,
    title: String,
    titleAlpha: Float,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coverRes = TripCovers.resFor(coverImage)

    Box(modifier = modifier) {
        // Imagem de capa
        if (coverRes != null) {
            Image(
                painter            = painterResource(coverRes),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                alignment          = Alignment.TopCenter,   // mantém o topo da imagem sempre visível
                modifier           = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(GreenMoss),
                contentAlignment = Alignment.Center
            ) {
                Text(coverEmoji, fontSize = 48.sp)
            }
        }

        // Scrim (escurece topo e base para legibilidade dos botões e do título)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.0f  to Color.Black.copy(alpha = 0.28f),
                        0.30f to Color.Transparent,
                        0.70f to Color.Transparent,
                        1.0f  to Color.Black.copy(alpha = 0.55f)
                    )
                )
        )

        // Botões: voltar (esq.) + compartilhar/editar (dir.)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            HeaderIconButton(
                icon               = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.home_back_to_trips),
                onClick            = onBack
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderIconButton(
                    icon               = ImageVector.vectorResource(R.drawable.ic_share),
                    contentDescription = stringResource(R.string.home_share_trip),
                    onClick            = onShare
                )
                HeaderIconButton(
                    icon               = ImageVector.vectorResource(R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.home_edit_trip),
                    onClick            = onEdit
                )
            }
        }

        // Título sobre a capa
        Text(
            text  = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                shadow   = Shadow(
                    color      = Color.Black.copy(alpha = 0.65f),
                    offset     = Offset(0f, 2f),
                    blurRadius = 8f
                )
            ),
            fontWeight = FontWeight.Bold,
            color      = Color.White,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .alpha(titleAlpha)
        )
    }
}

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

// ── DAY CARD ──────────────────────────────────────────────────────────────────

@Composable
private fun DayCard(
    day: TravelDay,
    liveWeather: LiveWeatherDay?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val hasWeather   = liveWeather != null && !(liveWeather.minTemp == 0 && liveWeather.maxTemp == 0)
    val displayCond  = if (hasWeather) liveWeather!!.condition else null

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border    = BorderStroke(
            width = if (day.isToday) 2.dp else 1.dp,
            color = if (day.isToday) GreenMoss else CardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(shape = RoundedCornerShape(10.dp), color = GreenMoss) {
                    Column(
                        modifier            = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text       = day.date.dayOfMonth.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize   = 24.sp,
                            color      = Color.White,
                            lineHeight = 26.sp
                        )
                        Text(
                            text          = day.date.format(DateTimeFormatter.ofPattern("MMM", Locale("pt", "BR"))).uppercase(),
                            fontSize      = 9.sp,
                            color         = Color.White.copy(alpha = 0.8f),
                            letterSpacing = 2.sp
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text          = day.dayOfWeek.uppercase(),
                            fontSize      = 10.sp,
                            color         = GreenMoss,
                            letterSpacing = 2.sp,
                            fontWeight    = FontWeight.Medium
                        )
                        if (day.isToday) HojeBadge()
                    }
                    Text(text = day.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }

                if (hasWeather) WeatherIcon(liveWeather!!.weatherCode, size = 40.dp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = AmberPrimary)
                } else if (!hasWeather) {
                    Text(
                        text  = stringResource(R.string.home_weather_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    Text(
                        text       = stringResource(R.string.home_temp_range, liveWeather!!.minTemp, liveWeather.maxTemp),
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = AmberPrimary
                    )
                    Text("·", color = DividerColor, fontSize = 14.sp)
                    Text(
                        text     = displayCond!!,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text          = stringResource(R.string.home_live_badge),
                        fontSize      = 9.sp,
                        color         = GreenMoss,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text  = pluralStringResource(R.plurals.home_day_activities, day.activities.size, day.activities.size),
                style = MaterialTheme.typography.labelSmall,
                color = GreenSage
            )
        }
    }
}

// ── HOTEL CARD ────────────────────────────────────────────────────────────────

@Composable
private fun HotelCard(hotelName: String, hotelAddress: String, hotelPhone: String = "") {
    val context = LocalContext.current

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = GreenMoss),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "🏨", fontSize = 28.sp)
                Column {
                    Text(
                        text       = hotelName,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White
                    )
                    Text(
                        text  = hotelAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.70f)
                    )
                    if (hotelPhone.isNotBlank()) {
                        Text(
                            text  = hotelPhone,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.70f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val buttonCount  = if (hotelPhone.isNotBlank()) 3 else 2
                val spacing      = 8.dp * (buttonCount - 1)
                val buttonWidth  = (maxWidth - spacing) / buttonCount
                val showText     = buttonWidth >= 82.dp
                val iconSize     = if (showText) 16.dp else 22.dp
                val btnPadding   = if (showText) ButtonDefaults.ContentPadding
                                   else PaddingValues(vertical = 12.dp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    // Maps
                    Button(
                        onClick = {
                            val uri = Uri.parse("geo:0,0?q=${Uri.encode(hotelAddress)}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        modifier        = Modifier.weight(1f),
                        shape           = RoundedCornerShape(10.dp),
                        contentPadding  = btnPadding,
                        colors          = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor   = Color.White
                        )
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_map), contentDescription = stringResource(R.string.home_maps_button), modifier = Modifier.size(iconSize))
                        if (showText) {
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.home_maps_button), fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    // Uber
                    Button(
                        onClick = {
                            val url = "https://m.uber.com/ul/?action=setPickup" +
                                "&pickup=my_location" +
                                "&dropoff[formatted_address]=${Uri.encode(hotelAddress)}" +
                                "&dropoff[nickname]=${Uri.encode(hotelName)}"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        modifier        = Modifier.weight(1f),
                        shape           = RoundedCornerShape(10.dp),
                        contentPadding  = btnPadding,
                        colors          = ButtonDefaults.buttonColors(
                            containerColor = AmberPrimary,
                            contentColor   = GreenMoss
                        )
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_car), contentDescription = stringResource(R.string.home_uber_button), modifier = Modifier.size(iconSize))
                        if (showText) {
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.home_uber_button), fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    // Ligar — só exibe se houver telefone
                    if (hotelPhone.isNotBlank()) {
                        Button(
                            onClick = {
                                val digits = hotelPhone.filter { it.isDigit() || it == '+' }
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
                            },
                            modifier        = Modifier.weight(1f),
                            shape           = RoundedCornerShape(10.dp),
                            contentPadding  = btnPadding,
                            colors          = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor   = Color.White
                            )
                        ) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_phone), contentDescription = stringResource(R.string.home_call_button), modifier = Modifier.size(iconSize))
                            if (showText) {
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.home_call_button), fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── HOJE BADGE ────────────────────────────────────────────────────────────────

@Composable
private fun HojeBadge() {
    Surface(shape = RoundedCornerShape(100.dp), color = GreenMoss) {
        Text(
            text          = stringResource(R.string.home_today_badge),
            modifier      = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize      = 9.sp,
            fontWeight    = FontWeight.Bold,
            color         = Color.White,
            letterSpacing = 2.sp
        )
    }
}
