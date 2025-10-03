# Sol-AI-Bot Project Guidelines

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
└── app/build.gradle.kts                  # App module build config
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
./gradlew assembleRelease
./gradlew bundleRelease

# Testing
./gradlew testDebugUnitTest           # Unit tests
./gradlew connectedDebugAndroidTest   # Instrumented tests

# Install
./gradlew installDebug
```

¡¡NOTE!!: you are running in a VM without android-sdk, these commands will fail. Let the developer handle compilation tasks

### Build Configuration Notes

- **Java Version:** Java 11 (source & target compatibility)
- **Kotlin JVM Target:** 11
- **Kotlin Compiler Args:** `-Xcontext-receivers` enabled
- **ProGuard:** Currently disabled for release builds
- **Deep Linking:** Custom scheme `com.quantaliz.solaibot://`
- **HuggingFace OAuth:** Redirect scheme `com.quantaliz.solaibot://oauth`

## 📝 Code Style & Conventions

### Kotlin Standards

Follow official guidelines:
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)

### Naming Conventions

```kotlin
// ViewModels: Suffix with ViewModel
class LlmChatViewModel : ChatViewModel()
class ModelManagerViewModel : ViewModel()

// Composables: PascalCase
@Composable
fun ChatPanel(...)
@Composable
fun ModelPicker(...)

// Files: Match primary class name
// Exception: Utils.kt for utility collections

// Packages: lowercase, descriptive
com.quantaliz.solaibot.ui.llmchat
com.quantaliz.solaibot.data
```

### Jetpack Compose Guidelines

```kotlin
// Modifier as first optional parameter
@Composable
fun CustomComponent(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) { ... }

// State hoisting pattern
@Composable
fun StatefulComponent() {
    var state by remember { mutableStateOf("") }
    StatelessComponent(
        value = state,
        onValueChange = { state = it }
    )
}

// Preview annotations for composable previews
@Preview(showBackground = true)
@Composable
fun ComponentPreview() { ... }
```

### Documentation

```kotlin
/**
 * KDoc for public APIs.
 *
 * @param input Description of parameter
 * @return Description of return value
 */
fun publicFunction(input: String): Result<String> { ... }

// Single-line comments for implementation details
// Explain complex logic or non-obvious decisions
```

### Error Handling

```kotlin
// Use Result type for operations that can fail
suspend fun downloadModel(): Result<Model> {
    return try {
        Result.success(performDownload())
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Provide user-friendly error messages
viewModelScope.launch {
    modelRepository.load().onFailure { error ->
        showError("Failed to load model: ${error.message}")
    }
}
```

## 🧪 Testing

### Test Structure

```
app/src/
├── test/java/                    # Unit tests (JUnit, MockK)
│   └── com/quantaliz/solaibot/
└── androidTest/java/             # Instrumented tests (Espresso, Compose)
    └── com/quantaliz/solaibot/
```

### Testing Libraries

- **Unit Tests:** JUnit 4/5, MockK for mocking
- **UI Tests:** Compose Test Rule, Espresso
- **Hilt Testing:** hilt-android-testing (1.2.0)

### Testing Guidelines

```kotlin
// Unit test ViewModels and business logic
@HiltAndroidTest
class LlmChatViewModelTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Test
    fun `generateResponse should update messages`() {
        // Test implementation
    }
}

// UI tests for composables
@Test
fun chatPanel_displaysMessages() {
    composeTestRule.setContent {
        ChatPanel(messages = testMessages)
    }
    composeTestRule
        .onNodeWithText("Test message")
        .assertIsDisplayed()
}
```

## 🔐 Security & Privacy

### Critical Security Practices

1. **Sensitive Data Storage**
   - Use Android Keystore exclusively for private keys
   - Never hardcode secrets in source code
   - Encrypt local data using Jetpack Security Crypto

2. **Permissions**
   ```xml
   <!-- Declared permissions -->
   - CAMERA (optional hardware feature)
   - RECORD_AUDIO
   - INTERNET
   - POST_NOTIFICATIONS
   - FOREGROUND_SERVICE & FOREGROUND_SERVICE_DATA_SYNC
   - WAKE_LOCK
   - ACCESS_NETWORK_STATE
   ```
   Request only when needed, explain to users

3. **Network Security**
   - Use HTTPS for all external communications
   - Validate all inputs, especially for blockchain transactions
   - Secure RPC node configuration for Solana

4. **Data Protection**
   ```kotlin
   // Example: Encrypted DataStore
   val encryptedDataStore = DataStoreFactory.create(
       serializer = EncryptedSerializer(),
       produceFile = { context.dataStoreFile("encrypted_data.pb") }
   )
   ```

## 🎨 UI/UX Patterns

### Material Design 3

The app uses Material 3 theming with custom colors and dynamic theming support.

