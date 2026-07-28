package com.bazaarlink.app.repository

import android.net.Uri
import android.util.Log
import com.bazaarlink.app.models.Chat
import com.bazaarlink.app.models.Message
import com.bazaarlink.app.models.MessageType
import com.bazaarlink.app.models.Quote
import com.bazaarlink.app.models.Request
import com.bazaarlink.app.models.RequestStatus
import com.bazaarlink.app.models.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import java.util.UUID

class BazaarLinkRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : BazaarLinkRepository {

    private val localUsers = mutableMapOf<String, User>()
    private val localRequests = MutableStateFlow<List<Request>>(emptyList())
    private val localQuotes = MutableStateFlow<List<Quote>>(emptyList())

    override suspend fun getUserProfile(userId: String): Result<User?> {
        localUsers[userId]?.let { return Result.success(it) }

        return try {
            val snapshot = withTimeoutOrNull(5000L) {
                firestore.collection("users").document(userId).get().await()
            }
            val user = snapshot?.toObject(User::class.java)
            if (user != null) localUsers[user.userId] = user
            Result.success(user)
        } catch (e: Exception) {
            Log.w("BazaarLink", "getUserProfile error: ${e.message}")
            Result.success(localUsers[userId])
        }
    }

    override suspend fun saveUserProfile(user: User): Result<Unit> {
        localUsers[user.userId] = user
        return try {
            withTimeoutOrNull(5000L) {
                firestore.collection("users").document(user.userId).set(user).await()
            }
            Log.d("BazaarLink", "saveUserProfile written for ${user.userId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BazaarLink", "saveUserProfile error: ${e.message}", e)
            Result.success(Unit)
        }
    }

    override fun saveUserProfileLocally(user: User) {
        localUsers[user.userId] = user
        Log.d("BazaarLink", "saveUserProfileLocally cached user ${user.userId} role=${user.role}")
    }

    override suspend fun syncUserProfileToCloud(user: User) {
        try {
            withTimeoutOrNull(8000L) {
                firestore.collection("users").document(user.userId).set(user).await()
            }
            Log.d("BazaarLink", "syncUserProfileToCloud SUCCESS for ${user.userId}")
        } catch (e: Exception) {
            Log.e("BazaarLink", "syncUserProfileToCloud FAILED: ${e.message}", e)
        }
    }

    override fun getActiveRequests(category: String): Flow<List<Request>> = callbackFlow {
        Log.d("BazaarLink", "getActiveRequests: Starting listener on collection 'requests'")
        var isClosed = false
        val subscription = try {
            firestore.collection("requests")
                .whereEqualTo("status", "BROADCASTING")
                .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BazaarLink", "getActiveRequests Snapshot ERROR: ${error.message}", error)
                    if (!isClosed) {
                        trySend(localRequests.value)
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    Log.d("BazaarLink", "getActiveRequests snapshot received! Doc count: ${snapshot.documents.size}")
                    val parsedList = mutableListOf<Request>()
                    for (doc in snapshot.documents) {
                        try {
                            val req = doc.toObject(Request::class.java)?.copy(requestId = doc.id)
                            if (req != null) {
                                Log.d("BazaarLink", "Parsed request: id=${req.requestId}, rawQuery='${req.rawQuery}', status='${req.status}'")
                                parsedList.add(req)
                            } else {
                                Log.w("BazaarLink", "doc.toObject returned null for doc ${doc.id}")
                            }
                        } catch (e: Exception) {
                            Log.e("BazaarLink", "Failed to parse doc ${doc.id}: ${e.message}", e)
                        }
                    }

                    val merged = (parsedList + localRequests.value)
                        .distinctBy { it.requestId }
                        .sortedByDescending { it.createdAt }

                    Log.d("BazaarLink", "getActiveRequests emitting ${merged.size} requests to Vendor UI")
                    trySend(merged)
                }
            }
        } catch (e: Exception) {
            Log.e("BazaarLink", "getActiveRequests listener exception: ${e.message}", e)
            null
        }

        if (subscription == null) {
            Log.w("BazaarLink", "getActiveRequests: Listener failed, falling back to local cache")
            trySend(localRequests.value)
        }

