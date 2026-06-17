package com.rodrigoleao.gramado2026.ui.map

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rodrigoleao.gramado2026.ui.theme.GreenMoss

@Composable
fun BustourMapScreen(contentPadding: PaddingValues = PaddingValues()) {
    val context = LocalContext.current

    var scale  by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale  = (scale * zoomChange).coerceIn(1f, 6f)
        val maxOffset = 800f * (scale - 1f)
        offset = Offset(
            x = (offset.x + offsetChange.x).coerceIn(-maxOffset, maxOffset),
            y = (offset.y + offsetChange.y).coerceIn(-maxOffset, maxOffset)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/images/mapa_bustour.webp")
                .crossfade(true)
                .build(),
            contentDescription = "Mapa de rotas e paradas do Bustour — Gramado e Canela",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(
                    scaleX       = scale,
                    scaleY       = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = transformableState)
        )

        // Dica de uso (quando zoom = 1)
        if (scale == 1f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text     = "🔍  Pinça para ampliar",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color    = Color.White,
                    style    = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Botão Resetar zoom (quando ampliado)
        if (scale > 1f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                color = GreenMoss.copy(alpha = 0.9f),
                shape = RoundedCornerShape(100.dp)
            ) {
                TextButton(
                    onClick = {
                        scale  = 1f
                        offset = Offset.Zero
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("↩  Resetar zoom", color = Color.White)
                }
            }
        }
    }
}
