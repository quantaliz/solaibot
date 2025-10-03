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
            .putProperties(
                "location",
                Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("The city or location to get weather for")
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
            )
            .setRequired(listOf("location"))
            .build()
    )
    .build()

// Example function: Get current time
val getTimeFunction = com.google.ai.edge.localagents.FunctionDeclaration.newBuilder()
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
            // Mock weather data - replace with actual weather API call
            "The weather in $location is sunny with a temperature of 72°F"
        }
        "get_time" -> {
            val timezone = args["timezone"] ?: "UTC"
            // Mock time data - replace with actual time service
            "The current time in $timezone is 3:45 PM"
        }
        "calculate" -> {
            val expression = args["expression"] ?: "0"
            // Mock calculation - replace with actual calculator
            try {
                // Simple evaluation for demo - use a proper expression evaluator in production
                when {
                    expression.contains("+") -> {
                        val parts = expression.split("+").map { it.trim().toDoubleOrNull() ?: 0.0 }
                        "${parts[0] + (parts.getOrNull(1) ?: 0.0)}"
                    }
                    expression.contains("*") -> {
                        val parts = expression.split("*").map { it.trim().toDoubleOrNull() ?: 0.0 }
                        "${parts[0] * (parts.getOrNull(1) ?: 0.0)}"
                    }
                    else -> "Result: $expression"
                }
            } catch (e: Exception) {
                "Error calculating: $expression"
            }
        }
        else -> "Unknown function: $functionName"
    }
}