package com.bazaarlink.app.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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

    fun stopAndSendRecording() {
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
                viewModel.sendVoice(chatId, currentUserId, Uri.fromFile(file), durationSecs)
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

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.back), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                title = {
                    Column {
                        Text(text = otherName.ifBlank { stringResource(id = R.string.chats_title) }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        nicknameInput = if (isBuyer) chat?.buyerNicknameForVendor ?: "" else chat?.vendorNicknameForBuyer ?: ""
                        showNicknameDialog = true
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(id = R.string.chat_set_nickname_title), tint = MaterialTheme.colorScheme.onPrimary)
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
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(messages, key = { it.messageId }) { message ->
                    val isMe = message.senderId == currentUserId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        when (message.type) {
                            MessageType.TEXT -> TextBubble(text = message.text, isMe = isMe, time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(message.createdAt))
                            MessageType.VOICE -> VoiceBubble(voiceUrl = message.voiceUrl, durationSecs = message.voiceDurationSecs, isMe = isMe, time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(message.createdAt))
                            MessageType.IMAGE -> ImageBubble(imageUrl = message.imageUrl, isMe = isMe)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

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
                            viewModel.sendText(chatId, currentUserId, messageText.trim())
                            messageText = ""
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = stringResource(id = R.string.submit), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                } else {
                    // Press-and-hold mic box for voice recording
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        startRecording()
                                        tryAwaitRelease()
                                        if (isRecording) stopAndSendRecording()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = stringResource(id = R.string.mic_button),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextBubble(text: String, isMe: Boolean, time: String) {
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
            Text(text = text, color = textColor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
        Text(text = time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
    }
}

@Composable
private fun VoiceBubble(voiceUrl: String, durationSecs: Int, isMe: Boolean, time: String) {
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
            modifier = Modifier.widthIn(max = 240.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "🎙️ Voice note (${durationSecs}s)", color = contentColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(text = time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
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
