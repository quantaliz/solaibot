Source: https://docs.solanamobile.com/android-native/rpc-requests
Extended and updated for current rpc-core library implementation

# Solana Mobile Documentation: RPC Client Usage Guide

## RPC CLIENT USAGE GUIDE

To interface with the Solana network, a client needs to construct and send JSON RPC requests to an RPC endpoint.

This guide will teach you how to use the SolanaRpcClient and send these RPC requests.

### ADD DEPENDENCIES

The rpc-core library provides a convenient SolanaRpcClient that implements an API to call these RPC methods and return responses.

* `build.gradle.kts`
```kotlin
dependencies {
    // Check Maven Central for the latest stable release
    // Based on git tags, latest version may be 0.2.10, but verify availability
    implementation("com.solanamobile:rpc-solana:0.2.10")    // Solana-specific RPC client
    implementation("com.solanamobile:rpc-core:0.2.10")     // Core RPC functionality
    implementation("com.solanamobile:rpc-ktordriver:0.2.10") // Ktor networking driver
    implementation("com.solanamobile:rpc-okiodriver:0.2.10") // OkHttp networking driver
}
```

**Note:** For the most current version, check:
* [Maven Central Repository](https://search.maven.org/search?q=g:com.solanamobile)
* The project's GitHub releases page: Look for tags like v0.2.10, v0.2.9, etc.

Choose the appropriate module:
* `rpc-solana`: For Solana blockchain-specific RPC calls (includes dependencies for Solana)
* `rpc-core`: For core JSON RPC functionality (if building custom RPC clients)
* `rpc-ktordriver`: For Ktor-based HTTP networking (multiplatform support)
* `rpc-okiodriver`: For OkHttp-based HTTP networking (JVM/Android)

You may also need to ensure your repositories block in the project-level build.gradle includes Maven Central:

```kotlin
// In project-level build.gradle (or build.gradle.kts)
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

If using SNAPSHOT versions, you may need to add Sonatype snapshots repository:
```kotlin
repositories {
    google()
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        content {
            includeGroup("com.solanamobile")
        }
    }
}
```

### CREATE AN RPC CLIENT

To create an instance of a SolanaRpcClient, pass in:
* an RPC url that the client will send requests.
* a networkDriver used to send HTTP requests.
* (optional) default transaction options

In this example, we construct an RPC client pointed at devnet and Ktor as a network driver:

```kotlin
import com.solana.rpc.SolanaRpcClient
import com.solana.networking.KtorNetworkDriver

val rpcClient = SolanaRpcClient("https://api.devnet.solana.com", KtorNetworkDriver())
// Or with custom transaction options
val rpcClient = SolanaRpcClient(
    "https://api.devnet.solana.com",
    KtorNetworkDriver(),
    TransactionOptions() // optional, uses defaults if not provided
)
```

### EXAMPLE: FETCHING LATEST BLOCKHASH

Calling the getLatestBlockhash method returns an RpcResponse<BlockhashResponse>.
* If successful, the response result will contain a BlockhashResponse with blockhash and context.
* If an error occurred, the response will contain an RpcError.

```kotlin
import com.solana.rpc.SolanaRpcClient
import com.solana.networking.KtorNetworkDriver

val rpcClient = SolanaRpcClient("https://api.devnet.solana.com", KtorNetworkDriver())

val response = rpcClient.getLatestBlockhash()

response.result?.let { blockhashResponse ->
    println("Latest blockhash: ${blockhashResponse.value.blockhash}")
    println("Context slot: ${blockhashResponse.context.slot}")
} ?: run {
    response.error?.let { error ->
        println("Failed to fetch latest blockhash: ${error.message}")
    }
}
```

### EXAMPLE: SENDING A TRANSACTION

To submit a transaction to the RPC, use the sendTransaction method.
* If successful, the response result will contain a transaction signature string.
* If an error occurred, the response will contain an RpcError.

```kotlin
import com.solana.rpc.SolanaRpcClient
import com.solana.networking.KtorNetworkDriver

val rpcClient = SolanaRpcClient("https://api.devnet.solana.com", KtorNetworkDriver())

val transaction = Transaction(/* ... */)

/* ...sign the transaction... */

val response = rpcClient.sendTransaction(transaction)

