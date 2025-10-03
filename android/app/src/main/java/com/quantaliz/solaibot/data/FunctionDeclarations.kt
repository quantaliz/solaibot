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

import com.google.ai.edge.localagents.FunctionDeclaration
import com.google.ai.edge.localagents.Schema
import com.google.ai.edge.localagents.Tool
import com.google.ai.edge.localagents.Type
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Function declarations for the Hammer 2.1 model to enable function calling.
 * These define the functions that the model can call during inference.
 */

// Example function: Get current weather
val getWeatherFunction = FunctionDeclaration.newBuilder()
    .setName("get_weather")
    .setDescription("Get the current weather for a specific location")
    .setParameters(
        Schema.newBuilder()
            .setType(Type.OBJECT)
            .putProperties("location", Schema.newBuilder()
                .setType(Type.STRING)
                .setDescription("The city or location to get weather for")
                .build())
            .setRequired(listOf("location"))
            .build()
    )
    .build()

// Example function: Get current time
val getTimeFunction = FunctionDeclaration.newBuilder()
    .setName("get_time")
    .setDescription("Get the current time in a specific timezone")
    .setParameters(
        Schema.newBuilder()
            .setType(Type.OBJECT)
            .putProperties(
                "timezone",
                Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("The timezone (e.g., 'America/New_York', 'Europe/London')")
                    .build()
            )
            .setRequired(listOf("timezone"))
            .build()
    )
    .build()

// Example function: Calculator
val calculateFunction = FunctionDeclaration.newBuilder()
    .setName("calculate")
    .setDescription("Perform a mathematical calculation")
    .setParameters(
        Schema.newBuilder()
            .setType(Type.OBJECT)
            .putProperties("expression", Schema.newBuilder()
                .setType(Type.STRING)
                .setDescription("The mathematical expression to evaluate (e.g., '2 + 3 * 4')")
                .build())
            .setRequired(listOf("expression"))
            .build()
    )
    .build()

// Tool containing all available functions
val hammerTool = Tool.newBuilder()
    .addFunctionDeclarations(getWeatherFunction)
    .addFunctionDeclarations(getTimeFunction)
    .addFunctionDeclarations(calculateFunction)
    .build()

// Example function: Get current time
val getTimeFunction = FunctionDeclaration.newBuilder()
    .setName("get_time")
    .setDescription("Get the current time in a specific timezone")
    .setParameters(
        Schema.newBuilder()
            .setType(Type.OBJECT)
            .putProperties(
                "timezone",
                Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("The timezone (e.g., 'America/New_York', 'Europe/London')")
                    .build()
            )
            .setRequired(listOf("timezone"))
            .build()
    )
    .build()

// Example function: Calculator
val calculateFunction = FunctionDeclaration.newBuilder()
    .setName("calculate")
    .setDescription("Perform a mathematical calculation")
    .setParameters(
        Schema.newBuilder()
            .setType(Type.OBJECT)
            .putProperties(
                "expression",
                Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("The mathematical expression to evaluate (e.g., '2 + 3 * 4')")
                    .build()
            )
            .setRequired(listOf("expression"))
            .build()
    )
    .build()

// Example function: Get current time
val getTimeFunction = FunctionDeclaration.newBuilder()
    .setName("get_time")
    .setDescription("Get the current time in a specific timezone")
    .setParameters(
        Schema.newBuilder()
            .setType(Type.OBJECT)
            .putProperties("timezone", Schema.newBuilder()
                .setType(Type.STRING)
                .setDescription("The timezone (e.g., 'America/New_York', 'Europe/London')")
                .build())
            .setRequired(listOf("timezone"))
            .build()
    )
    .build()

// Example function: Calculator
val calculateFunction = com.google.ai.edge.localagents.FunctionDeclaration.newBuilder()
    .setName("calculate")
    .setDescription("Perform a mathematical calculation")
    .setParameters(
        Schema.newBuilder()
            .setType(Type.OBJECT)
            .putProperties(
                "expression",
                Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("The mathematical expression to evaluate (e.g., '2 + 3 * 4')")
                    .build()
            )
            .setRequired(listOf("expression"))
            .build()
    )
    .build()

// Tool containing all available functions
val hammerTool = com.google.ai.edge.localagents.Tool.newBuilder()
    .addFunctionDeclarations(getWeatherFunction)
    .addFunctionDeclarations(getTimeFunction)
    .addFunctionDeclarations(calculateFunction)
    .build()

/**
 * Execute a function call and return the result.
 */
fun executeFunction(functionName: String, args: Map<String, String>): String {
    return when (functionName) {
        "get_weather" -> {
            val location = args["location"] ?: "unknown"
            // TODO: Replace with actual weather API call (e.g., OpenWeatherMap)
            // For now, return mock data
            "The weather in $location is sunny with a temperature of 72°F"
        }
        "get_time" -> {
            val timezone = args["timezone"] ?: "UTC"
            try {
                val zoneId = ZoneId.of(timezone)
                val now = ZonedDateTime.now(zoneId)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                "The current time in $timezone is ${now.format(formatter)}"
            } catch (e: Exception) {
                "Error getting time for timezone $timezone: ${e.message}"
            }
        }
        "calculate" -> {
            val expression = args["expression"] ?: "0"
            try {
                // Simple calculator implementation - supports basic arithmetic
                val result = evaluateExpression(expression)
                "The result of $expression is $result"
            } catch (e: Exception) {
                "Error calculating $expression: ${e.message}"
            }
        }
        else -> "Unknown function: $functionName"
    }
}

/**
 * Simple expression evaluator for basic arithmetic.
 * Supports +, -, *, /, and parentheses.
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
            if (pos < expression.length) throw RuntimeException("Unexpected: " + ch.toChar())
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
                    eat('/'.code) -> x /= parseFactor()
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
                eat(')'.code)
            } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                x = expression.substring(startPos, pos).toDouble()
            } else {
                throw RuntimeException("Unexpected: " + ch.toChar())
            }

            return x
        }
    }.parse()
}