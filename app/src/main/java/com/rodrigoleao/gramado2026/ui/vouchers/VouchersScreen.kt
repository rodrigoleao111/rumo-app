package com.rodrigoleao.gramado2026.ui.vouchers

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.rodrigoleao.gramado2026.data.model.Voucher
import com.rodrigoleao.gramado2026.ui.theme.*
import com.rodrigoleao.gramado2026.utils.openAssetFile
import com.rodrigoleao.gramado2026.utils.openInternalFile
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File

// ── Tipo de voucher e paleta associada ───────────────────────────────────────

private enum class VoucherType { PDF, IMAGE, LINK, FILE }

private data class VoucherPalette(
    val accent: Color,
    val emojiBackground: Color,
    val badgeBackground: Color,
    val badgeText: Color,
    val buttonColor: Color,
    val label: String
)

private fun voucherType(assetPath: String): VoucherType = when {
    assetPath.startsWith("http")                                                -> VoucherType.LINK
    assetPath.endsWith(".pdf",  ignoreCase = true)                              -> VoucherType.PDF
    assetPath.endsWith(".jpeg", ignoreCase = true) ||
    assetPath.endsWith(".jpg",  ignoreCase = true) ||
    assetPath.endsWith(".png",  ignoreCase = true) ||
    assetPath.endsWith(".webp", ignoreCase = true)                              -> VoucherType.IMAGE
    else                                                                        -> VoucherType.FILE
}

private fun voucherPalette(type: VoucherType): VoucherPalette = when (type) {
    VoucherType.PDF -> VoucherPalette(
        accent          = GreenMoss,
        emojiBackground = GreenForest,
        badgeBackground = GreenWarm,
        badgeText       = GreenMoss,
        buttonColor     = GreenMoss,
        label           = "PDF"
    )
    VoucherType.LINK -> VoucherPalette(
        accent          = AmberPrimary,
        emojiBackground = AmberLight,
        badgeBackground = Color(0xFFEDD47A),   // âmbar mais saturado para badge
        badgeText       = Color(0xFF5A3A00),   // texto escuro sobre fundo âmbar
        buttonColor     = Color(0xFFAD820E),   // AmberPrimary um tom abaixo para botão
        label           = "Link"
    )
    VoucherType.IMAGE -> VoucherPalette(
        accent          = GreenSage,
        emojiBackground = GreenForest,
        badgeBackground = GreenWarm,
        badgeText       = Color(0xFF1B4332),
        buttonColor     = GreenSage,
        label           = "Imagem"
    )
    VoucherType.FILE -> VoucherPalette(
        accent          = TextSecondary,
        emojiBackground = GreenLight,
        badgeBackground = GreenForest,
        badgeText       = TextSecondary,
        buttonColor     = TextSecondary,
        label           = "Arquivo"
    )
}

// ── Modo de agrupamento ───────────────────────────────────────────────────────

enum class VoucherSortMode { BY_CATEGORY, BY_PERSON, BY_DAY }

// ── Itens da lista plana ─────────────────────────────────────────────────────

private sealed class VoucherListItem {
    data class Header(val groupName: String, val count: Int) : VoucherListItem()
    data class Item(val voucher: Voucher, val groupName: String) : VoucherListItem()
}

// ── Tela principal ────────────────────────────────────────────────────────────

