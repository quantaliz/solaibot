# Build/Lint/Test Commands
- Build: `./gradlew assembleDebug` or `./gradlew assembleRelease`
- Test: `./gradlew testDebugUnitTest` (unit tests), `./gradlew connectedDebugAndroidTest` (instrumented)
- Single test: `./gradlew testDebugUnitTest --tests "*TestClass.testMethod"`
- Install: `./gradlew installDebug`
- Clean: `./gradlew clean`

# Code Style Guidelines
- **Kotlin style**: `kotlin.code.style=official` (gradle.properties)
- **Naming**: ViewModels end with `ViewModel`, Composables PascalCase, files match class names
- **Imports**: Alphabetical, no unused imports
- **Error handling**: Use `Result<T>` type, avoid exceptions for expected failures
- **Types**: Prefer `val` over `var`, use data classes for immutable data
- **Constants**: `const val` for compile-time constants, `private const val TAG` for logging
- **Documentation**: KDoc for public APIs, single-line comments for implementation details
- **Architecture**: MVVM with Hilt DI, Jetpack Compose UI, Coroutines/Flow for async
- **Formatting**: 4-space indentation, 120 char line limit, trailing commas in multi-line structures
