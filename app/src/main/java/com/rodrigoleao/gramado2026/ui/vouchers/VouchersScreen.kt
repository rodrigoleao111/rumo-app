package com.rodrigoleao.gramado2026.ui.vouchers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rodrigoleao.gramado2026.data.model.Voucher
import com.rodrigoleao.gramado2026.ui.theme.*
import com.rodrigoleao.gramado2026.utils.openAssetFile

@Composable
fun VouchersScreen(
    vouchers: List<Voucher>,
    contentPadding: PaddingValues = PaddingValues(),
    onEditVoucher: (Long) -> Unit = {}
) {
    val context = LocalContext.current

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top    = contentPadding.calculateTopPadding()    + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 80.dp,
            start  = 16.dp,
            end    = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val grouped = vouchers.groupBy { it.groupName }

        grouped.forEach { (groupName, groupVouchers) ->
            item { GroupHeader(groupName) }
            items(groupVouchers) { voucher ->
                val openAction: () -> Unit = {
                    if (voucher.assetPath.startsWith("http")) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(voucher.assetPath)))
                    } else {
                        openAssetFile(context, voucher.assetPath)
                    }
                }
                VoucherCard(
                    voucher     = voucher,
                    onClick     = openAction,
                    onEditClick = { onEditVoucher(voucher.id) }
                )
            }
        }

        if (vouchers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🎫", fontSize = 36.sp)
                        Text("Nenhum voucher ainda", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("Toque em + para adicionar", style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(label: String) {
    Text(
        text          = label.uppercase(),
        modifier      = Modifier.padding(top = 12.dp, bottom = 4.dp),
        fontSize      = 10.sp,
        color         = GreenMoss,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 2.sp
    )
}

@Composable
private fun VoucherCard(voucher: Voucher, onClick: () -> Unit, onEditClick: () -> Unit) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border    = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(voucher.emoji, fontSize = 30.sp)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = voucher.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary
                )
                voucher.person?.let { person ->
                    Text(text = person, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Spacer(Modifier.height(4.dp))
                val fileType = when {
                    voucher.assetPath.startsWith("http")                                   -> "LINK"
                    voucher.assetPath.endsWith(".pdf",  ignoreCase = true)                 -> "PDF"
                    voucher.assetPath.endsWith(".jpeg", ignoreCase = true) ||
                    voucher.assetPath.endsWith(".jpg",  ignoreCase = true)                 -> "IMAGEM"
                    else                                                                   -> "ARQUIVO"
                }
                Surface(
                    shape  = RoundedCornerShape(100.dp),
                    color  = BadgeBookedBg,
                    border = BorderStroke(0.5.dp, BadgeBookedText.copy(alpha = 0.35f))
                ) {
                    Text(
                        text          = fileType,
                        modifier      = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize      = 9.sp,
                        color         = BadgeBookedText,
                        letterSpacing = 1.sp,
                        fontWeight    = FontWeight.Medium
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, "Editar voucher", tint = GreenSage.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }
                FilledTonalIconButton(
                    onClick = onClick,
                    colors  = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color(0xFFE8F0E8))
                ) {
                    Icon(Icons.Default.OpenInNew, "Abrir", tint = GreenMoss)
                }
            }
        }
    }
}
