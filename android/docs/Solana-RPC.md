Source: https://docs.solanamobile.com/android-native/rpc-requests

# Solana Mobile Documentation: RPC Client Usage Guide

## RPC CLIENT USAGE GUIDE

To interface with the Solana network, a client needs to construct and send JSON RPC requests to an RPC endpoint.

This guide will teach you how to use the SolanaRpcClient and send these RPC requests.

### ADD DEPENDENCIES

The rpc-core library provides a convenient SolanaRpcClient that implements an API to call these RPC methods and return responses.

* `build.gradle.kts`
```kotlin
dependencies {
    implementation("com.solanamobile:rpc-core:0.2.6")
}
```

### CREATE AN RPC CLIENT

To create an instance of a SolanaRpcClient, pass in:
* an RPC url that the client will send requests.
* a networkDriver used to send HTTP requests.

In this example, we construct an RPC client pointed at devnet and Ktor as a network driver:

```kotlin
import com.solana.rpc.SolanaRpcClient
import com.solana.networking.KtorNetworkDriver

val rpcClient = SolanaRpcClient("https://api.devnet.solana.com", KtorNetworkDriver())
```

### EXAMPLE: FETCHING LATEST BLOCKHASH

Calling the getLatestBlockhash method returns an RpcResponse.
* If successful, the response result will contain a BlockhashResult.
* If an error occurred, the response will contain an RpcError.

```kotlin
import com.solana.rpc.SolanaRpcClient
import com.solana.networking.KtorNetworkDriver

val rpcClient = SolanaRpcClient("https://api.devnet.solana.com", KtorNetworkDriver())

val response = rpcClient.getLatestBlockhash()

if (response.result) {
    println("Latest blockhash: ${response.result.blockhash}")
} else if (response.error) {
    println("Failed to fetch latest blockhash: ${response.error.message}")
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

val response = rpc.sendTransaction(transaction)

if (response.result) {
    println("Transaction signature: ${response.result}")
} else if (response.error) {
    println("Failed to send transaction: ${response.error.message}")
}
```

### NEXT STEPS

These examples are just some of the methods supported by SolanaRpcClient. Here are suggestions to continue learning:

* Read the following guide to learn how to build Solana program instructions and transactions.
* For a complete reference of the RPC methods supported, view the SolanaRpcClient source code and unit tests.
* Read the Building JSON RPC requests deep dive to learn how to create requests for RPC methods that aren't immediately supported by SolanaRpcClient.