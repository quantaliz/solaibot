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

/**
 * Function declarations for LLM function calling using prompt engineering.
 * These define the Solana wallet functions that the model can call during inference.
 */

data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: List<FunctionParameter>
)

data class FunctionParameter(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = true
)

// Available Solana wallet functions
val availableFunctions = getSolanaWalletFunctions()

/**
 * Generate system prompt that teaches the model how to use functions.
 */
fun generateFunctionCallingSystemPrompt(): String {
    val sb = StringBuilder()
    sb.append("You are a helpful assistant with access to the following functions:\n\n")

    for (func in availableFunctions) {
        sb.append("Function: ${func.name}\n")
        sb.append("Description: ${func.description}\n")
        sb.append("Parameters:\n")
        for (param in func.parameters) {
            sb.append("  - ${param.name} (${param.type}): ${param.description}")
            if (param.required) sb.append(" [REQUIRED]")
            sb.append("\n")
        }
        sb.append("\n")
    }

    sb.append("""
To call a function, respond EXACTLY in this format:
FUNCTION_CALL: function_name(param1="value1", param2="value2")

Examples:
FUNCTION_CALL: get_solana_balance()
FUNCTION_CALL: send_solana(recipient="7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU", amount="0.1")
FUNCTION_CALL: solana_payment(url="https://api.example.com/premium-data")

Important:
- Only call functions when the user explicitly asks for information that requires them
- Use exact function names and parameter names as defined above
- Always put string values in double quotes
- If you don't need a function, respond normally with natural language
- When responding to users after a function call, speak naturally without using markdown code blocks (no ``` syntax)
- Keep responses conversational and friendly
- IMPORTANT: When presenting function results (especially payment transactions), include ALL relevant details from the function result such as:
  * Transaction hashes/signatures
  * Payment amounts and networks
  * Wallet addresses
  * Balance information
  * Premium content or data received
  * Any confirmation details or receipts
  * Format these details clearly for the user to see

Solana Wallet Usage Notes:
- For balance queries, use get_solana_balance() - it will automatically connect if needed and returns detailed token data with USD values via Zerion API
- For portfolio overview with total value, use get_portfolio() - shows complete wallet value and distribution
- For specific token balance, use get_balance(token="SYMBOL") - e.g., get_balance(token="SOL") or get_balance(token="USDC")
- For transaction history, use get_transactions(limit="5") - shows recent wallet activity
- To verify a payment, use verify_transaction(hash="...") - confirms transaction status and details
- The solana_payment() function uses the x402 protocol for micropayments to access paid APIs and resources
- All Solana addresses should be valid Base58-encoded public keys
- SOL amounts should be specified as decimal values (e.g., "0.1" for 0.1 SOL)

New Zerion Features:
- get_portfolio(): Complete wallet overview with total USD value and asset distribution
- get_balance(token?): Detailed token balances with prices and values (replaces basic RPC balance)
- get_transactions(limit?): Recent transaction history with full details
- verify_transaction(hash): Verify specific transactions (useful after x402 payments)
""".trimIndent())

    return sb.toString()
}

/**
 * Execute a function call and return the result.
 * Routes all function calls to Solana wallet functions.
 */
suspend fun executeFunction(
    context: Context,
    functionName: String,
    args: Map<String, String>,
    activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender? = null
): String {
    return executeSolanaWalletFunction(context, functionName, args, activityResultSender)
}

/**
 * Parse function call from model response.
 * Returns Pair(functionName, arguments) or null if no function call detected.
 */
fun parseFunctionCall(response: String): Pair<String, Map<String, String>>? {
    // Look for pattern: FUNCTION_CALL: function_name(arg1="value1", arg2="value2")
    val functionCallRegex = Regex("""FUNCTION_CALL:\s*(\w+)\((.*?)\)""")
    val match = functionCallRegex.find(response) ?: return null

    val functionName = match.groupValues[1]
    val argsString = match.groupValues[2]

    // Parse arguments
    val args = mutableMapOf<String, String>()
    val argRegex = Regex("""(\w+)="([^"]*)"""")
    for (argMatch in argRegex.findAll(argsString)) {
        val key = argMatch.groupValues[1]
        val value = argMatch.groupValues[2]
        args[key] = value
    }

    return Pair(functionName, args)
}
