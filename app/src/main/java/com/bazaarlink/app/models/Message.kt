package com.bazaarlink.app.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/** Types of chat messages */
object MessageType {
    const val TEXT = "text"
    const val VOICE = "voice"
    const val IMAGE = "image"
}

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val type: String = MessageType.TEXT,
    // Firebase Storage download URLs (empty for text messages)
    val voiceUrl: String = "",
    val imageUrl: String = "",
    // Duration in seconds for voice messages (0 if not a voice message)
    val voiceDurationSecs: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    @ServerTimestamp
    val createdAt: Date? = null,
    val replyToMessageId: String = "",
    val replyToSenderName: String = "",
    val replyToTextPreview: String = "",
    @get:Exclude
    var isSending: Boolean = false
)



