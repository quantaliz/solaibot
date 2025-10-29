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
 * Zerion API Configuration
 *
 * ⚠️ IMPORTANT: YOU MUST REPLACE THE API_KEY VALUE BELOW ⚠️
 *
 * Instructions:
 * 1. Get your Zerion API key from: https://developers.zerion.io/reference/authentication
 * 2. Replace "YOUR_ZERION_API_KEY_HERE" with your actual API key
 * 3. NEVER commit your real API key to version control!
 * 4. For production, use Android Keystore or encrypted SharedPreferences
 *
 * The app will fail to compile if you don't replace this value.
 */

object ZerionConfig {
    /**
     * Your Zerion API key.
     * Get one from: https://developers.zerion.io/reference/authentication
     *
     * IMPORTANT: Replace this value or the app will not compile!
     */
    const val API_KEY: String = "YOUR_ZERION_API_KEY_HERE"
        get() {
            // Compile-time check to ensure API key is configured
            require(field != "YOUR_ZERION_API_KEY_HERE") {
                """
                ╔═══════════════════════════════════════════════════════════════════════╗
                ║                    ZERION API KEY NOT CONFIGURED                      ║
                ╠═══════════════════════════════════════════════════════════════════════╣
                ║                                                                       ║
                ║  You must replace the API_KEY value in:                              ║
                ║  app/src/main/java/com/quantaliz/solaibot/data/zerion/ZerionConfig.kt║
                ║                                                                       ║
                ║  Steps:                                                               ║
                ║  1. Visit: https://developers.zerion.io/reference/authentication    ║
                ║  2. Sign up and generate an API key                                  ║
                ║  3. Replace "YOUR_ZERION_API_KEY_HERE" with your actual key          ║
                ║                                                                       ║
                ║  Example:                                                             ║
                ║  const val API_KEY: String = "zjk_live_abc123def456..."              ║
                ║                                                                       ║
                ║  ⚠️  NEVER commit your real API key to version control!              ║
                ║                                                                       ║
                ╚═══════════════════════════════════════════════════════════════════════╝
                """.trimIndent()
            }
            return field
        }

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
