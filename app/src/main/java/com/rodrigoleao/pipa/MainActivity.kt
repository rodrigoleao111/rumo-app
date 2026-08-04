package com.rodrigoleao.pipa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.appupdate.AppUpdateManager
import com.rodrigoleao.pipa.data.analytics.AnalyticsService
import com.rodrigoleao.pipa.data.db.TravelDatabase
import com.rodrigoleao.pipa.data.seeder.DatabaseSeeder
import com.rodrigoleao.pipa.locale.LocaleHelper
import com.rodrigoleao.pipa.navigation.AppNavigation
import com.rodrigoleao.pipa.notifications.NotificationHelper
import com.rodrigoleao.pipa.ui.theme.AmberPrimary
import com.rodrigoleao.pipa.ui.theme.GreenMoss
import com.rodrigoleao.pipa.ui.theme.PipaTheme
import com.rodrigoleao.pipa.update.InAppUpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appUpdateManager: AppUpdateManager
    @Inject lateinit var analytics: AnalyticsService

    // Estado reativo: atualizado tanto em onCreate (intent inicial) quanto em onNewIntent
    private val importUriState = mutableStateOf<Uri?>(null)

    // true quando um update flexível terminou de baixar e está pronto para instalar
    private val updateReadyState = mutableStateOf(false)

    private lateinit var inAppUpdate: InAppUpdateManager

    // Recebe o resultado do diálogo de atualização da Play (aceitar/cancelar/falhar)
    private val updateFlowLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> inAppUpdate.onFlowResult(result.resultCode) }

    // Aplica o idioma escolhido pelo usuário antes de qualquer recurso ser resolvido.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannel(this)

        // Atualização in-app da Play (modo flexível). A checagem em si ocorre em onResume.
        inAppUpdate = InAppUpdateManager(
            appUpdateManager = appUpdateManager,
            analytics        = analytics,
            scope            = lifecycleScope,
            launcher         = updateFlowLauncher,
            onDownloaded     = { updateReadyState.value = true }
        )
        inAppUpdate.register()

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
                AppRoot(
                    importUriState  = importUriState,
                    updateReady     = updateReadyState.value,
                    onInstallUpdate = { inAppUpdate.completeUpdate() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Checa por nova versão ao abrir e a cada retorno ao primeiro plano; também
        // reexibe o aviso se um download flexível já tiver concluído.
        inAppUpdate.checkForUpdate()
    }

    override fun onDestroy() {
        inAppUpdate.unregister()
        super.onDestroy()
    }

    // Chamado quando o app já está em foreground e outro arquivo .travel é aberto
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) importUriState.value = intent.data
    }
}

/**
 * Raiz da UI: a navegação do app + um Snackbar de nível global que oferece reiniciar
 * para instalar uma atualização já baixada (Play In-App Updates, modo flexível).
 */
@Composable
private fun AppRoot(
    importUriState: MutableState<Uri?>,
    updateReady: Boolean,
    onInstallUpdate: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message     = stringResource(R.string.update_downloaded_message)
    val actionLabel = stringResource(R.string.update_downloaded_action)

    LaunchedEffect(updateReady) {
        if (updateReady) {
            val result = snackbarHostState.showSnackbar(
                message     = message,
                actionLabel = actionLabel,
                duration    = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) onInstallUpdate()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AppNavigation(importUriState = importUriState)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) { data ->
            Snackbar(
                snackbarData   = data,
                containerColor = GreenMoss,
                contentColor   = Color.White,
                actionColor    = AmberPrimary
            )
        }
    }
}
