package com.rodrigoleao.gramado2026.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.rodrigoleao.gramado2026.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        delay(2000)
        alpha.animateTo(0f, animationSpec = tween(durationMillis = 300))
        onFinished()
    }

    Image(
        painter            = painterResource(R.drawable.splash_background),
        contentDescription = null,
        modifier           = Modifier.fillMaxSize().alpha(alpha.value),
        contentScale       = ContentScale.Crop
    )
}
