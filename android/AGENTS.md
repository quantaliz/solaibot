# Sol-AI-Bot Agent Guidelines

Sol-AI-Bot is an Android application bringing privacy-first, offline-capable on-device Generative AI powered by Google AI Edge (LiteRT), with blockchain integration capabilities via Solana. This document provides comprehensive project-specific guidelines when working with this codebase.

## 🎯 Project Overview

**Package Name:** `com.quantaliz.solaibot`
**Project Name:** Sol-AI-Bot (formerly Edge Gallery)
**Owner:** Quantaliz PTY LTD
**License:** Apache License 2.0
**Version:** 1.0.7 (versionCode: 13)
**Hackathon Submission:** Cypherpunk 2025 & Hackaroo 2025

## 🏗️ Architecture & Technologies

### Core Technologies

- **Language:** Kotlin (100% Kotlin codebase with 121 source files)
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

### Blockchain Integration (Active)

- **Platform:** Solana
- **Integration:**
  - Mobile Wallet Adapter (MWA) clientlib-ktx (2.0.3)
  - Solana Web3 SDK (0.2.5)
  - Solana RPC (0.2.10)
  - sol4k library (0.5.17)
- **Use Cases:**
  - x402 HTTP Payment Protocol implementation
  - Token-gated prompts
  - Signed AI transactions
  - Wallet connection (Solflare, MWA-compatible wallets)

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

// Blockchain/Solana
mobile-wallet-adapter-clientlib-ktx (2.0.3)
web3-solana (0.2.5)
rpc-solana, rpc-core, rpc-ktordriver (0.2.10)
sol4k (0.5.17)

