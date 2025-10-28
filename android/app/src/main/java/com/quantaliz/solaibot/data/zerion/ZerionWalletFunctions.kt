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

package com.quantaliz.solaibot.data.zerion

import android.content.Context
import android.util.Log
import com.quantaliz.solaibot.data.FunctionDefinition
import com.quantaliz.solaibot.data.FunctionParameter
import com.quantaliz.solaibot.data.NetworkConnectivityHelper
import com.quantaliz.solaibot.data.WalletConnectionManager

/**
 * Zerion wallet functions for LLM function calling.
 *
 * These functions wrap the Zerion API to provide wallet data:
 * - Portfolio overview (total balance, asset distribution)
 * - Token positions (balances, prices, values)
 * - Transaction history (recent activity, payment verification)
 *
 * Replaces direct RPC calls with Zerion's aggregated data API.
 */

private const val TAG = "ZerionWalletFunctions"

// Singleton Zerion API client
// API key is configured in ZerionConfig.kt
object ZerionClientHolder {
    private var _client: ZerionApiClient? = null

    fun getClient(apiKey: String? = null): ZerionApiClient {
        if (_client == null) {
            // Use API key from ZerionConfig (will fail if not configured)
            // Or use provided apiKey parameter (for testing/override)
            val key = apiKey ?: ZerionConfig.API_KEY
            _client = ZerionApiClient(
                apiKey = key,
                baseUrl = ZerionConfig.BASE_URL
            )
        }
        return _client!!
    }

    fun setApiKey(apiKey: String) {
        _client = ZerionApiClient(
            apiKey = apiKey,
            baseUrl = ZerionConfig.BASE_URL
        )
    }
}

/**
 * Get wallet portfolio overview using Zerion API.
 * Shows total balance, asset distribution by type and chain.
 */
suspend fun getZerionPortfolio(context: Context): String {
    // Check network connectivity
    if (!NetworkConnectivityHelper.isInternetAvailable(context)) {
        val networkStatus = NetworkConnectivityHelper.getNetworkStatusDescription(context)
        return "ERROR:NO_INTERNET:Cannot retrieve portfolio. No internet connection available. Please check your network settings and try again. ($networkStatus)"
    }

    // Check if wallet is connected
    val connectionState = WalletConnectionManager.getConnectionState()
    if (!connectionState.isConnected || connectionState.address == null) {
        return "ERROR:WALLET_NOT_CONNECTED:Wallet not connected. Please connect your Solana wallet first to view your portfolio."
    }

    val address = connectionState.address

    return try {
        val client = ZerionClientHolder.getClient()
        val result = client.getWalletPortfolio(address)

        if (result.isFailure) {
            val error = result.exceptionOrNull()
            Log.e(TAG, "Failed to fetch portfolio", error)
            return "Error fetching portfolio: ${error?.message ?: "Unknown error"}"
        }

        val portfolio = result.getOrNull()
            ?: return "Error: Empty portfolio response"

        val total = portfolio.data.attributes.total
        val totalValue = total.value?.let { String.format("$%.2f", it) } ?: "N/A"

        val distribution = portfolio.data.attributes.positionsDistributionByType
        val distributionText = distribution?.entries?.joinToString("\n") { (type, value) ->
            val typeValue = value.value?.let { String.format("$%.2f", it) } ?: "N/A"
            "  - $type: $typeValue"
        } ?: "No distribution data"

        """
        Portfolio for ${address.take(8)}...${address.takeLast(6)}:
        Total Value: $totalValue

        Distribution by Type:
        $distributionText
        """.trimIndent()

    } catch (e: Exception) {
        Log.e(TAG, "Exception getting portfolio", e)
        "Error getting portfolio: ${e.message}"
    }
}

/**
 * Get wallet token positions (balances) using Zerion API.
 * This replaces the direct RPC balance call with Zerion's enriched data.
 */
