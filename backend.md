# BACKEND.md — BazaarLink Firebase & Architecture Specification

## 1. System Overview & Real-Time Data Flow
BazaarLink utilizes a serverless, event-driven architecture relying on Firebase Firestore and Firebase Storage. The core loop operates entirely on reactive data streams (Kotlin `Flow`), eliminating the need for manual UI polling.

**The Reverse-Marketplace Loop:**
1. **Buyer Query**: Buyer creates a request in `/requests` (with optional reference photos, tags, or voice notes).
2. **Vendor Discovery**: Vendor's active `callbackFlow` listener detects new `/requests` matching their market zone & tags.
3. **Vendor Bid**: Vendor submits a live bid to `/quotes`.
4. **Buyer Evaluation**: Buyer listens to `/quotes` matching their `requestId`.
5. **Atomic Deal Acceptance & E-Receipt**: Buyer accepts a quote ➔ Triggers a Firestore Atomic Transaction:
   - Updates `request` status to `ACCEPTED` and stores `acceptedQuoteId` & `acceptedVendorId`.
   - Updates `quote` status to `ACCEPTED`.
   - Deducts 1 Connect from Vendor's `vendorProfile.connectsBalance`.
   - Creates a `/chats` session pre-populated with a **Matching 3-Digit E-Receipt Claim Code** (e.g., `849`).
6. **Chat & Voice Notes**: Real-time buyer-seller messaging via `/chats/{chatId}/messages` with swipe-to-reply and press-and-hold voice notes.
7. **Verified Vendor Review System**: Upon deal acceptance, buyer can rate & review vendor in `/reviews`, recalculating vendor average rating with a **3-review threshold cap** and **anti-self-rating protection**.

---

## 2. Firestore Database Schema (Denormalized)

*Architecture Note: Data is aggressively denormalized in NoSQL. Non-volatile attributes (like shop names and item query titles) are duplicated across collections to ensure instant single-read rendering without client-side joins.*

---

### Collection: `users`
**Path:** `/users/{userId}`

```json
{
  "userId": "string (Matches Firebase Auth UID)",
  "role": "string (BUYER | VENDOR)",
  "registeredRoles": ["array of strings (e.g., ['BUYER', 'VENDOR'])"],
  "displayName": "string (e.g., 'Al-Rehman Electronics')",
  "email": "string",
  "phoneNumber": "string (e.g., '+923001234567')",
  "cnic": "string (e.g., '42101-1234567-1')",
  "createdAt": "timestamp",
  
  // Vendor-Specific Fields (Null for BUYERS)
  "vendorProfile": {
    "shopName": "string (e.g., 'Star Mobile Shop #42')",
    "marketZone": "string (e.g., 'Star City Mall, Saddar')",
    "categories": ["array of strings (e.g., ['Mobile Parts', 'Accessories'])"],
    "location": {
      "latitude": "number",
      "longitude": "number",
      "marketName": "string (e.g., 'Saddar Market')"
    },
    "connectsBalance": "number (Default MVP initialization: 50)",
    "rating": "number (Weighted average rating, default: 5.0)",
    "totalRatings": "number (Total completed ratings count, default: 0)"
  }
}
```

---

### Collection: `requests`
**Path:** `/requests/{requestId}`

```json
{
  "requestId": "string (Auto-generated UUID)",
  "buyerId": "string (References users.userId)",
  "rawQuery": "string (e.g., 'Need original iPhone 13 Pro display panel')",
  "voiceNoteUrl": "string (Nullable - Firebase Storage URL)",
  "imageUrls": ["array of strings (Firebase Storage URLs)"],
  "category": "string (e.g., 'Mobile Parts')",
  "aiTags": ["array of strings (e.g., ['iPhone', 'Screen', 'OLED', 'Display'])"],
  "location": {
    "latitude": "number",
    "longitude": "number",
    "marketName": "string (e.g., 'Star City Mall')"
  },
  "status": "string (BROADCASTING | ACCEPTED | EXPIRED)",
  "acceptedQuoteId": "string (Nullable - Populated upon acceptance)",
  "acceptedVendorId": "string (Nullable)",
  "createdAt": "timestamp",
  "expiresAt": "timestamp (Default: createdAt + 30 minutes)"
}
```

*Required Firestore Index:* Composite index on `buyerId` (Ascending) + `createdAt` (Descending), and `status` (Ascending) + `createdAt` (Descending).

---

### Collection: `quotes`
**Path:** `/quotes/{quoteId}`

```json
{
  "quoteId": "string (Auto-generated UUID)",
  "requestId": "string (References requests.requestId)",
  "vendorId": "string (References users.userId)",
  "vendorShopName": "string (Denormalized from users)",
  "vendorRating": "number (Denormalized)",
  "vendorLocation": {
    "latitude": "number",
    "longitude": "number",
    "marketName": "string"
  },
  "offeredPricePKR": "number",
  "note": "string (e.g., 'Original pull out condition, 3 days warranty')",
  "imageUrls": ["array of strings (Firebase Storage URLs)"],
  "status": "string (PENDING | ACCEPTED | REJECTED)",
  "createdAt": "timestamp"
}
```

*Required Firestore Index:* Composite index on `requestId` (Ascending) + `createdAt` (Ascending).

---

### Collection: `chats`
**Path:** `/chats/{chatId}`