// Other
work-runtime (2.10.0) for background tasks
protobuf-javalite (4.26.1)
commonmark & richtext for markdown rendering
okhttp for HTTP client functionality
```

## 📁 Project Structure

```
/proj/android
├── android/
│   ├── app/src/main/java/com/quantaliz/solaibot/
│   │   ├── MainActivity.kt                    # Entry point with Hilt integration
│   │   ├── SolAIBotApp.kt                    # Main app composable
│   │   ├── SolAIBotApplication.kt            # Application class
│   │   ├── SolAIBotTopAppBar.kt              # Shared top app bar
│   │   ├── SolAIBotLifecycleProvider.kt      # Lifecycle management
│   │   ├── SettingsSerializer.kt             # Settings persistence
│   │   ├── UserDataSerializer.kt             # User data persistence
│   │   ├── common/                           # Common utilities and types
│   │   ├── customtasks/                      # Custom task framework
│   │   │   ├── common/                       # CustomTask interfaces
│   │   │   └── examplecustomtask/            # Example implementation
│   │   ├── data/                             # Data layer
│   │   │   ├── Model.kt                      # Model definitions
│   │   │   ├── Tasks.kt                      # Task definitions
│   │   │   ├── Config.kt                     # Configuration system
│   │   │   ├── ConfigValue.kt                # Config value types
│   │   │   ├── DataStoreRepository.kt        # DataStore wrapper
│   │   │   ├── DownloadRepository.kt         # Model download management
│   │   │   ├── FunctionDeclarations.kt       # LLM function calling
│   │   │   ├── NetworkConnectivityHelper.kt  # Network utilities
│   │   │   ├── SolanaWalletFunctions.kt      # Solana wallet integration
│   │   │   └── x402/                         # x402 payment protocol
│   │   │       ├── X402Models.kt             # x402 data models
│   │   │       ├── X402HttpClient.kt         # HTTP client for x402
│   │   │       ├── X402FacilitatorClient.kt  # Facilitator API client
│   │   │       ├── X402TransactionBuilder.kt # Transaction construction
│   │   │       ├── SolanaPaymentBuilder.kt   # Payment builder (MWA)
│   │   │       └── SolanaPaymentBuilderSelfSigned.kt  # Self-signed payments
│   │   ├── di/                               # Dependency injection
│   │   │   └── AppModule.kt                  # Hilt module
│   │   ├── ui/                               # UI layer
│   │   │   ├── common/                       # Shared UI components
│   │   │   │   ├── chat/                     # Chat UI components
│   │   │   │   ├── modelitem/                # Model list items
│   │   │   │   └── tos/                      # Terms of service
│   │   │   ├── home/                         # Home screen
│   │   │   ├── icon/                         # Custom icons
│   │   │   │   └── Deploy.kt                 # Deployment icon
│   │   │   ├── llmchat/                      # LLM chat feature
│   │   │   ├── llmsingleturn/                # Single-turn LLM
│   │   │   ├── modelmanager/                 # Model management
│   │   │   ├── navigation/                   # Navigation setup
│   │   │   ├── theme/                        # Material3 theming
│   │   │   └── wallet/                       # Wallet UI
│   │   │       └── WalletViewModel.kt        # Wallet state management
│   │   └── worker/                           # Background workers
│   │       └── DownloadWorker.kt             # Model download worker
│   ├── app/src/main/res/                     # Android resources
│   │   ├── values/strings.xml                # App name and strings
│   │   ├── drawable/                         # Vector drawables
│   │   │   ├── ic_launcher_foreground.xml    # Launcher icon foreground
│   │   │   ├── ic_launcher_background.xml    # Launcher icon background
│   │   │   ├── logo.xml                      # App logo
│   │   │   └── splash_screen_animated_icon.xml  # Splash screen icon
│   │   └── mipmap-*/                         # App launcher icons (all densities)
│   ├── build.gradle.kts                      # Root build config
│   ├── settings.gradle.kts                   # Gradle settings
│   ├── app/build.gradle.kts                  # App module build config
│   ├── AGENTS.md                             # This file
│   ├── prompts.txt                           # Development prompts
│   └── docs/                                 # Related repositories and supplementary project docs
│       ├── CBx402.md                         # x402 protocol documentation
│       ├── PayAIx402/                        # PayAI x402 implementation
│       ├── Solana-RPC.md                     # Solana RPC guide
│       ├── x402-Header.md                    # x402 header specification
│       └── helpers/                          # Helper code and utilities
├── README.md                                 # Project README
├── images/                                   # Screenshots and branding
│   ├── SolAIBot1-Result.png                  # App interface screenshot
│   ├── SolAIBot2-Sign.png                    # Payment signing screenshot
│   └── Quantaliz.png                         # Quantaliz logo
└── context/                                  # Context files for AI assistants
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

### Default Model

The app uses **Hammer 2.1 (1.5B)** as the default on-device LLM. This model:
- Supports function calling for payment operations
- Runs efficiently on mobile devices
- Provides conversational AI capabilities
- Can be replaced with other `.litertlm` models

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
- Wallet connections secured via MWA - app never has direct access to private keys
- Payment transactions require explicit user approval

## 🌐 Blockchain Features (Implemented)

### x402 Payment Protocol

The app implements the x402 HTTP Payment Protocol for AI-driven micropayments:

**Flow:**
1. LLM generates function call: `solana_payment(url="...")`
2. Client requests URL without payment
3. Server responds with `402 Payment Required` + payment details
4. Client builds Solana transaction via `SolanaPaymentBuilder`
5. User signs transaction via Mobile Wallet Adapter (MWA)
6. Client retries request with `X-PAYMENT` header
7. Facilitator verifies and settles payment on-chain
8. Server returns content + settlement confirmation
9. LLM explicitly shows the payment result to the user with a success message

