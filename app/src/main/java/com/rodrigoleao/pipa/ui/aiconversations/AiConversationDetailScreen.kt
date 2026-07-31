package com.rodrigoleao.pipa.ui.aiconversations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodrigoleao.pipa.R
import com.rodrigoleao.pipa.data.model.AiChatMessage
import com.rodrigoleao.pipa.data.model.AiConversation
import com.rodrigoleao.pipa.ui.components.MarkdownText
import com.rodrigoleao.pipa.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConversationDetailScreen(
    viewModel: AiConversationDetailViewModel,
    onBack: () -> Unit
) {
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()
    val loaded       by viewModel.loaded.collectAsStateWithLifecycle()
    val clipboard    = LocalClipboardManager.current
    val snackbarHost = remember { SnackbarHostState() }
    val scope        = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf(false) }

    val fallbackTitle  = stringResource(R.string.aiconv_title)
    val youLabel       = stringResource(R.string.aiconv_you)
    val assistantLabel = stringResource(R.string.aiconv_assistant)
    val copiedMsg      = stringResource(R.string.create_copied)

    val titleText = conversation?.tripName?.takeIf { it.isNotBlank() }
        ?: conversation?.destination?.takeIf { it.isNotBlank() }
        ?: fallbackTitle

    Scaffold(
        containerColor = Sand,
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(snackbarData = data, containerColor = AmberPrimary, contentColor = GreenMoss)
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(titleText, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.common_back),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (conversation != null) {
                        IconButton(onClick = { pendingDelete = true }) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.aiconv_delete_cd),
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = GreenMoss,
                    titleContentColor      = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            val conv = conversation
            if (conv != null && conv.messages.isNotEmpty()) {
                Surface(color = SurfaceWhite, tonalElevation = 2.dp) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick  = {
                                clipboard.setText(AnnotatedString(buildTranscript(conv, youLabel, assistantLabel)))
                                scope.launch { snackbarHost.showSnackbar(copiedMsg) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = GreenMoss)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.create_copy_conversation), fontWeight = FontWeight.SemiBold, color = AmberPrimary)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val conv = conversation
        when {
            !loaded -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenMoss)
            }

            conv == null -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.aiconv_not_found), color = TextSecondary)
            }

            else -> LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(
                    start  = 16.dp,
                    end    = 16.dp,
                    top    = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(conv.messages) { msg -> DetailBubble(msg) }
            }
        }
    }

    if (pendingDelete) {
        AlertDialog(
            onDismissRequest = { pendingDelete = false },
            icon  = { Icon(ImageVector.vectorResource(R.drawable.ic_delete), contentDescription = null, tint = Color(0xFFD32F2F)) },
            title = { Text(stringResource(R.string.aiconv_delete_title)) },
            text  = { Text(stringResource(R.string.aiconv_delete_msg), style = MaterialTheme.typography.bodyMedium, color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { pendingDelete = false; viewModel.delete(onBack) },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(stringResource(R.string.common_delete), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun DetailBubble(msg: AiChatMessage) {
    val isUser = msg.fromUser
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier         = Modifier.size(32.dp).clip(RoundedCornerShape(50)).background(GreenMoss),
                contentAlignment = Alignment.Center
            ) { Text("✨", fontSize = 16.sp) }
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape    = RoundedCornerShape(
                topStart    = if (isUser) 18.dp else 4.dp,
                topEnd      = if (isUser) 4.dp  else 18.dp,
                bottomStart = 18.dp,
                bottomEnd   = 18.dp
            ),
            color          = if (isUser) GreenMoss else SurfaceWhite,
            tonalElevation = if (isUser) 0.dp else 1.dp
        ) {
            if (isUser) {
                Text(
                    text       = msg.text,
                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontSize   = 14.sp,
                    color      = Color.White,
                    lineHeight = 20.sp
                )
            } else {
                MarkdownText(
                    text     = msg.text,
                    color    = TextPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

/** Monta a transcrição completa para a área de transferência ("Copiar conversa"). */
private fun buildTranscript(conv: AiConversation, youLabel: String, assistantLabel: String): String =
    conv.messages.joinToString("\n\n") { m ->
        val who = if (m.fromUser) youLabel else assistantLabel
        "$who: ${m.text}"
    }
