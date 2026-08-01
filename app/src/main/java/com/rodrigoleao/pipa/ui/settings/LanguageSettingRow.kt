package com.rodrigoleao.pipa.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rodrigoleao.pipa.R
import com.rodrigoleao.pipa.locale.LocaleHelper
import com.rodrigoleao.pipa.locale.findActivity
import com.rodrigoleao.pipa.ui.theme.AmberPrimary
import com.rodrigoleao.pipa.ui.theme.GreenMoss
import com.rodrigoleao.pipa.ui.theme.SurfaceWhite
import com.rodrigoleao.pipa.ui.theme.TextPrimary
import com.rodrigoleao.pipa.ui.theme.TextSecondary

/** Nome exibível de cada idioma suportado. */
@StringRes
private fun languageLabel(code: String): Int = when (code) {
    "pt" -> R.string.lang_pt
    "en" -> R.string.lang_en
    "es" -> R.string.lang_es
    else -> R.string.lang_system
}

/**
 * Linha de "Idioma" na tela de Configurações. Abre um diálogo com as opções
 * (Sistema / Português / English / Español); ao escolher, persiste a preferência
 * e recria a Activity para reaplicar o locale.
 */
@Composable
fun LanguageSettingRow(
    onLanguageChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    val current = remember { LocaleHelper.getLanguage(context) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text       = stringResource(R.string.settings_language_title),
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = stringResource(R.string.settings_language_desc),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Text(
            text       = stringResource(languageLabel(current)),
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color      = GreenMoss
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor   = SurfaceWhite,
            titleContentColor = GreenMoss,
            title = { Text(stringResource(R.string.settings_language_title), fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    LocaleHelper.SUPPORTED.forEach { code ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = code == current,
                                    onClick  = {
                                        showDialog = false
                                        if (code != current) {
                                            onLanguageChanged(code)
                                            LocaleHelper.setLanguage(context, code)
                                            context.findActivity()?.recreate()
                                        }
                                    }
                                )
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = code == current,
                                onClick  = {
                                    showDialog = false
                                    if (code != current) {
                                        onLanguageChanged(code)
                                        LocaleHelper.setLanguage(context, code)
                                        context.findActivity()?.recreate()
                                    }
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor   = GreenMoss,
                                    unselectedColor = AmberPrimary
                                )
                            )
                            Text(
                                text     = stringResource(languageLabel(code)),
                                style    = MaterialTheme.typography.bodyLarge,
                                color    = TextPrimary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.common_cancel), color = GreenMoss)
                }
            }
        )
    }
}
