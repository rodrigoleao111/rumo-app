package com.rodrigoleao.gramado2026.ui.contacts

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rodrigoleao.gramado2026.data.model.Contact
import com.rodrigoleao.gramado2026.data.model.ContactType
import com.rodrigoleao.gramado2026.ui.theme.*
import com.rodrigoleao.gramado2026.utils.dialPhone
import com.rodrigoleao.gramado2026.utils.openWhatsApp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// ── List item types ───────────────────────────────────────────────────────────

private sealed class ContactListItem {
    data class Header(val label: String, val emoji: String) : ContactListItem()
    data class Item(val contact: Contact, val groupKey: String) : ContactListItem()
}

// ── Contatos fixos de emergência (IDs negativos, não vêm do banco) ────────────

private val BUILTIN_EMERGENCY_CONTACTS = listOf(
    Contact(id = -1L, name = "SAMU",            role = "Serviço de Atendimento Móvel de Urgência", phone = "192", type = ContactType.EMERGENCY, isEmergency = true),
    Contact(id = -2L, name = "Bombeiros",        role = "Corpo de Bombeiros",                       phone = "193", type = ContactType.EMERGENCY, isEmergency = true),
    Contact(id = -3L, name = "Polícia Militar",  role = "Emergências policiais",                    phone = "190", type = ContactType.EMERGENCY, isEmergency = true),
)