suspend fun getZerionBalance(context: Context, args: Map<String, String> = emptyMap()): String {
    // Check network connectivity
    if (!NetworkConnectivityHelper.isInternetAvailable(context)) {
        val networkStatus = NetworkConnectivityHelper.getNetworkStatusDescription(context)
        return "ERROR:NO_INTERNET:Cannot retrieve balance. No internet connection available. Please check your network settings and try again. ($networkStatus)"
    }

    // Check if wallet is connected
    val connectionState = WalletConnectionManager.getConnectionState()
    if (!connectionState.isConnected || connectionState.address == null) {
        return "ERROR:WALLET_NOT_CONNECTED:Wallet not connected. Please connect your Solana wallet first to view your token balances."
    }

    val address = connectionState.address
    val tokenSymbol = args["token"]?.uppercase()

    return try {
        val client = ZerionClientHolder.getClient()
        val result = client.getWalletPositions(address)

        if (result.isFailure) {
            val error = result.exceptionOrNull()
            Log.e(TAG, "Failed to fetch positions", error)
            return "Error fetching balance: ${error?.message ?: "Unknown error"}"
        }

        val positions = result.getOrNull()
            ?: return "Error: Empty positions response"

        if (positions.data.isEmpty()) {
            return "INFO:NO_TOKENS:This wallet currently has no token positions. The wallet may be empty or only contain NFTs which are not displayed in token balances."
        }

        // If specific token requested, filter for it
        if (tokenSymbol != null) {
            val matchingPosition = positions.data.find { position ->
                position.attributes.fungibleInfo?.symbol?.uppercase() == tokenSymbol
            }

            if (matchingPosition == null) {
                return "Token $tokenSymbol not found in wallet."
            }

            val attrs = matchingPosition.attributes
            val symbol = attrs.fungibleInfo?.symbol ?: "Unknown"
            val quantity = attrs.quantity.float
            val value = attrs.value?.let { String.format("$%.2f", it) } ?: "N/A"

            return """
            $symbol Balance:
            Amount: ${String.format("%.6f", quantity)} $symbol
            Value: $value
            """.trimIndent()
        }

        // Otherwise, show all positions
        val positionsText = positions.data.take(10).joinToString("\n") { position ->
            val attrs = position.attributes
            val symbol = attrs.fungibleInfo?.symbol ?: "Unknown"
            val quantity = attrs.quantity.float
            val value = attrs.value?.let { String.format("$%.2f", it) } ?: "N/A"
            val verified = if (attrs.fungibleInfo?.flags?.verified == true) "✓" else ""

            "  $verified $symbol: ${String.format("%.6f", quantity)} ($value)"
        }

        val totalCount = positions.data.size
        val showingText = if (totalCount > 10) "\n\n(Showing top 10 of $totalCount tokens)" else ""

        """
        Wallet: ${address.take(8)}...${address.takeLast(6)}

        Token Balances:
        $positionsText$showingText
        """.trimIndent()

    } catch (e: Exception) {
        Log.e(TAG, "Exception getting balance", e)
        "Error getting balance: ${e.message}"
    }
}

/**
 * Get wallet transaction history using Zerion API.
 * Shows recent transactions with details.
 */
suspend fun getZerionTransactions(context: Context, args: Map<String, String> = emptyMap()): String {
    // Check network connectivity
    if (!NetworkConnectivityHelper.isInternetAvailable(context)) {
        val networkStatus = NetworkConnectivityHelper.getNetworkStatusDescription(context)
        return "ERROR:NO_INTERNET:Cannot retrieve transactions. No internet connection available. Please check your network settings and try again. ($networkStatus)"
    }

    // Check if wallet is connected
    val connectionState = WalletConnectionManager.getConnectionState()
    if (!connectionState.isConnected || connectionState.address == null) {
        return "ERROR:WALLET_NOT_CONNECTED:Wallet not connected. Please connect your Solana wallet first to view transaction history."
    }

    val address = connectionState.address
    val limit = args["limit"]?.toIntOrNull() ?: 5

    return try {
        val client = ZerionClientHolder.getClient()
        val result = client.getWalletTransactions(address, pageSize = limit)

        if (result.isFailure) {
            val error = result.exceptionOrNull()
            Log.e(TAG, "Failed to fetch transactions", error)
            return "Error fetching transactions: ${error?.message ?: "Unknown error"}"
        }

        val transactions = result.getOrNull()
            ?: return "Error: Empty transactions response"

        if (transactions.data.isEmpty()) {
            return "No transactions found for this wallet."
        }

        val txText = transactions.data.joinToString("\n\n") { tx ->
            val attrs = tx.attributes
            val hash = attrs.hash.take(12) + "..."
            val type = attrs.operationType
            val status = attrs.status
            val timestamp = attrs.minedAt

            val transferInfo = attrs.transfers?.firstOrNull()?.let { transfer ->
                val symbol = transfer.fungibleInfo?.symbol ?: "Unknown"
                val amount = transfer.quantity.float
                val direction = transfer.direction
                "\n  ${direction.uppercase()}: ${String.format("%.6f", amount)} $symbol"
            } ?: ""

            val feeInfo = attrs.fee?.let { fee ->
                val feeAmount = fee.quantity.float
                val feeSymbol = fee.fungibleInfo?.symbol ?: "SOL"
                "\n  Fee: ${String.format("%.6f", feeAmount)} $feeSymbol"
            } ?: ""

            """
            [$status] $type
            Hash: $hash
            Time: $timestamp$transferInfo$feeInfo
            """.trimIndent()
        }

        """
        Recent Transactions for ${address.take(8)}...${address.takeLast(6)}:

        $txText
        """.trimIndent()

    } catch (e: Exception) {
        Log.e(TAG, "Exception getting transactions", e)
        "Error getting transactions: ${e.message}"
    }
}

