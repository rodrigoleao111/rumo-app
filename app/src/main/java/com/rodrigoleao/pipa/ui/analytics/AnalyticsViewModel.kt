package com.rodrigoleao.pipa.ui.analytics

import androidx.lifecycle.ViewModel
import com.rodrigoleao.pipa.data.analytics.AnalyticsService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Wrapper mínimo para expor o [AnalyticsService] a composables (que não têm
 * `@Inject constructor`). Obtido em telas via `hiltViewModel()`; usado na
 * `AppNavigation` para `screen_view` e `trip_opened`.
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    val analytics: AnalyticsService
) : ViewModel()
