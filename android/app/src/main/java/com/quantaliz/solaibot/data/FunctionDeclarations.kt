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
    sb.append("You have access to the following functions. Use them when needed:\n\n")

    for (func in availableFunctions) {
        sb.append("${func.name}\n")
        sb.append("${func.description}\n")
        if (func.parameters.isNotEmpty()) {
            sb.append("Parameters:\n")
            for (param in func.parameters) {
                sb.append("  - ${param.name} (${param.type}): ${param.description}")
                if (param.required) sb.append(" [REQUIRED]")
                sb.append("\n")
            }
        }
        sb.append("\n")
    }

    sb.append("""
To call a function, you MUST use this JSON format:
{"name": "function_name", "parameters": {"param1": "value1", "param2": "value2"}}

Examples:
{"name": "get_portfolio", "parameters": {}}
{"name": "get_balance", "parameters": {"token": "SOL"}}
{"name": "get_transactions", "parameters": {"limit": "5"}}
{"name": "solana_payment", "parameters": {"url": "https://api.example.com/premium-data"}}
{"name": "verify_transaction", "parameters": {"hash": "abc123..."}}

Rules:
- Use functions only when needed to answer the user's question
- Use exact function names and parameter names
- For functions with no parameters, use empty object: "parameters": {}
- Respond naturally in text when not calling functions
- When showing function results, include all important details (transaction hashes, amounts, addresses, etc.)
- IMPORTANT: If a function returns an error or info message (starting with ERROR: or INFO:), DO NOT make additional function calls. The error/info will be displayed directly to the user.
- If you see "ERROR:WALLET_NOT_CONNECTED" in function results, the user needs to connect their wallet first before any wallet operations can succeed.

get_portfolio: Shows total wallet value and asset distribution
get_balance: Shows token balances with USD values (optional token filter)
get_transactions: Shows recent transaction history (optional limit)
verify_transaction: Confirms transaction by hash
solana_payment: Makes x402 micropayments for paid resources
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
 *
 * Supports both JSON format (preferred for Gemma) and legacy format for backwards compatibility.
 */
fun parseFunctionCall(response: String): Pair<String, Map<String, String>>? {
    // Try JSON format first (Gemma 3 preferred format)
    // Look for: {"name": "function_name", "parameters": {"param1": "value1"}}
    try {
        val jsonRegex = Regex("""\{[^}]*"name"\s*:\s*"([^"]+)"[^}]*"parameters"\s*:\s*\{([^}]*)\}[^}]*\}""")
        val jsonMatch = jsonRegex.find(response)

        if (jsonMatch != null) {
            val functionName = jsonMatch.groupValues[1]
            val paramsString = jsonMatch.groupValues[2]

            // Parse parameters from JSON
            val args = mutableMapOf<String, String>()
            val paramRegex = Regex(""""(\w+)"\s*:\s*"([^"]*)"""")
            for (paramMatch in paramRegex.findAll(paramsString)) {
                val key = paramMatch.groupValues[1]
                val value = paramMatch.groupValues[2]
                args[key] = value
            }

            return Pair(functionName, args)
        }
    } catch (e: Exception) {
        // Fall through to legacy format
    }

    // Fall back to legacy format: FUNCTION_CALL: function_name(arg1="value1")
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
