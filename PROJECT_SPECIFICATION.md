# BazaarLink — End-to-End Technical & Architectural Specification

**Project Name:** BazaarLink  
**Platform:** Android (Kotlin Native)  
**Target Market:** Hyperlocal Reverse-Marketplace for Unorganized Dense Markets (Saddar, Karachi)  
**Competition Context:** ibex. iSprint Challenge (Round 2: The Blueprint — Karachi 2026)  
**Primary Authors:** Zaid, Ahsan & Engineering Team  

---

## 📋 Table of Contents
1. [Executive Summary & Problem Statement](#1-executive-summary--problem-statement)
2. [High-Level System Architecture](#2-high-level-system-architecture)
3. [Key Architectural Decisions](#3-key-architectural-decisions)
4. [Database Schema & Data Model (Firestore NoSQL)](#4-database-schema--data-model-firestore-nosql)
5. [Core User Journeys & Technical Flow](#5-core-user-journeys--technical-flow)
6. [Component Breakdown & File Mapping](#6-component-breakdown--file-mapping)
7. [Localization & Urdu Accessibility](#7-localization--urdu-accessibility)
8. [Self-Sustainability & Monetization Economy](#8-self-sustainability--monetization-economy)
9. [Scalability & Zonal Replication Plan](#9-scalability--zonal-replication-plan)
10. [Risk Analysis & Mitigation Matrix](#10-risk-analysis--mitigation-matrix)

---

## 1. Executive Summary & Problem Statement

### The Problem
Karachi’s wholesale and retail electronic markets (e.g., Saddar, Star City Mall, Regal, Cooperative Market) are chaotic, unorganized, and dense. Buyers face:
- **Physical Fatigue:** Walking through 100+ crowded shops to find a specific mobile part, accessory, or repair service.
- **Price Gouging:** Asymmetric information where different shopkeepers quote wildly different prices for identical items.
- **Inventory Disconnection:** Merchants have physical stock but zero digital presence.

### The Solution: BazaarLink
BazaarLink is a **hyperlocal reverse-marketplace app**. Instead of buyers searching through thousands of static catalog items:
1. **Buyer Broadcasts a Request:** Describes the item needed (via text, reference photo, or voice note).
2. **Vendors Submit Live Bids:** Nearby Saddar shopkeepers get notified and reply in real-time with prices, warranty terms, and photos of exact stock.
3. **Buyer Accepts & Claims:** Buyer selects the best bid, receives a digital **E-Receipt with a matching 3-digit claim code**, and opens a direct chat/phone channel to complete the deal at the shop counter.

---

## 2. High-Level System Architecture

BazaarLink follows a modern, reactive Android architecture built for performance, reliability, and instant real-time synchronization.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           UI LAYER (Declarative)                         │
│   Jetpack Compose + Material 3 Design System + Fullscreen Dialogs        │
└─────────────────────────────────────────────────────────────────────────┘
                                     │ (StateFlow / Flow)
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         VIEWMODEL / STATE LAYER                          │
│   AuthViewModel    ·    BuyerViewModel    ·    VendorViewModel          │
│                          ChatViewModel                                  │
└─────────────────────────────────────────────────────────────────────────┘
                                     │ (Coroutines / Result<T>)
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        DATA & REPOSITORY LAYER                          │
│   BazaarLinkRepositoryImpl   ·   UserSessionManager (SharedPreferences) │
└─────────────────────────────────────────────────────────────────────────┘
                                     │ (Firebase SDK & Offline Cache)
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       CLOUD BACKEND (Firebase)                          │
│   Cloud Firestore NoSQL    ·    Firebase Auth    ·    Firebase Storage  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Key Architectural Decisions

### 1. Jetpack Compose (100% Declarative UI)
- **Rationale:** Zero XML layout files. All UI components are declarative, allowing rapid iteration, dynamic theme styling, state hoisting, and instant previewing.

### 2. Basic MVVM + ServiceLocator (No Hilt/Dagger)
- **Rationale:** To eliminate heavy annotation processor overhead, slow build times, and runtime dependency reflection bugs, dependency injection is handled cleanly via `ServiceLocator.kt`. ViewModels consume repository interfaces directly.

### 3. Dual-Shell Navigation Architecture
- **Rationale:** The application is split into two primary shell containers:
  - `BuyerShell`: Home Request Form → Waiting Radar → Quote Feed → Chat List → Account.
  - `VendorShell`: Incoming Requests Feed → Submitted Bids → Chat List → Account.
- Controlled via a single `NavGraph.kt` host using `Jetpack Navigation Compose`.

### 4. Triple-Redundant Session Persistence
- **Rationale:** To guarantee that registered users **NEVER** get repeatedly asked to fill in their profile after logging in with Google:
  1. **Local Device Storage (`UserSessionManager`):** Instant 0ms load from Android `SharedPreferences`.
  2. **Direct Firestore Key (`users/email_key`):** Immediate document fetch by sanitized email key.
  3. **Case-Insensitive Firestore Email Query:** Normalizes all emails (`email.trim().lowercase()`) to eliminate Firestore's strict case-sensitivity mismatch bugs.

### 5. MediaRecorder & MediaPlayer Voice Notes Integration
- **Rationale:** Many Saddar shopkeepers prefer speaking over typing. Voice notes are recorded locally as compressed `.m4a` files using Android `MediaRecorder`, uploaded to Firebase Storage (`voice_messages/`), and streamed cross-device via `MediaPlayer`.

---

## 4. Database Schema & Data Model (Firestore NoSQL)

Firestore uses a flat NoSQL collection structure for maximum real-time query efficiency.

```
cloud-firestore/
├── users/                # User accounts & merchant profiles
├── requests/             # Broadcasted buyer queries
├── quotes/               # Live bids submitted by vendors
├── chats/                # 1:1 active deals & claim codes
└── messages/             # Real-time chat messages (text, voice, image)
```

### Collection Schemas

#### 1. `users/{userId}`
```kotlin
data class User(
    val userId: String = "",              // Document ID (Firebase UID or Email Key)
    val email: String = "",               // Normalized Google Email
    val role: String = "BUYER",           // Active Role ("BUYER" or "VENDOR")
    val registeredRoles: List<String> = listOf("BUYER"), // Roles registered by user
    val displayName: String = "",         // Full Name
    val phoneNumber: String = "",         // Contact Number
    val cnic: String = "",                // Identification CNIC
    val createdAt: Date = Date(),
    val vendorProfile: VendorProfile? = null
)

data class VendorProfile(
    val shopName: String = "",            // Merchant Shop Name
    val marketZone: String = "",          // e.g. "Star City Mall, Saddar"
    val categories: List<String> = emptyList(),
    val connectsBalance: Int = 50,         // Monetization credit balance
    val rating: Double = 5.0,
    val totalRatings: Int = 0
)
```

#### 2. `requests/{requestId}`
```kotlin
data class Request(
    val requestId: String = "",           // Unique UUID
    val buyerId: String = "",             // ID of broadcasting buyer
    val rawQuery: String = "",            // Item description / text query
    val category: String = "mobile parts",
    val status: String = "BROADCASTING",  // "BROADCASTING", "ACCEPTED", "EXPIRED"
    val location: GeoLocation = GeoLocation(marketName = "Star City Mall, Saddar"),
    val imageUrls: List<String> = emptyList(),  // Buyer reference photo URLs
    val voiceNoteUrl: String? = null,
    val aiTags: List<String> = emptyList(),
    val acceptedQuoteId: String? = null,
    val acceptedVendorId: String? = null,
    val createdAt: Date = Date(),
    val expiresAt: Date = Date()          // Live broadcast expiration (e.g. +15 min)
)
```

#### 3. `quotes/{quoteId}`
```kotlin
data class Quote(
    val quoteId: String = "",             // Unique UUID
    val requestId: String = "",           // Parent request ID
    val vendorId: String = "",            // Bidding merchant ID
    val vendorShopName: String = "",      // Merchant Shop Name
    val vendorRating: Double = 5.0,
    val vendorLocation: GeoLocation = GeoLocation(),
    val offeredPricePKR: Double = 0.0,    // Offered price in PKR
    val note: String = "",                // Warranty & stock condition notes
    val imageUrls: List<String> = emptyList(),  // Vendor attached product photos
    val status: String = "PENDING",       // "PENDING", "ACCEPTED", "REJECTED"
    val createdAt: Date = Date()
)
```

#### 4. `chats/{chatId}`
```kotlin
data class Chat(
    val chatId: String = "",              // Equal to requestId for 1:1 deal mapping
    val requestId: String = "",
    val buyerId: String = "",
    val vendorId: String = "",
    val buyerDisplayName: String = "",
    val vendorDisplayName: String = "",
    val buyerNicknameForVendor: String = "",
    val vendorNicknameForBuyer: String = "",
    val claimCode: String = "",           // Matching 3-Digit Verification Code (e.g. "742")
    val offeredPricePKR: Double = 0.0,    // Agreed deal price
    val agreedNote: String = "",          // Agreed warranty note
    val buyerPhone: String = "",
    val vendorPhone: String = "",
    val itemQuery: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Date = Date(),
    val createdAt: Date = Date()
)
```

#### 5. `messages/{messageId}`
```kotlin
data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val type: String = "text",            // "text", "voice", "image"
    val voiceUrl: String = "",            // Firebase Storage URL for .m4a audio
    val imageUrl: String = "",           // Firebase Storage URL for .jpg photo
    val voiceDurationSecs: Int = 0,
    val timestamp: Long = System.currentTimeMillis(), // Primary key for strict chronological sorting
    val createdAt: Date = Date()
)
```

---

## 5. Core User Journeys & Technical Flow

### Journey A: Buyer Request Broadcast
1. Buyer opens `BuyerHomeScreen`, types a request (e.g., *"iPhone 13 Pro original OLED screen panel"*), attaches optional reference photos, and selects category tags.
2. Tapping **"Broadcast to Saddar Merchants"** uploads photos to Firebase Storage (`request_images/`), writes a new document to `requests/`, and navigates to `BuyerWaitingRadarScreen`.
3. A real-time Firestore `addSnapshotListener` listens for incoming bids on `quotes` collection where `requestId == currentRequestId`.

### Journey B: Vendor Live Bidding
1. Vendors open `VendorFeedScreen`. A live listener fetches active requests in their market zone (`status == "BROADCASTING"`).
2. Vendor taps a request card to view buyer details and reference photos in `RequestDetailSheet`.
3. Vendor fills in **Offered Price (PKR)**, **Warranty Notes**, attaches product stock photos, and taps **"Submit Quote"**.
4. Submitting a bid deducts **1 Connect** from the vendor’s `connectsBalance` and saves the bid in `quotes/`.

### Journey C: Deal Acceptance & Matching 3-Digit E-Receipt
1. Buyer sees vendor bids stream onto `QuoteFeedScreen`. Bids display offered price, vendor shop name, rating, warranty terms, and photos.
2. Buyer taps **"Accept Deal & Get Receipt"**.
3. An atomic Firestore transaction:
   - Updates `request.status` to `"ACCEPTED"`.
   - Updates `quote.status` to `"ACCEPTED"`.
   - Deducts 1 Connect from vendor profile.
   - Generates a **random 3-digit claim code** (e.g., `#742`).
   - Creates a document in `chats/` containing the matching claim code, price, and shop details.
4. Both Buyer and Vendor can open **"🧾 E-Receipt"** to view the matching claim code and tap **"Save Receipt"** to export the receipt canvas directly to their phone's photo gallery (`Pictures/BazaarLink`).

### Journey D: Real-Time Chat & Communications
1. Once a deal is accepted, a 1:1 chat room opens.
2. **Text Messages:** Instant realtime messaging sorted strictly by millisecond timestamp.
3. **Voice Notes:** Holding the mic button records `.m4a` audio via `MediaRecorder`. Releasing uploads the file to Firebase Storage (`voice_messages/`) and renders an inline audio player with play/pause controls driven by `MediaPlayer`.
4. **Photos:** Tapping the gallery button uploads a `.jpg` photo to Storage (`chat_images/`) and renders a chat image bubble. Tapping any photo opens `FullscreenImageDialog` for full-screen zooming.
5. **Direct Phone Call:** Tapping the phone call icon (`Icons.Default.Phone`) in the top app bar launches the native Android phone dialer (`Intent.ACTION_DIAL`) pre-filled with the merchant's phone number.

---

## 6. Component Breakdown & File Mapping

```text
app/src/main/java/com/bazaarlink/app/
├── MainActivity.kt                      # Main Activity entry point
├── di/
│   └── ServiceLocator.kt                # Singleton registry (Firebase Auth, Firestore, Storage, Repository)
├── models/
│   ├── User.kt                          # User & VendorProfile data classes
│   ├── Request.kt                       # Buyer Broadcast Request data class
│   ├── Quote.kt                         # Vendor Bid data class
│   ├── Chat.kt                          # Active Deal & Claim Code data class
│   ├── Message.kt                       # In-Chat Message data class
│   └── TagModel.kt                      # Category Tag model
├── repository/
│   ├── BazaarLinkRepository.kt          # Interface contract
│   └── BazaarLinkRepositoryImpl.kt      # Firebase & Firestore implementation
├── ui/
│   ├── account/
│   │   └── AccountScreen.kt             # User profile, role switcher, English/Urdu toggle & log out
│   ├── auth/
│   │   └── AuthScreen.kt                # Google Sign-In & Onboarding form
│   ├── buyer/
│   │   ├── BuyerHomeScreen.kt           # Buyer request input & photo upload
│   │   ├── BuyerShell.kt                # Buyer bottom navigation container (3 tabs)
│   │   ├── BuyerWaitingRadarScreen.kt   # Live radar listener for incoming quotes
│   │   └── QuoteFeedScreen.kt           # Bids feed & deal acceptance
│   ├── vendor/
│   │   ├── VendorFeedScreen.kt          # Incoming requests live feed
│   │   ├── VendorShell.kt               # Vendor bottom navigation container (3 tabs)
│   │   ├── RequestDetailSheet.kt        # Full request & reference photo viewer
│   │   └── SubmitQuoteDialog.kt         # Price & warranty bid submission form
│   ├── chat/
│   │   ├── ChatListScreen.kt            # Active chats overview screen
│   │   └── ChatDetailScreen.kt          # In-chat text, voice recorder, photos & call dialer
│   ├── common/
│   │   ├── FullscreenImageDialog.kt     # Full-screen photo zoom modal
│   │   └── EReceiptDialog.kt            # Deal E-Receipt modal with 3-digit code & gallery exporter
│   ├── navigation/
│   │   └── NavGraph.kt                  # Jetpack Compose route controller
│   └── theme/
│       ├── Color.kt                     # Material 3 Karachi color palette
│       ├── Theme.kt                     # Dark / Light theme wrapper
│       └── Type.kt                      # Typography system
├── util/
│   ├── LocaleHelper.kt                  # Dynamic English / Urdu runtime switcher
│   └── UserSessionManager.kt            # Local SharedPreferences session manager
└── viewmodels/
    ├── AuthViewModel.kt                 # Auth & role switching logic
    ├── BuyerViewModel.kt                # Broadcast & quote acceptance logic
    ├── VendorViewModel.kt               # Request listening & bid submission logic
    └── ChatViewModel.kt                 # Real-time messages, voice notes & nicknames logic
```

---

## 7. Localization & Urdu Accessibility

Karachi market shopkeepers primarily operate in **Urdu** and **Roman Urdu**. BazaarLink fully supports bilingual operation:
- Hardcoded zero strings in UI code; all text references `R.string`.
- `res/values/strings.xml`: English translations.
- `res/values-ur/strings.xml`: Urdu translations.
- `LocaleHelper.kt`: Dynamically updates `Configuration.setLocale()` at runtime.
- **Account Screen Switcher:** Tapping **🌐 Language / زبان** toggles the entire app between English and Urdu instantly.

---

## 8. Self-Sustainability & Monetization Economy

To meet **ibex Evaluation Criterion 03 (Self-Sustainability)**, BazaarLink incorporates a self-sustaining revenue engine that does not rely on external grants or government funding:

```
                          ┌──────────────────────────┐
                          │   BazaarLink Platform    │
                          └─────────────┬────────────┘
                                        │
           ┌────────────────────────────┴────────────────────────────┐
           ▼                                                         ▼
┌──────────────────────────────┐                         ┌──────────────────────────────┐
│    Vendor Connects Economy   │                         │  Verified Merchant Subscriptions│
│  Vendors buy credit packs    │                         │  Shopkeepers pay PKR 1,500/mo│
│  (e.g., PKR 500 = 50 Bids)   │                         │  for "Verified Saddar Badge" │
│  Deducted 1 Connect per bid. │                         │  & top search placement.     │
└──────────────────────────────┘                         └──────────────────────────────┘
```

1. **Vendor Connects Model:**
   - Vendors start with 50 free credits (`connectsBalance = 50`).
   - Submitting a bid deducts 1 Connect.
   - Vendors replenish Connects via mobile wallet top-ups (Easypaisa / JazzCash), creating a direct recurring revenue stream for BazaarLink.
2. **Verified Merchant Badges:**
   - Saddar shopkeepers can pay a monthly fee for a **"Verified Saddar Merchant 4.9★"** trust badge, increasing buyer conversion rates.

---

## 9. Scalability & Zonal Replication Plan

To meet **ibex Evaluation Criterion 04 (Scalability Logic)**, BazaarLink scales across Karachi using a **Hyperlocal Zonal Replication Model**:

```
[ Phase 1: Zone 01 ] ──► [ Phase 2: Zone 02 ] ──► [ Phase 3: Zone 03 ] ──► [ Phase 4: Citywide ]
  Saddar Electronics        Tariq Road Mobile        Urdu Bazar Books &        Nazimabad & Hyderi
 (Star City, Regal, Coop)      Market Zone           Stationery Wholesale           Markets
```

### Why the Model Scales Without Rewriting Code:
- The backend relies on a indexed `marketZone` metadata attribute (e.g. `marketZone = "Star City Mall, Saddar"`).
- Expanding to a new market requires zero architecture changes—simply add a new zonal geographic boundary tag in Firestore.

---

## 10. Risk Analysis & Mitigation Matrix

To meet **ibex Evaluation Criterion 06 (Awareness of Gaps)**, BazaarLink explicitly identifies and mitigates real-world operational risks in Karachi:

| Identified Risk / Gap | Potential Impact | BazaarLink Architectural Mitigation |
|---|---|---|
| **1. Low Merchant Literacy / Typing Barrier** | Shopkeepers avoid typing long descriptions in English. | Implemented **press-and-hold Voice Notes** (`MediaRecorder`) and bilingual Urdu interface so vendors can respond by voice in 5 seconds. |
| **2. Basement Internet Blindspots** | Signal drops inside Saddar plaza basements. | Integrated **`UserSessionManager` SharedPreferences local caching** and Firestore offline persistence so data syncs automatically when connection resumes. |
| **3. Fake Bids / No-Show Transactions** | Buyers/Vendors agree on price but fail to complete the transaction. | Created **Matching 3-Digit Claim Code E-Receipts** (`#742`). The deal code must match at the shop counter, establishing mutual accountability. |
| **4. Vendor Over-Spam** | Buyers get spammed by too many low-quality quotes. | Implemented **Connects Deduction per Quote**, discouraging spam and forcing vendors to submit competitive, accurate bids. |

---

## 🏁 Summary
BazaarLink is a production-ready, fully executable Android MVP designed specifically for Karachi’s unique commercial environment. Built with clean Kotlin, Jetpack Compose, and Firebase, it delivers real-time value to buyers and vendors while fulfilling all evaluation requirements for **ibex. iSprint Challenge Round 2: The Blueprint**.
