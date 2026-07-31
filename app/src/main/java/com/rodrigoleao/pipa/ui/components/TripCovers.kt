package com.rodrigoleao.pipa.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rodrigoleao.pipa.R
import com.rodrigoleao.pipa.ui.theme.AmberPrimary
import com.rodrigoleao.pipa.ui.theme.CardBorder
import com.rodrigoleao.pipa.ui.theme.GreenMoss
import com.rodrigoleao.pipa.ui.theme.SurfaceWhite
import com.rodrigoleao.pipa.ui.theme.TextPrimary

// ── Catálogo de capas ─────────────────────────────────────────────────────────

/** Uma capa (ilustração) selecionável para a viagem. */
data class TripCover(
    /** id estável salvo no banco (ex.: "cover_praia_tropical"). */
    val id: String,
    @DrawableRes val res: Int,
    @StringRes val labelRes: Int
)

/** Grupo de capas exibido como categoria expansível no seletor. */
data class TripCoverCategory(
    @StringRes val titleRes: Int,
    /** Emoji representativo — usado como fallback de `coverEmoji`. */
    val emoji: String,
    val covers: List<TripCover>
)

object TripCovers {

    val CATEGORIES: List<TripCoverCategory> = listOf(
        TripCoverCategory(R.string.cover_cat_beach, "🏖️", listOf(
            TripCover("cover_praia_tropical", R.drawable.cover_praia_tropical, R.string.cover_beach_tropical),
            TripCover("cover_orla",           R.drawable.cover_orla,           R.string.cover_seafront),
            TripCover("cover_dunas",          R.drawable.cover_dunas,          R.string.cover_dunes),
            TripCover("cover_navio",          R.drawable.cover_navio,          R.string.cover_cruise),
        )),
        TripCoverCategory(R.string.cover_cat_nature, "🌿", listOf(
            TripCover("cover_cachoeira",      R.drawable.cover_cachoeira,      R.string.cover_waterfall),
            TripCover("cover_floresta",       R.drawable.cover_floresta,       R.string.cover_forest),
            TripCover("cover_lago",           R.drawable.cover_lago,           R.string.cover_lake),
            TripCover("cover_lago_2",         R.drawable.cover_lago_2,         R.string.cover_lake_2),
            TripCover("cover_campo",          R.drawable.cover_campo,          R.string.cover_countryside),
            TripCover("cover_montanha_serra", R.drawable.cover_montanha_serra, R.string.cover_mountains),
        )),
        TripCoverCategory(R.string.cover_cat_city, "🏙️", listOf(
            TripCover("cover_cidade_grande",    R.drawable.cover_cidade_grande,    R.string.cover_big_city),
            TripCover("cover_cidade_historica", R.drawable.cover_cidade_historica, R.string.cover_historic_city),
            TripCover("cover_cultural",         R.drawable.cover_cultural,         R.string.cover_cultural),
            TripCover("cover_musical",          R.drawable.cover_musical,          R.string.cover_musical),
            TripCover("cover_generica",         R.drawable.cover_generica,         R.string.cover_generic),
        )),
        TripCoverCategory(R.string.cover_cat_winter, "❄️", listOf(
            TripCover("cover_inverno_cidade", R.drawable.cover_inverno_cidade, R.string.cover_winter_city),
            TripCover("cover_inverno_campo",  R.drawable.cover_inverno_campo,  R.string.cover_winter_country),
            TripCover("cover_neve",           R.drawable.cover_neve,           R.string.cover_snow),
        )),
    )

    val ALL: List<TripCover> = CATEGORIES.flatMap { it.covers }

    fun byId(id: String?): TripCover? = ALL.firstOrNull { it.id == id }

    /** Resource da capa, ou null se o id não existir. */
    @DrawableRes
    fun resFor(id: String?): Int? = byId(id)?.res

    /** Emoji representativo da categoria da capa (fallback para `coverEmoji`). */
    fun emojiFor(id: String?): String =
        CATEGORIES.firstOrNull { cat -> cat.covers.any { it.id == id } }?.emoji ?: "✈️"
}

// ── Seletor de capa ─────────────────────────────────────────────────────────

private val COVER_ASPECT = 1376f / 768f   // proporção original das ilustrações
private val COVER_CARD_WIDTH = 210.dp

/**
 * Seletor de capa da viagem: para cada categoria, um cabeçalho retrátil
 * `[nome] [linha verde musgo] [chevron]` e, abaixo, uma faixa horizontal rolável de
 * cards ilustrados. O card selecionado recebe destaque (borda âmbar + selo de check)
 * e a lupa abre a ilustração em tela cheia.
 */
