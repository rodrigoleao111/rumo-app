package com.rodrigoleao.pipa.data.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ponto único de registro de eventos do Firebase Analytics. Concentra os nomes de
 * evento/parâmetro (constantes abaixo) para evitar strings soltas espalhadas pelos
 * ViewModels. Injetável via Hilt em qualquer VM (`@Inject constructor`); para Compose,
 * use o [com.rodrigoleao.pipa.ui.analytics.AnalyticsViewModel].
 *
 * Convenções: nomes em snake_case (≤40 chars), sem prefixos reservados
 * (firebase_/google_/ga_) e SEM PII nos parâmetros (só contagens e enums).
 */
@Singleton
class AnalyticsService @Inject constructor(
    private val fa: FirebaseAnalytics
) {

    // ── Propriedades de usuário (segmentam todos os relatórios) ──────────────────

    /** Idioma escolhido (system|pt|en|es). Setar no start e a cada troca. */
    fun setLanguage(language: String) = fa.setUserProperty(PROP_LANGUAGE, language)

    // ── Screen view (Compose não gera automático) ────────────────────────────────

    fun logScreenView(screenName: String) = fa.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
    })

    // ── Ciclo de vida da viagem ──────────────────────────────────────────────────

    fun logTripCreated(method: String, daysCount: Int) = fa.logEvent(EVENT_TRIP_CREATED, Bundle().apply {
        putString(PARAM_METHOD, method)
        putLong(PARAM_DAYS_COUNT, daysCount.toLong())
    })

    fun logTripImported(overwrite: Boolean) = fa.logEvent(EVENT_TRIP_IMPORTED, Bundle().apply {
        putString(PARAM_MODE, if (overwrite) MODE_OVERWRITE else MODE_NEW)
    })

    fun logTripOpened(isActive: Boolean) = fa.logEvent(EVENT_TRIP_OPENED, Bundle().apply {
        putLong(PARAM_IS_ACTIVE, if (isActive) 1L else 0L)
    })

    fun logTripShared() = fa.logEvent(FirebaseAnalytics.Event.SHARE, Bundle().apply {
        putString(FirebaseAnalytics.Param.CONTENT_TYPE, CONTENT_TRIP)
    })

    fun logContentAdded(type: String) = fa.logEvent(EVENT_CONTENT_ADDED, Bundle().apply {
        putString(PARAM_TYPE, type)
    })

    // ── IA ───────────────────────────────────────────────────────────────────────

    fun logAiItineraryGenerated(
        success: Boolean,
        daysCount: Int,
        promptTokens: Int,
        outputTokens: Int,
        totalTokens: Int,
        latencyMs: Long
    ) = fa.logEvent(EVENT_AI_ITINERARY_GENERATED, Bundle().apply {
        putLong(PARAM_SUCCESS, if (success) 1L else 0L)
        putLong(PARAM_DAYS_COUNT, daysCount.toLong())
        putLong(PARAM_PROMPT_TOKENS, promptTokens.toLong())
        putLong(PARAM_OUTPUT_TOKENS, outputTokens.toLong())
        putLong(PARAM_TOTAL_TOKENS, totalTokens.toLong())
        putLong(PARAM_LATENCY_MS, latencyMs)
    })

    fun logAiChatMessageSent(totalTokens: Int, messageIndex: Int) = fa.logEvent(EVENT_AI_CHAT_MESSAGE_SENT, Bundle().apply {
        putLong(PARAM_TOTAL_TOKENS, totalTokens.toLong())
        putLong(PARAM_MESSAGE_INDEX, messageIndex.toLong())
    })

    fun logAiLimitReached(type: String) = fa.logEvent(EVENT_AI_LIMIT_REACHED, Bundle().apply {
        putString(PARAM_TYPE, type)
    })

    // ── Atualização do app (Play In-App Updates) ────────────────────────────────

    /** Uma nova versão foi encontrada na Play e o fluxo flexível foi iniciado. */
    fun logUpdateAvailable() = fa.logEvent(EVENT_APP_UPDATE_AVAILABLE, Bundle())

    /** Resultado do diálogo de atualização da Play (accepted | canceled | failed). */
    fun logUpdateFlowResult(result: String) = fa.logEvent(EVENT_APP_UPDATE_FLOW, Bundle().apply {
        putString(PARAM_RESULT, result)
    })

    /** Usuário aceitou reiniciar para instalar a atualização baixada. */
    fun logUpdateInstalled() = fa.logEvent(EVENT_APP_UPDATE_INSTALLED, Bundle())

    // ── Preferências ─────────────────────────────────────────────────────────────

    /** Registra a troca de idioma e atualiza a propriedade de usuário [PROP_LANGUAGE]. */
    fun logLanguageChanged(language: String) {
        fa.logEvent(EVENT_LANGUAGE_CHANGED, Bundle().apply { putString(PARAM_LANGUAGE, language) })
        setLanguage(language)
    }

    companion object {
        // Eventos
        const val EVENT_TRIP_CREATED           = "trip_created"
        const val EVENT_TRIP_IMPORTED          = "trip_imported"
        const val EVENT_TRIP_OPENED            = "trip_opened"
        const val EVENT_CONTENT_ADDED          = "content_added"
        const val EVENT_AI_ITINERARY_GENERATED = "ai_itinerary_generated"
        const val EVENT_AI_CHAT_MESSAGE_SENT   = "ai_chat_message_sent"
        const val EVENT_AI_LIMIT_REACHED       = "ai_limit_reached"
        const val EVENT_LANGUAGE_CHANGED       = "language_changed"
        const val EVENT_APP_UPDATE_AVAILABLE   = "app_update_available"
        const val EVENT_APP_UPDATE_FLOW        = "app_update_flow"
        const val EVENT_APP_UPDATE_INSTALLED   = "app_update_installed"

        // Parâmetros
        const val PARAM_METHOD        = "method"
        const val PARAM_MODE          = "mode"
        const val PARAM_DAYS_COUNT    = "days_count"
        const val PARAM_IS_ACTIVE     = "is_active"
        const val PARAM_TYPE          = "type"
        const val PARAM_SUCCESS       = "success"
        const val PARAM_PROMPT_TOKENS = "prompt_tokens"
        const val PARAM_OUTPUT_TOKENS = "output_tokens"
        const val PARAM_TOTAL_TOKENS  = "total_tokens"
        const val PARAM_LATENCY_MS    = "latency_ms"
        const val PARAM_MESSAGE_INDEX = "message_index"
        const val PARAM_LANGUAGE      = "language"
        const val PARAM_RESULT        = "result"

        // Propriedades de usuário
        const val PROP_LANGUAGE = "app_language"

        // Valores — método de criação da viagem
        const val METHOD_MANUAL    = "manual"
        const val METHOD_AI        = "ai"
        const val METHOD_AI_IMPORT = "ai_import"

        // Valores — modo de importação
        const val MODE_NEW       = "new"
        const val MODE_OVERWRITE = "overwrite"

        // Valores — tipo de conteúdo (content_added)
        const val CONTENT_TRIP          = "trip"
        const val CONTENT_VOUCHER       = "voucher"
        const val CONTENT_BOARDING_PASS = "boarding_pass"
        const val CONTENT_CONTACT       = "contact"
        const val CONTENT_NOTE          = "note"
        const val CONTENT_ACTIVITY      = "activity"

        // Valores — tipo de limite de IA
        const val LIMIT_TOKEN_BUDGET = "token_budget"
        const val LIMIT_DAILY_CAP    = "daily_cap"

        // Valores — resultado do fluxo de atualização
        const val RESULT_ACCEPTED = "accepted"
        const val RESULT_CANCELED = "canceled"
        const val RESULT_FAILED   = "failed"
    }
}
