package com.rodrigoleao.pipa

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.rodrigoleao.pipa.data.db.TravelDatabase
import com.rodrigoleao.pipa.data.seeder.DatabaseSeeder
import com.rodrigoleao.pipa.navigation.AppNavigation
import com.rodrigoleao.pipa.notifications.NotificationHelper
import com.rodrigoleao.pipa.ui.theme.PipaTheme
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

        // Seeder da viagem de exemplo ("Gramado & Canela") só roda em builds de debug —
        // em release o app nasce limpo. Idempotente: só insere se o banco estiver vazio.
        if (BuildConfig.DEBUG) {
            val db = TravelDatabase.getInstance(this)
            lifecycleScope.launch(Dispatchers.IO) {
                DatabaseSeeder.seedIfEmpty(db)
            }
        }

        if (intent?.action == Intent.ACTION_VIEW) importUriState.value = intent.data

        setContent {
            PipaTheme {
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