@Composable
fun CoverPicker(
    selectedId: String,
    onSelect: (TripCover) -> Unit,
    modifier: Modifier = Modifier
) {
    var zoomCover by remember { mutableStateOf<TripCover?>(null) }

    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TripCovers.CATEGORIES.forEachIndexed { index, category ->
            CoverCategorySection(
                category          = category,
                selectedId        = selectedId,
                // Praia & Mar (primeira) nasce expandida; as demais só se contiverem a seleção.
                initiallyExpanded = index == 0 || category.covers.any { it.id == selectedId },
                onSelect          = onSelect,
                onZoom            = { zoomCover = it }
            )
        }
    }

    zoomCover?.let { cover ->
        CoverFullscreenDialog(cover = cover, onDismiss = { zoomCover = null })
    }
}

@Composable
private fun CoverCategorySection(
    category: TripCoverCategory,
    selectedId: String,
    initiallyExpanded: Boolean,
    onSelect: (TripCover) -> Unit,
    onZoom: (TripCover) -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Column(
        modifier            = Modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cabeçalho: [nome] [linha verde musgo] [chevron]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text       = stringResource(category.titleRes),
                fontWeight = FontWeight.SemiBold,
                color      = GreenMoss,
                fontSize   = 14.sp
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.5.dp)
                    .background(GreenMoss.copy(alpha = 0.35f))
            )
            Icon(
                imageVector = if (expanded)
                    ImageVector.vectorResource(R.drawable.ic_chevron_up)
                else
                    ImageVector.vectorResource(R.drawable.ic_chevron_down),
                contentDescription = if (expanded) stringResource(R.string.cover_collapse) else stringResource(R.string.cover_expand),
                tint     = GreenMoss,
                modifier = Modifier.size(22.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    // respiro para a elevação (sombra) dos cards não ser cortada
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                category.covers.forEach { cover ->
                    CoverCard(
                        cover    = cover,
                        selected = cover.id == selectedId,
                        onClick  = { onSelect(cover) },
                        onZoom   = { onZoom(cover) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverCard(
    cover: TripCover,
    selected: Boolean,
    onClick: () -> Unit,
    onZoom: () -> Unit
) {
    Card(
        modifier  = Modifier
            .width(COVER_CARD_WIDTH)
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 5.dp else 2.dp),
        border    = BorderStroke(
            width = if (selected) 2.dp else 0.5.dp,
            color = if (selected) AmberPrimary else CardBorder
        )
    ) {
        Column {
            Box {
                // Imagem completa (sem cortar) — respeita a proporção original
                Image(
                    painter            = painterResource(cover.res),
                    contentDescription = stringResource(cover.labelRes),
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .aspectRatio(COVER_ASPECT)
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .clip(RoundedCornerShape(50))
                            .background(AmberPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = ImageVector.vectorResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(14.dp)
                        )
                    }
                }
            }
            // Nome + lupa de zoom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = stringResource(cover.labelRes),
                    fontSize   = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color      = if (selected) GreenMoss else TextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onZoom),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.ZoomIn,
                        contentDescription = stringResource(R.string.cover_view_fullscreen),
                        tint               = GreenMoss,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverFullscreenDialog(cover: TripCover, onDismiss: () -> Unit) {
    // Enquanto o visualizador estiver aberto, libera a rotação (sensor) mesmo que o
    // rotação automática do sistema esteja desligada; restaura ao fechar.
    val context  = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(Unit) {
        val original = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        onDispose { activity?.requestedOrientation = original }
    }

    Dialog(
        onDismissRequest = onDismiss,   // botão Voltar do dispositivo aciona isto
        properties       = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside   = false   // não fecha ao tocar/interagir com a imagem
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            ZoomableImage(
                res      = cover.res,
                desc     = stringResource(cover.labelRes),
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text       = stringResource(cover.labelRes),
                color      = Color.White,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
            IconButton(
                onClick  = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 12.dp)
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint               = Color.White,
                    modifier           = Modifier.size(28.dp)
                )
            }
        }
    }
}

/** Imagem com zoom (pinça e duplo toque) e arraste quando ampliada. */
@Composable
private fun ZoomableImage(
    @DrawableRes res: Int,
    desc: String,
    modifier: Modifier = Modifier
) {
    var scale   by remember { mutableStateOf(1f) }
    var offset  by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    fun clampOffset(candidate: Offset, s: Float): Offset {
        if (s <= 1f) return Offset.Zero
        val maxX = (boxSize.width  * (s - 1f)) / 2f
        val maxY = (boxSize.height * (s - 1f)) / 2f
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }

    Box(
        modifier = modifier
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) { scale = 1f; offset = Offset.Zero }
                        else            { scale = 2.5f }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale  = newScale
                    offset = clampOffset(offset + pan, newScale)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter            = painterResource(res),
            contentDescription = desc,
            contentScale       = ContentScale.Fit,
            modifier           = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX      = scale
                    scaleY      = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}

/** Desembrulha o Context até a Activity hospedeira (para controlar a orientação). */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
