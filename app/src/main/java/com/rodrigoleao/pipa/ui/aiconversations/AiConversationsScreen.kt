package com.rodrigoleao.pipa.ui.aiconversations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.pipa.R
import com.rodrigoleao.pipa.data.model.AiConversation
import com.rodrigoleao.pipa.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConversationsScreen(
    viewModel: AiConversationsViewModel,
    onConversationClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<AiConversation?>(null) }

    Scaffold(
        containerColor = Sand,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.aiconv_title), fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.common_back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = GreenMoss,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        val list = conversations
        when {
            list == null -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = GreenMoss) }

            list.isEmpty() -> EmptyState(innerPadding)

            else -> LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(
                    start  = 16.dp,
                    end    = 16.dp,
                    top    = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list, key = { it.id }) { conv ->
                    ConversationCard(
                        conv,
                        onClick  = { onConversationClick(conv.id) },
                        onDelete = { pendingDelete = conv }
                    )
                }
            }
        }
    }

    pendingDelete?.let { conv ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon  = { Icon(ImageVector.vectorResource(R.drawable.ic_delete), contentDescription = null, tint = Color(0xFFD32F2F)) },
            title = { Text(stringResource(R.string.aiconv_delete_title)) },
            text  = { Text(stringResource(R.string.aiconv_delete_msg), style = MaterialTheme.typography.bodyMedium, color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { viewModel.delete(conv.id); pendingDelete = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(stringResource(R.string.common_delete), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun ConversationCard(conv: AiConversation, onClick: () -> Unit, onDelete: () -> Unit) {
    val fallbackTitle = stringResource(R.string.aiconv_untitled)
    val title = conv.tripName.ifBlank { conv.destination }.ifBlank { fallbackTitle }

    Card(
        onClick    = onClick,
        modifier   = Modifier.fillMaxWidth(),
        shape      = RoundedCornerShape(16.dp),
        colors     = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation  = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border     = BorderStroke(0.5.dp, CardBorder)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GreenMoss.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(ImageVector.vectorResource(R.drawable.ic_auto_awesome), contentDescription = null, tint = GreenMoss, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (conv.destination.isNotBlank()) {
                    Text("📍 ${conv.destination}", color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(conversationDateLabel(conv), color = TextSecondary, fontSize = 12.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.aiconv_delete_cd),
                    tint     = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(innerPadding: PaddingValues) {
    Box(
        Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("💬", fontSize = 56.sp)
            Text(
                text       = stringResource(R.string.aiconv_empty_title),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                textAlign  = TextAlign.Center
            )
            Text(
                text      = stringResource(R.string.aiconv_empty_msg),
                style     = MaterialTheme.typography.bodySmall,
                color     = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Rótulo de data do card: período da viagem se houver; senão a data da conversa. */
private fun conversationDateLabel(conv: AiConversation): String {
    val fmt   = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    val start = conv.startDate?.let { runCatching { LocalDate.parse(it).format(fmt) }.getOrNull() }
    val end   = conv.endDate?.let { runCatching { LocalDate.parse(it).format(fmt) }.getOrNull() }
    return when {
        start != null && end != null -> "$start → $end"
        start != null                -> start
        else -> LocalDate.ofInstant(Instant.ofEpochMilli(conv.createdAt), ZoneId.systemDefault()).format(fmt)
    }
}