/**
 * Verify a specific transaction by hash.
 * Useful for confirming x402 payments.
 */
suspend fun verifyZerionTransaction(context: Context, args: Map<String, String>): String {
    val txHash = args["hash"] ?: return "Error: Missing transaction hash"

    // Check network connectivity
    if (!NetworkConnectivityHelper.isInternetAvailable(context)) {
        val networkStatus = NetworkConnectivityHelper.getNetworkStatusDescription(context)
        return "ERROR:NO_INTERNET:Cannot verify transaction. No internet connection available. Please check your network settings and try again. ($networkStatus)"
    }

    // Check if wallet is connected
    val connectionState = WalletConnectionManager.getConnectionState()
    if (!connectionState.isConnected || connectionState.address == null) {
        return "ERROR:WALLET_NOT_CONNECTED:Wallet not connected. Please connect your Solana wallet first to verify transactions."
    }

    val address = connectionState.address

    return try {
        val client = ZerionClientHolder.getClient()
        val result = client.verifyTransaction(address, txHash)

        if (result.isFailure) {
            val error = result.exceptionOrNull()
            Log.e(TAG, "Failed to verify transaction", error)
            return "Error verifying transaction: ${error?.message ?: "Unknown error"}"
        }

        val tx = result.getOrNull()
            ?: return "Transaction not found: $txHash"

        val attrs = tx.attributes
        val status = attrs.status
        val type = attrs.operationType
        val timestamp = attrs.minedAt

        val transferInfo = attrs.transfers?.joinToString("\n") { transfer ->
            val symbol = transfer.fungibleInfo?.symbol ?: "Unknown"
            val amount = transfer.quantity.float
            val direction = transfer.direction
            "  ${direction.uppercase()}: ${String.format("%.6f", amount)} $symbol"
        } ?: "No transfers"

        """
        Transaction Verified ✓
        Hash: ${attrs.hash}
        Status: $status
        Type: $type
        Time: $timestamp

        Transfers:
        $transferInfo
        """.trimIndent()

    } catch (e: Exception) {
        Log.e(TAG, "Exception verifying transaction", e)
        "Error verifying transaction: ${e.message}"
    }
}

/**
 * Get list of Zerion wallet functions for LLM function calling.
 * These replace/supplement the existing Solana wallet functions.
 */
fun getZerionWalletFunctions(): List<FunctionDefinition> {
    return listOf(
        FunctionDefinition(
            name = "get_portfolio",
            description = "Get complete wallet portfolio overview including total value and asset distribution across different chains and types. Shows USD values for all holdings.",
            parameters = listOf()
        ),
        FunctionDefinition(
            name = "get_balance",
            description = "Get wallet token balances with current prices and USD values. Can fetch all tokens or filter by specific token symbol (e.g., SOL, USDC). Replaces get_solana_balance with richer data.",
            parameters = listOf(
                FunctionParameter(
                    name = "token",
                    type = "string",
                    description = "Optional token symbol to filter (e.g., 'SOL', 'USDC'). If omitted, shows all tokens.",
                    required = false
                )
            )
        ),
        FunctionDefinition(
            name = "get_transactions",
            description = "Get recent transaction history with details including transfers, fees, and timestamps. Useful for tracking payments and wallet activity.",
            parameters = listOf(
                FunctionParameter(
                    name = "limit",
                    type = "string",
                    description = "Number of transactions to fetch (1-20). Default is 5.",
                    required = false
                )
            )
        ),
        FunctionDefinition(
            name = "verify_transaction",
            description = "Verify a specific transaction by its hash/signature. Returns transaction status, transfers, and confirmation details. Essential for confirming x402 payments.",
            parameters = listOf(
                FunctionParameter(
                    name = "hash",
                    type = "string",
                    description = "Transaction hash/signature to verify",
                    required = true
                )
            )
        )
    )
}

/**
 * Execute Zerion wallet functions.
 * Routes function calls to appropriate Zerion API methods.
 */
suspend fun executeZerionWalletFunction(
    context: Context,
    functionName: String,
    args: Map<String, String>
): String {
    return when (functionName) {
        "get_portfolio" -> getZerionPortfolio(context)
        "get_balance" -> getZerionBalance(context, args)
        "get_transactions" -> getZerionTransactions(context, args)
        "verify_transaction" -> verifyZerionTransaction(context, args)
        else -> "Error: Unknown Zerion function '$functionName'"
    }
}
