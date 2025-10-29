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

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Zerion API client for fetching wallet data.
 *
 * API Documentation: https://developers.zerion.io/reference
 *
 * Usage:
 * - Requires API key from Zerion dashboard
 * - Supports Solana wallets on mainnet-beta, devnet, testnet
 * - Returns JSON:API formatted responses
 *
 * Phase 1 Endpoints:
 * - GET /wallets/{address}/portfolio - Portfolio overview
 * - GET /wallets/{address}/positions - Fungible token positions
 * - GET /wallets/{address}/transactions - Transaction history
 */
class ZerionApiClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.zerion.io/v1"
) {
    companion object {
        private const val TAG = "ZerionApiClient"
        private const val DEFAULT_TIMEOUT_SECONDS = 30L
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * Get wallet portfolio overview.
     *
     * @param address Wallet address (Solana or EVM)
     * @param currency Currency for values (default: "usd")
     * @return Portfolio data with total value and distribution
     */
    suspend fun getWalletPortfolio(
        address: String,
        currency: String = "usd"
    ): Result<ZerionPortfolioResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/wallets/$address/portfolio/?currency=$currency"

            Log.d(TAG, "Fetching portfolio for address: $address")

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Basic $apiKey")
                .header("Accept", "application/json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Portfolio request failed: ${response.code} - $errorBody")

                return@withContext Result.failure(
                    IOException("Portfolio request failed: ${response.code} - ${response.message}")
                )
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(IOException("Empty response body"))

            Log.d(TAG, "Portfolio response received: ${responseBody.take(200)}...")

            val portfolioResponse = json.decodeFromString<ZerionPortfolioResponse>(responseBody)
            Result.success(portfolioResponse)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching portfolio", e)
            Result.failure(e)
        }
    }

    /**
     * Get wallet fungible token positions.
     *
     * @param address Wallet address
     * @param currency Currency for values (default: "usd")
     * @param filterTrash Filter out trash/spam tokens (default: true)
     * @param sort Sorting order (default: "value")
     * @return List of token positions with balances and values
     */
    suspend fun getWalletPositions(
        address: String,
        currency: String = "usd",
        filterTrash: Boolean = true,
        sort: String = "value"
    ): Result<ZerionPositionsResponse> = withContext(Dispatchers.IO) {
        try {
            val params = mutableListOf(
                "currency=$currency",
                "sort=$sort"
            )

            if (filterTrash) {
                params.add("filter[trash]=only_non_trash")
            }

            val queryString = params.joinToString("&")
            val url = "$baseUrl/wallets/$address/positions/?$queryString"

            Log.d(TAG, "Fetching positions for address: $address")

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Basic $apiKey")
                .header("Accept", "application/json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Positions request failed: ${response.code} - $errorBody")

                return@withContext Result.failure(
                    IOException("Positions request failed: ${response.code} - ${response.message}")
                )
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(IOException("Empty response body"))

            Log.d(TAG, "Positions response received with ${responseBody} characters")

            val positionsResponse = json.decodeFromString<ZerionPositionsResponse>(responseBody)
            Result.success(positionsResponse)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching positions", e)
            Result.failure(e)
        }
    }

    /**
     * Get wallet transaction history.
     *
     * @param address Wallet address
     * @param currency Currency for values (default: "usd")
     * @param filterTrash Filter out trash transactions (default: true)
     * @param pageSize Number of transactions per page (default: 10, max: 100)
     * @return List of transactions
     */
    suspend fun getWalletTransactions(
        address: String,
        currency: String = "usd",
        filterTrash: Boolean = true,
        pageSize: Int = 10
    ): Result<ZerionTransactionsResponse> = withContext(Dispatchers.IO) {
        try {
            val params = mutableListOf(
                "currency=$currency",
                "page[size]=$pageSize"
            )

            if (filterTrash) {
                params.add("filter[trash]=only_non_trash")
            }

            val queryString = params.joinToString("&")
            val url = "$baseUrl/wallets/$address/transactions/?$queryString"

            Log.d(TAG, "Fetching transactions for address: $address")

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Basic $apiKey")
                .header("Accept", "application/json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Transactions request failed: ${response.code} - $errorBody")

                return@withContext Result.failure(
                    IOException("Transactions request failed: ${response.code} - ${response.message}")
                )
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(IOException("Empty response body"))

            Log.d(TAG, "Transactions response received")

            val transactionsResponse = json.decodeFromString<ZerionTransactionsResponse>(responseBody)
            Result.success(transactionsResponse)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transactions", e)
            Result.failure(e)
        }
    }

    /**
     * Verify transaction by hash.
     * Useful for confirming x402 payment completion.
     *
     * @param address Wallet address
     * @param txHash Transaction hash/signature
     * @return Transaction data if found, null otherwise
     */
    suspend fun verifyTransaction(
        address: String,
        txHash: String
    ): Result<ZerionTransactionData?> = withContext(Dispatchers.IO) {
        try {
            // Fetch recent transactions and search for the hash
            val transactionsResult = getWalletTransactions(
                address = address,
                pageSize = 20
            )

            if (transactionsResult.isFailure) {
                return@withContext Result.failure(transactionsResult.exceptionOrNull()!!)
            }

            val transactions = transactionsResult.getOrNull()?.data ?: emptyList()
            val matchingTx = transactions.find { it.attributes.hash.equals(txHash, ignoreCase = true) }

            Result.success(matchingTx)

        } catch (e: Exception) {
            Log.e(TAG, "Error verifying transaction", e)
            Result.failure(e)
        }
    }
}
