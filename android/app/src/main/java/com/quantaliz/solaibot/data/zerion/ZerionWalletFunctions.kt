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
import com.quantaliz.solaibot.data.getSolanaBalanceViaRpc

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
private const val SOLANA_CHAIN_ID = "solana"
private const val DEFAULT_TRANSACTION_LIMIT = ZerionConfig.DEFAULT_TX_PAGE_SIZE
private const val MAX_TRANSACTION_LIMIT = 50

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

private sealed class WalletContextResult {
    data class Success(
        val address: String,
        val usedConnectedWallet: Boolean
    ) : WalletContextResult()

    data class Error(val message: String) : WalletContextResult()
}

private fun resolveWalletContext(args: Map<String, String>): WalletContextResult {
    val addressOverride = args["address"]?.trim()?.takeIf { it.isNotEmpty() }
    val connectionState = WalletConnectionManager.getConnectionState()
    val resolvedAddress = addressOverride ?: connectionState.address

    if (resolvedAddress.isNullOrBlank()) {
        return WalletContextResult.Error(
            "ERROR:WALLET_NOT_CONNECTED:Wallet not connected. Connect your wallet or provide an 'address' parameter."
        )
    }

    return WalletContextResult.Success(
        address = resolvedAddress,
        usedConnectedWallet = addressOverride == null
    )
}

private fun normalizeNetwork(raw: String?): String? {
    if (raw.isNullOrBlank()) return null

    val value = raw.trim().lowercase()
    return when (value) {
        "sol", "solana", "mainnet", "mainnet-beta", "solana-mainnet" -> "solana"
        "devnet", "solana-devnet", "solana_devnet", "solana devnet" -> "solana-devnet"
        else -> null
    }
}

private fun formatNetworkLabel(network: String?): String? {
    return when (network) {
        "solana" -> "Solana (mainnet-beta)"
        "solana-devnet" -> "Solana Devnet"
        else -> null
    }
}

private fun shortAddress(address: String): String {
    return if (address.length <= 12) address else "${address.take(8)}...${address.takeLast(6)}"
}

/**
 * Get wallet portfolio overview using Zerion API.
 * Shows total balance, asset distribution by type and chain.
 */
suspend fun getZerionPortfolio(context: Context, args: Map<String, String> = emptyMap()): String {
    if (!NetworkConnectivityHelper.isInternetAvailable(context)) {
        val networkStatus = NetworkConnectivityHelper.getNetworkStatusDescription(context)
        return "ERROR:NO_INTERNET:Cannot retrieve portfolio. No internet connection available. Please check your network settings and try again. ($networkStatus)"
    }

    when (val walletContext = resolveWalletContext(args)) {
        is WalletContextResult.Error -> return walletContext.message
        is WalletContextResult.Success -> {
            val network = normalizeNetwork(args["network"])
            val client = ZerionClientHolder.getClient()

            return try {
                val result = client.getWalletPortfolio(
                    address = walletContext.address,
                    chainId = SOLANA_CHAIN_ID,
                    network = network
                )

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

                val addressLabel = shortAddress(walletContext.address)
                val networkLabel = formatNetworkLabel(network)

                buildString {
                    append("Portfolio for $addressLabel")
                    networkLabel?.let { append(" on $it") }
                    append(":\n")
                    append("Total Value: $totalValue\n\n")
                    append("Distribution by Type:\n")
                    append(distributionText)
                }.trim()

            } catch (e: Exception) {
                Log.e(TAG, "Exception getting portfolio", e)
                "Error getting portfolio: ${e.message}"
            }
        }
    }
}

/**
 * Get wallet token positions (balances) using Zerion API.
 * This replaces the direct RPC balance call with Zerion's enriched data.
 */
