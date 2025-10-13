# Sol-AI-Bot Agent Guidelines

Sol-AI-Bot is an Android application bringing privacy-first, offline-capable on-device Generative AI powered by Google AI Edge (LiteRT), with blockchain integration capabilities via Solana. This document provides comprehensive project-specific guidelines when working with this codebase.

## 🎯 Project Overview

**Package Name:** `com.quantaliz.solaibot`
**Project Name:** Sol-AI-Bot (formerly Edge Gallery)
**Owner:** Quantaliz PTY LTD
**License:** Apache License 2.0
**Version:** 1.0.7 (versionCode: 13)

## 🏗️ Architecture & Technologies

### Core Technologies

- **Language:** Kotlin (100% Kotlin codebase with 111+ source files)
- **Build System:** Gradle with Kotlin DSL (.kts)
- **UI Framework:** Jetpack Compose (modern, declarative UI)
- **Architecture Pattern:** MVVM (Model-View-ViewModel)
- **Dependency Injection:** Hilt (Dagger)
- **Concurrency:** Kotlin Coroutines and Flows
- **Data Persistence:**
  - DataStore (Protocol Buffers) for settings and user data
  - Android Keystore for sensitive credentials
  - Potentially Room for structured data
- **Navigation:** Jetpack Navigation Compose

### AI & Machine Learning

- **Framework:** Google AI Edge with LiteRT (TensorFlow Lite)
- **Library:** `litertlm` (v0.0.0-alpha01) for on-device LLM inference
- **TFLite Components:**
  - play-services-tflite-java (16.4.0)
  - play-services-tflite-gpu (16.4.0) for GPU acceleration
  - play-services-tflite-support (16.4.0)

### Blockchain Integration (Planned)

- **Platform:** Solana
- **Integration:** Solana Web3.js, Phantom SDK (via WebView or Kotlin wrappers)
- **Use Cases:** On-chain prompt verification, blockchain-powered features

### Key Dependencies

```kotlin
// UI & Compose
compose-bom: 2025.05.00
material3, navigation-compose (2.8.9)
material-icons-extended (1.7.8)

// Android Core
core-ktx (1.15.0), lifecycle-runtime-ktx (2.8.7)
activity-compose (1.10.1)
security-crypto (1.1.0)

// Networking & Auth
openid-appauth (0.11.1) for HuggingFace OAuth

// Media
CameraX (1.4.2) for camera functionality
Audio recording capabilities

// Other
work-runtime (2.10.0) for background tasks
protobuf-javalite (4.26.1)
commonmark & richtext for markdown rendering
Firebase (Analytics via firebase-bom 33.16.0)
```

## 📁 Project Structure

```
/proj
├── app/src/main/java/com/quantaliz/solaibot/
│   ├── MainActivity.kt                    # Entry point with Hilt integration
│   ├── SolAIBotApp.kt                    # Main app composable
│   ├── SolAIBotApplication.kt            # Application class
│   ├── SolAIBotTopAppBar.kt              # Shared top app bar
│   ├── common/                           # Common utilities and types
│   ├── customtasks/                      # Custom task framework
│   │   ├── common/                       # CustomTask interfaces
│   │   └── examplecustomtask/            # Example implementation
│   ├── data/                             # Data layer
│   │   ├── Model.kt                      # Model definitions
│   │   ├── Tasks.kt                      # Task definitions
│   │   ├── Config.kt                     # Configuration system
│   │   ├── DataStoreRepository.kt        # DataStore wrapper
│   │   ├── DownloadRepository.kt         # Model download management
│   │   └── Categories.kt                 # Task categories
│   ├── di/                               # Dependency injection
│   │   └── AppModule.kt                  # Hilt module
│   ├── ui/                               # UI layer
│   │   ├── common/                       # Shared UI components
│   │   │   ├── chat/                     # Chat UI components
│   │   │   ├── modelitem/                # Model list items
│   │   │   └── tos/                      # Terms of service
│   │   ├── home/                         # Home screen
│   │   ├── llmchat/                      # LLM chat feature
│   │   ├── llmsingleturn/                # Single-turn LLM
│   │   ├── modelmanager/                 # Model management
│   │   ├── navigation/                   # Navigation setup
│   │   └── theme/                        # Material3 theming
│   └── worker/                           # Background workers
│       └── DownloadWorker.kt             # Model download worker
├── build.gradle.kts                      # Root build config
├── settings.gradle.kts                   # Gradle settings
├── app/build.gradle.kts                  # App module build config
└── docs/                                 # Related repositories and supplementary project docs
    ├── CBx402.md
    ├── Coinbasex402/
    ├── PayAIx402/
    ├── Solana-RPC.md
    ├── mobile-wallet-adapter/
    ├── mobile-wallet-adapter.md
    └── sol4k/
```

## 🛠️ Build Environment

### SDK & Version Requirements

```kotlin
minSdk = 31          // Android 12
targetSdk = 35       // Android 15
compileSdk = 35
```

### Gradle Tasks

```bash
# Clean
./gradlew clean

# Build
./gradlew assembleDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Package release build
./gradlew assembleRelease
```
¡¡NOTE!!: you are running in a VM without android-sdk, these commands will fail. Let the developer handle compilation tasks

### Tips

