package com.rodrigoleao.gramado2026.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rodrigoleao.gramado2026.data.model.TravelDay
import com.rodrigoleao.gramado2026.data.weather.LiveWeatherDay
import com.rodrigoleao.gramado2026.data.weather.WeatherRepository
import com.rodrigoleao.gramado2026.ui.components.WeatherIcon
import com.rodrigoleao.gramado2026.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.rodrigoleao.gramado2026.R

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

    LazyColumn(
        state          = listState,
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top    = contentPadding.calculateTopPadding()    + 12.dp,
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
                        text  = "Temperatura indisponível",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    Text(
                        text       = "${liveWeather!!.minTemp}°  ~  ${liveWeather.maxTemp}°C",
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
                        text          = "● ao vivo",
                        fontSize      = 9.sp,
                        color         = GreenMoss,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text  = "${day.activities.size} atividades  ·  toque para ver o roteiro",
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
                        Icon(ImageVector.vectorResource(R.drawable.ic_map), contentDescription = "Maps", modifier = Modifier.size(iconSize))
                        if (showText) {
                            Spacer(Modifier.width(6.dp))
                            Text("Maps", fontWeight = FontWeight.SemiBold, maxLines = 1)
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
                        Icon(ImageVector.vectorResource(R.drawable.ic_car), contentDescription = "Uber", modifier = Modifier.size(iconSize))
                        if (showText) {
                            Spacer(Modifier.width(6.dp))
                            Text("Uber", fontWeight = FontWeight.SemiBold, maxLines = 1)
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
                            Icon(ImageVector.vectorResource(R.drawable.ic_phone), contentDescription = "Ligar", modifier = Modifier.size(iconSize))
                            if (showText) {
                                Spacer(Modifier.width(6.dp))
                                Text("Ligar", fontWeight = FontWeight.SemiBold, maxLines = 1)
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
            text          = "HOJE",
            modifier      = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize      = 9.sp,
            fontWeight    = FontWeight.Bold,
            color         = Color.White,
            letterSpacing = 2.sp
        )
    }
}