response.result?.let { signature ->
    println("Transaction signature: $signature")
} ?: run {
    response.error?.let { error ->
        println("Failed to send transaction: ${error.message}")
    }
}
```

### EXAMPLE: SENDING AND CONFIRMING A TRANSACTION

You can also send and confirm a transaction in one call using sendAndConfirmTransaction:

```kotlin
import com.solana.rpc.SolanaRpcClient
import com.solana.networking.KtorNetworkDriver

val rpcClient = SolanaRpcClient("https://api.devnet.solana.com", KtorNetworkDriver())

val transaction = Transaction(/* ... */)

/* ...sign the transaction... */

val response = rpcClient.sendAndConfirmTransaction(transaction)

response.result?.let { isConfirmed ->
    if (isConfirmed) {
        println("Transaction confirmed successfully")
    } else {
        println("Transaction confirmation timed out")
    }
} ?: run {
    response.error?.let { error ->
        println("Failed to send transaction: ${error.message}")
    }
}
```

### EXAMPLE: GETTING ACCOUNT INFO

The library supports both simple account info retrieval and custom deserialization:

```kotlin
import com.solana.rpc.SolanaRpcClient
import com.solana.networking.KtorNetworkDriver
import com.solana.publickey.SolanaPublicKey

val rpcClient = SolanaRpcClient("https://api.devnet.solana.com", KtorNetworkDriver())
val accountPublicKey = SolanaPublicKey("AccountPublicKeyHere")

// Get basic account info
val response = rpcClient.getAccountInfo(accountPublicKey)

response.result?.let { accountInfo ->
    println("Account owner: ${accountInfo.owner}")
    println("Account lamports: ${accountInfo.lamports}")
} ?: run {
    response.error?.let { error ->
        println("Failed to get account info: ${error.message}")
    }
}

// Get account info with custom deserialization
@kotlinx.serialization.Serializable
data class CustomAccountData(
    val value1: String,
    val value2: Int
)

val deserializedResponse = rpcClient.getAccountInfo<CustomAccountData>(accountPublicKey)

deserializedResponse.result?.let { accountData ->
    println("Custom value 1: ${accountData.data.value1}")
    println("Custom value 2: ${accountData.data.value2}")
} ?: run {
    deserializedResponse.error?.let { error ->
        println("Failed to get account info: ${error.message}")
    }
}
```

### EXAMPLE: GETTING BALANCE

```kotlin
import com.solana.rpc.SolanaRpcClient
import com.solana.networking.KtorNetworkDriver
import com.solana.publickey.SolanaPublicKey

val rpcClient = SolanaRpcClient("https://api.devnet.solana.com", KtorNetworkDriver())
val address = SolanaPublicKey("AccountPublicKeyHere")

val response = rpcClient.getBalance(address)

response.result?.let { balance ->
    println("Balance in lamports: ${balance.value}")
} ?: run {
    response.error?.let { error ->
        println("Failed to get balance: ${error.message}")
    }
}
```

### NEXT STEPS

These examples are just some of the methods supported by SolanaRpcClient. The client provides many more RPC methods including:

* `getMultipleAccounts` - Get information for multiple accounts
* `getProgramAccounts` - Get all accounts owned by a program
* `getSignatureStatuses` - Get the statuses of a list of signatures
* `getMinBalanceForRentExemption` - Get the minimum balance needed to exempt an account from rent
* `requestAirdrop` - Request an airdrop of SOL to an address

For a complete reference of the RPC methods supported, view the SolanaRpcClient source code and unit tests.

Additional features include:
* Transaction confirmation utilities with configurable commitment levels
* Custom deserialization for on-chain account data
* Support for various commitment levels (processed, confirmed, finalized)

### TROUBLESHOOTING DEPENDENCY ISSUES

If you encounter "Could not find" errors when trying to sync the dependency:

1. **Verify the repository configuration:** Make sure you have added all required repositories:
   ```kotlin
   repositories {
       google()
       mavenCentral()
   }
   ```

2. **Check for the latest version:** Visit Maven Central to find the most recent published version.

3. **For Android projects:** Make sure you're using the correct version code that exists in the repositories.

4. **Clear Gradle cache:** Try clearing your Gradle cache if you've recently updated your dependencies:
   ```bash
   ./gradlew clean
   # Then in Android Studio: File → Invalidate Caches and Restart
   ```