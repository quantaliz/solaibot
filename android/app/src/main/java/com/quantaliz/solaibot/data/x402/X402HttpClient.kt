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

package com.quantaliz.solaibot.data.x402

import android.content.Context
import android.util.Log
import com.quantaliz.solaibot.data.NetworkConnectivityHelper
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "X402HttpClient"

/**
 * x402 HTTP client for making paid requests to resource servers.
 *
 * This client implements the x402 payment flow:
 * 1. Make initial request to resource server
 * 2. Receive 402 Payment Required with payment requirements
 * 3. Build and sign payment transaction using wallet
 * 4. Retry request with X-PAYMENT header
 * 5. Receive resource with X-PAYMENT-RESPONSE header
 *
 * Usage:
 * ```kotlin
 * val client = X402HttpClient(context, facilitatorUrl = "https://x402.payai.network")
 * val result = client.get(url = "https://api.example.com/data", activityResultSender)
 * ```
 */
class X402HttpClient(
    private val context: Context,
    private val facilitatorUrl: String = "https://x402.payai.network"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val facilitatorClient = X402FacilitatorClient(facilitatorUrl)

    /**
     * Result of an x402 HTTP request.
     */
    data class X402Response(
        val success: Boolean,
        val statusCode: Int,
        val body: String?,
        val settlementResponse: SettlementResponse? = null,
        val errorMessage: String? = null
    )

    /**
     * Makes a GET request to a resource that may require x402 payment.
     *
     * @param url The URL to request
     * @param activityResultSender Required for wallet interaction (signing transactions)
     * @return X402Response containing the result and settlement details
     */
    suspend fun get(
        url: String,
        activityResultSender: ActivityResultSender?
    ): X402Response = withContext(Dispatchers.IO) {
        // Check network connectivity before making any requests
        if (!NetworkConnectivityHelper.isInternetAvailable(context)) {
            val networkStatus = NetworkConnectivityHelper.getNetworkStatusDescription(context)
            Log.e(TAG, "No internet connection: $networkStatus")
            return@withContext X402Response(
                success = false,
                statusCode = 0,
                body = null,
                errorMessage = "No internet connection. $networkStatus"
            )
        }

        if (activityResultSender == null) {
            return@withContext X402Response(
                success = false,
                statusCode = 0,
                body = null,
                errorMessage = "ActivityResultSender required for x402 payments"
            )
        }

        // Step 1: Make initial request
        val initialRequest = Request.Builder()
            .url(url)
            .get()
            .build()

        Log.d(TAG, "Making initial request to: $url")

        httpClient.newCall(initialRequest).execute().use { response ->
            // If not 402, return response as-is
            if (response.code != 402) {
                return@withContext X402Response(
                    success = response.isSuccessful,
                    statusCode = response.code,
                    body = response.body?.string()
                )
            }

            // Step 2: Parse payment requirements from 402 response
            val paymentRequirementsResponse = try {
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "Received 402 Payment Required: $responseBody")
                json.decodeFromString<PaymentRequirementsResponse>(responseBody)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse payment requirements", e)
                return@withContext X402Response(
                    success = false,
                    statusCode = 402,
                    body = null,
                    errorMessage = "Failed to parse payment requirements: ${e.message}"
                )
            }

            // Get first supported payment requirement (prefer Solana)
            val requirement = paymentRequirementsResponse.accepts.firstOrNull {
                it.scheme == "exact" && (it.network == "solana" || it.network == "solana-devnet")
            } ?: paymentRequirementsResponse.accepts.firstOrNull()

            if (requirement == null) {
                return@withContext X402Response(
                    success = false,
                    statusCode = 402,
                    body = null,
                    errorMessage = "No supported payment methods available"
                )
            }

            Log.d(TAG, "Selected payment requirement: scheme=${requirement.scheme}, network=${requirement.network}, amount=${requirement.maxAmountRequired}")

            // Step 3: Build and sign payment transaction
            val paymentPayload = try {
                buildSolanaPaymentPayload(requirement, activityResultSender)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build payment payload", e)
                return@withContext X402Response(
                    success = false,
                    statusCode = 402,
                    body = null,
                    errorMessage = "Failed to build payment: ${e.message}"
                )
            }

            // Step 4: Make request with payment
            val paymentHeader = paymentPayload.toHeader()
            val paidRequest = Request.Builder()
                .url(url)
                .header("X-PAYMENT", paymentHeader)
                .get()
                .build()

            Log.d(TAG, "Making paid request with X-PAYMENT header")

            httpClient.newCall(paidRequest).execute().use { paidResponse ->
                // Step 5: Parse settlement response from header
                val settlementResponse = paidResponse.header("X-PAYMENT-RESPONSE")?.let { header ->
                    try {
                        SettlementResponse.fromHeader(header)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse settlement response", e)
                        null
                    }
                }

                Log.d(TAG, "Paid request response: code=${paidResponse.code}, settlement=$settlementResponse")

                X402Response(
                    success = paidResponse.isSuccessful,
                    statusCode = paidResponse.code,
                    body = paidResponse.body?.string(),
                    settlementResponse = settlementResponse
                )
            }
        }
    }

    /**
     * Builds a Solana payment payload by creating and signing a transaction.
     *
     * This delegates to the Solana payment builder which:
     * 1. Creates a transfer transaction to the payTo address
     * 2. Signs it with the user's wallet via MWA
     * 3. Encodes it as a partially-signed transaction (facilitator will add fee payer signature)
     */
    private suspend fun buildSolanaPaymentPayload(
        requirement: PaymentRequirements,
        activityResultSender: ActivityResultSender
    ): PaymentPayload {
        val transaction = SolanaPaymentBuilder.buildSolanaPaymentTransaction(
            context = context,
            requirement = requirement,
            activityResultSender = activityResultSender
        )

        return PaymentPayload(
            x402Version = 1,
            scheme = "exact",
            network = requirement.network,
            payload = mapOf("transaction" to transaction)
        )
    }
}