**Key Components:**
- `data/x402/X402HttpClient.kt`: HTTP client with x402 support
- `data/x402/X402FacilitatorClient.kt`: Facilitator API integration
- `data/x402/SolanaPaymentBuilder.kt`: Transaction builder with MWA
- `data/x402/X402TransactionBuilder.kt`: Low-level transaction construction
- `data/x402/X402Models.kt`: Payment protocol data models
- `data/x402/SolanaPaymentBuilderSelfSigned.kt`: Alternative self-signed implementation
- `data/SolanaWalletFunctions.kt`: Wallet function declarations for LLM
- `data/FunctionDeclarations.kt`: Function call interface for LLM
- `ui/wallet/WalletViewModel.kt`: Wallet UI state management

**Implementation Notes:**
- Payments are facilitated by a third-party service that handles on-chain settlement
- The app builds the transaction but never holds private keys
- Users must have a MWA-compatible wallet installed (e.g., Solflare)
- Supports devnet and mainnet-beta networks
- Transaction fees are paid by the merchant/facilitator, not the user
- Payment results are displayed explicitly in the chat interface
- Successfully tested with Solflare wallet on devnet

### Solana Integration Guidelines

When implementing or extending Solana features:

1. **Transaction Signing**
   - Use Mobile Wallet Adapter (MWA) for transaction signing
   - Support Solflare and other MWA-compatible wallets
   - Clearly indicate to users when signing is required
   - Never auto-sign transactions
   - See `SolanaPaymentBuilder.kt` for reference implementation

2. **RPC Configuration**
   - Allow user configuration of RPC nodes
   - Provide sensible defaults (mainnet-beta, devnet, testnet)
   - Handle network errors gracefully
   - Use `rpc-solana` and `rpc-ktordriver` for RPC communication

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

5. **LLM Function Calling**
   - Define wallet functions in `SolanaWalletFunctions.kt`
   - Use `FunctionDeclarations.kt` for LLM-accessible functions
   - Allow LLM to initiate payments via function calls

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
8. **Don't bypass MWA for transactions** - Always use Mobile Wallet Adapter for transaction signing
9. **Don't hardcode RPC endpoints** - Make them configurable for different networks
10. **Don't ignore x402 error responses** - Handle 402 status codes properly in HTTP clients

## 📚 Important Files to Reference

When working on specific features, consult these key files:

- **Architecture Setup:** `di/AppModule.kt`, `SolAIBotApplication.kt`
- **Model System:** `data/Model.kt`, `data/Tasks.kt`, `data/DownloadRepository.kt`
- **Chat Implementation:** `ui/common/chat/ChatViewModel.kt`, `ui/llmchat/LlmChatViewModel.kt`
- **UI Patterns:** `ui/common/` directory for reusable components
- **Configuration System:** `data/Config.kt`, `data/ConfigValue.kt`
- **Custom Tasks:** `customtasks/examplecustomtask/` for reference implementation
- **x402 Payment Protocol:** `data/x402/` directory for all payment-related code
- **Solana Integration:** `data/SolanaWalletFunctions.kt`, `data/x402/SolanaPaymentBuilder.kt`
- **Wallet UI:** `ui/wallet/WalletViewModel.kt`
- **Function Calling:** `data/FunctionDeclarations.kt` for LLM-accessible functions

## 🎯 Project Goals & Priorities

1. **Privacy-First:** All AI inference happens on-device
2. **Offline-Capable:** Core functionality works without internet (payments require network)
3. **Performance:** Optimize for mobile constraints (memory, battery, compute)
4. **Extensibility:** Support custom tasks and models via plugin system
5. **Security:** Protect user data and credentials using Android best practices
6. **Blockchain Integration:** Full Solana integration with x402 payment protocol
7. **AI-Driven Payments:** Enable LLM to autonomously initiate and manage micropayments

## 📋 Recent Changes & Updates

### Version 1.0.7 (versionCode: 13)

#### Branding Update (PR #5 - UpdateBranding)
- **App Icon Redesign:** Updated all launcher icons across all densities with new color scheme
- **Vector Drawables:** Redesigned foreground and background layers for adaptive icons
- **Logo Updates:** Modified app logo and splash screen with new branding colors
- **Screenshots Added:** Added marketing screenshots showing app interface and payment flow
  - `SolAIBot1-Result.png` - Chat interface with LLM response
  - `SolAIBot2-Sign.png` - Payment signing with Mobile Wallet Adapter
