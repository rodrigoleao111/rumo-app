package com.rodrigoleao.gramado2026

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.rodrigoleao.gramado2026.data.db.TravelDatabase
import com.rodrigoleao.gramado2026.data.seeder.DatabaseSeeder
import com.rodrigoleao.gramado2026.navigation.AppNavigation
import com.rodrigoleao.gramado2026.notifications.NotificationHelper
import com.rodrigoleao.gramado2026.ui.theme.GramadoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
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

        val initialImportUri: Uri? = if (intent?.action == Intent.ACTION_VIEW) intent.data else null

        setContent {
            GramadoTheme {
                AppNavigation(initialImportUri = initialImportUri)
            }
        }
    }
}
