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

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Function declarations for LLM function calling using prompt engineering.
 * These define the functions that the model can call during inference.
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

// Available functions
val availableFunctions = listOf(
    FunctionDefinition(
        name = "get_weather",
        description = "Get the current weather for a specific location",
        parameters = listOf(
            FunctionParameter(
                name = "location",
                type = "string",
                description = "The city or location to get weather for",
                required = true
            )
        )
    ),
    FunctionDefinition(
        name = "get_time",
        description = "Get the current time in a specific timezone",
        parameters = listOf(
            FunctionParameter(
                name = "timezone",
                type = "string",
                description = "The timezone (e.g., 'America/New_York', 'Europe/London', 'UTC')",
                required = true
            )
        )
    ),
    FunctionDefinition(
        name = "calculate",
        description = "Perform a mathematical calculation",
        parameters = listOf(
            FunctionParameter(
                name = "expression",
                type = "string",
                description = "The mathematical expression to evaluate (e.g., '2 + 3 * 4')",
                required = true
            )
        )
    )
)

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
FUNCTION_CALL: get_weather(location="San Francisco")
FUNCTION_CALL: get_time(timezone="America/New_York")
FUNCTION_CALL: calculate(expression="2 + 3 * 4")

Important:
- Only call functions when the user explicitly asks for information that requires them
- Use exact function names and parameter names as defined above
- Always put string values in double quotes
- If you don't need a function, respond normally with natural language
""".trimIndent())

    return sb.toString()
}

/**
 * Execute a function call and return the result.
 */
fun executeFunction(functionName: String, args: Map<String, String>): String {
    return when (functionName) {
        "get_weather" -> {
            val location = args["location"] ?: "unknown"
            // TODO: Replace with actual weather API call (e.g., OpenWeatherMap)
            // For now, return mock data
            "The weather in $location is sunny with a temperature of 72°F (22°C)"
        }
        "get_time" -> {
            val timezone = args["timezone"] ?: "UTC"
            try {
                val zoneId = ZoneId.of(timezone)
                val now = ZonedDateTime.now(zoneId)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                "The current time in $timezone is ${now.format(formatter)}"
            } catch (e: Exception) {
                "Error: Invalid timezone '$timezone'. Please use a valid timezone like 'America/New_York' or 'UTC'"
            }
        }
        "calculate" -> {
            val expression = args["expression"] ?: "0"
            try {
                val result = evaluateExpression(expression)
                "The result of $expression is $result"
            } catch (e: Exception) {
                "Error: Cannot calculate '$expression'. ${e.message}"
            }
        }
        else -> "Error: Unknown function '$functionName'"
    }
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

/**
 * Simple expression evaluator for basic arithmetic.
 * Supports +, -, *, /, parentheses, and decimal numbers.
 */
private fun evaluateExpression(expression: String): Double {
    return object {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < expression.length) expression[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < expression.length) throw RuntimeException("Unexpected character: '${ch.toChar()}'")
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw RuntimeException("Division by zero")
                        x /= divisor
                    }
                    else -> return x
                }
            }
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                if (!eat(')'.code)) throw RuntimeException("Missing closing parenthesis")
            } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                x = expression.substring(startPos, pos).toDouble()
            } else {
                throw RuntimeException("Unexpected character: '${ch.toChar()}'")
            }

            return x
        }
    }.parse()
}
