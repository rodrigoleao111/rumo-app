package com.rodrigoleao.pipa

import android.app.Application
import com.rodrigoleao.pipa.data.analytics.AnalyticsService
import com.rodrigoleao.pipa.locale.LocaleHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PipaApplication : Application() {

    @Inject lateinit var analytics: AnalyticsService

    override fun onCreate() {
        super.onCreate()
        // Propriedade de usuário: idioma escolhido — permite fatiar todos os relatórios.
        analytics.setLanguage(LocaleHelper.getLanguage(this))
    }
}
