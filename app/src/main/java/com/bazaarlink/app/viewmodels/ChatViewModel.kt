package com.bazaarlink.app.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bazaarlink.app.di.ServiceLocator
import com.bazaarlink.app.models.Chat
import com.bazaarlink.app.models.Message
import com.bazaarlink.app.repository.BazaarLinkRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Sending : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

class ChatViewModel(
    private val repository: BazaarLinkRepository = ServiceLocator.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _activeChat = MutableStateFlow<Chat?>(null)
    val activeChat: StateFlow<Chat?> = _activeChat.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private var chatsJob: Job? = null
    private var messagesJob: Job? = null
    private var chatJob: Job? = null

    /** Load all chats for the current user. Call once after login. */
    fun loadUserChats(userId: String) {
        chatsJob?.cancel()
        chatsJob = viewModelScope.launch {
            repository.getUserChats(userId).collect { _chats.value = it }
        }
    }

    /** Open a specific chat and start streaming its messages. */
    fun openChat(chatId: String) {
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            repository.getChat(chatId).collect { _activeChat.value = it }
        }
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessages(chatId).collect { _messages.value = it }
        }
    }

    fun closeChat() {
        chatJob?.cancel()
        messagesJob?.cancel()
        _activeChat.value = null
        _messages.value = emptyList()
    }

    /** Create a chat when a quote is accepted. Returns chatId. */
    fun createChat(
        requestId: String,
        buyerId: String,
        vendorId: String,
        buyerDisplayName: String,
        vendorDisplayName: String,
        onCreated: (chatId: String) -> Unit
    ) {
        viewModelScope.launch {
            repository.createChat(requestId, buyerId, vendorId, buyerDisplayName, vendorDisplayName)
                .onSuccess { chatId -> onCreated(chatId) }
                .onFailure { _uiState.value = ChatUiState.Error(it.message ?: "Failed to create chat") }
        }
    }

    fun sendText(chatId: String, senderId: String, text: String) {
        if (text.isBlank()) return
        _uiState.value = ChatUiState.Sending
        viewModelScope.launch {
            repository.sendTextMessage(chatId, senderId, text)
                .onSuccess { _uiState.value = ChatUiState.Idle }
                .onFailure { _uiState.value = ChatUiState.Error(it.message ?: "Send failed") }
        }
    }

    fun sendVoice(chatId: String, senderId: String, localFileUri: Uri, durationSecs: Int) {
        _uiState.value = ChatUiState.Sending
        viewModelScope.launch {
            repository.sendVoiceMessage(chatId, senderId, localFileUri, durationSecs)
                .onSuccess { _uiState.value = ChatUiState.Idle }
                .onFailure { _uiState.value = ChatUiState.Error(it.message ?: "Voice send failed") }
        }
    }

    fun sendImage(chatId: String, senderId: String, imageUri: Uri) {
        _uiState.value = ChatUiState.Sending
        viewModelScope.launch {
            repository.sendImageMessage(chatId, senderId, imageUri)
                .onSuccess { _uiState.value = ChatUiState.Idle }
                .onFailure { _uiState.value = ChatUiState.Error(it.message ?: "Image send failed") }
        }
    }

    fun updateNickname(chatId: String, currentUserId: String, buyerId: String, nickname: String) {
        viewModelScope.launch {
            repository.updateNickname(chatId, currentUserId, buyerId, nickname)
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatsJob?.cancel()
        messagesJob?.cancel()
        chatJob?.cancel()
    }
}
