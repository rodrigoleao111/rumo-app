package com.rodrigoleao.gramado2026

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.rodrigoleao.gramado2026.data.db.TravelDatabase
import com.rodrigoleao.gramado2026.data.seeder.DatabaseSeeder
import com.rodrigoleao.gramado2026.navigation.AppNavigation
import com.rodrigoleao.gramado2026.notifications.NotificationHelper
import com.rodrigoleao.gramado2026.ui.theme.GramadoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Estado reativo: atualizado tanto em onCreate (intent inicial) quanto em onNewIntent
    private val importUriState = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannel(this)

        // Inicializa DB e roda o seeder em background (idempotente — só insere se vazio)
        val db = TravelDatabase.getInstance(this)
        lifecycleScope.launch(Dispatchers.IO) {
            DatabaseSeeder.seedIfEmpty(db)
        }

        if (intent?.action == Intent.ACTION_VIEW) importUriState.value = intent.data

        setContent {
            GramadoTheme {
                AppNavigation(importUriState = importUriState)
            }
        }
    }

    // Chamado quando o app já está em foreground e outro arquivo .travel é aberto
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) importUriState.value = intent.data
    }
}
