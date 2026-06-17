package com.rodrigoleao.gramado2026.ui.contacts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rodrigoleao.gramado2026.data.model.Contact
import com.rodrigoleao.gramado2026.data.model.ContactType
import com.rodrigoleao.gramado2026.ui.theme.*
import com.rodrigoleao.gramado2026.utils.dialPhone
import com.rodrigoleao.gramado2026.utils.openWhatsApp

@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    contentPadding: PaddingValues = PaddingValues(),
    onEditContact: (Long) -> Unit = {}
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
        val groups = listOf(
            ContactType.AGENCY     to "Agência & Transfers",
            ContactType.HOTEL      to "Hospedagem",
            ContactType.ATTRACTION to "Atrações",
            ContactType.EMERGENCY  to "Emergências"
        )

        groups.forEach { (type, label) ->
            val groupContacts = contacts.filter { it.type == type }
            if (groupContacts.isNotEmpty()) {
                item {
                    GroupHeader(
                        label = label,
                        emoji = when (type) {
                            ContactType.AGENCY     -> "📋"
                            ContactType.HOTEL      -> "🏨"
                            ContactType.ATTRACTION -> "🎡"
                            ContactType.EMERGENCY  -> "🚨"
                        }
                    )
                }
                items(groupContacts) { contact ->
                    ContactCard(
                        contact       = contact,
                        onCallClick   = contact.phone?.let { { dialPhone(context, it) } },
                        onWhatsAppClick = if (contact.hasWhatsApp && contact.phone != null) {
                            { openWhatsApp(context, contact.phone) }
                        } else null,
                        onEditClick   = { onEditContact(contact.id) }
                    )
                }
            }
        }

        if (contacts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📞", fontSize = 36.sp)
                        Text("Nenhum contato ainda", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("Toque em + para adicionar", style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(label: String, emoji: String) {
    Row(
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(emoji, fontSize = 14.sp)
        Text(
            text          = label.uppercase(),
            fontSize      = 10.sp,
            color         = GreenMoss,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun ContactCard(
    contact: Contact,
    onCallClick: (() -> Unit)?,
    onWhatsAppClick: (() -> Unit)?,
    onEditClick: () -> Unit
) {
    val isEmergency = contact.isEmergency

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isEmergency) Color(0xFFFFF5F5) else SurfaceWhite
        ),
        border    = BorderStroke(1.dp, if (isEmergency) Color(0xFFE88888) else CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = contact.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (isEmergency) Color(0xFF8A1515) else TextPrimary
                )
                Text(text = contact.role, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                contact.phone?.let { phone ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text       = formatPhone(phone),
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = if (isEmergency) Color(0xFF8A1515) else GreenMoss,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Editar
                IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, "Editar contato", tint = GreenSage.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }

                if (onCallClick != null) {
                    if (isEmergency) {
                        Button(
                            onClick        = onCallClick,
                            colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            shape          = RoundedCornerShape(8.dp)
                        ) {
                            Text("🚨  Ligar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick        = onCallClick,
                            border         = BorderStroke(1.dp, GreenMoss),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier       = Modifier.height(32.dp),
                            shape          = RoundedCornerShape(8.dp)
                        ) {
                            Text("📞  Ligar", fontSize = 12.sp, color = GreenMoss)
                        }
                        onWhatsAppClick?.let { onClick ->
                            OutlinedButton(
                                onClick        = onClick,
                                border         = BorderStroke(1.dp, Color(0xFF25D366)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier       = Modifier.height(32.dp),
                                shape          = RoundedCornerShape(8.dp)
                            ) {
                                Text("💬  WhatsApp", fontSize = 12.sp, color = Color(0xFF0A7A30))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatPhone(raw: String): String = when (raw.length) {
    3    -> raw
    10   -> "(${raw.take(2)}) ${raw.drop(2).take(4)}-${raw.drop(6)}"
    11   -> "(${raw.take(2)}) ${raw.drop(2).take(5)}-${raw.drop(7)}"
    else -> raw
}
