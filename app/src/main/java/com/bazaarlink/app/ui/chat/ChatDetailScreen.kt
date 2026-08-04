package com.bazaarlink.app.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import java.util.Date

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.style.TextOverflow
import com.bazaarlink.app.models.Message
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Close


import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.bazaarlink.app.R
import com.bazaarlink.app.models.MessageType
import com.bazaarlink.app.viewmodels.ChatViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: ChatViewModel,
    chatId: String,
    currentUserId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val chat by viewModel.activeChat.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val scrollToMessage: (String) -> Unit = { targetMsgId ->
        if (targetMsgId.isNotBlank()) {
            val targetIndex = messages.indexOfFirst { it.messageId == targetMsgId }
            if (targetIndex >= 0) {
                coroutineScope.launch {
                    listState.animateScrollToItem((targetIndex + 1).coerceAtMost(messages.size))
                }
            }
        }
    }

    val isBuyer = currentUserId == (chat?.buyerId ?: "")

    val otherName = chat?.let { c ->
        if (isBuyer) c.buyerNicknameForVendor.ifBlank { c.vendorDisplayName }
        else c.vendorNicknameForBuyer.ifBlank { c.buyerDisplayName }
    } ?: ""

    // UI state
    var messageText by remember { mutableStateOf("") }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var nicknameInput by remember { mutableStateOf("") }

    // Voice recording state
    var isRecording by remember { mutableStateOf(false) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingStartMs by remember { mutableIntStateOf(0) }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendImage(chatId, currentUserId, it) }
    }

    // Mic permission launcher
    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Log.w("BazaarLink", "Audio recording permission denied")
        }
    }

    // Load chat + messages when screen opens
    LaunchedEffect(chatId) {
        viewModel.openChat(chatId)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Clean up recorder on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder?.stop()
            } catch (_: Exception) {}
            recorder?.release()
            recorder = null
        }
    }

    fun startRecording() {
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            recordingFile = file
            val mr = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mr
            recordingStartMs = System.currentTimeMillis().toInt()
            isRecording = true
            Log.d("BazaarLink", "startRecording: started audio recording to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("BazaarLink", "startRecording error: ${e.message}", e)
            isRecording = false
            recordingFile = null
        }
    }

    fun stopAndSendRecording(
        replyToMessageId: String = "",
        replyToSenderName: String = "",
        replyToTextPreview: String = ""
    ) {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.e("BazaarLink", "stopAndSendRecording stop failed: ${e.message}")
        }
        recorder?.release()
        recorder = null
        isRecording = false

        val durationSecs = ((System.currentTimeMillis().toInt() - recordingStartMs) / 1000).coerceAtLeast(1)
        recordingFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                Log.d("BazaarLink", "stopAndSendRecording: sending voice note (${file.length()} bytes, ${durationSecs}s)")
                viewModel.sendVoice(chatId, currentUserId, Uri.fromFile(file), durationSecs, replyToMessageId, replyToSenderName, replyToTextPreview)
            }
        }
        recordingFile = null
    }


    if (showNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text(text = stringResource(id = R.string.chat_set_nickname_title)) },
            text = {
                OutlinedTextField(
                    value = nicknameInput,
                    onValueChange = { nicknameInput = it },
                    placeholder = { Text(text = otherName) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nicknameInput.isNotBlank()) {
                        viewModel.updateNickname(chatId, currentUserId, chat?.buyerId ?: "", nicknameInput)
                    }
                    showNicknameDialog = false
                }) { Text(text = stringResource(id = R.string.done)) }
            },
            dismissButton = {
                TextButton(onClick = { showNicknameDialog = false }) { Text(text = stringResource(id = R.string.cancel)) }
            }
        )
    }

    var showEReceiptDialog by remember { mutableStateOf(false) }
    var selectedMessageIds by remember { mutableStateOf(setOf<String>()) }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }

    val otherPhone = chat?.let { c -> if (isBuyer) c.vendorPhone else c.buyerPhone } ?: ""

    if (showEReceiptDialog && chat != null) {
        com.bazaarlink.app.ui.common.EReceiptDialog(
            chat = chat!!,
            onDismiss = { showEReceiptDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    AnimatedContent(
                        targetState = selectedMessageIds.isNotEmpty(),
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                        label = "NavIconAnim"
                    ) { inSelection ->
                        if (inSelection) {
                            IconButton(onClick = { selectedMessageIds = emptySet() }) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Deselect", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        } else {
                            IconButton(onClick = onBack) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.back), tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                },
                title = {
                    AnimatedContent(
                        targetState = selectedMessageIds.isNotEmpty(),
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                        label = "TitleAnim"
                    ) { inSelection ->
                        if (inSelection) {
                            Text(text = "${selectedMessageIds.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(
                                text = otherName.ifBlank { stringResource(id = R.string.chats_title) },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    AnimatedContent(
                        targetState = selectedMessageIds.isNotEmpty(),
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                        label = "ActionsAnim"
                    ) { inSelection ->
                        if (inSelection) {
                            val selectedMsgs = messages.filter { it.messageId in selectedMessageIds }
                            val hasCopyableText = selectedMsgs.any { it.text.isNotBlank() || it.type == MessageType.TEXT }
                            Row {
                                if (hasCopyableText) {
                                    IconButton(onClick = {
                                        val textToCopy = selectedMsgs.mapNotNull { it.text.ifBlank { null } }.joinToString("\n")
                                        if (textToCopy.isNotBlank()) {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("BazaarLink Chat", textToCopy)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                        selectedMessageIds = emptySet()
                                    }) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                                if (selectedMessageIds.size == 1) {
                                    IconButton(onClick = {
                                        replyingToMessage = selectedMsgs.firstOrNull()
                                        selectedMessageIds = emptySet()
                                    }) {
                                        Icon(imageVector = Icons.Default.Reply, contentDescription = "Reply", tint = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                                IconButton(onClick = {
                                    viewModel.deleteMessages(chatId, selectedMessageIds.toList())
                                    selectedMessageIds = emptySet()
                                }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        } else {
                            Row {
                                IconButton(onClick = {
                                    val phoneNum = otherPhone.ifBlank { "03001234567" }
                                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:$phoneNum")
                                    }
                                    context.startActivity(intent)
                                }) {
                                    Icon(imageVector = Icons.Default.Phone, contentDescription = "Call", tint = MaterialTheme.colorScheme.onPrimary)
                                }

                                IconButton(onClick = { showEReceiptDialog = true }) {
                                    Icon(imageVector = Icons.Default.Receipt, contentDescription = "E-Receipt", tint = MaterialTheme.colorScheme.onPrimary)
                                }

                                IconButton(onClick = {
                                    nicknameInput = if (isBuyer) chat?.buyerNicknameForVendor ?: "" else chat?.vendorNicknameForBuyer ?: ""
                                    showNicknameDialog = true
                                }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(id = R.string.chat_set_nickname_title), tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(messages, key = { it.messageId }) { message ->
                    val isMe = message.senderId == currentUserId
                    val isSelected = selectedMessageIds.contains(message.messageId)
                    var offsetX by remember { mutableStateOf(0f) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent)
                            .pointerInput(message.messageId, selectedMessageIds) {
                                detectTapGestures(
                                    onLongPress = {
                                        val alreadySelected = selectedMessageIds.contains(message.messageId)
                                        selectedMessageIds = if (alreadySelected) {
                                            selectedMessageIds - message.messageId
                                        } else {
                                            selectedMessageIds + message.messageId
                                        }
                                    },
                                    onTap = {
                                        if (selectedMessageIds.isNotEmpty()) {
                                            val alreadySelected = selectedMessageIds.contains(message.messageId)
                                            selectedMessageIds = if (alreadySelected) {
                                                selectedMessageIds - message.messageId
                                            } else {
                                                selectedMessageIds + message.messageId
                                            }
                                        }
                                    }
                                )
                            }


                            .pointerInput(message.messageId) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (offsetX > 60f) {
                                            replyingToMessage = message
                                        }
                                        offsetX = 0f
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        if (dragAmount > 0f || offsetX > 0f) {
                                            offsetX = (offsetX + dragAmount).coerceIn(0f, 100f)
                                        }
                                    }
                                )
                            }
                            .offset { IntOffset(offsetX.roundToInt(), 0) }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            val timeDate = message.createdAt ?: Date(message.timestamp)
                            val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timeDate)
                            val isSent = !message.isSending && (message.createdAt != null || message.timestamp > 0L)
                            when (message.type) {
                                MessageType.TEXT -> TextBubble(
                                    text = message.text,
                                    isMe = isMe,
                                    time = formattedTime,
                                    isSent = isSent,
                                    replyToSenderName = message.replyToSenderName,
                                    replyToTextPreview = message.replyToTextPreview,
                                    onQuotedClick = { scrollToMessage(message.replyToMessageId) }
                                )
                                MessageType.VOICE -> VoiceBubble(
                                    voiceUrl = message.voiceUrl,
                                    durationSecs = message.voiceDurationSecs,
                                    isMe = isMe,
                                    time = formattedTime,
                                    isPending = message.voiceUrl.isBlank() || message.isSending,
                                    isSent = isSent,
                                    replyToSenderName = message.replyToSenderName,
                                    replyToTextPreview = message.replyToTextPreview,
                                    onQuotedClick = { scrollToMessage(message.replyToMessageId) }
                                )
                                MessageType.IMAGE -> ImageBubble(imageUrl = message.imageUrl, isMe = isMe)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Animated mic size expansion during press-and-hold recording
            val micContainerSize by animateDpAsState(
                targetValue = if (isRecording) 64.dp else 48.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "micContainerSizeAnim"
            )
            val micIconSize by animateDpAsState(
                targetValue = if (isRecording) 28.dp else 22.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "micIconSizeAnim"
            )

            // Recording indicator banner
            if (isRecording) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "🎙️ Recording voice note... Release to send", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            // Reply Preview Bar above bottom input box
            if (replyingToMessage != null) {
                val target = replyingToMessage!!
                val targetSender = if (target.senderId == currentUserId) "You" else otherName
                val targetPreview = when (target.type) {
                    MessageType.VOICE -> "🎙️ Voice note (${target.voiceDurationSecs}s)"
                    MessageType.IMAGE -> "📷 Photo"
                    else -> target.text
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clickable { scrollToMessage(target.messageId) },
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {

                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Replying to $targetSender",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = targetPreview,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                        }
                        IconButton(
                            onClick = { replyingToMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel reply", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Bottom Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image button
                IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = stringResource(id = R.string.chat_send_image), tint = MaterialTheme.colorScheme.primary)
                }

                // Text field
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text(text = stringResource(id = R.string.chat_input_hint)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(6.dp))

                if (messageText.isNotBlank()) {
                    // Send text button
                    IconButton(
                        onClick = {
                            val rMsg = replyingToMessage
                            val rId = rMsg?.messageId ?: ""
                            val rName = if (rMsg != null) { if (rMsg.senderId == currentUserId) "You" else otherName } else ""
                            val rText = if (rMsg != null) {
                                when (rMsg.type) {
                                    MessageType.VOICE -> "🎙️ Voice note (${rMsg.voiceDurationSecs}s)"
                                    MessageType.IMAGE -> "📷 Photo"
                                    else -> rMsg.text
                                }
                            } else ""

                            viewModel.sendText(chatId, currentUserId, messageText.trim(), rId, rName, rText)
                            messageText = ""
                            replyingToMessage = null
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = stringResource(id = R.string.submit), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                } else {
                    // Press-and-hold mic box for voice recording with dynamic spring expansion
                    Box(
                        modifier = Modifier
                            .size(micContainerSize)
                            .clip(CircleShape)
                            .background(if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        startRecording()
                                        tryAwaitRelease()
                                        if (isRecording) {
                                            val rMsg = replyingToMessage

                                            val rId = rMsg?.messageId ?: ""
                                            val rName = if (rMsg != null) { if (rMsg.senderId == currentUserId) "You" else otherName } else ""
                                            val rText = if (rMsg != null) {
                                                when (rMsg.type) {
                                                    MessageType.VOICE -> "🎙️ Voice note (${rMsg.voiceDurationSecs}s)"
                                                    MessageType.IMAGE -> "📷 Photo"
                                                    else -> rMsg.text
                                                }
                                            } else ""

                                            stopAndSendRecording(rId, rName, rText)
                                            replyingToMessage = null

                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = stringResource(id = R.string.mic_button),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(micIconSize)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun TextBubble(
    text: String,
    isMe: Boolean,
    time: String,
    isSent: Boolean = false,
    replyToSenderName: String = "",
    replyToTextPreview: String = "",
    onQuotedClick: () -> Unit = {}
) {
    val bgColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                if (replyToSenderName.isNotBlank() || replyToTextPreview.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = textColor.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clickable { onQuotedClick() }
                    ) {
                        Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(textColor, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = replyToSenderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = replyToTextPreview,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = textColor.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
                Text(text = text, color = textColor, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(text = time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isMe && isSent) {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Sent",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceBubble(
    voiceUrl: String,
    durationSecs: Int,
    isMe: Boolean,
    time: String,
    isPending: Boolean = false,
    isSent: Boolean = false,
    replyToSenderName: String = "",
    replyToTextPreview: String = "",
    onQuotedClick: () -> Unit = {}
) {
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(voiceUrl) {
        onDispose {
            try {
                mediaPlayer?.stop()
            } catch (_: Exception) {}
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    val bgColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            modifier = Modifier.widthIn(max = 250.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                if (replyToSenderName.isNotBlank() || replyToTextPreview.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = contentColor.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clickable { onQuotedClick() }
                    ) {

                        Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(contentColor, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = replyToSenderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor
                                )
                                Text(
                                    text = replyToTextPreview,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = contentColor.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPending || voiceUrl.isBlank()) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = contentColor
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        try { mediaPlayer?.pause() } catch (_: Exception) {}
                                        isPlaying = false
                                    } else {
                                        try {
                                            val mp = MediaPlayer().apply {
                                                setDataSource(voiceUrl)
                                                prepareAsync()
                                                setOnPreparedListener {
                                                    it.start()
                                                    isPlaying = true
                                                }
                                                setOnCompletionListener {
                                                    isPlaying = false
                                                    it.release()
                                                    mediaPlayer = null
                                                }
                                            }
                                            mediaPlayer = mp
                                        } catch (e: Exception) {
                                            Log.e("BazaarLink", "Voice play error: ${e.message}")
                                            isPlaying = false
                                        }
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPending || voiceUrl.isBlank()) "🎙️ Sending voice note..." else "🎙️ Voice note (${durationSecs}s)",
                        color = contentColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(text = time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isMe && isSent && !isPending && voiceUrl.isNotBlank()) {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Sent",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}




@Composable
private fun ImageBubble(imageUrl: String, isMe: Boolean) {
    var isEnlarged by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { isEnlarged = true }
        )
    }
    if (isEnlarged) {
        com.bazaarlink.app.ui.common.FullscreenImageDialog(imageUrl = imageUrl, onDismiss = { isEnlarged = false })
    }
}
