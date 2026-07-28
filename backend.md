# BACKEND_SPEC.md — BazaarLink Firebase & Architecture Specification

## 1. System Overview & Real-Time Data Flow
BazaarLink utilizes a serverless, event-driven architecture relying on Firebase Firestore. The core loop operates entirely on reactive data streams (Kotlin `Flow`), eliminating the need for manual UI polling.

**The Reverse-Marketplace Loop:**
1. Buyer writes to `requests`.
2. Vendor's active `callbackFlow` listener detects new `requests` matching their tags.
3. Vendor writes to `quotes`.
4. Buyer's `callbackFlow` listener detects new `quotes` matching their `requestId`.
5. Buyer accepts a quote -> Triggers a Firestore Atomic Batch Write (updates `request` status + deducts Vendor's `connectsBalance`).

---

## 2. Firestore Database Schema (Denormalized)

*Architecture Note: Data is aggressively denormalized. In NoSQL, read operations and client-side joins are expensive. We duplicate non-volatile data (like shop names) into the `quotes` collection so the UI can render instantly from a single document read.*

### Collection: `users`
**Path:** `/users/{userId}`
```json
{
  "userId": "string (Matches Firebase Auth UID)",
  "role": "string (BUYER | VENDOR)",
  "displayName": "string (e.g., 'Al-Rehman Electronics')",
  "phoneNumber": "string (e.g., '+923001234567')",
  "createdAt": "timestamp",
  
  // Vendor-Specific Fields (Null for BUYERS)
  "vendorProfile": {
    "shopName": "string (e.g., 'Star Mobile Shop #42')",
    "marketZone": "string (e.g., 'Star City Mall, Saddar')",
    "categories": ["array of strings (e.g., ['Mobile Parts', 'Accessories'])"],
    "location": {
      "latitude": "number",
      "longitude": "number"
    },
    "connectsBalance": "number (Default MVP initialization: 50)",
    "rating": "number (Default: 5.0)",
    "totalRatings": "number (Default: 0)"
  }
}

```

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

*Required Firestore Index:* Composite index on `status` (Ascending) + `category` (Ascending) + `createdAt` (Descending).

### Collection: `quotes`

**Path:** `/quotes/{quoteId}`

```json
{
  "quoteId": "string (Auto-generated UUID)",
  "requestId": "string (References requests.requestId)",
  "vendorId": "string (References users.userId)",
  "vendorShopName": "string (Denormalized - copied from users to avoid secondary reads)",
  "vendorRating": "number (Denormalized)",
  "vendorLocation": {
    "latitude": "number",
    "longitude": "number"
  },
  "offeredPricePKR": "number",
  "note": "string (e.g., 'Original pull out condition, 3 days warranty')",
  "status": "string (PENDING | ACCEPTED | REJECTED)",
  "createdAt": "timestamp"
}

```

*Required Firestore Index:* Composite index on `requestId` (Ascending) + `createdAt` (Ascending).

---

## 3. MVP Logic: The "Connects" Wallet & Atomic Writes

For the 12-day MVP, we are **faking** the Stripe/Easypaisa payment gateway.
When a Buyer accepts a quote, the system must perform two actions simultaneously. If one fails, both must fail.

**The Firestore Batch Write Strategy (To be implemented in Kotlin):**

```kotlin
// Pseudocode for the Antigravity Agent
fun acceptQuote(requestId: String, quoteId: String, vendorId: String) {
    db.runTransaction { transaction ->
        val requestRef = db.collection("requests").document(requestId)
        val quoteRef = db.collection("quotes").document(quoteId)
        val vendorRef = db.collection("users").document(vendorId)

        // 1. Mark request as accepted
        transaction.update(requestRef, "status", "ACCEPTED", "acceptedQuoteId", quoteId, "acceptedVendorId", vendorId)
        
        // 2. Mark quote as accepted
        transaction.update(quoteRef, "status", "ACCEPTED")
        
        // 3. Deduct 1 Connect from Vendor
        transaction.update(vendorRef, "vendorProfile.connectsBalance", FieldValue.increment(-1))
    }
}

```

---

## 4. Kotlin Integration Strategy (For the AI Agents)

1. **Data Models:** Create strictly typed `data class` files for `User`, `Request`, and `Quote`. Use `@DocumentId` and `@PropertyName` annotations to ensure robust mapping from Firestore JSON to Kotlin objects.
2. **Repository Layer:** Abstract Firestore behind an interface.
3. **Reactive Streams:** Use Kotlin Coroutines `callbackFlow` to wrap Firestore `addSnapshotListener`. This ensures the stream is automatically closed when the Jetpack Compose screen leaves the composition, preventing memory leaks.
4. **State Hoisting:** ViewModels must collect the `Flow` from the Repository and expose it to the UI as a `StateFlow`. Compose will observe this `StateFlow`.

---

## 5. Security Rules (`firestore.rules`)

To prevent the app from failing Firebase's default 30-day security lockdown during the presentation, implement these baseline rules:

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
      // Only buyers can create requests
      allow create: if isAuthenticated(); 
      // Buyers can update their own request (to accept a quote)
      allow update: if isAuthenticated() && resource.data.buyerId == request.auth.uid;
    }

    match /quotes/{quoteId} {
      allow read: if isAuthenticated();
      // Vendors can create quotes
      allow create: if isAuthenticated();
      // Buyers can update a quote's status to ACCEPTED
      allow update: if isAuthenticated(); 
    }
  }
}

```
