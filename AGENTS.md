# Antigravity Agent Configuration for BazaarLink

## Mission & Competition Focus
You are building **BazaarLink**, an MVP for a hyperlocal reverse-marketplace Android app designed to connect buyers and sellers in dense, unorganized markets (like Saddar, Karachi) in real-time. 

🏆 **Competition Context:** **ibex. iSprint Challenge (Round 2: The Blueprint - Karachi 2026)**.
Total Prize Pool: **PKR 1,750,000**. See full competition details in [ISPRINT_BLUEPRINT.md](file:///c:/Users/Zaid/Downloads/BazaarLink-main/ISPRINT_BLUEPRINT.md).

---

## The Team
The human users (Zaid, Ahsan, and systems architects) DO NOT write Kotlin code. You (the agent swarm) are fully responsible for generating, debugging, and self-healing all code. If you encounter an error, fix it autonomously.

---

## Core Rules of Engagement

### 1. Technology Constraints (STRICT)
*   **Language:** Kotlin only. No Java.
*   **UI Framework:** Jetpack Compose ONLY. Do absolutely zero XML layout design. All UI must be declarative.
*   **Backend:** Firebase (Firestore + Firebase Auth + Firebase Storage).
*   **Architecture:** Basic MVVM (Model-View-ViewModel). No Dagger or Hilt.
*   **Navigation:** Standard Jetpack Navigation Compose.

### 2. Database Design (Firestore)
*   Flat data structure: `users`, `requests`, `quotes`, `messages`, `chats`.

### 3. UI/UX Directives (Jetpack Compose)
*   **Material 3 Design System:** Modern, dark/light theme, clean Karachi-adapted UI.
*   **Localization Ready:** English / Urdu dynamic switcher ([LocaleHelper.kt](file:///c:/Users/Zaid/Downloads/BazaarLink-main/app/src/main/java/com/bazaarlink/app/util/LocaleHelper.kt)) supported via `strings.xml` and `values-ur/strings.xml`.

### 4. ibex iSprint Round 2 Evaluation Pillars
All features & code must align with the **6 ibex Evaluation Criteria**:
1.  **Solution Clarity:** Simple 3-step loop (Query → Bids → E-Receipt Deal).
2.  **Execution Feasibility:** Bulletproof Kotlin + Firebase working prototype with press-and-hold voice notes, photos, and matching 3-digit E-Receipt claim codes.
3.  **Self-Sustainability:** Vendor Connects monetization economy (`connectsBalance` deduction per quote).
4.  **Scalability Logic:** Replicable zonal structure (Saddar → Tariq Road → Urdu Bazar → Gulshan).
5.  **Problem-Solution Fit:** Solves unorganized Karachi market friction.
6.  **Awareness of Gaps:** Mitigates merchant tech barriers (Voice Notes support) & trust verification.

---

## Development Workflow & Self-Healing
*   **Focus on the Core Loop:** Buyer query -> Vendor live bid -> Buyer acceptance -> E-Receipt & Chat.
*   **No Placeholders:** Write full, production-ready Kotlin code for every feature.
