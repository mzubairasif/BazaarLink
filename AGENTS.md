# Antigravity Agent Configuration for BazaarLink

## Mission
You are building BazaarLink, an MVP for a hyperlocal reverse-marketplace Android app designed to connect buyers and sellers in dense, unorganized markets (like Saddar, Karachi) in real-time. You have exactly 12 days to complete a functional prototype. Speed, stability, and simplicity are paramount.

## The Team
The human users (Zaid, Ahsan, and I) are systems architects. We DO NOT write Kotlin. You (the agent swarm) are fully responsible for generating, debugging, and self-healing all code. If you encounter an error, fix it autonomously—do not wait for our input unless you require a fundamental architectural decision.

## Core Rules of Engagement

### 1. Technology Constraints (STRICT)
*   **Language:** Kotlin only. No Java.
*   **UI Framework:** Jetpack Compose ONLY. Do absolutely zero XML layout design. All UI must be declarative.
*   **Backend:** Firebase (Firestore + Firebase Auth).
*   **Architecture:** Use basic MVVM (Model-View-ViewModel). Do not implement Clean Architecture, Use Cases, or complex Repository patterns. We need it functional, not enterprise-ready.
*   **Dependency Injection:** Do not use Dagger or Hilt. Manual dependency injection or simple ViewModel instantiation is required to save time and reduce build complexity.
*   **Navigation:** Use standard Jetpack Navigation Compose.

### 2. Database Design (Firestore)
*   Keep the data structure entirely flat. Do not nest sub-collections unnecessarily.
*   We need exactly three primary collections:
    1.  `users` (Stores buyer/vendor role and location).
    2.  `requests` (Stores the broadcasted buyer queries, tags, and status).
    3.  `quotes` (Stores the vendor bids tied to a specific `requestId`).
*   Denormalize data if it saves read operations or complex queries.

### 3. UI/UX Directives (Jetpack Compose)
*   **Minimalist Design:** Stick to Material 3 standard components. Do not spend time on custom animations or complex styling unless explicitly requested.
*   **State Hoisting:** Keep UI state management simple. Use `StateFlow` or `LiveData` within the ViewModels to drive Compose updates.
*   **Localization Ready:** Hardcode ZERO text strings in the UI components. Every single string MUST be stored in `res/values/strings.xml` to allow for rapid English/Urdu switching.

### 4. Development Workflow & Self-Healing
*   **Focus on the Core Loop:** The only features that matter are the Buyer broadcasting a request -> the Vendor seeing the request and submitting a quote -> the Buyer accepting the quote. If a feature does not directly serve this loop, ignore it.
*   **Faking Features:** We are building a demo. Hardcode the vendor onboarding. Hardcode the "Connects" wallet logic (just show a static number). Do not integrate real payment gateways.
*   **Error Handling:** When a build fails or an app crashes, read the stack trace, identify the breaking change, and apply the fix. Never explain the fix to the user and ask them to copy-paste it. You must apply the code changes directly.
*   **No Placeholders:** Never generate code with comments like `// TODO: Implement later` for critical path logic. If a function is needed for the MVP loop, write the full implementation.

## Priority Tasks for Agent Swarm
1.  Initialize the standard Android project structure with Compose dependencies.
2.  Set up the Firebase integration and establish the Firestore connection.
3.  Generate the Buyer UI (Home Search -> Waiting Radar -> Quote Feed).
4.  Generate the Vendor UI (Incoming Request Feed -> Quote Input Pad).
5.  Wire the ViewModels to execute the real-time Firebase read/writes.
