package com.rodrigoleao.pipa.update

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.rodrigoleao.pipa.data.analytics.AnalyticsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Encapsula o fluxo de atualização in-app da Google Play no modo **FLEXIBLE**:
 * a nova versão é baixada em segundo plano (o usuário continua usando o app) e,
 * ao concluir, oferecemos reiniciar para instalar.
 *
 * Uso (na MainActivity):
 * 1. criar em `onCreate`, passando o `lifecycleScope`, um
 *    `ActivityResultLauncher<IntentSenderRequest>` e o callback [onDownloaded];
 * 2. [register] em `onCreate` / [unregister] em `onDestroy`;
 * 3. [checkForUpdate] em `onResume` (checa ao abrir e a cada retorno ao 1º plano);
 * 4. encaminhar o resultado do launcher para [onFlowResult];
 * 5. chamar [completeUpdate] quando o usuário aceitar reiniciar.
 *
 * ⚠️ Só funciona em builds instalados pela Play. O build **debug** tem `applicationId`
 * com sufixo `.debug` (fora da loja), então a checagem simplesmente não encontra
 * atualização — para testar, use um canal de teste interno ou o `FakeAppUpdateManager`.
 */
class InAppUpdateManager(
    private val appUpdateManager: AppUpdateManager,
    private val analytics: AnalyticsService,
    private val scope: CoroutineScope,
    private val launcher: ActivityResultLauncher<IntentSenderRequest>,
    private val onDownloaded: () -> Unit,
) {
    // Oferece o diálogo da Play no máximo uma vez por sessão do processo (evita insistir
    // a cada retomada). O aviso de "baixado, reiniciar" continua reexibível via onDownloaded.
    private var flowStarted = false

    private val listener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> onDownloaded()
            InstallStatus.FAILED     -> analytics.logUpdateFlowResult(AnalyticsService.RESULT_FAILED)
            else                     -> Unit  // PENDING / DOWNLOADING / INSTALLING / … — sem ação
        }
    }

    fun register() = appUpdateManager.registerListener(listener)

    fun unregister() = appUpdateManager.unregisterListener(listener)

    /**
     * Consulta a Play por uma nova versão. Se um download flexível já tiver concluído,
     * reexibe o aviso de reiniciar; senão, havendo update disponível e nada em andamento,
     * inicia o fluxo (a própria Play exibe o diálogo de confirmação).
     */
    fun checkForUpdate() {
        scope.launch {
            val info = runCatching { appUpdateManager.requestAppUpdateInfo() }.getOrNull() ?: return@launch

            // Download já concluído (ex.: terminou com o app em segundo plano) → pede reinício.
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                onDownloaded()
                return@launch
            }

            if (!flowStarted &&
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                flowStarted = true
                analytics.logUpdateAvailable()
                runCatching {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        launcher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                    )
                }.onFailure { flowStarted = false }  // se não abriu, permite nova tentativa
            }
        }
    }

    /** Resultado do diálogo da Play (encaminhado pelo ActivityResultLauncher da Activity). */
    fun onFlowResult(resultCode: Int) {
        analytics.logUpdateFlowResult(
            when (resultCode) {
                Activity.RESULT_OK       -> AnalyticsService.RESULT_ACCEPTED
                Activity.RESULT_CANCELED -> AnalyticsService.RESULT_CANCELED
                else                     -> AnalyticsService.RESULT_FAILED
            }
        )
    }

    /** Instala a atualização já baixada (a Play reinicia o app). Chamar quando o usuário aceitar. */
    fun completeUpdate() {
        analytics.logUpdateInstalled()
        appUpdateManager.completeUpdate()
    }
}
