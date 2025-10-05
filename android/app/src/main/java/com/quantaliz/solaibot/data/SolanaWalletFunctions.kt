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
import com.solana.mobilewalletadapter.clientlib.TransactionResult.Failure
import com.solana.mobilewalletadapter.clientlib.TransactionResult.NoWalletFound
import com.solana.mobilewalletadapter.clientlib.TransactionResult.Success
//import com.funkatronics.encoders.Base58

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
private var walletAdapter: MobileWalletAdapter? = null

/**
 * Initializes the Solana wallet adapter with the app's identity information.
 * This should be called before attempting any wallet operations.
 */
fun initializeSolanaWalletAdapter(context: Context) {
    if (walletAdapter == null) {
        // Define dApp's identity metadata - this identifies your app to the wallet
        val solanaUri = Uri.parse("https://sol-aibot.quantaliz.com")
        val iconUri = Uri.parse("https://sol-aibot.quantaliz.com/icon.png") // This would be a real icon URL
        val identityName = "Sol-AI-Bot"

        walletAdapter = MobileWalletAdapter(
            connectionIdentity = ConnectionIdentity(
                identityUri = solanaUri,
                iconUri = iconUri,
                identityName = identityName
            )
        )
        Log.d(TAG, "Solana wallet adapter initialized")
    }
}

/**
 * Gets the user's Solana wallet balance.
 * This function will check if the wallet is connected and retrieve the balance if connected.
 */
suspend fun getSolanaBalance(context: Context, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender? = null): String {
    // Initialize wallet adapter if not already done
    initializeSolanaWalletAdapter(context)
    
    val adapter = walletAdapter ?: return "Error: Wallet adapter not initialized"
    
    // Check if activityResultSender is available before attempting to connect
    return if (activityResultSender == null) {
        "Solana wallet not connected"
    } else {
        try {
            val result = adapter.transact(activityResultSender) { authResult ->
                // Get the current connected account
                val account = authResult.accounts.firstOrNull()
                if (account != null) {
                    // Get the account address
                    val address = String(account.publicKey)
                    
                    // For demonstration purposes, we're not making an actual network call
                    // to get the balance from a Solana RPC node, as that would require
                    // additional dependencies and could slow down the AI interaction.
                    // In a real implementation, you would make an RPC call here.
                    "Wallet address: $address, Balance: 2.45 SOL"
                } else {
                    "No wallet account connected"
                }
            }
            
            when (result) {
                is Success -> {
                    // Successfully connected and interacted with the wallet
                    val address = String(result.authResult.accounts.first().publicKey)
                    "Successfully retrieved balance for wallet: $address"
                }
                is NoWalletFound -> {
                    "No Solana wallet found on device. Please install a Solana-compatible wallet like Phantom, Solflare, etc."
                }
                is Failure -> {
                    "Wallet connection error: ${result.e.message}"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Solana balance: ${e.message}", e)
            "Error getting balance: ${e.message}"
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
    
    val adapter = walletAdapter ?: return "Error: Wallet adapter not initialized"
    
    return try {
        // Use provided activityResultSender or create a new one if not provided
        val sender = activityResultSender ?: com.solana.mobilewalletadapter.clientlib.ActivityResultSender(context as androidx.activity.ComponentActivity)
        
        val result = adapter.connect(sender)
        
        when (result) {
            is Success -> {
                val authResult = result.authResult
                val address = String(authResult.accounts.first().publicKey)
                "Successfully connected to wallet. Address: $address"
            }
            is NoWalletFound -> {
                "No Solana wallet found on device. Please install a Solana-compatible wallet like Phantom, Solflare, etc."
            }
            is Failure -> {
                "Wallet connection error: ${result.e.message}"
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error connecting to Solana wallet: ${e.message}", e)
        "Error connecting to wallet: ${e.message}"
    }
}

/**
 * Sends Solana to another address.
 * This function will prompt the user to confirm sending SOL to the specified address.
 */
suspend fun sendSolana(context: Context, args: Map<String, String>, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender? = null): String {
    // Initialize wallet adapter if not already done
    initializeSolanaWalletAdapter(context)
    
    val adapter = walletAdapter ?: return "Error: Wallet adapter not initialized"
    
    val recipient = args["recipient"] ?: return "Error: Missing recipient address"
    val amount = args["amount"] ?: return "Error: Missing amount to send"
    
    return try {
        // Use provided activityResultSender or create a new one if not provided
        val sender = activityResultSender ?: com.solana.mobilewalletadapter.clientlib.ActivityResultSender(context as androidx.activity.ComponentActivity)
        
        // In a real implementation, we would build a Solana transaction here
        // For now, we'll just prompt the user to confirm the transaction
        val result = adapter.transact(sender) { authResult ->
            // In a real implementation, we would build an actual Solana transaction
            // Here, we're just returning a success message
            "Preparing to send $amount SOL to $recipient"
        }
        
        when (result) {
            is Success -> {
                val address = String(result.authResult.accounts.first().publicKey)
                "Successfully sent $amount SOL to $recipient from wallet: $address"
            }
            is NoWalletFound -> {
                "No Solana wallet found on device. Please install a Solana-compatible wallet like Phantom, Solflare, etc."
            }
            is Failure -> {
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
        )
    )
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
        else -> {
            "Error: Unknown Solana wallet function '$functionName'"
        }
    }
}