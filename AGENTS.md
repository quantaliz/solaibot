# Sol-AI-Bot Project (Android)

Sol-AI-Bot is an Android application bringing privacy-first, offline-capable Generative AI on device, with blockchain-powered features via Solana.
This document outlines project-specific guidelines and conventions for Gemini in Android Studio.

## 🎯 Core Technologies & Architecture

*   **Primary Language:** Kotlin
*   **UI Toolkit:** Jetpack Compose for modern, declarative UI.
*   **Architecture:** MVVM (Model-View-ViewModel) with Android Architecture Components (ViewModel, LiveData/StateFlow, Room for local storage if applicable beyond Keystore).
*   **On-Device AI:** Google AI Edge with LiteRT models (Gemma3N, etc.).
*   **Blockchain Integration:** Solana Web3.js, Phantom SDK (via WebView or dedicated Kotlin wrappers).
*   **Local Storage:** Android Keystore for sensitive data (private keys), potentially Room for structured non-sensitive data or encrypted chat history.
*   **Concurrency:** Kotlin Coroutines and Flows.
*   **Dependency Injection:** Hilt or Koin (specify if one is chosen).
*   **Networking:** Retrofit for any non-blockchain API calls, OkHttp.

## 🛠️ Build & Development Environment

*   **IDE:** Android Studio (latest stable version recommended).
*   **Build System:** Gradle with Groovy or Kotlin DSL (specify if one is preferred).
*   **Version Control:** Git.
*   **Minimum SDK:** API Level 31 (Android 12).
*   **Target SDK:** Latest available API level.

**Common Gradle Tasks:**
*   Clean project: `./gradlew clean`
*   Build debug APK: `./gradlew assembleDebug`
*   Build release APK/Bundle: `./gradlew assembleRelease` or `./gradlew bundleRelease`
*   Run unit tests: `./gradlew testDebugUnitTest`
*   Run instrumented tests: `./gradlew connectedDebugAndroidTest`
*   Install on connected device/emulator: `./gradlew installDebug`

##  Kode Style & Conventions

*   **Kotlin:** Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) and [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide).
*   **Jetpack Compose:** Adhere to Compose-specific guidelines (e.g., lowercase composable function names, `Modifier` as the first optional parameter).
*   **Naming:**
    *   Use descriptive names for classes, functions, and variables.
    *   ViewModels: Suffix with `ViewModel` (e.g., `ChatViewModel`).
    *   Composables: PascalCase for functions emitting UI (e.g., `UserProfileScreen`).
    *   Layout files (XML, if any for legacy parts): `activity_*.xml`, `fragment_*.xml`, `item_*.xml`.
*   **Linting & Formatting:**
    *   Utilize Android Studio's built-in lint checks.
    *   Use `ktlint` or Android Studio's default formatter (Ctrl+Alt+L or Cmd+Option+L).
*   **Comments:**
    *   Use KDoc for public APIs (classes, functions, properties).
    *   Use `//` for implementation details or complex logic clarification.
*   **Imports:** Keep imports organized; remove unused imports.
*   **Error Handling:**
    *   Use Kotlin's `Result` type or try-catch blocks appropriately.
    *   Provide clear user feedback for errors, especially for blockchain transactions or model loading failures.

## 🧪 Testing

*   **Unit Tests:** JUnit 4/5 and Mockito/MockK for ViewModels, UseCases, Repositories.
    *   Place in `app/src/test/java/`.
*   **UI/Instrumentation Tests:** Espresso or Compose Test Rule for UI testing.
    *   Place in `app/src/androidTest/java/`.
*   **Blockchain Interactions:** Mock or use a local Solana test validator for testing Solana-specific logic where feasible.
*   **On-Device AI Model Testing:** Focus on integration points; actual model performance testing might require separate scripts or manual verification.

## 🔐 Security & Privacy

*   **Sensitive Data:**
    *   Store Solana private keys and other sensitive credentials **only** in the Android Keystore.
    *   Never hardcode secrets. Use `build.gradle` (via `buildConfigField` or `resValue` for non-sensitive placeholders) or secure configuration loading for API keys if needed (though on-device AI reduces this).
*   **Permissions:** Request only necessary permissions. Clearly explain why permissions are needed.
*   **Network:** Use HTTPS for any external calls (e.g., fetching models if not bundled, interacting with Solana RPC nodes).
*   **Data Encryption:** Encrypt user data at rest (e.g., chat history) if stored locally outside the Keystore, possibly using Jetpack Security.
*   **Input Validation:** Validate all inputs, especially those used for blockchain transactions or model prompts.

## Solana-Specific Considerations

*   **Transaction Signing:** Clearly indicate to the user when a transaction needs tobe signed. Use Phantom SDK's UI prompts.
*   **RPC Node:** Allow configuration of Solana RPC node if not hardcoded (or provide a default with an option to change).
*   **Error Handling:** Provide user-friendly messages for common Solana errors (e.g., insufficient SOL, network issues, transaction failures).
*   **Developer Mode:**
    *   Logs should clearly show transaction IDs and links to Solana Explorer (e.g., Solscan, SolanaFM).
    *   Distinguish between on-chain and off-chain logs.

## 💡 Gemini Prompting Guidelines for Sol-AI-Bot

*   **Specify Target Component:** When asking for code, mention if it's for a `@Composable` function, a `ViewModel`, a Repository, a Solana interaction service, etc.
    *   *Example: "Generate a Jetpack Compose screen for displaying chat messages."*
    *   *Example: "Create a Kotlin function in a ViewModel to handle sending a signed transaction to Solana using the Phantom SDK."*
*   **Mention Key Libraries:** If a specific library should be used (e.g., "using StateFlow", "for a Room DAO").
    *   *Example: "How do I observe a StateFlow in a Jetpack Compose composable to update the UI?"*
*   **Contextualize AI/Blockchain Features:** When asking about features from your `README.md`.
    *   *Example: "Suggest how to implement the 'On-Chain Prompt Verification' feature. What data should be stored on Solana?"*
    *   *Example: "How can I securely pass a user's prompt to a LiteRT model for inference on-device?"*
*   **Ask for Android Best Practices:**
    *   *Example: "What's the best way to handle background tasks for model downloading in Android, ensuring it's lifecycle-aware?"*
*   **Code Style Preference:** "Ensure the generated Kotlin code follows Android Kotlin style guidelines."