suspend fun getZerionBalance(context: Context, args: Map<String, String> = emptyMap()): String {
    if (!NetworkConnectivityHelper.isInternetAvailable(context)) {
        val networkStatus = NetworkConnectivityHelper.getNetworkStatusDescription(context)
        return "ERROR:NO_INTERNET:Cannot retrieve balance. No internet connection available. Please check your network settings and try again. ($networkStatus)"
    }

    val tokenSymbol = args["token"]?.uppercase()
    when (val walletContext = resolveWalletContext(args)) {
        is WalletContextResult.Error -> return walletContext.message
        is WalletContextResult.Success -> {
            val network = normalizeNetwork(args["network"])
            val client = ZerionClientHolder.getClient()

            return try {
                val result = client.getWalletPositions(
                    address = walletContext.address,
                    chainId = SOLANA_CHAIN_ID,
                    network = network
                )

                if (result.isFailure) {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Failed to fetch positions", error)

                    return try {
                        getSolanaBalanceViaRpc(context, walletContext.address)
                    } catch (fallbackError: Exception) {
                        "Error fetching balance: ${error?.message ?: fallbackError.message ?: "Unknown error"}"
                    }
                }

                val positions = result.getOrNull()
                    ?: return "Error: Empty positions response"

                if (positions.data.isEmpty()) {
                    return "INFO:NO_TOKENS:No fungible token positions found for this wallet on the requested network."
                }

                val addressLabel = shortAddress(walletContext.address)
                val networkLabel = formatNetworkLabel(network)

                // If specific token requested, filter for it
                if (tokenSymbol != null) {
                    val matchingPosition = positions.data.find { position ->
                        position.attributes.fungibleInfo?.symbol?.uppercase() == tokenSymbol
                    } ?: return "Token $tokenSymbol not found for wallet $addressLabel."

                    val attrs = matchingPosition.attributes
                    val symbol = attrs.fungibleInfo?.symbol ?: "Unknown"
                    val quantity = attrs.quantity.float
                    val value = attrs.value?.let { String.format("$%.2f", it) } ?: "N/A"

                    return buildString {
                        append("$symbol Balance for $addressLabel")
                        networkLabel?.let { append(" on $it") }
                        append(":\n")
                        append("Amount: ${String.format("%.6f", quantity)} $symbol\n")
                        append("Value: $value")
                    }.trim()
                }

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

                buildString {
                    append("Wallet: $addressLabel")
                    networkLabel?.let { append(" on $it") }
                    append("\n\nToken Balances:\n")
                    append(positionsText)
                    append(showingText)
                }.trim()

            } catch (e: Exception) {
                Log.e(TAG, "Exception getting balance", e)
                "Error getting balance: ${e.message}"
            }
        }
    }
}

/**
 * Get wallet transaction history using Zerion API.
 * Shows recent transactions with details.
 */
