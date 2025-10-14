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

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "X402FacilitatorClient"

/**
 * HTTP client for interacting with an x402 facilitator server.
 *
 * Facilitators handle payment verification and settlement on the blockchain.
 * This client provides methods to:
 * - Verify payment payloads without settling
 * - Settle payments on the blockchain
 * - Query supported payment schemes
 */
class X402FacilitatorClient(
    private val baseUrl: String  // e.g., "https://x402.payai.network"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Verifies a payment payload without settling it on the blockchain.
     *
     * @param paymentHeader The base64-encoded X-PAYMENT header value
     * @param requirements The payment requirements from the resource server
     * @return VerificationResponse indicating if the payment is valid
     * @throws IOException if the network request fails
     */
    suspend fun verify(
        paymentHeader: String,
        requirements: PaymentRequirements
    ): VerificationResponse = withContext(Dispatchers.IO) {
        val requestBody = FacilitatorRequest(
            x402Version = 1,
            paymentHeader = paymentHeader,
            paymentRequirements = requirements
        )

        val jsonBody = json.encodeToString(requestBody)
        val body = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/verify")
            .post(body)
            .build()

        Log.d(TAG, "Verifying payment with facilitator: $baseUrl/verify")

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Verification failed: HTTP ${response.code}: $responseBody")
                throw IOException("HTTP ${response.code}: $responseBody")
            }

            Log.d(TAG, "Verification response: $responseBody")
            json.decodeFromString<VerificationResponse>(responseBody)
        }
    }

    /**
     * Settles a payment on the blockchain.
     *
     * This completes the payment by:
     * 1. Verifying the payment payload
     * 2. Adding the facilitator's signature (for Solana, as the fee payer)
     * 3. Submitting the transaction to the blockchain
     *
     * @param paymentHeader The base64-encoded X-PAYMENT header value
     * @param requirements The payment requirements from the resource server
     * @return SettlementResponse with transaction details if successful
     * @throws IOException if the network request fails
     */
    suspend fun settle(
        paymentHeader: String,
        requirements: PaymentRequirements
    ): SettlementResponse = withContext(Dispatchers.IO) {
        val requestBody = FacilitatorRequest(
            x402Version = 1,
            paymentHeader = paymentHeader,
            paymentRequirements = requirements
        )

        val jsonBody = json.encodeToString(requestBody)
        val body = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/settle")
            .post(body)
            .build()

        Log.d(TAG, "Settling payment with facilitator: $baseUrl/settle")

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Settlement failed: HTTP ${response.code}: $responseBody")
                throw IOException("HTTP ${response.code}: $responseBody")
            }

            Log.d(TAG, "Settlement response: $responseBody")
            json.decodeFromString<SettlementResponse>(responseBody)
        }
    }

    /**
     * Queries the facilitator for supported payment schemes and networks.
     *
     * @return List of Kind objects representing supported (scheme, network) pairs
     * @throws IOException if the network request fails
     */
    suspend fun supported(): List<Kind> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/supported")
            .get()
            .build()

        Log.d(TAG, "Querying supported schemes: $baseUrl/supported")

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Supported query failed: HTTP ${response.code}: $responseBody")
                throw IOException("HTTP ${response.code}: $responseBody")
            }

            Log.d(TAG, "Supported schemes response: $responseBody")
            val supported = json.decodeFromString<SupportedSchemesResponse>(responseBody)
            supported.kinds
        }
    }
}
