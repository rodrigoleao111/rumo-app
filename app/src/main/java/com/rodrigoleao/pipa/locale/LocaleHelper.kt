package com.rodrigoleao.pipa.locale

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

/**
 * Troca de idioma do app sem depender de AppCompat. A escolha do usuário é
 * guardada em [SharedPreferences] (leitura síncrona) para poder ser aplicada bem
 * cedo, em [android.app.Activity.attachBaseContext], antes de qualquer recurso ser
 * resolvido.
 *
 * Valores possíveis: [SYSTEM] (segue o idioma do aparelho, com fallback para os
 * recursos padrão = português), ou uma tag de idioma como "pt", "en", "es".
 */
object LocaleHelper {

    const val SYSTEM = "system"

    private const val PREFS = "pipa_locale"
    private const val KEY_LANGUAGE = "app_language"

    /** Idiomas oferecidos no seletor, na ordem de exibição. */
    val SUPPORTED = listOf(SYSTEM, "pt", "en", "es")

    fun getLanguage(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, SYSTEM) ?: SYSTEM

    fun setLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    /**
     * Envolve [context] com o locale escolhido. Em [SYSTEM] devolve o contexto
     * inalterado (mantém o locale do aparelho). Deve ser chamado em
     * `attachBaseContext` para valer em toda a UI.
     */
    fun wrap(context: Context): Context {
        val language = getLanguage(context)
        if (language == SYSTEM) return context

        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}

/** Desembrulha um [Context] até achar a [Activity] hospedeira (para chamar `recreate()`). */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