### Key UI Components

```kotlin
// Chat interface
ChatPanel()           // Main chat view
ChatMessage()         // Individual messages
MessageInputText()    // Text input with history
AudioRecorderPanel()  // Audio recording UI
ZoomableImage()       // Image zoom functionality

// Model management
ModelPicker()         // Model selection
ModelItem()           // Model list item
DownloadModelPanel()  // Model download UI
ModelDownloadStatusInfoPanel()

// Common components
MarkdownText()        // Markdown rendering
ConfigDialog()        // Configuration dialogs
ErrorDialog()         // Error display
RotationalLoader()    // Loading animations
```

### Navigation Structure

```kotlin
// Main navigation routes
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object LlmChat : Screen("llm_chat/{taskId}")
    object LlmSingleTurn : Screen("llm_singleturn/{taskId}")
    object ModelManager : Screen("model_manager")
}
```

## 🤖 AI Model Integration

### Model Architecture

```kotlin
// Model data structure
data class Model(
    val name: String,              // Unique identifier
    val displayName: String,       // UI display name
    val info: String,              // Markdown description
    val configs: List<Config>,     // Configurable parameters
    val learnMoreUrl: String,      // Documentation link
    val bestForTaskIds: List<String>,
    val minDeviceMemoryInGb: Float,
    val dataFiles: List<ModelDataFile>,  // Model files to download
    val tags: List<ModelTag>,
    // Runtime state
    var instance: ModelInstance? = null
)

// Model instance (LLM example)
interface LlmModelInstance : ModelInstance {
    fun runInference(
        input: String,
        images: List<Bitmap>,
        audioClips: List<ByteArray>,
        resultListener: (String, Boolean) -> Unit
    )
}
```

### Task System

```kotlin
// Built-in task IDs
object BuiltInTaskId {
    const val LLM_CHAT = "llm_chat"
    const val LLM_PROMPT_LAB = "llm_prompt_lab"
    const val LLM_ASK_IMAGE = "llm_ask_image"
    const val LLM_ASK_AUDIO = "llm_ask_audio"
}

// Task structure
data class Task(
    val id: String,
    val label: String,
    val category: CategoryInfo,
    val icon: ImageVector?,
    val description: String,
    val models: MutableList<Model>,
    val modelNames: List<String>
)
```

### Custom Task Extension

To add a custom task:

1. Implement `CustomTask` interface in `customtasks/common/`
2. Create task-specific ViewModel extending base ViewModels
3. Create UI screens as Composables
4. Register task in dependency injection module
5. See `examplecustomtask/` for reference implementation

## 🔄 Background Processing

### WorkManager Integration

```kotlin
// DownloadWorker for model downloads
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadRepository: DownloadRepository
) : CoroutineWorker(context, params)

// Foreground service configuration
android:foregroundServiceType="dataSync"
```

## 📊 Analytics & Monitoring

### Firebase Analytics

```kotlin
// Configured but optional (google-services plugin apply = false)
firebaseAnalytics?.logEvent(
    FirebaseAnalytics.Event.APP_OPEN,
    bundleOf(
        "app_version" to BuildConfig.VERSION_NAME,
        "os_version" to Build.VERSION.SDK_INT.toString(),
        "device_model" to Build.MODEL
    )
)
```

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

### Specify Technical Context

When asking for code:
- Mention if it's a `@Composable`, `ViewModel`, `Repository`, etc.
- Reference relevant libraries (e.g., "using StateFlow", "with Hilt injection")
- Specify architecture layer (UI, data, domain)

### Examples of Good Prompts

```
"Generate a Jetpack Compose screen for displaying chat messages with
support for text, images, and audio clips. Follow the existing ChatMessage
component pattern in ui/common/chat/"

"Create a ViewModel for managing LLM model downloads that uses the
DownloadRepository and exposes download progress via StateFlow"

"Implement a Room DAO for caching chat history with support for
full-text search and pagination"

"Add a Solana transaction signing flow using Phantom SDK, ensuring
proper error handling for insufficient SOL and network failures"

"How do I observe a StateFlow in a Compose screen and trigger
recomposition when the state changes?"
```

### Context-Aware Questions

When asking about features:
- Reference existing implementations in the codebase
- Ask about Android best practices specific to the use case
- Consider lifecycle awareness and memory management

```
"What's the best way to handle model downloading in the background
while respecting Android's battery optimization and WorkManager constraints?"

"How should I structure the Solana integration to keep private keys
secure using Android Keystore while allowing transaction signing?"

"Suggest how to implement on-chain prompt verification. What data
should be hashed and stored on Solana, and how should verification work?"
```

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

---

**Last Updated:** 2025-10-01
**Project Version:** 1.0.7
**Maintained by:** Quantaliz PTY LTD
