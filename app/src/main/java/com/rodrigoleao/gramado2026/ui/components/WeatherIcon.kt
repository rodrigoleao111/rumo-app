package com.rodrigoleao.gramado2026.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.rodrigoleao.gramado2026.R
import com.rodrigoleao.gramado2026.data.weather.weatherCodeEmoji

/** Mapeia o código WMO do clima para a ilustração da marca (ou null para usar o emoji). */
fun weatherCodeDrawable(code: Int): Int? = when (code) {
    0                      -> R.drawable.weather_sol
    1                      -> R.drawable.weather_sol_poucas_nuvens
    2                      -> R.drawable.weather_sol_muitas_nuvens
    3                      -> R.drawable.weather_totalmente_nublado
    45, 48                 -> R.drawable.weather_nublado
    51, 53, 55, 56, 57     -> R.drawable.weather_sol_chuva
    61, 63, 65, 66, 67     -> R.drawable.weather_chuva
    71, 73, 75, 77, 85, 86 -> R.drawable.weather_neve
    80, 81, 82             -> R.drawable.weather_chuva
    95                     -> R.drawable.weather_raio
    96, 99                 -> R.drawable.weather_tempestade
    else                   -> null
}

/** Ícone de clima: usa a ilustração da marca quando há, senão cai no emoji. */
@Composable
fun WeatherIcon(code: Int, size: Dp, modifier: Modifier = Modifier) {
    val res = weatherCodeDrawable(code)
    if (res != null) {
        Image(
            painter            = painterResource(res),
            contentDescription = null,
            modifier           = modifier.size(size)
        )
    } else {
        Text(weatherCodeEmoji(code), fontSize = (size.value * 0.9f).sp, modifier = modifier)
    }
}