// ── Groups definition — replaced by dynamic listItems building ────────────────

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    contentPadding: PaddingValues = PaddingValues(),
    onEditContact: (Long) -> Unit = {},
    onDeleteContact: (Long) -> Unit = {},
    onReorderContacts: (List<Contact>) -> Unit = {},
    onToggleFavoriteContact: (Long, Boolean) -> Unit = { _, _ -> },
    showEmergencyContacts: Boolean = true
) {
    val context = LocalContext.current
    var localContacts by remember(contacts) { mutableStateOf(contacts) }
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }

    LaunchedEffect(localContacts) {
        if (localContacts != contacts) onReorderContacts(localContacts)
    }

    val listItems: List<ContactListItem> = remember(localContacts, showEmergencyContacts) {
        buildList {
            // Fixed groups in order
            val fixedGroups = listOf(
                Triple("favorites", "Favoritos", "⭐")         to { c: Contact -> c.isFavorite },
                Triple("agency",    "Agência & Transfers", "📋") to { c: Contact -> c.type == ContactType.AGENCY && !c.isFavorite },
                Triple("hotel",     "Hospedagem", "🏨")         to { c: Contact -> c.type == ContactType.HOTEL && !c.isFavorite },
                Triple("family",    "Família", "👨‍👩‍👧")            to { c: Contact -> c.type == ContactType.FAMILY && !c.isFavorite },
                Triple("attraction","Atrações", "🎡")           to { c: Contact -> c.type == ContactType.ATTRACTION && !c.isFavorite },
                Triple("emergency", "Emergências", "🚨")        to { c: Contact -> c.type == ContactType.EMERGENCY && !c.isFavorite },
            )
            fixedGroups.forEach { (meta, filter) ->
                val (key, label, emoji) = meta
                val builtins = if (key == "emergency" && showEmergencyContacts) BUILTIN_EMERGENCY_CONTACTS else emptyList()
                val userContacts = localContacts.filter(filter).sortedBy { it.sortOrder }
                val all = builtins + userContacts
                if (all.isNotEmpty()) {
                    add(ContactListItem.Header(label, emoji))
                    all.forEach { add(ContactListItem.Item(it, key)) }
                }
            }
            // Custom groups: contacts with type == CUSTOM, grouped by customTypeName
            val customContacts = localContacts.filter { it.type == ContactType.CUSTOM && !it.isFavorite }
            val customGroups = customContacts
                .groupBy { it.customTypeName.ifBlank { "Outros" } }
                .entries.sortedBy { it.key }
            customGroups.forEach { (groupName, groupContacts) ->
                add(ContactListItem.Header(groupName, "🏷️"))
                groupContacts.sortedBy { it.sortOrder }.forEach { add(ContactListItem.Item(it, "custom_$groupName")) }
            }
        }
    }

    if (contacts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📞", fontSize = 48.sp)
                Text("Nenhum contato ainda", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Toque em + para adicionar", fontSize = 14.sp, color = TextSecondary)
            }
        }
        return
    }

    val lazyListState = rememberLazyListState()

    val reorderState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            val fromItem = listItems.getOrNull(from.index) as? ContactListItem.Item ?: return@rememberReorderableLazyListState
            val toItem   = listItems.getOrNull(to.index)   as? ContactListItem.Item ?: return@rememberReorderableLazyListState
            if (fromItem.groupKey != toItem.groupKey) return@rememberReorderableLazyListState
            val mutable = localContacts.toMutableList()
            val fromIdx = mutable.indexOfFirst { it.id == fromItem.contact.id }
            val toIdx   = mutable.indexOfFirst { it.id == toItem.contact.id }
            if (fromIdx >= 0 && toIdx >= 0) {
                val moved = mutable.removeAt(fromIdx)
                mutable.add(toIdx, moved)
                localContacts = mutable
            }
        }
    )

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
        items(
            items = listItems,
            key   = { item ->
                when (item) {
                    is ContactListItem.Header -> "header_${item.label}"
                    is ContactListItem.Item   -> "contact_${item.contact.id}"
                }
            }
        ) { listItem ->
            when (listItem) {
                is ContactListItem.Header -> {
                    ContactGroupHeader(listItem.label, listItem.emoji)
                }
                is ContactListItem.Item -> {
                    val contact = listItem.contact
                    val isBuiltin = contact.id < 0

                    if (isBuiltin) {
                        // Contatos fixos: sem swipe, drag ou estrela
                        ContactCard(
                            contact          = contact,
                            dragHandle       = {},
                            onCallClick      = contact.phone?.let { { dialPhone(context, it) } },
                            onWhatsAppClick  = null,
                            onEditClick      = {},
                            onToggleFavorite = {},
                            showActions      = false
                        )
                    } else {
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                // Só abre o dialog se nenhum outro já estiver aberto
                                if (value == SwipeToDismissBoxValue.EndToStart && contactToDelete == null) {
                                    contactToDelete = contact
                                }
                                false
                            }
                        )
                        // Reseta o swipe quando o dialog é fechado (cancelar ou confirmar)
                        LaunchedEffect(contactToDelete) {
                            if (contactToDelete == null) dismissState.reset()
                        }

                        ReorderableItem(
                            state = reorderState,
                            key   = "contact_${contact.id}"
                        ) { isDragging ->
                            val elevation by animateDpAsState(
                                targetValue = if (isDragging) 10.dp else 0.dp,
                                label       = "dragElevation"
                            )
                            SwipeToDismissBox(
                                state            = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    val color by animateColorAsState(
                                        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                                            Color(0xFFE53935) else Color.Transparent,
                                        label = "swipe_bg"
                                    )
                                    Box(
                                        modifier         = Modifier
                                            .fillMaxSize()
                                            .background(color, RoundedCornerShape(12.dp))
                                            .padding(end = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                            Icon(
                                                imageVector        = Icons.Default.Delete,
                                                contentDescription = "Remover",
                                                tint               = Color.White,
                                                modifier           = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            ) {
                                ContactCard(
                                    contact       = contact,
                                    elevation     = elevation,
                                    isDragging    = isDragging,
                                    dragHandle    = {
                                        IconButton(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .longPressDraggableHandle(),
                                            onClick  = {}
                                        ) {
                                            Icon(
                                                imageVector        = Icons.Default.DragHandle,
                                                contentDescription = "Reordenar",
                                                tint               = Color.White.copy(alpha = 0.8f),
                                                modifier           = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    onCallClick   = contact.phone?.let { { dialPhone(context, it) } },
                                    onWhatsAppClick = if (contact.hasWhatsApp && contact.phone != null) {
                                        { openWhatsApp(context, contact.phone) }
                                    } else null,
                                    onEditClick   = { onEditContact(contact.id) },
                                    onToggleFavorite = {
                                        val newFav = !contact.isFavorite
                                        localContacts = localContacts.map {
                                            if (it.id == contact.id) it.copy(isFavorite = newFav) else it
                                        }
                                        onToggleFavoriteContact(contact.id, newFav)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    contactToDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Remover ${c.name}?") },
            text  = { Text("Esse contato será removido permanentemente.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteContact(c.id)
                    contactToDelete = null
                }) {
                    Text("Remover", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

// ── Group header ──────────────────────────────────────────────────────────────

@Composable
private fun ContactGroupHeader(label: String, emoji: String) {
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

// ── Contact card ──────────────────────────────────────────────────────────────

@Composable
private fun ContactCard(
    contact: Contact,
    elevation: Dp = 0.dp,
    isDragging: Boolean = false,
    dragHandle: @Composable () -> Unit = {},
    onCallClick: (() -> Unit)?,
    onWhatsAppClick: (() -> Unit)?,
    onEditClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    showActions: Boolean = true
) {
    val isEmergency = contact.isEmergency

    val headerColor = if (isEmergency) Color(0xFFB71C1C) else GreenMoss

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border    = if (isDragging) BorderStroke(1.5.dp, GreenMoss.copy(alpha = 0.4f))
                    else BorderStroke(1.dp, if (isEmergency) Color(0xFFE88888) else GreenMoss),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        // ── Header colorido ──────────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .background(headerColor)
                .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            dragHandle()

            Spacer(Modifier.width(6.dp))

            Text(
                text       = contact.name,
                style      = MaterialTheme.typography.titleMedium,
                fontSize   = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White,
                modifier   = Modifier.weight(1f)
            )

            if (showActions) {
                IconButton(
                    onClick  = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector        = if (contact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (contact.isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                        tint               = if (contact.isFavorite) AmberPrimary else Color.White.copy(alpha = 0.5f),
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ── Corpo ────────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
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

                Spacer(Modifier.height(8.dp))

                // Action buttons at bottom
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
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

                    Spacer(Modifier.weight(1f))

                    if (showActions) {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, "Editar contato", tint = GreenSage.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
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
