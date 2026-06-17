package com.rodrigoleao.gramado2026.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rodrigoleao.gramado2026.data.model.Badge
import com.rodrigoleao.gramado2026.data.model.BadgeType
import com.rodrigoleao.gramado2026.ui.theme.*

@Composable
fun BadgeChip(badge: Badge) {
    val (bgColor, textColor) = when (badge.type) {
        BadgeType.FREE     -> BadgeFreeBg       to BadgeFreeText
        BadgeType.PAID     -> BadgePaidBg       to BadgePaidText
        BadgeType.BOOKED   -> BadgeBookedBg     to BadgeBookedText
        BadgeType.INCLUDED -> Color(0x1A1E6450) to Color(0xFF115540)
        BadgeType.UBER     -> BadgeUberBg       to BadgeUberText
        BadgeType.WALKING  -> BadgeFreeBg       to BadgeFreeText
        BadgeType.CUSTOM   -> {
            val base = try {
                Color(android.graphics.Color.parseColor(badge.color ?: "#607D8B"))
            } catch (e: Exception) { Color(0xFF607D8B) }
            base.copy(alpha = 0.15f) to base
        }
    }
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = bgColor,
        border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.4f))
    ) {
        Text(
            text = badge.label.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = textColor,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
    }
}