@Composable
fun VouchersScreen(
    vouchers: List<Voucher>,
    contentPadding: PaddingValues = PaddingValues(),
    sortMode: VoucherSortMode = VoucherSortMode.BY_CATEGORY,
    onEditVoucher: (Long) -> Unit = {},
    onReorderVouchers: (List<Voucher>) -> Unit = {},
    onDeleteVoucher: (Long) -> Unit = {},
    onToggleUsed: (Long, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

    // localVouchers só é mutável para o drag (BY_CATEGORY); nos outros modos usa a prop diretamente
    var localVouchers by remember(vouchers) { mutableStateOf(vouchers) }

    val flatList: List<VoucherListItem> = remember(localVouchers, sortMode) {
        val grouped: Map<String, List<Voucher>> = when (sortMode) {
            VoucherSortMode.BY_CATEGORY -> localVouchers.groupBy { it.groupName }
            VoucherSortMode.BY_PERSON   -> localVouchers
                .groupBy { v -> v.person?.takeIf { it.isNotBlank() } ?: "Sem pessoa" }
                .entries.sortedWith(compareBy { if (it.key == "Sem pessoa") "￿" else it.key })
                .associate { it.key to it.value }
            VoucherSortMode.BY_DAY      -> localVouchers
                .groupBy { v -> v.dayId?.let { "Dia $it" } ?: "Sem dia" }
                .entries.sortedWith(compareBy {
                    val key = it.key
                    if (key == "Sem dia") Int.MAX_VALUE
                    else key.removePrefix("Dia ").toIntOrNull() ?: Int.MAX_VALUE
                })
                .associate { it.key to it.value }
        }
        buildList {
            grouped.forEach { (header, items) ->
                add(VoucherListItem.Header(header, items.size))
                items.forEach { add(VoucherListItem.Item(it, header)) }
            }
        }
    }


    val lazyListState = rememberLazyListState()

    val reorderState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            val fromItem = flatList.getOrNull(from.index) as? VoucherListItem.Item ?: return@rememberReorderableLazyListState
            val toItem   = flatList.getOrNull(to.index)   as? VoucherListItem.Item ?: return@rememberReorderableLazyListState
            if (fromItem.groupName != toItem.groupName) return@rememberReorderableLazyListState
            val mutable = localVouchers.toMutableList()
            val fromIdx = mutable.indexOfFirst { it.id == fromItem.voucher.id }
            val toIdx   = mutable.indexOfFirst { it.id == toItem.voucher.id }
            if (fromIdx >= 0 && toIdx >= 0) {
                val moved = mutable.removeAt(fromIdx)
                mutable.add(toIdx, moved)
                localVouchers = mutable
            }
        }
    )

    LaunchedEffect(localVouchers) {
        if (localVouchers != vouchers) onReorderVouchers(localVouchers)
    }

    if (vouchers.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🎫", fontSize = 36.sp)
                Text("Nenhum voucher ainda", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("Toque em + para adicionar", style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(alpha = 0.6f))
            }
        }
        return
    }

    LazyColumn(
        state          = lazyListState,
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top    = contentPadding.calculateTopPadding()    + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 80.dp,
            start  = 16.dp,
            end    = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = flatList,
            key   = { _, item ->
                when (item) {
                    is VoucherListItem.Header -> "header_${item.groupName}"
                    is VoucherListItem.Item   -> "voucher_${item.voucher.id}"
                }
            }
        ) { _, listItem ->
            when (listItem) {
                is VoucherListItem.Header -> GroupHeader(listItem.groupName, listItem.count)

                is VoucherListItem.Item -> {
                    val voucher = listItem.voucher
                    ReorderableItem(
                        state = reorderState,
                        key   = "voucher_${voucher.id}"
                    ) { isDragging ->
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 10.dp else 0.dp,
                            label       = "dragElevation"
                        )
                        val openAction: () -> Unit = {
                            when {
                                voucher.assetPath.startsWith("http") ->
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(voucher.assetPath)))
                                voucher.assetPath.startsWith(context.filesDir.absolutePath) ->
                                    openInternalFile(context, voucher.assetPath)
                                voucher.assetPath.isNotBlank() ->
                                    openAssetFile(context, voucher.assetPath)
                            }
                        }
                        val shareAction: () -> Unit = {
                            shareVoucher(context, voucher)
                        }
                        VoucherCard(
                            voucher    = voucher,
                            elevation  = elevation,
                            isDragging = isDragging,
                            dragHandle = {
                                IconButton(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .longPressDraggableHandle(),
                                    onClick  = {}
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.DragHandle,
                                        contentDescription = "Reordenar",
                                        tint               = TextSecondary.copy(alpha = 0.3f),
                                        modifier           = Modifier.size(20.dp)
                                    )
                                }
                            },
                            onOpen        = openAction,
                            onEdit        = { onEditVoucher(voucher.id) },
                            onShare       = shareAction,
                            onDelete      = { confirmDeleteId = voucher.id },
                            onToggleUsed  = { onToggleUsed(voucher.id, !voucher.isUsed) }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmação de exclusão
    confirmDeleteId?.let { deleteId ->
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Remover voucher?") },
            text  = { Text("Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    localVouchers = localVouchers.filter { it.id != deleteId }
                    onDeleteVoucher(deleteId)
                    confirmDeleteId = null
                }) {
                    Text("Remover", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text("Cancelar") }
            }
        )
    }
}