```json
{
  "chatId": "string (Formatted as {requestId}_{vendorId} or UUID)",
  "requestId": "string (References requests.requestId)",
  "buyerId": "string (References users.userId)",
  "vendorId": "string (References users.userId)",
  "buyerDisplayName": "string",
  "vendorDisplayName": "string",
  "buyerNicknameForVendor": "string (Custom participant nickname)",
  "vendorNicknameForBuyer": "string (Custom participant nickname)",
  "itemQuery": "string (Denormalized request rawQuery)",
  "offeredPricePKR": "number (Agreed deal price)",
  "eReceiptClaimCode": "string (3-digit claim code e.g., '849')",
  "lastMessage": "string (Snippet of latest message)",
  "lastMessageTime": "number (Epoch milliseconds)",
  "createdAt": "timestamp"
}
```

---

### Collection: `messages` (Subcollection of `chats`)
**Path:** `/chats/{chatId}/messages/{messageId}`

```json
{
  "messageId": "string (Auto-generated UUID)",
  "chatId": "string (References chats.chatId)",
  "senderId": "string (References users.userId)",
  "senderRole": "string (BUYER | VENDOR)",
  "content": "string (Text message or Firebase Storage audio URL)",
  "type": "string (TEXT | VOICE)",
  "timestamp": "number (Epoch milliseconds)",
  "quotedMessageId": "string (Nullable - For swipe-to-reply)",
  "quotedSenderName": "string (Nullable)",
  "quotedContent": "string (Nullable)",
  "quotedMessageType": "string (Nullable)",
  "createdAt": "timestamp"
}
```

*Required Firestore Index:* Composite index on `chatId` (Ascending) + `createdAt` (Ascending).

---

### Collection: `reviews`
**Path:** `/reviews/{reviewId}`

```json
{
  "reviewId": "string (Auto-generated UUID)",
  "vendorId": "string (References users.userId)",
  "buyerId": "string (References users.userId)",
  "buyerDisplayName": "string",
  "requestId": "string (References requests.requestId)",
  "rating": "number (1.0 to 5.0 stars)",
  "comment": "string (Written customer feedback)",
  "createdAt": "timestamp"
}
```

*Required Firestore Index:* Composite index on `vendorId` (Ascending) + `createdAt` (Descending).

---

### Collection: `tags`
**Path:** `/tags/{tagId}`

```json
{
  "id": "string",
  "label": "string (e.g., 'OLED', 'Screen', 'Battery', 'Charger')",
  "category": "string (e.g., 'Mobile Parts')"
}
```

---

## 3. Business Logic & Algorithms

### A. Matching 3-Digit E-Receipt Claim Code Algorithm
When a buyer accepts a quote, a 3-digit E-Receipt claim code is deterministically generated:
```kotlin
val rawHash = Math.abs((requestId + vendorId).hashCode())
val claimCode = String.format("%03d", rawHash % 1000)
```
This claim code is stored in the `/chats` document and rendered in the `EReceiptDialog` for instant counter-claim verification at Saddar shops.

### B. Vendor Review & Rating Engine
1. **Anti-Self-Rating Guard Rule**:
   - `if (buyerId == vendorId)` ➔ Submission is strictly blocked on repository (`submitReview`) and UI levels (`ReviewDialog`).
2. **3-Review Threshold Cap Rule**:
   - **$\le 3$ Reviews**: The system suppresses numerical rating scores and displays `"🆕 New Saddar Merchant"` badge. Review cards list is left blank until 3 reviews are gathered.
   - **$3+$ Reviews**: The weighted average rating is dynamically calculated and updated in `/users/{vendorId}.vendorProfile`:
     $$\text{New Average} = \frac{(\text{oldRating} \times \text{totalRatings}) + \text{newRating}}{\text{totalRatings} + 1}$$
3. **Single Review Scoping**:
   - Scoped strictly to 1 review per accepted request (`requestId` + `buyerId` uniqueness).

### C. 5-Day Buyer Request History Filter
`getBuyerRequests(buyerId, days = 5)` queries `/requests` where `buyerId == uid` and `createdAt >= (currentTime - 5 days)`, allowing buyers to track live bids and active deals across recent requests.

---

## 4. MVP Logic: The "Connects" Wallet & Atomic Writes

When a Buyer accepts a quote, the system executes an atomic transaction:

```kotlin
suspend fun acceptQuote(requestId: String, quoteId: String, vendorId: String): Result<Unit> {
    return try {
        firestore.runTransaction { transaction ->
            val requestRef = firestore.collection("requests").document(requestId)
            val quoteRef = firestore.collection("quotes").document(quoteId)
            val vendorRef = firestore.collection("users").document(vendorId)

            // 1. Update Request status to ACCEPTED
            transaction.update(requestRef, mapOf(
                "status" to "ACCEPTED",
                "acceptedQuoteId" to quoteId,
                "acceptedVendorId" to vendorId
            ))

            // 2. Update Quote status to ACCEPTED
            transaction.update(quoteRef, "status", "ACCEPTED")

            // 3. Deduct 1 Connect from Vendor balance
            transaction.update(vendorRef, "vendorProfile.connectsBalance", FieldValue.increment(-1))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## 5. Security Rules (`firestore.rules`)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    function isAuthenticated() { return request.auth != null; }
    
    match /users/{userId} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated() && request.auth.uid == userId;
    }

    match /requests/{requestId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated(); 
      allow update: if isAuthenticated();
    }

    match /quotes/{quoteId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated();
      allow update: if isAuthenticated(); 
    }

    match /chats/{chatId} {
      allow read: if isAuthenticated();
      allow create, update: if isAuthenticated();
      
      match /messages/{messageId} {
        allow read, create: if isAuthenticated();
      }
    }

    match /reviews/{reviewId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated() && request.resource.data.buyerId != request.resource.data.vendorId;
    }

    match /tags/{tagId} {
      allow read: if isAuthenticated();
    }
  }
}
```