- **Company Branding:** Added Quantaliz logo to images directory
- **README Enhancement:** Updated with screenshots and improved documentation
- **Dependency Cleanup:** Removed unused gradle dependency reference

#### x402 Payment Integration (PR #4 - x402)
- **Full x402 Implementation:** Complete HTTP 402 payment protocol support
- **Mobile Wallet Adapter:** Integration with Solflare and MWA-compatible wallets
- **Payment UX:** Enhanced user feedback showing explicit payment results
- **Function Calling:** LLM can trigger `solana_payment()` function autonomously
- **Transaction Building:** Custom transaction builder ensuring correct account ordering
- **Facilitator Integration:** Third-party payment facilitator for on-chain settlement
- **Clear Session:** Added session clearing functionality to settings (Note: behavior inconsistent)
- **ModelManager Updates:** Improved model display in ModelManager view

#### Technical Improvements
- **Working Directory:** Fixed gradle working directory configuration
- **Payment Messaging:** Improved user-facing messages for payment flows
- **Error Handling:** Better handling of payment errors and edge cases
- **Testing:** Successfully tested end-to-end payment flow on Solana devnet with Solflare

## 📖 Additional Resources

- [Android Developer Docs](https://developer.android.com)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Google AI Edge](https://ai.google.dev/edge)
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Solana Developer Docs](https://docs.solana.com)
- [Mobile Wallet Adapter Docs](https://docs.solanamobile.com/android-native/using_mobile_wallet_adapter)
- [x402 Protocol Specification](docs/x402-Header.md)
- [sol4k Library](https://github.com/sol4k/sol4k)
- [Related source code repositories](docs/)

## 🏆 Hackathon Context

This project is a submission for:
- **Cypherpunk 2025** - Solana hackathon focused on privacy and cryptography
- **Hackaroo 2025** - Innovation hackathon for blockchain applications

**Key Innovation:** Combining on-device AI with blockchain payments to create autonomous AI agents that can access paid resources without compromising user privacy.

## 🎨 Branding & Visual Assets

### App Name & Identity
- **Official Name:** SolAIBot
- **Display Name:** SolAIBot (in app/res/values/strings.xml)
- **Tagline:** "Quantaliz brings you on-device AI with Solana extensions"
- **Logo:** Custom vector drawable with updated color scheme

### App Icon
The app uses adaptive icons with separate foreground and background layers:
- **Location:** `app/src/main/res/mipmap-*/ic_launcher*.png`
- **Densities:** hdpi, mdpi, xhdpi, xxhdpi, xxxhdpi
- **Variants:**
  - `ic_launcher.png` - Full color icon
  - `ic_launcher_foreground.png` - Foreground layer
  - `ic_launcher_background.png` - Background layer
  - `ic_launcher_monochrome.png` - Monochrome variant for themed icons

### Vector Drawables
- `drawable/ic_launcher_foreground.xml` - Updated with new branding colors
- `drawable/ic_launcher_background.xml` - Simplified background design
- `drawable/logo.xml` - App logo used in UI
- `drawable/splash_screen_animated_icon.xml` - Animated splash icon

### Screenshots & Marketing
- `images/SolAIBot1-Result.png` - Main app interface showing chat and LLM response
- `images/SolAIBot2-Sign.png` - Payment signing flow with Mobile Wallet Adapter
- `images/Quantaliz.png` - Company logo

### Color Updates
Recent branding update changed app icon colors and theme elements. See:
- `ui/theme/Color.kt` for color definitions
- Commit `ee14fc0` for icon color changes

---

**Last Updated:** 2025-10-15
**Project Version:** 1.0.7
**Maintained by:** Quantaliz PTY LTD