        awaitClose {
            isClosed = true
            subscription?.remove()
            Log.d("BazaarLink", "getActiveRequests: Listener removed")
        }
    }

    override suspend fun broadcastRequest(request: Request): Result<String> {
        val reqId = request.requestId.ifEmpty { UUID.randomUUID().toString() }
        val finalRequest = request.copy(requestId = reqId)

        localRequests.value = listOf(finalRequest) + localRequests.value
        Log.d("BazaarLink", "broadcastRequest: Cached locally id=$reqId query='${finalRequest.rawQuery}'")

        try {
            val data = hashMapOf(
                "requestId" to reqId,
                "buyerId" to finalRequest.buyerId,
                "rawQuery" to finalRequest.rawQuery,
                "voiceNoteUrl" to finalRequest.voiceNoteUrl,
                "imageUrls" to finalRequest.imageUrls,
                "category" to finalRequest.category,
                "aiTags" to finalRequest.aiTags,
                "location" to hashMapOf(
                    "latitude" to finalRequest.location.latitude,
                    "longitude" to finalRequest.location.longitude,
                    "marketName" to finalRequest.location.marketName
                ),
                "status" to finalRequest.status,
                "acceptedQuoteId" to finalRequest.acceptedQuoteId,
                "acceptedVendorId" to finalRequest.acceptedVendorId,
                "createdAt" to finalRequest.createdAt,
                "expiresAt" to finalRequest.expiresAt
            )

            val result = withTimeoutOrNull(8000L) {
                firestore.collection("requests").document(reqId).set(data).await()
                true
            }

            if (result == true) {
                Log.d("BazaarLink", "broadcastRequest: Cloud write SUCCESS for $reqId")
            } else {
                Log.w("BazaarLink", "broadcastRequest: Cloud write TIMED OUT for $reqId")
            }
        } catch (e: Exception) {
            Log.e("BazaarLink", "broadcastRequest: Cloud write FAILED for $reqId: ${e.message}", e)
        }

        return Result.success(reqId)
    }

    override fun getQuotesForRequest(requestId: String): Flow<List<Quote>> = callbackFlow {
        Log.d("BazaarLink", "getQuotesForRequest: Starting listener for requestId=$requestId")
        var isClosed = false
        val subscription = try {
            firestore.collection("quotes")
                .whereEqualTo("requestId", requestId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("BazaarLink", "getQuotesForRequest snapshot ERROR: ${error.message}", error)
                        if (!isClosed) {
                            trySend(localQuotes.value.filter { it.requestId == requestId })
                        }
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val cloudQuotes = snapshot.toObjects(Quote::class.java)
                        Log.d("BazaarLink", "getQuotesForRequest received ${cloudQuotes.size} quotes")
                        val merged = (cloudQuotes + localQuotes.value.filter { it.requestId == requestId })
                            .distinctBy { it.quoteId }
                            .sortedBy { it.createdAt }
                        trySend(merged)
                    }
                }
        } catch (e: Exception) {
            Log.e("BazaarLink", "getQuotesForRequest listener error: ${e.message}", e)
            null
        }

        if (subscription == null) {
            trySend(localQuotes.value.filter { it.requestId == requestId })
        }

        awaitClose {
            isClosed = true
            subscription?.remove()
        }
    }

    override suspend fun submitQuote(quote: Quote): Result<String> {
        val qId = quote.quoteId.ifEmpty { UUID.randomUUID().toString() }
        val finalQuote = quote.copy(quoteId = qId)

        localQuotes.value = localQuotes.value + finalQuote
        Log.d("BazaarLink", "submitQuote: Cached locally id=$qId for request=${finalQuote.requestId}")

        try {
            val data = hashMapOf(
                "quoteId" to qId,
                "requestId" to finalQuote.requestId,
                "vendorId" to finalQuote.vendorId,
                "vendorShopName" to finalQuote.vendorShopName,
                "vendorRating" to finalQuote.vendorRating,
                "vendorLocation" to hashMapOf(
                    "latitude" to finalQuote.vendorLocation.latitude,
                    "longitude" to finalQuote.vendorLocation.longitude,
                    "marketName" to finalQuote.vendorLocation.marketName
                ),
                "offeredPricePKR" to finalQuote.offeredPricePKR,
                "note" to finalQuote.note,
                "imageUrls" to finalQuote.imageUrls,
                "status" to finalQuote.status,
                "createdAt" to finalQuote.createdAt
            )

            val result = withTimeoutOrNull(8000L) {
                firestore.collection("quotes").document(qId).set(data).await()
                true
            }

            if (result == true) {
                Log.d("BazaarLink", "submitQuote: Cloud write SUCCESS for $qId")
            } else {
                Log.w("BazaarLink", "submitQuote: Cloud write TIMED OUT for $qId")
            }
        } catch (e: Exception) {
            Log.e("BazaarLink", "submitQuote: Cloud write FAILED for $qId: ${e.message}", e)
        }

        return Result.success(qId)
    }

    override suspend fun acceptQuote(requestId: String, quoteId: String, vendorId: String): Result<Unit> {
        localRequests.value = localRequests.value.map {
            if (it.requestId == requestId) it.copy(status = RequestStatus.ACCEPTED.name, acceptedQuoteId = quoteId, acceptedVendorId = vendorId)
            else it
        }
        localQuotes.value = localQuotes.value.map {
            if (it.quoteId == quoteId) it.copy(status = "ACCEPTED")
            else it
        }
        val currentVendor = localUsers[vendorId]
        if (currentVendor?.vendorProfile != null) {
            val updatedProfile = currentVendor.vendorProfile.copy(
                connectsBalance = (currentVendor.vendorProfile.connectsBalance - 1).coerceAtLeast(0)
            )
            localUsers[vendorId] = currentVendor.copy(vendorProfile = updatedProfile)
        }

        return runCatching {
            firestore.runTransaction { transaction ->
                val requestRef = firestore.collection("requests").document(requestId)
                val quoteRef = firestore.collection("quotes").document(quoteId)
                val vendorRef = firestore.collection("users").document(vendorId)

                val requestSnap = transaction.get(requestRef)
                val quoteSnap = transaction.get(quoteRef)
                val vendorSnap = transaction.get(vendorRef)

                if (requestSnap.exists() && requestSnap.getString("status") != RequestStatus.BROADCASTING.name) {
                    throw IllegalStateException("Request is no longer active")
                }
                if (quoteSnap.exists() && quoteSnap.getString("status") != "PENDING") {
                    throw IllegalStateException("Quote is no longer pending")
                }
                if (vendorSnap.exists()) {
                    val connects = vendorSnap.getLong("vendorProfile.connectsBalance") ?: 0L
                    if (connects < 1) throw IllegalStateException("Vendor out of connects")
                }

                transaction.update(
                    requestRef,
                    mapOf(
                        "status" to RequestStatus.ACCEPTED.name,
                        "acceptedQuoteId" to quoteId,
                        "acceptedVendorId" to vendorId
                    )
                )
                transaction.update(quoteRef, "status", "ACCEPTED")
                transaction.update(vendorRef, "vendorProfile.connectsBalance", FieldValue.increment(-1))
            }.await()
            Log.d("BazaarLink", "acceptQuote transaction SUCCESS for request=$requestId quote=$quoteId")
            Unit
        }.onFailure { e ->
            Log.e("BazaarLink", "acceptQuote transaction FAILED: ${e.message}", e)
        }
    }

    // ─── Tag Operations ───────────────────────────────────────────────────────

    override fun getSuggestedTags(): Flow<List<com.bazaarlink.app.models.TagModel>> = callbackFlow {
        val subscription = firestore.collection("tags")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BazaarLink", "getSuggestedTags error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val tags = snapshot.documents.mapNotNull { doc ->
                        try {
                            com.bazaarlink.app.models.TagModel(
                                id = doc.id,
                                label = doc.getString("label") ?: "",
                                category = doc.getString("category") ?: "",
                                emoji = doc.getString("emoji") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedBy { it.category }
                    Log.d("BazaarLink", "getSuggestedTags: fetched ${tags.size} tags")
                    trySend(tags)
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Seeds the `tags` Firestore collection with pre-determined tags if it is empty.
     * These will be replaced or augmented by AI-generated tags in a future sprint.
     * Organised by category: mobile_parts, accessories, repair.
     */
    override suspend fun seedTagsIfEmpty() {
        try {
            val existing = withTimeoutOrNull(5000L) {
                firestore.collection("tags").limit(1).get().await()
            }
            if (existing != null && !existing.isEmpty) {
                Log.d("BazaarLink", "seedTagsIfEmpty: tags already seeded, skipping")
                return
            }

            val dummyTags = listOf(
                // Mobile Parts
                mapOf("label" to "OLED Screen", "category" to "mobile_parts", "emoji" to "📱"),
                mapOf("label" to "LCD Display", "category" to "mobile_parts", "emoji" to "🖥️"),
                mapOf("label" to "Battery", "category" to "mobile_parts", "emoji" to "🔋"),
                mapOf("label" to "Back Glass", "category" to "mobile_parts", "emoji" to "🪟"),
                mapOf("label" to "Charging Port", "category" to "mobile_parts", "emoji" to "🔌"),
                mapOf("label" to "Front Camera", "category" to "mobile_parts", "emoji" to "📸"),
                mapOf("label" to "Rear Camera", "category" to "mobile_parts", "emoji" to "📷"),
                mapOf("label" to "Motherboard", "category" to "mobile_parts", "emoji" to "🔧"),
                mapOf("label" to "Speaker", "category" to "mobile_parts", "emoji" to "🔊"),
                mapOf("label" to "Microphone", "category" to "mobile_parts", "emoji" to "🎙️"),
                mapOf("label" to "Earpiece", "category" to "mobile_parts", "emoji" to "👂"),
                mapOf("label" to "Fingerprint Sensor", "category" to "mobile_parts", "emoji" to "👆"),
                // Accessories
                mapOf("label" to "Phone Case", "category" to "accessories", "emoji" to "🛡️"),
                mapOf("label" to "Tempered Glass", "category" to "accessories", "emoji" to "🔲"),
                mapOf("label" to "Charger", "category" to "accessories", "emoji" to "⚡"),
                mapOf("label" to "USB Cable", "category" to "accessories", "emoji" to "🔗"),
                mapOf("label" to "Earphones", "category" to "accessories", "emoji" to "🎧"),
                mapOf("label" to "Power Bank", "category" to "accessories", "emoji" to "🔋"),
                mapOf("label" to "PopSocket", "category" to "accessories", "emoji" to "🔵"),
                mapOf("label" to "Car Mount", "category" to "accessories", "emoji" to "🚗"),
                mapOf("label" to "Wireless Charger", "category" to "accessories", "emoji" to "📡"),
                // Repair Services
                mapOf("label" to "Screen Repair", "category" to "repair", "emoji" to "🛠️"),
                mapOf("label" to "Water Damage", "category" to "repair", "emoji" to "💧"),
                mapOf("label" to "Battery Replacement", "category" to "repair", "emoji" to "🔋"),
                mapOf("label" to "Software Flash", "category" to "repair", "emoji" to "💾"),
                mapOf("label" to "Back Glass Repair", "category" to "repair", "emoji" to "🪟"),
                mapOf("label" to "Charging Fix", "category" to "repair", "emoji" to "⚡"),
                mapOf("label" to "Camera Repair", "category" to "repair", "emoji" to "📷"),
                mapOf("label" to "Unlock Service", "category" to "repair", "emoji" to "🔓"),
                mapOf("label" to "Network Fix", "category" to "repair", "emoji" to "📶")
            )

            val batch = firestore.batch()
            dummyTags.forEach { tag ->
                val ref = firestore.collection("tags").document()
                batch.set(ref, tag)
            }
            withTimeoutOrNull(8000L) { batch.commit().await() }
            Log.d("BazaarLink", "seedTagsIfEmpty: Seeded ${dummyTags.size} tags to Firestore")
        } catch (e: Exception) {
            Log.e("BazaarLink", "seedTagsIfEmpty failed: ${e.message}", e)
        }
    }

    // ─── Chat Operations ──────────────────────────────────────────────────────

    override suspend fun createChat(
        requestId: String,
        buyerId: String,
        vendorId: String,
        buyerDisplayName: String,
        vendorDisplayName: String
    ): Result<String> {
        val chatId = requestId // 1:1 mapping keeps things flat
        return try {
            val existing = withTimeoutOrNull(4000L) {
                firestore.collection("chats").document(chatId).get().await()
            }
            if (existing != null && existing.exists()) {
                Log.d("BazaarLink", "createChat: chat $chatId already exists")
                return Result.success(chatId)
            }

            // Fetch request, quote, and user details to populate E-Receipt
            val reqSnap = withTimeoutOrNull(3000L) { firestore.collection("requests").document(requestId).get().await() }
            val itemQuery = reqSnap?.getString("rawQuery") ?: ""
            val acceptedQuoteId = reqSnap?.getString("acceptedQuoteId") ?: ""
            
            var offeredPricePKR = 0.0
            var agreedNote = ""
            if (acceptedQuoteId.isNotBlank()) {
                val qSnap = withTimeoutOrNull(3000L) { firestore.collection("quotes").document(acceptedQuoteId).get().await() }
                offeredPricePKR = qSnap?.getDouble("offeredPricePKR") ?: 0.0
                agreedNote = qSnap?.getString("note") ?: ""
            }

            val buyerSnap = withTimeoutOrNull(3000L) { firestore.collection("users").document(buyerId).get().await() }
            val vendorSnap = withTimeoutOrNull(3000L) { firestore.collection("users").document(vendorId).get().await() }
            val buyerPhone = buyerSnap?.getString("phoneNumber") ?: ""
            val vendorPhone = vendorSnap?.getString("phoneNumber") ?: ""

            val randomClaimCode = (100..999).random().toString()

            val data = hashMapOf(
                "chatId" to chatId,
                "requestId" to requestId,
                "buyerId" to buyerId,
                "vendorId" to vendorId,
                "buyerDisplayName" to buyerDisplayName,
                "vendorDisplayName" to vendorDisplayName,
                "buyerNicknameForVendor" to "",
                "vendorNicknameForBuyer" to "",
                "claimCode" to randomClaimCode,
                "offeredPricePKR" to offeredPricePKR,
                "agreedNote" to agreedNote,
                "buyerPhone" to buyerPhone,
                "vendorPhone" to vendorPhone,
                "itemQuery" to itemQuery,
                "lastMessage" to "",
                "lastMessageAt" to Date(),
                "createdAt" to Date()
            )
            withTimeoutOrNull(8000L) {
                firestore.collection("chats").document(chatId).set(data).await()
            }
            Log.d("BazaarLink", "createChat: created chat $chatId with claimCode #$randomClaimCode")
            Result.success(chatId)
        } catch (e: Exception) {
            Log.e("BazaarLink", "createChat failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getChat(chatId: String): Flow<Chat?> = callbackFlow {
        val sub = firestore.collection("chats").document(chatId)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(null); return@addSnapshotListener }
                val chat = snap?.toObject(Chat::class.java)
                trySend(chat)
            }
        awaitClose { sub.remove() }
    }

    override fun getUserChats(userId: String): Flow<List<Chat>> = callbackFlow {
        // Listen for chats where user is buyer OR vendor (two queries merged)
        var buyerChats: List<Chat> = emptyList()
        var vendorChats: List<Chat> = emptyList()

        fun emit() { trySend((buyerChats + vendorChats).distinctBy { it.chatId }.sortedByDescending { it.lastMessageAt }) }

        val subBuyer = firestore.collection("chats")
            .whereEqualTo("buyerId", userId)
            .addSnapshotListener { snap, _ ->
                buyerChats = snap?.toObjects(Chat::class.java) ?: emptyList()
                emit()
            }
        val subVendor = firestore.collection("chats")
            .whereEqualTo("vendorId", userId)
            .addSnapshotListener { snap, _ ->
                vendorChats = snap?.toObjects(Chat::class.java) ?: emptyList()
                emit()
            }
        awaitClose { subBuyer.remove(); subVendor.remove() }
    }

    override suspend fun sendTextMessage(chatId: String, senderId: String, text: String): Result<Unit> {
        return sendMessage(chatId, senderId, mapOf(
            "type" to MessageType.TEXT,
            "text" to text,
            "voiceUrl" to "",
            "imageUrl" to "",
            "voiceDurationSecs" to 0
        ), preview = text)
    }

    override suspend fun sendVoiceMessage(
        chatId: String,
        senderId: String,
        localFileUri: Uri,
        durationSecs: Int
    ): Result<Unit> {
        return try {
            val msgId = UUID.randomUUID().toString()
            val ref = storage.reference.child("voice_messages/$chatId/$msgId.m4a")
            val uploadTask = withTimeoutOrNull(30000L) {
                ref.putFile(localFileUri).await()
                ref.downloadUrl.await().toString()
            } ?: return Result.failure(Exception("Voice upload timed out"))
            sendMessage(chatId, senderId, mapOf(
                "type" to MessageType.VOICE,
                "text" to "",
                "voiceUrl" to uploadTask,
                "imageUrl" to "",
                "voiceDurationSecs" to durationSecs
            ), preview = "🎙️ Voice message", forcedId = msgId)
        } catch (e: Exception) {
            Log.e("BazaarLink", "sendVoiceMessage failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun sendImageMessage(
        chatId: String,
        senderId: String,
        imageUri: Uri
    ): Result<Unit> {
        return try {
            val msgId = UUID.randomUUID().toString()
            val ref = storage.reference.child("chat_images/$chatId/$msgId.jpg")
            val downloadUrl = withTimeoutOrNull(30000L) {
                ref.putFile(imageUri).await()
                ref.downloadUrl.await().toString()
            } ?: return Result.failure(Exception("Image upload timed out"))
            sendMessage(chatId, senderId, mapOf(
                "type" to MessageType.IMAGE,
                "text" to "",
                "voiceUrl" to "",
                "imageUrl" to downloadUrl,
                "voiceDurationSecs" to 0
            ), preview = "📷 Photo", forcedId = msgId)
        } catch (e: Exception) {
            Log.e("BazaarLink", "sendImageMessage failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** Shared helper: writes message doc + updates chat preview */
    private suspend fun sendMessage(
        chatId: String,
        senderId: String,
        extraFields: Map<String, Any>,
        preview: String,
        forcedId: String? = null
    ): Result<Unit> {
        return try {
            val msgId = forcedId ?: UUID.randomUUID().toString()
            val now = Date()
            val timeMs = System.currentTimeMillis()
            val msgData = hashMapOf(
                "messageId" to msgId,
                "chatId" to chatId,
                "senderId" to senderId,
                "timestamp" to timeMs,
                "createdAt" to now
            ) + extraFields

            val batch = firestore.batch()
            batch.set(firestore.collection("messages").document(msgId), msgData)
            batch.update(
                firestore.collection("chats").document(chatId),
                mapOf("lastMessage" to preview, "lastMessageAt" to now)
            )
            withTimeoutOrNull(8000L) { batch.commit().await() }
            Log.d("BazaarLink", "sendMessage: sent $msgId to chat $chatId (ts=$timeMs)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BazaarLink", "sendMessage failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        // Messages are sorted strictly by millisecond timestamp in ascending order (oldest top, newest bottom)
        val sub = firestore.collection("messages")
            .whereEqualTo("chatId", chatId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("BazaarLink", "getMessages error: ${err.message}", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val msgs = (snap?.toObjects(Message::class.java) ?: emptyList())
                    .sortedBy { if (it.timestamp > 0L) it.timestamp else it.createdAt.time }
                trySend(msgs)
            }
        awaitClose { sub.remove() }
    }

    override suspend fun updateNickname(
        chatId: String,
        currentUserId: String,
        buyerId: String,
        nickname: String
    ): Result<Unit> {
        val field = if (currentUserId == buyerId) "buyerNicknameForVendor" else "vendorNicknameForBuyer"
        return try {
            withTimeoutOrNull(5000L) {
                firestore.collection("chats").document(chatId)
                    .update(field, nickname).await()
            }
            Log.d("BazaarLink", "updateNickname: set $field='$nickname' in chat $chatId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BazaarLink", "updateNickname failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun uploadImage(uri: Uri, storagePath: String): Result<String> {
        return try {
            val ref = storage.reference.child(storagePath)
            val downloadUrl = withTimeoutOrNull(30000L) {
                ref.putFile(uri).await()
                ref.downloadUrl.await().toString()
            } ?: return Result.failure(Exception("Upload timed out"))
            Log.d("BazaarLink", "uploadImage: uploaded to $storagePath -> $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("BazaarLink", "uploadImage failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}

