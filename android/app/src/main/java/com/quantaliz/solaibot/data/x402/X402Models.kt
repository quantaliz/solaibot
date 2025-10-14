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

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

private val compactJson = Json { encodeDefaults = false }

/**
 * x402 Protocol data models for Solana payments.
 *
 * Based on the x402 protocol specification:
 * https://github.com/coinbase/x402
 */

/**
 * Payment requirements sent by the resource server in the 402 response.
 * Defines how the client can pay for access to the resource.
 */
@Serializable
data class PaymentRequirements(
    val scheme: String,              // e.g., "exact"
    val network: String,             // e.g., "solana", "solana-devnet"
    val maxAmountRequired: String,   // Amount in smallest units (lamports for Solana)
    val asset: String,               // Token mint address for SPL tokens, or "SOL" for native SOL
    val payTo: String,               // Recipient address
    val resource: String,            // URL path being paid for
    val description: String,         // Human-readable description
    val mimeType: String,            // Expected response MIME type
    val maxTimeoutSeconds: Int,      // Maximum time to complete payment
    val outputSchema: JsonObject? = null,  // Optional JSON schema for response
    val extra: Map<String, String> = emptyMap()  // Scheme-specific extras (e.g., feePayer for Solana)
)

/**
 * Payment requirements response wrapper.
 * This is what the server returns in a 402 response.
 */
@Serializable
data class PaymentRequirementsResponse(
    val x402Version: Int,
    val error: String,
    val accepts: List<PaymentRequirements>
)

/**
 * Payment payload sent by the client in the X-PAYMENT header.
 * Contains the signed transaction or authorization.
 */
@Serializable
data class PaymentPayload(
    val x402Version: Int,
    val scheme: String,
    val network: String,
    val payload: Map<String, String>  // Scheme-specific payload (e.g., {"transaction": "base64..."})
) {
    /**
     * Encodes this payload as a base64 string for the X-PAYMENT header.
     */
    fun toHeader(): String {
        val json = compactJson.encodeToString(this)
        Log.d("X402Models", "JSON before base64: '$json'")
        Log.d("X402Models", "JSON length: ${json.length}")
        Log.d("X402Models", "JSON contains newlines: ${json.contains('\n')}")
        Log.d("X402Models", "JSON contains carriage returns: ${json.contains('\r')}")

        // Sanitize JSON to remove any control characters that might cause issues
        val sanitizedJson = json.replace(Regex("[\\x00-\\x1f\\x7f-\\x9f]"), "")
        val base64Result = Base64.encodeToString(sanitizedJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        Log.d("X402Models", "Base64 result: '$base64Result'")
        Log.d("X402Models", "Base64 contains newlines: ${base64Result.contains('\n')}")
        Log.d("X402Models", "Base64 contains carriage returns: ${base64Result.contains('\r')}")
        return base64Result
    }

    companion object {
        /**
         * Decodes a payment payload from an X-PAYMENT header value.
         */
        fun fromHeader(header: String): PaymentPayload {
            val decoded = Base64.decode(header, Base64.DEFAULT)
            val json = String(decoded, Charsets.UTF_8)
            return compactJson.decodeFromString(json)
        }
    }
}

/**
 * Settlement response sent by the server in the X-PAYMENT-RESPONSE header.
 * Contains the result of payment settlement.
 */
@Serializable
data class SettlementResponse(
    val success: Boolean,
    val transaction: String? = null,     // Transaction signature/hash
    val network: String? = null,
    val payer: String? = null,           // Payer's address
    val errorReason: String? = null      // Error reason if success=false
) {
    /**
     * Encodes this response as a base64 string for the X-PAYMENT-RESPONSE header.
     */
    fun toHeader(): String {
        val json = compactJson.encodeToString(this)
        // Sanitize JSON to remove any control characters that might cause issues
        val sanitizedJson = json.replace(Regex("[\\x00-\\x1f\\x7f-\\x9f]"), "")
        return Base64.encodeToString(sanitizedJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    companion object {
        /**
         * Decodes a settlement response from an X-PAYMENT-RESPONSE header value.
         */
        fun fromHeader(header: String): SettlementResponse {
            val decoded = Base64.decode(header, Base64.DEFAULT)
            val json = String(decoded, Charsets.UTF_8)
            return compactJson.decodeFromString(json)
        }
    }
}

/**
 * Verification response from the facilitator's /verify endpoint.
 */
@Serializable
data class VerificationResponse(
    val isValid: Boolean,
    val invalidReason: String? = null,
    val payer: String? = null
)

/**
 * Kind represents a supported (scheme, network) pair from facilitator's /supported endpoint.
 */
@Serializable
data class Kind(
    val x402Version: Int,
    val scheme: String,
    val network: String
)

/**
 * Supported schemes response from facilitator's /supported endpoint.
 */
@Serializable
data class SupportedSchemesResponse(
    val kinds: List<Kind>
)

/**
 * Request body for facilitator's /verify and /settle endpoints.
 */
@Serializable
data class FacilitatorRequest(
    val x402Version: Int,
    val paymentHeader: String,          // Base64-encoded PaymentPayload
    val paymentRequirements: PaymentRequirements
)
