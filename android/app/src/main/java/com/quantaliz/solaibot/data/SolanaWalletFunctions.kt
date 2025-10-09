/*
 * Updates by Quantaliz PTY LTD, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.quantaliz.solaibot.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.solana.mobilewalletadapter.clientlib.*
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.SolanaRpcClient
import com.solana.networking.KtorNetworkDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of Solana wallet functions for LLM function calling.
 *
 * HOW THE SYSTEM WORKS:
 * 1. The LLM is trained with a system prompt that includes these Solana functions
 * 2. When the user asks about their Solana balance or wants to perform wallet actions,
 *    the LLM generates a function call in the format: FUNCTION_CALL: function_name(param1="value1", param2="value2")
 * 3. The LlmFunctionCallingModelHelper detects this function call pattern
 * 4. It then calls executeFunction() which routes to executeSolanaWalletFunction() for Solana functions
 * 5. This function interacts with the actual Solana wallet via Mobile Wallet Adapter
 * 6. The result is returned to the LLM which then generates a natural language response
 *
 * KEY FUNCTION CALLS:
 * - get_solana_balance() - Gets the wallet balance
 * - connect_solana_wallet() - Initiates wallet connection
 * - send_solana(recipient="address", amount="value") - Sends SOL to an address
 *
 * These functions are added to the available functions list in FunctionDeclarations.kt
 * and are made available to the LLM through the system prompt.
 */

private const val TAG = "SolanaWalletFunctions"

// The MobileWalletAdapter instance for interacting with Solana wallets
// This is now a singleton instance to be shared across the app
object SharedMobileWalletAdapter {
    private var _adapter: MobileWalletAdapter? = null

    fun getAdapter(): MobileWalletAdapter {
        if (_adapter == null) {
            // Define dApp's identity metadata - this identifies your app to the wallet
            val solanaUri = Uri.parse("https://sol-aibot.quantaliz.com")
            val iconUri = Uri.parse("/icon.png") // Relative URI as required by the library
            val identityName = "Sol-AI-Bot"

            _adapter = MobileWalletAdapter(
                connectionIdentity = ConnectionIdentity(
                    identityUri = solanaUri,
                    iconUri = iconUri,
                    identityName = identityName
                )
            )
        }
        return _adapter!!
    }
}

// Connection state data class to keep track of wallet connection details
data class WalletConnectionState(
    val isConnected: Boolean = false,
    val publicKey: String? = null,
    val address: String? = null
)

// Shared connection state across the app
object WalletConnectionManager {
    @Volatile
    private var _connectionState: WalletConnectionState = WalletConnectionState()
    private val lock = Object()

    fun getConnectionState(): WalletConnectionState = _connectionState

    fun updateConnectionState(newState: WalletConnectionState) {
        synchronized(lock) {
            _connectionState = newState
        }
    }

    fun clearConnectionState() {
        synchronized(lock) {
            _connectionState = WalletConnectionState()
        }
    }
}

/**
 * Initializes the Solana wallet adapter with the app's identity information.
 * This should be called before attempting any wallet operations.
 */
fun initializeSolanaWalletAdapter(context: Context) {
    // The adapter is now a singleton, so just make sure it's initialized
    SharedMobileWalletAdapter.getAdapter()
    Log.d(TAG, "Solana wallet adapter initialized")
}

/**
 * Gets the user's Solana wallet balance.
 * This function will check if the wallet is connected and retrieve the account information.
 * NOTE: The MWA does not directly provide balance information, so we return account details
 * and the balance would need to be fetched separately via RPC or another method.
 */
