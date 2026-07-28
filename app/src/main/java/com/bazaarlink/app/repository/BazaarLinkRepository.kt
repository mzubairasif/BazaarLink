package com.bazaarlink.app.repository

import android.net.Uri
import com.bazaarlink.app.models.Chat
import com.bazaarlink.app.models.Message
import com.bazaarlink.app.models.Quote
import com.bazaarlink.app.models.Request
import com.bazaarlink.app.models.TagModel
import com.bazaarlink.app.models.User
import kotlinx.coroutines.flow.Flow

interface BazaarLinkRepository {
    // User profile operations
    suspend fun getUserProfile(userId: String): Result<User?>
    suspend fun saveUserProfile(user: User): Result<Unit>

    // Local-first user profile (no network, instant)
    fun saveUserProfileLocally(user: User)
    suspend fun syncUserProfileToCloud(user: User)

    // Request operations
    fun getActiveRequests(category: String = "mobile phones accessories"): Flow<List<Request>>
    suspend fun broadcastRequest(request: Request): Result<String>

    // Quote operations
    fun getQuotesForRequest(requestId: String): Flow<List<Quote>>
    suspend fun submitQuote(quote: Quote): Result<String>

    // Atomic transaction logic: Accept quote, update request status, deduct vendor connect
    suspend fun acceptQuote(requestId: String, quoteId: String, vendorId: String): Result<Unit>

    // Tag operations (AI-ready: populated by AI later, seeded with dummies for now)
    fun getSuggestedTags(): Flow<List<TagModel>>
    suspend fun seedTagsIfEmpty()

    /** Upload a local file to Firebase Storage and return its download URL. */
    suspend fun uploadImage(uri: Uri, storagePath: String): Result<String>

    // ── Chat operations ──────────────────────────────────────────────────────
    /** Create a new chat document when a quote is accepted. Returns the chatId. */
    suspend fun createChat(
        requestId: String,
        buyerId: String,
        vendorId: String,
        buyerDisplayName: String,
        vendorDisplayName: String
    ): Result<String>

    /** Get a single chat by its ID. */
    fun getChat(chatId: String): Flow<Chat?>

    /** Get all chats for a given user (buyer or vendor). */
    fun getUserChats(userId: String): Flow<List<Chat>>

    /** Send a text message to a chat. */
    suspend fun sendTextMessage(chatId: String, senderId: String, text: String): Result<Unit>

    /** Upload a voice recording to Firebase Storage and send the message. */
    suspend fun sendVoiceMessage(
        chatId: String,
        senderId: String,
        localFileUri: Uri,
        durationSecs: Int
    ): Result<Unit>

    /** Upload an image to Firebase Storage and send the message. */
    suspend fun sendImageMessage(
        chatId: String,
        senderId: String,
        imageUri: Uri
    ): Result<Unit>

    /** Stream all messages for a chat in real-time, ordered by timestamp. */
    fun getMessages(chatId: String): Flow<List<Message>>

    /** Update what one party calls the other in their chat. */
    suspend fun updateNickname(
        chatId: String,
        currentUserId: String,
        buyerId: String,
        nickname: String
    ): Result<Unit>
}