private fun shareVoucher(context: android.content.Context, voucher: Voucher) {
    val text = buildString {
        append("🎫 ${voucher.name}")
        if (!voucher.person.isNullOrBlank()) append("\n👤 ${voucher.person}")
        if (voucher.groupName.isNotBlank()) append("\n📂 ${voucher.groupName}")
        if (voucher.assetPath.startsWith("http")) append("\n🔗 ${voucher.assetPath}")
    }

    val intent = when {
        voucher.assetPath.startsWith(context.filesDir.absolutePath) -> {
            val file = File(voucher.assetPath)
            val uri  = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        voucher.assetPath.isNotBlank() && !voucher.assetPath.startsWith("http") -> {
            val fileName = File(voucher.assetPath).name
            val cacheFile = File(context.cacheDir, fileName)
            try {
                context.assets.open(voucher.assetPath).use { i -> cacheFile.outputStream().use { o -> i.copyTo(o) } }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                Intent(Intent.ACTION_SEND).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, text)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } catch (e: Exception) {
                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
            }
        }
        else -> Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar voucher"))
}

// ── Card (Conceito A) ─────────────────────────────────────────────────────────

@Composable
private fun VoucherCard(
    voucher: Voucher,
    elevation: Dp = 0.dp,
    isDragging: Boolean = false,
    dragHandle: @Composable () -> Unit = {},
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onToggleUsed: () -> Unit = {}
) {
    val type    = voucherType(voucher.assetPath)
    val palette = voucherPalette(type)
    val usedAlpha = if (voucher.isUsed) 0.45f else 1f

    Card(
        onClick   = onOpen,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (voucher.isUsed) SurfaceWhite.copy(alpha = 0.6f) else SurfaceWhite
        ),
        border    = if (isDragging) BorderStroke(1.5.dp, palette.accent.copy(alpha = 0.4f))
                    else if (voucher.isUsed) BorderStroke(0.5.dp, CardBorder.copy(alpha = 0.15f))
                    else BorderStroke(0.5.dp, CardBorder.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Acento lateral colorido por tipo
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(palette.accent.copy(alpha = usedAlpha))
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                // ── Corpo superior ────────────────────────────────────────
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Emoji em container arredondado
                    Box(
                        modifier        = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.emojiBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(voucher.emoji, fontSize = 22.sp)
                    }

                    // Nome e pessoa
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text     = voucher.name,
                            style    = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color    = TextPrimary.copy(alpha = usedAlpha),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!voucher.person.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text  = voucher.person,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary.copy(alpha = 0.7f * usedAlpha)
                            )
                        }
                    }

                    // Handle de drag — segure para reordenar
                    dragHandle()
                }

                // ── Divisória fina ─────────────────────────────────────────
                HorizontalDivider(
                    modifier  = Modifier.padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color     = CardBorder.copy(alpha = 0.2f)
                )

                // ── Footer: badge + ações ──────────────────────────────────
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge de tipo
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = palette.badgeBackground
                    ) {
                        Text(
                            text          = palette.label,
                            modifier      = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.SemiBold,
                            color         = palette.badgeText,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Marcar como usado
                    IconButton(onClick = onToggleUsed, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector        = if (voucher.isUsed) Icons.Default.CheckCircle
                                                 else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (voucher.isUsed) "Marcar como não usado" else "Marcar como usado",
                            tint               = if (voucher.isUsed) GreenMoss else TextSecondary.copy(alpha = 0.4f),
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                    // Editar
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector        = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint               = TextSecondary.copy(alpha = 0.6f),
                            modifier           = Modifier.size(17.dp)
                        )
                    }
                    // Compartilhar
                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector        = Icons.Default.Share,
                            contentDescription = "Compartilhar",
                            tint               = TextSecondary.copy(alpha = 0.6f),
                            modifier           = Modifier.size(17.dp)
                        )
                    }
                    // Deletar
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector        = Icons.Default.Delete,
                            contentDescription = "Remover",
                            tint               = Color(0xFFD32F2F).copy(alpha = 0.7f),
                            modifier           = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Cabeçalho de grupo ────────────────────────────────────────────────────────

@Composable
private fun GroupHeader(label: String, count: Int) {
    Row(
        modifier          = Modifier.padding(top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text          = label.uppercase(),
            fontSize      = 10.sp,
            color         = GreenMoss,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = GreenMoss.copy(alpha = 0.12f)
        ) {
            Text(
                text       = count.toString(),
                modifier   = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                fontSize   = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color      = GreenMoss
            )
        }
    }
}