- Ensure Android SDK 35 is installed (via Android Studio or `sdkmanager`)
- Use Android Studio Ladybug or newer for best Compose tooling support
- Configure `local.properties` with the SDK path to avoid build failures

## 🤖 LLM Model Management

### Model Download Flow

1. **Config**: `data/Config.kt` defines available models
2. **Repository**: `DownloadRepository.kt` handles download lifecycle
3. **Worker**: `DownloadWorker.kt` performs background downloads
4. **UI**: `modelmanager` screens show download state

### Adding New Models

- Update `data/Model.kt` and `data/ModelDataFile.proto`
- Ensure new models respect device constraints (`minDeviceMemoryInGb`)
- Provide metadata: display name, description, capabilities

### Model Storage

- Default storage path managed via `ModelDataFile`
- Avoid hardcoded paths; use repository APIs
- Clean up temporary files on cancellation or failure

## 🧠 AI Interaction Patterns

### Conversation Flow

- Use `LlmChatViewModel` for multi-turn conversations
- `StateFlow` exposes chat state to Compose UI
- Handle streaming responses via Flow collectors

### Prompt Handling

- Input validation before sending to model
- Support attachments (images/audio) when model permits
- Provide user feedback during inference (loading indicators)

### Error Scenarios

- Network fallback for downloads only; inference stays offline
- Surface actionable errors (e.g., "Model not downloaded")
- Log details for developer builds

## 🔐 Security & Privacy

- Store sensitive data in Android Keystore
- Use HTTPS for any remote config or updates
- Respect user opt-in for analytics (Firebase)
- Anonymize logs; never store raw prompts without consent

## 🌐 Blockchain Features (Future)

### Solana Integration Guidelines

When implementing Solana features:

1. **Transaction Signing**
   - Use Phantom SDK UI prompts
   - Clearly indicate to users when signing is required
   - Never auto-sign transactions

2. **RPC Configuration**
   - Allow user configuration of RPC nodes
   - Provide sensible defaults (mainnet-beta, devnet, testnet)
   - Handle network errors gracefully

3. **Error Handling**
   ```kotlin
   sealed class SolanaError {
       data class InsufficientFunds(val required: Long, val available: Long)
       data class NetworkError(val message: String)
       data class TransactionFailed(val signature: String, val reason: String)
   }
   ```

4. **Developer Logging**
   - Log transaction IDs
   - Include Solana Explorer links (Solscan, SolanaFM)
   - Distinguish on-chain vs off-chain operations

## 💡 CLI Code Prompting Best Practices

### Be Specific About Components

❌ **Don't:** "Add a button"
✅ **Do:** "Create a Composable button in the LlmChatScreen that triggers model inference when clicked"

❌ **Don't:** "Fix the data layer"
✅ **Do:** "Update the DownloadRepository to handle network errors gracefully and emit failure states via Flow"

X **Don't:** "Compile yourself this project"

### Specify Technical Context

When asking for code:
- Mention if it's a `@Composable`, `ViewModel`, `Repository`, etc.
- Reference relevant libraries (e.g., "using StateFlow", "with Hilt injection")
- Specify architecture layer (UI, data, domain)

## 🚨 Common Pitfalls to Avoid

1. **Don't hardcode model file paths** - Use the Model and ModelDataFile system
2. **Don't block the main thread** - Always use Coroutines for I/O and heavy computation
3. **Don't forget Hilt annotations** - `@AndroidEntryPoint` for Activities, `@HiltViewModel` for ViewModels
4. **Don't skip input validation** - Especially for user prompts and blockchain data
5. **Don't ignore memory constraints** - Respect `minDeviceMemoryInGb` in Model definitions
6. **Don't use GlobalScope** - Use `viewModelScope` or `lifecycleScope`
7. **Don't forget to handle configuration changes** - ViewModels survive rotation, but compose state needs proper hoisting

## 📚 Important Files to Reference

When working on specific features, consult these key files:

- **Architecture Setup:** `di/AppModule.kt`, `SolAIBotApplication.kt`
- **Model System:** `data/Model.kt`, `data/Tasks.kt`, `data/DownloadRepository.kt`
- **Chat Implementation:** `ui/common/chat/ChatViewModel.kt`, `ui/llmchat/LlmChatViewModel.kt`
- **UI Patterns:** `ui/common/` directory for reusable components
- **Configuration System:** `data/Config.kt`, `data/ConfigValue.kt`
- **Custom Tasks:** `customtasks/examplecustomtask/` for reference implementation

## 🎯 Project Goals & Priorities

1. **Privacy-First:** All AI inference happens on-device
2. **Offline-Capable:** Core functionality works without internet
3. **Performance:** Optimize for mobile constraints (memory, battery, compute)
4. **Extensibility:** Support custom tasks and models via plugin system
5. **Security:** Protect user data and credentials using Android best practices
6. **Blockchain-Ready:** Architecture prepared for Solana integration

## 📖 Additional Resources

- [Android Developer Docs](https://developer.android.com)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Google AI Edge](https://ai.google.dev/edge)
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Solana Developer Docs](https://docs.solana.com)
- [Related source code repositories](docs/)

---

**Last Updated:** 2025-10-01
**Project Version:** 1.0.7
**Maintained by:** Quantaliz PTY LTD