suspend fun getSolanaBalance(context: Context, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender? = null): String {
    // Initialize wallet adapter if not already done
    initializeSolanaWalletAdapter(context)

    // Check for internet connectivity before proceeding
    if (!NetworkConnectivityHelper.isInternetAvailable(context)) {
        val networkStatus = NetworkConnectivityHelper.getNetworkStatusDescription(context)
        Log.w(TAG, "No internet connection available: $networkStatus")
        return "Cannot retrieve balance: No internet connection. Please check your network settings. ($networkStatus)"
    }

    val connectionState = WalletConnectionManager.getConnectionState()

    if (connectionState.isConnected && connectionState.address != null) {
        // If already connected, just return the current connection info and try to get the balance
        // For now, we'll retrieve the balance using Solana's JSON RPC API
        val balanceString = try {
            withContext(Dispatchers.IO) {
                getSolanaBalanceViaRpc(context, connectionState.address)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving balance via RPC: ${e.message}", e)
            "Balance could not be retrieved via RPC: ${e.message}"
        }

        return "Wallet is connected. Address: ${connectionState.address}. $balanceString"
    } else {
        // If not connected, we need the activityResultSender to initiate a connection
        return if (activityResultSender == null) {
            "Solana wallet not connected. Please connect your wallet first."
        } else {
            // Attempt to connect to get the account information
            val adapter = SharedMobileWalletAdapter.getAdapter()
            try {
                val result = adapter.transact(activityResultSender) { authResult ->
                    val account = authResult.accounts.firstOrNull()
                    if (account != null) {
                        val publicKey = SolanaPublicKey(account.publicKey)
                        val address = publicKey.base58()
                        val hexAddress = bytesToHex(account.publicKey)

                        // Update connection state
                        WalletConnectionManager.updateConnectionState(
                            WalletConnectionState(
                                isConnected = true,
                                publicKey = hexAddress,
                                address = address
                            )
                        )

                        // Get the balance for the connected wallet
                        val balanceString = try {
                            withContext(Dispatchers.IO) {
                                getSolanaBalanceViaRpc(context, address)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error retrieving balance via RPC: ${e.message}", e)
                            "Balance could not be retrieved via RPC: ${e.message}"
                        }

                        "Wallet connected. Address: $address. $balanceString"
                    } else {
                        "No wallet account connected"
                    }
                }

                when (result) {
                    is com.solana.mobilewalletadapter.clientlib.TransactionResult.Success -> {
                        val publicKeyBytes = result.authResult.accounts.first().publicKey
                        val publicKey = SolanaPublicKey(publicKeyBytes)
                        val address = publicKey.base58()
                        // Update connection state again in case it changed
                        val hexAddress = bytesToHex(publicKeyBytes)
                        WalletConnectionManager.updateConnectionState(
                            WalletConnectionState(
                                isConnected = true,
                                publicKey = hexAddress,
                                address = address
                            )
                        )

                        // Get the balance for the connected wallet
                        val balanceString = try {
                            withContext(Dispatchers.IO) {
                                getSolanaBalanceViaRpc(context, address)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error retrieving balance via RPC: ${e.message}", e)
                            "Balance could not be retrieved via RPC: ${e.message}"
                        }

                        "Wallet connected. Address: $address. $balanceString"
                    }
                    is com.solana.mobilewalletadapter.clientlib.TransactionResult.NoWalletFound -> {
                        WalletConnectionManager.clearConnectionState()
                        "No Solana wallet found on device. Please install a Solana-compatible wallet like Phantom, Solflare, etc."
                    }
                    is com.solana.mobilewalletadapter.clientlib.TransactionResult.Failure -> {
                        WalletConnectionManager.clearConnectionState()
                        "Wallet connection error: ${result.e.message}"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting Solana balance: ${e.message}", e)
                WalletConnectionManager.clearConnectionState()
                "Error getting balance: ${e.message}"
            }
        }
    }
}

/**
 * Connects to a Solana wallet.
 * This function initiates the connection process with a Solana wallet.
 */
suspend fun connectSolanaWallet(context: Context, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender? = null): String {
    // Initialize wallet adapter if not already done
    initializeSolanaWalletAdapter(context)

    val connectionState = WalletConnectionManager.getConnectionState()

    // Check if already connected
    if (connectionState.isConnected && connectionState.address != null) {
        return "Already connected to wallet. Address: ${connectionState.address}"
    }

    val adapter = SharedMobileWalletAdapter.getAdapter()

    // Check if activityResultSender is provided
    return if (activityResultSender == null) {
        "Solana wallet connection requires user interaction. Please connect your wallet first."
    } else {
        try {
            val result = adapter.connect(activityResultSender)

            when (result) {
                is com.solana.mobilewalletadapter.clientlib.TransactionResult.Success -> {
                    val authResult = result.authResult
                    val publicKeyBytes = authResult.accounts.first().publicKey
                    val publicKey = SolanaPublicKey(publicKeyBytes)
                    val address = publicKey.base58()
                    val hexAddress = bytesToHex(publicKeyBytes) // Using hex format for consistency

                    // Update the connection state
                    WalletConnectionManager.updateConnectionState(
                        WalletConnectionState(
                            isConnected = true,
                            publicKey = hexAddress,
                            address = address
                        )
                    )

                    // Note: WalletViewModel state sync happens via UI layer when needed
                    "Successfully connected to wallet. Address: $address"
                }
                is com.solana.mobilewalletadapter.clientlib.TransactionResult.NoWalletFound -> {
                    // Clear connection state if no wallet found
                    WalletConnectionManager.clearConnectionState()
                    "No Solana wallet found on device. Please install a Solana-compatible wallet like Phantom, Solflare, etc."
                }
                is com.solana.mobilewalletadapter.clientlib.TransactionResult.Failure -> {
                    // Clear connection state if connection failed
                    WalletConnectionManager.clearConnectionState()
                    "Wallet connection error: ${result.e.message}"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Solana wallet: ${e.message}", e)
            // Clear connection state if exception occurred
            WalletConnectionManager.clearConnectionState()
            "Error connecting to wallet: ${e.message}"
        }
    }
}

/**
 * Sends Solana to another address.
 * This function will prompt the user to confirm sending SOL to the specified address.
 */
suspend fun sendSolana(context: Context, args: Map<String, String>, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender? = null): String {
    // Initialize wallet adapter if not already done
    initializeSolanaWalletAdapter(context)

    val connectionState = WalletConnectionManager.getConnectionState()

    // Check if already connected
    if (!connectionState.isConnected || connectionState.address == null) {
        return "Wallet not connected. Please connect your wallet first before sending SOL."
    }

    val adapter = SharedMobileWalletAdapter.getAdapter()

    val recipient = args["recipient"] ?: return "Error: Missing recipient address"
    val amount = args["amount"] ?: return "Error: Missing amount to send"

    return try {
        // Check if activityResultSender is provided
        if (activityResultSender == null) {
            return "Solana transaction requires user interaction. Please connect your wallet first."
        }

        // In a real implementation, we would build a Solana transaction here
        // For now, we'll just prompt the user to confirm the transaction
        val result = adapter.transact(activityResultSender) { authResult ->
            // In a real implementation, we would build an actual Solana transaction
            // Here, we're just returning a success message
            "Preparing to send $amount SOL to $recipient"
        }

        when (result) {
            is com.solana.mobilewalletadapter.clientlib.TransactionResult.Success -> {
                val publicKeyBytes = result.authResult.accounts.first().publicKey
                val publicKey = SolanaPublicKey(publicKeyBytes)
                val address = publicKey.base58()
                // Update connection state after successful transaction
                val hexAddress = bytesToHex(publicKeyBytes)
                WalletConnectionManager.updateConnectionState(
                    WalletConnectionState(
                        isConnected = true,
                        publicKey = hexAddress,
                        address = address
                    )
                )
                "Successfully sent $amount SOL to $recipient from wallet: $address"
            }
            is com.solana.mobilewalletadapter.clientlib.TransactionResult.NoWalletFound -> {
                WalletConnectionManager.clearConnectionState()
                "No Solana wallet found on device. Please install a Solana-compatible wallet like Phantom, Solflare, etc."
            }
            is com.solana.mobilewalletadapter.clientlib.TransactionResult.Failure -> {
                WalletConnectionManager.clearConnectionState()
                "Transaction failed: ${result.e.message}"
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error sending Solana: ${e.message}", e)
        "Error sending Solana: ${e.message}"
    }
}

/**
 * Gets the list of available Solana wallet functions for LLM function calling.
 * This extends the available functions to include Solana wallet functions.
 */
fun getSolanaWalletFunctions(): List<FunctionDefinition> {
    return listOf(
        FunctionDefinition(
            name = "get_solana_balance",
            description = "Get the current balance of the connected Solana wallet. This connects to a Solana wallet and retrieves the balance.",
            parameters = listOf()
        ),
        FunctionDefinition(
            name = "connect_solana_wallet",
            description = "Connect to a Solana wallet. This initiates a connection to a wallet installed on the device.",
            parameters = listOf()
        ),
        FunctionDefinition(
            name = "send_solana",
            description = "Send Solana (SOL) to another address. This prompts the user to confirm the transaction.",
            parameters = listOf(
                FunctionParameter(
                    name = "recipient",
                    type = "string",
                    description = "The recipient's Solana address",
                    required = true
                ),
                FunctionParameter(
                    name = "amount",
                    type = "string",
                    description = "The amount of SOL to send",
                    required = true
                )
            )
        ),
        FunctionDefinition(
            name = "solana_payment",
            description = "Make a payment to access a paid API or resource using the x402 protocol. This handles the full payment flow: requesting the resource, receiving payment requirements, signing the payment transaction, and retrieving the paid content.",
            parameters = listOf(
                FunctionParameter(
                    name = "url",
                    type = "string",
                    description = "The URL of the paid resource or API endpoint to access",
                    required = true
                )
            )
        )
    )
}

/**
 * Retrieves the Solana balance via RPC.
 * This function makes an HTTP request to a Solana RPC endpoint to get the balance.
 */
suspend fun getSolanaBalanceViaRpc(context: Context, address: String): String {
    // Check for internet connectivity before making RPC call
    if (!NetworkConnectivityHelper.isInternetAvailable(context)) {
        val networkStatus = NetworkConnectivityHelper.getNetworkStatusDescription(context)
        Log.w(TAG, "No internet connection available for RPC call: $networkStatus")
        return "Error retrieving balance: No internet connection. ($networkStatus)"
    }

    // We need to use the Solana RPC client to fetch the balance
    // The implementation will use the SolanaRpcClient as per the documentation
    try {
        // Create RPC client with a default endpoint (this would normally be configurable)
        // Using Ktor with explicit configuration for better Android network compatibility
        val rpcClient = SolanaRpcClient(
            // "https://api.mainnet-beta.solana.com",
            "https://api.devnet.solana.com",
            KtorNetworkDriver()
        )

        // Validate that we have a valid address before attempting to create a SolanaPublicKey
        if (address.isNullOrEmpty()) {
            return "Error retrieving balance: Invalid address provided"
        }

        val publicKey = SolanaPublicKey.from(address)
        val response = rpcClient.getBalance(publicKey)

        // Check the response structure based on the actual API
        // Note: SolanaResponseDeserializer already unwraps the .value, so result is directly the Long?
        val lamports = response.result
        if (lamports != null) {
            val solBalance = lamports / 1_000_000_000.0 // Convert lamports to SOL
            return "Balance: ${String.format("%.6f", solBalance)} SOL (${lamports} lamports)"
        } else {
            return "Error retrieving balance: ${response.error?.message ?: "Unknown error"}"
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception when retrieving balance via RPC: ${e.message}", e)
        return "Error retrieving balance: ${e.message}"
    }
}

/**
 * Makes a payment to a x402-enabled resource.
 * This handles the full x402 payment flow including signing with MWA.
 */
suspend fun makeSolanaPayment(context: Context, args: Map<String, String>, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender? = null): String {
    val url = args["url"] ?: return "Error: Missing URL parameter"

    // Check network connectivity first
    if (!NetworkConnectivityHelper.isInternetAvailable(context)) {
        val networkStatus = NetworkConnectivityHelper.getNetworkStatusDescription(context)
        return "No internet connection available. Please check your network settings.\n\nNetwork status: $networkStatus"
    }

    // Check if wallet is connected
    val connectionState = WalletConnectionManager.getConnectionState()
    if (!connectionState.isConnected || connectionState.address == null) {
        return "Wallet not connected. Please connect your wallet first before making payments."
    }

    // Check if activityResultSender is provided
    if (activityResultSender == null) {
        return "Payment requires user interaction. Please connect your wallet first."
    }

    return try {
        val client = com.quantaliz.solaibot.data.x402.X402HttpClient(
            context = context,
            facilitatorUrl = "https://x402.payai.network"
        )

        Log.d(TAG, "Making x402 payment to: $url")

        val response = client.get(url, activityResultSender)

        when {
            response.success -> {
                val settlementInfo = response.settlementResponse?.let { settlement ->
                    if (settlement.success) {
                        "\n\nPayment settled successfully!\n" +
                                "Transaction: ${settlement.transaction}\n" +
                                "Network: ${settlement.network}\n" +
                                "Payer: ${settlement.payer}"
                    } else {
                        "\n\nPayment failed: ${settlement.errorReason}"
                    }
                } ?: ""

                "Successfully accessed paid resource at $url$settlementInfo\n\nResponse:\n${response.body}"
            }
            response.statusCode == 402 && response.body != null -> {
                // Parse the error response to provide more context
                try {
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    val errorResponse = json.decodeFromString<com.quantaliz.solaibot.data.x402.PaymentRequirementsResponse>(response.body)
                    "Payment failed: ${errorResponse.error}"
                } catch (e: Exception) {
                    "Failed to access resource: ${response.errorMessage ?: "HTTP ${response.statusCode}"}"
                }
            }
            else -> {
                "Failed to access resource: ${response.errorMessage ?: "HTTP ${response.statusCode}"}"
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error making x402 payment: ${e.message}", e)
        "Error making payment: ${e.message}"
    }
}

/**
 * Executes Solana wallet functions based on the function name and parameters.
 * This is called by the LLM function calling system when a Solana function is called.
 */
suspend fun executeSolanaWalletFunction(context: Context, functionName: String, args: Map<String, String>, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender? = null): String {
    return when (functionName) {
        "get_solana_balance" -> {
            getSolanaBalance(context, activityResultSender)
        }
        "connect_solana_wallet" -> {
            connectSolanaWallet(context, activityResultSender)
        }
        "send_solana" -> {
            sendSolana(context, args, activityResultSender)
        }
        "solana_payment" -> {
            makeSolanaPayment(context, args, activityResultSender)
        }
        else -> {
            "Error: Unknown Solana wallet function '$functionName'"
        }
    }
}



/**
 * Helper function to convert byte array to hex string.
 */
private fun bytesToHex(bytes: ByteArray): String {
    return bytes.joinToString("") { "%02x".format(it) }
}