suspend fun getZerionTransactions(context: Context, args: Map<String, String> = emptyMap()): String {
    if (!NetworkConnectivityHelper.isInternetAvailable(context)) {
        val networkStatus = NetworkConnectivityHelper.getNetworkStatusDescription(context)
        return "ERROR:NO_INTERNET:Cannot retrieve transactions. No internet connection available. Please check your network settings and try again. ($networkStatus)"
    }

    val rawLimit = args["limit"]?.toIntOrNull()
    val pageSize = rawLimit?.coerceIn(1, MAX_TRANSACTION_LIMIT) ?: DEFAULT_TRANSACTION_LIMIT

    when (val walletContext = resolveWalletContext(args)) {
        is WalletContextResult.Error -> return walletContext.message
        is WalletContextResult.Success -> {
            val network = normalizeNetwork(args["network"])
            val client = ZerionClientHolder.getClient()

            return try {
                val result = client.getWalletTransactions(
                    address = walletContext.address,
                    pageSize = pageSize,
                    chainId = SOLANA_CHAIN_ID,
                    network = network
                )

                if (result.isFailure) {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Failed to fetch transactions", error)
                    return "Error fetching transactions: ${error?.message ?: "Unknown error"}"
                }

                val transactions = result.getOrNull()
                    ?: return "Error: Empty transactions response"

                if (transactions.data.isEmpty()) {
                    val networkLabel = formatNetworkLabel(network)
                    val addressLabel = shortAddress(walletContext.address)
                    return buildString {
                        append("No transactions found for $addressLabel")
                        networkLabel?.let { append(" on $it") }
                        append(".")
                    }
                }

                val txText = transactions.data.joinToString("\n\n") { tx ->
                    val attrs = tx.attributes
                    val hashShort = shortAddress(attrs.hash)
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
                    Hash: $hashShort
                    Time: $timestamp$transferInfo$feeInfo
                    """.trimIndent()
                }

                val addressLabel = shortAddress(walletContext.address)
                val networkLabel = formatNetworkLabel(network)

                buildString {
                    append("Recent Transactions for $addressLabel")
                    networkLabel?.let { append(" on $it") }
                    append(":\n\n")
                    append(txText)
                }.trim()

            } catch (e: Exception) {
                Log.e(TAG, "Exception getting transactions", e)
                "Error getting transactions: ${e.message}"
            }
        }
    }
}

/**
 * Verify a specific transaction by hash.
 * Useful for confirming x402 payments.
 */
suspend fun verifyZerionTransaction(context: Context, args: Map<String, String>): String {
    val txHash = args["hash"]?.trim()?.takeIf { it.isNotEmpty() }
        ?: return "Error: Missing transaction hash"

    if (!NetworkConnectivityHelper.isInternetAvailable(context)) {
        val networkStatus = NetworkConnectivityHelper.getNetworkStatusDescription(context)
        return "ERROR:NO_INTERNET:Cannot verify transaction. No internet connection available. Please check your network settings and try again. ($networkStatus)"
    }

    when (val walletContext = resolveWalletContext(args)) {
        is WalletContextResult.Error -> return walletContext.message
        is WalletContextResult.Success -> {
            val network = normalizeNetwork(args["network"])
            val client = ZerionClientHolder.getClient()

            return try {
                val result = client.verifyTransaction(
                    address = walletContext.address,
                    txHash = txHash,
                    chainId = SOLANA_CHAIN_ID,
                    network = network
                )

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

                val networkLabel = formatNetworkLabel(network)

                buildString {
                    append("Transaction Verified ✓\n")
                    networkLabel?.let { append("Network: $it\n") }
                    append("Hash: ${attrs.hash}\n")
                    append("Status: $status\n")
                    append("Type: $type\n")
                    append("Time: $timestamp\n\n")
                    append("Transfers:\n")
                    append(transferInfo)
                }.trim()

            } catch (e: Exception) {
                Log.e(TAG, "Exception verifying transaction", e)
                "Error verifying transaction: ${e.message}"
            }
        }
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
            parameters = listOf(
                FunctionParameter(
                    name = "address",
                    type = "string",
                    description = "Optional Solana wallet address. If omitted, uses the connected wallet.",
                    required = false
                ),
                FunctionParameter(
                    name = "network",
                    type = "string",
                    description = "Optional Solana network to target ('solana' or 'solana-devnet'). Defaults to mainnet.",
                    required = false
                )
            )
        ),
        FunctionDefinition(
            name = "get_balance",
            description = "Get wallet token balances with current prices and USD values. Can fetch all tokens or filter by specific token symbol (e.g., SOL, USDC). Replaces get_solana_balance with richer data.",
            parameters = listOf(
                FunctionParameter(
                    name = "address",
                    type = "string",
                    description = "Optional Solana wallet address. If omitted, uses the connected wallet.",
                    required = false
                ),
                FunctionParameter(
                    name = "network",
                    type = "string",
                    description = "Optional Solana network to target ('solana' or 'solana-devnet'). Defaults to mainnet.",
                    required = false
                ),
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
                    name = "address",
                    type = "string",
                    description = "Optional Solana wallet address. If omitted, uses the connected wallet.",
                    required = false
                ),
                FunctionParameter(
                    name = "network",
                    type = "string",
                    description = "Optional Solana network to target ('solana' or 'solana-devnet'). Defaults to mainnet.",
                    required = false
                ),
                FunctionParameter(
                    name = "limit",
                    type = "string",
                    description = "Number of transactions to fetch (1-50). Defaults to 10.",
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
                ),
                FunctionParameter(
                    name = "address",
                    type = "string",
                    description = "Optional Solana wallet address. If omitted, uses the connected wallet.",
                    required = false
                ),
                FunctionParameter(
                    name = "network",
                    type = "string",
                    description = "Optional Solana network to target ('solana' or 'solana-devnet'). Defaults to mainnet.",
                    required = false
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
        "get_portfolio" -> getZerionPortfolio(context, args)
        "get_balance" -> getZerionBalance(context, args)
        "get_transactions" -> getZerionTransactions(context, args)
        "verify_transaction" -> verifyZerionTransaction(context, args)
        else -> "Error: Unknown Zerion function '$functionName'"
    }
}
