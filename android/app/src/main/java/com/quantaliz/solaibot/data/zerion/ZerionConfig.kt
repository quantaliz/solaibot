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

/**
 * Zerion API Configuration Example
 *
 * This is an example configuration file. DO NOT use this in production.
 *
 * Instructions:
 * 1. Copy this file to ZerionConfig.kt
 * 2. Replace YOUR_ZERION_API_KEY with your actual API key
 * 3. Add ZerionConfig.kt to .gitignore to prevent committing secrets
 * 4. For production, use Android Keystore or encrypted SharedPreferences
 *
 * Get your API key from: https://developers.zerion.io/
 */

object ZerionConfig {
    /**
     * Your Zerion API key.
     * Get one from: https://developers.zerion.io/
     *
     * IMPORTANT: Never commit your actual API key to version control!
     */
    const val API_KEY = "YOUR_ZERION_API_KEY_HERE"

    /**
     * Zerion API base URL.
     * Default: https://api.zerion.io/v1
     */
    const val BASE_URL = "https://api.zerion.io/v1"

    /**
     * Default currency for values.
     * Options: "usd", "eur", "gbp", etc.
     */
    const val DEFAULT_CURRENCY = "usd"

    /**
     * Filter out trash/spam tokens by default.
     */
    const val FILTER_TRASH_TOKENS = true

    /**
     * Default page size for transactions.
     * Range: 1-100
     */
    const val DEFAULT_TX_PAGE_SIZE = 10

    /**
     * Request timeout in seconds.
     */
    const val REQUEST_TIMEOUT_SECONDS = 30L

    /**
     * Enable debug logging.
     * Set to false in production builds.
     */
    const val DEBUG_LOGGING = true
}

/**
 * Example usage in ZerionWalletFunctions.kt:
 *
 * ```kotlin
 * object ZerionClientHolder {
 *     private var _client: ZerionApiClient? = null
 *
 *     fun getClient(apiKey: String? = null): ZerionApiClient {
 *         if (_client == null) {
 *             val key = apiKey ?: ZerionConfig.API_KEY
 *             _client = ZerionApiClient(
 *                 apiKey = key,
 *                 baseUrl = ZerionConfig.BASE_URL
 *             )
 *         }
 *         return _client!!
 *     }
 * }
 * ```
 *
 * For production, retrieve from secure storage:
 *
 * ```kotlin
 * suspend fun getSecureApiKey(context: Context): String {
 *     val dataStore = context.dataStore
 *     return dataStore.data.map { it.zerionApiKey }.first()
 * }
 * ```
 */
