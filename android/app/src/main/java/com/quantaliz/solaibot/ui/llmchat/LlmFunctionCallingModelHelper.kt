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

package com.quantaliz.solaibot.ui.llmchat

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.quantaliz.solaibot.common.cleanUpMediapipeTaskErrorMessage
import com.quantaliz.solaibot.data.Accelerator
import com.quantaliz.solaibot.data.ConfigKeys
import com.quantaliz.solaibot.data.DEFAULT_MAX_TOKEN
import com.quantaliz.solaibot.data.DEFAULT_TEMPERATURE
import com.quantaliz.solaibot.data.DEFAULT_TOPK
import com.quantaliz.solaibot.data.DEFAULT_TOPP
import com.quantaliz.solaibot.data.Model
import com.quantaliz.solaibot.data.executeFunction
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.quantaliz.solaibot.data.generateFunctionCallingSystemPrompt
import com.quantaliz.solaibot.data.parseFunctionCall
import com.quantaliz.solaibot.data.x402.SettlementResponse
import kotlinx.serialization.json.Json
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.ResponseObserver
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.Session
import com.google.ai.edge.litertlm.SessionConfig

private const val TAG = "AGLlmFunctionCallingModelHelper"

// Note: This must match the structure of LlmModelInstance for compatibility
data class FunctionCallingModelInstance(
    val engine: Engine,
    var session: Session
) {
    val systemPrompt: String = generateFunctionCallingSystemPrompt()
    var conversationHistory: MutableList<ConversationTurn> = mutableListOf()
}

data class ConversationTurn(
    val role: String, // "user", "assistant", "function"
    val content: String
)

object LlmFunctionCallingModelHelper {
    // Indexed by model name.
    private val cleanUpListeners: MutableMap<String, CleanUpListener> = mutableMapOf()

    fun initialize(
        context: Context,
        model: Model,
        supportImage: Boolean,
        supportAudio: Boolean,
        onDone: (String) -> Unit,
    ) {
        try {
            Log.d(TAG, "Initializing function calling model...")

            // Prepare options
            val maxTokens = model.getIntConfigValue(key = ConfigKeys.MAX_TOKENS, defaultValue = DEFAULT_MAX_TOKEN)
            val topK = model.getIntConfigValue(key = ConfigKeys.TOPK, defaultValue = DEFAULT_TOPK)
            val topP = model.getFloatConfigValue(key = ConfigKeys.TOPP, defaultValue = DEFAULT_TOPP)
            val temperature = model.getFloatConfigValue(key = ConfigKeys.TEMPERATURE, defaultValue = DEFAULT_TEMPERATURE)
            val accelerator = model.getStringConfigValue(key = ConfigKeys.ACCELERATOR, defaultValue = Accelerator.GPU.label)

            val preferredBackend = when (accelerator) {
                Accelerator.CPU.label -> Backend.CPU
                Accelerator.GPU.label -> Backend.GPU
                else -> Backend.GPU
            }

            val engineConfig = EngineConfig(
                modelPath = model.getPath(context = context),
                backend = preferredBackend,
                visionBackend = if (supportImage) Backend.GPU else null,
                audioBackend = if (supportAudio) Backend.CPU else null,
                maxNumTokens = maxTokens,
                enableBenchmark = true,
            )

            // Create engine and session
            try {
                val engine = Engine(engineConfig)
                engine.initialize()

                val sessionConfig = SessionConfig(
                    SamplerConfig(topK = topK, topP = topP.toDouble(), temperature = temperature.toDouble())
                )
                val session = engine.createSession(sessionConfig)

                model.instance = FunctionCallingModelInstance(
                    engine = engine,
                    session = session
                )

                Log.d(TAG, "Function calling model initialized successfully with ${if (preferredBackend == Backend.GPU) "GPU" else "CPU"}")
                onDone("")
            } catch (e: Exception) {
                // Try CPU fallback if GPU failed
                if (preferredBackend == Backend.GPU) {
                    Log.w(TAG, "GPU initialization failed, trying CPU fallback: ${e.message}")
                    val cpuEngineConfig = EngineConfig(
                        modelPath = model.getPath(context = context),
                        backend = Backend.CPU,
                        visionBackend = if (supportImage) Backend.GPU else null,
                        audioBackend = if (supportAudio) Backend.CPU else null,
                        maxNumTokens = maxTokens,
                        enableBenchmark = true,
                    )
                    val engine = Engine(cpuEngineConfig)
                    engine.initialize()

                    val sessionConfig = SessionConfig(
                        SamplerConfig(topK = topK, topP = topP.toDouble(), temperature = temperature.toDouble())
                    )
                    val session = engine.createSession(sessionConfig)

                    model.instance = FunctionCallingModelInstance(
                        engine = engine,
                        session = session
                    )

                    Log.d(TAG, "Function calling model initialized successfully with CPU fallback")
                    onDone("")
                } else {
                    throw e
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize function calling model: ${e.message}", e)
            onDone(cleanUpMediapipeTaskErrorMessage(e.message ?: "Unknown error"))
        }
    }

    fun resetSession(model: Model, supportImage: Boolean, supportAudio: Boolean) {
        try {
            Log.d(TAG, "Resetting function calling session for model '${model.name}'")

            val instance = model.instance as? FunctionCallingModelInstance ?: return
            instance.session.close()

            val engine = instance.engine
            val topK = model.getIntConfigValue(key = ConfigKeys.TOPK, defaultValue = DEFAULT_TOPK)
            val topP = model.getFloatConfigValue(key = ConfigKeys.TOPP, defaultValue = DEFAULT_TOPP)
            val temperature = model.getFloatConfigValue(key = ConfigKeys.TEMPERATURE, defaultValue = DEFAULT_TEMPERATURE)

            val sessionConfig = SessionConfig(
                SamplerConfig(topK = topK, topP = topP.toDouble(), temperature = temperature.toDouble())
            )
            val newSession = engine.createSession(sessionConfig)
            instance.session = newSession
            instance.conversationHistory.clear()

            Log.d(TAG, "Function calling session reset done")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset function calling session", e)
        }
    }

    fun cleanUp(model: Model, onDone: () -> Unit) {
        if (model.instance == null) {
            onDone()
            return
        }

        val instance = model.instance as FunctionCallingModelInstance

        try {
            instance.session.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close function calling session: ${e.message}")
        }

        try {
            instance.engine.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close function calling engine: ${e.message}")
        }

        val onCleanUp = cleanUpListeners.remove(model.name)
        onCleanUp?.invoke()

        model.instance = null

        onDone()
        Log.d(TAG, "Function calling model cleanup done.")
    }

    fun runInference(
        model: Model,
        input: String,
        resultListener: ResultListener,
        cleanUpListener: CleanUpListener,
        images: List<Bitmap> = listOf(),
        audioClips: List<ByteArray> = listOf(),
        context: android.content.Context,
        activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender? = null
    ) {
        val instance = model.instance as FunctionCallingModelInstance

        // Set listener
        if (!cleanUpListeners.containsKey(model.name)) {
            cleanUpListeners[model.name] = cleanUpListener
        }

        try {
            // Add user input to conversation history
            instance.conversationHistory.add(ConversationTurn("user", input))

            // Build full prompt with system instruction and conversation history
            val fullPrompt = buildPrompt(instance)

            Log.d(TAG, "Sending prompt to model (length: ${fullPrompt.length})")

            val session = instance.session
            val inputDataList = mutableListOf<InputData>()

            // Add images if any
            for (image in images) {
                inputDataList.add(InputData.Image(image.toPngByteArray()))
            }

            // Add audio clips if any
            for (audioClip in audioClips) {
                inputDataList.add(InputData.Audio(audioClip))
            }

            // Add the text prompt
            inputDataList.add(InputData.Text(fullPrompt))

            val responseBuilder = StringBuilder()
            var bufferUntilSafe = true  // Buffer initial tokens while checking for function-call metadata
            val safeBuffer = StringBuilder()
            var potentialFunctionCall = false

            session.generateContentStream(
                inputDataList,
                object : ResponseObserver {
                    override fun onNext(response: String) {
                        responseBuilder.append(response)
                        val accumulated = responseBuilder.toString()
                        val trimmedAccumulated = accumulated.trimStart()

                        // Detect JSON function call early and suppress streaming
                        if (!potentialFunctionCall && trimmedAccumulated.startsWith("{\"name\"")) {
                            potentialFunctionCall = true
                            bufferUntilSafe = false
                            safeBuffer.clear()
                            return
                        }

                        if (potentialFunctionCall) {
                            // Suppress streaming until onDone confirms the call
                            return
                        }

                        // Buffer initial tokens so we only stream once we're sure it's regular text
                        if (bufferUntilSafe) {
                            safeBuffer.append(response)
                            // Once we have enough text and no JSON function call indicator, flush buffer
                            if (safeBuffer.length > 20 && !trimmedAccumulated.startsWith("{\"name\"")) {
                                bufferUntilSafe = false
                                resultListener(safeBuffer.toString(), false)
                                safeBuffer.clear()
                            }
                        } else {
                            // Normal streaming after buffer is flushed
                            resultListener(response, false)
                        }
                    }

                    override fun onDone() {
                        val fullResponse = responseBuilder.toString()
                        Log.d(TAG, "Model response completed: $fullResponse")

                        // Check if the response contains a function call
                        val functionCall = parseFunctionCall(fullResponse)

                        if (functionCall != null) {
                            Log.d(TAG, "Function call detected: ${functionCall.first}(${functionCall.second})")

                            // Add assistant's function call to history (keep the original for context)
                            instance.conversationHistory.add(ConversationTurn("assistant", fullResponse))

                            // Display user-friendly function call message
                            val functionDisplayName = when {
                                functionCall.first.startsWith("get_solana_balance") -> "get_solana_balance"
                                functionCall.first.startsWith("send_solana") -> "send_solana_transaction"
                                functionCall.first.startsWith("solana_payment") -> "make_solana_payment"
                                else -> functionCall.first
                            }
                            val callingMessage = "Calling... $functionDisplayName"
                            resultListener(callingMessage, false)

                            // All functions are Solana wallet functions that may require user interaction
                            val isWalletFunction = functionCall.first.startsWith("get_solana_balance") ||
                                                   functionCall.first.startsWith("send_solana") ||
                                                   functionCall.first.startsWith("solana_payment")

                            if (!isWalletFunction) {
                                // Execute regular functions synchronously
                                val functionResult = runBlocking {
                                    executeFunction(context, functionCall.first, functionCall.second)
                                }

                                Log.d(TAG, "Function result: $functionResult")

                                // Check if the result is an actionable error that should be displayed directly
                                val errorInfo = parseErrorOrInfoMessage(functionResult)
                                if (errorInfo != null) {
                                    Log.d(TAG, "Detected ${errorInfo.type} message, displaying directly to user")

                                    // Add the error to conversation history
                                    instance.conversationHistory.add(ConversationTurn("function", functionResult))
                                    instance.conversationHistory.add(ConversationTurn("assistant", errorInfo.userMessage))

                                    // Display directly to user without LLM interpretation
                                    resultListener("", true)
                                    resultListener(errorInfo.userMessage, false)
                                    resultListener("", true)
                                } else {
                                    // Add function result to history
                                    instance.conversationHistory.add(ConversationTurn("function", functionResult))
                                    instance.conversationHistory.add(ConversationTurn("assistant", functionResult))

                                    // Close the function-call bubble and emit the result directly to the user
                                    resultListener("", true)
                                    resultListener(functionResult, false)
                                    resultListener("", true)
                                }
                            } else {
                                // For wallet functions, we need special handling since they might involve user interaction
                                // Add the wallet processing message directly to conversation history so it shows up
                                instance.conversationHistory.add(ConversationTurn("assistant", "Processing wallet request..."))

                                // Signal completion of function call message (filtered out), then send processing message
                                resultListener("Processing wallet request...", false)

                                // Execute the actual wallet function async (in the background)
                                // This would trigger the wallet interaction
                                GlobalScope.launch(Dispatchers.Main) {
                                    val actualResult = executeFunction(context, functionCall.first, functionCall.second, activityResultSender)
                                    Log.d(TAG, "Wallet function actual result: $actualResult")

                                    // Check if the result is an actionable error that should be displayed directly
                                    val errorInfo = parseErrorOrInfoMessage(actualResult)
                                    if (errorInfo != null) {
                                        Log.d(TAG, "Detected ${errorInfo.type} message, displaying directly to user")

                                        // Add the error to conversation history
                                        instance.conversationHistory.add(ConversationTurn("function", actualResult))
                                        instance.conversationHistory.add(ConversationTurn("assistant", errorInfo.userMessage))

                                        // Display directly to user without LLM interpretation
                                        resultListener(errorInfo.userMessage, true)
                                    } else {
                                        // Add function result to history
                                        instance.conversationHistory.add(ConversationTurn("function", actualResult))

                                        // Check if this is a solana_payment function and extract x402 details
                                        val x402Details = if (functionCall.first.startsWith("solana_payment")) {
                                            extractX402SettlementInfo(actualResult)
                                        } else null

                                        // Create a new prompt with the updated conversation history to have the LLM process the actual result
                                        val updatedPrompt = buildPrompt(instance)
                                        val updatedResponseBuilder = StringBuilder()

                                        session.generateContentStream(
                                            listOf(InputData.Text(updatedPrompt)),
                                            object : ResponseObserver {
                                                override fun onNext(response: String) {
                                                    updatedResponseBuilder.append(response)
                                                    resultListener(response, false)
                                                }

                                                override fun onDone() {
                                                    val finalResponse = updatedResponseBuilder.toString()
                                                    Log.d(TAG, "Final response after wallet function: $finalResponse")

                                                    // Add final response to history
                                                    instance.conversationHistory.add(ConversationTurn("assistant", finalResponse))

                                                    // If we have x402 details, send them as a separate message bubble
                                                    if (x402Details != null) {
                                                        Log.d(TAG, "Sending x402 settlement details as separate message")
                                                        resultListener(x402Details, false)
                                                    }

                                                    resultListener("", true)
                                                }

                                                override fun onError(throwable: Throwable) {
                                                    Log.e(TAG, "Final response error after wallet function: ${throwable.message}", throwable)
                                                    resultListener("Error processing wallet function result: ${throwable.message}", true)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // No function call, just a normal response
                            if (potentialFunctionCall && fullResponse.isNotBlank()) {
                                // Looked like a function call but failed to parse; emit full response now
                                resultListener(fullResponse, false)
                                safeBuffer.clear()
                            } else if (safeBuffer.isNotEmpty()) {
                                resultListener(safeBuffer.toString(), false)
                                safeBuffer.clear()
                            }

                            instance.conversationHistory.add(ConversationTurn("assistant", fullResponse))
                            resultListener("", true)
                        }
                    }

                    override fun onError(throwable: Throwable) {
                        Log.e(TAG, "Failed to run inference: ${throwable.message}", throwable)
                        resultListener("Error: ${throwable.message}", true)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during function calling inference: ${e.message}", e)
            resultListener("Error: ${e.message}", true)
        }
    }

    private fun buildPrompt(instance: FunctionCallingModelInstance): String {
        val sb = StringBuilder()

        // Add system prompt
        sb.append(instance.systemPrompt)
        sb.append("\n\n")

        // Add conversation history
        for (turn in instance.conversationHistory) {
            when (turn.role) {
                "user" -> sb.append("User: ${turn.content}\n")
                "assistant" -> sb.append("Assistant: ${turn.content}\n")
                "function" -> sb.append("Function Result: ${turn.content}\n")
            }
        }

        sb.append("Assistant: ")

        return sb.toString()
    }

    private fun Bitmap.toPngByteArray(): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    /**
     * Data class to hold parsed error/info message details.
     */
    private data class ErrorOrInfoMessage(
        val type: String, // "ERROR" or "INFO"
        val code: String, // e.g., "WALLET_NOT_CONNECTED", "NO_TOKENS"
        val userMessage: String // The message to display to the user
    )

    /**
     * Parses error or info messages from function results.
     * Format: "ERROR:CODE:message" or "INFO:CODE:message"
     * Returns ErrorOrInfoMessage if found, null otherwise.
     */
    private fun parseErrorOrInfoMessage(functionResult: String): ErrorOrInfoMessage? {
        try {
            val pattern = """^(ERROR|INFO):([A-Z_]+):(.+)$""".toRegex()
            val match = pattern.find(functionResult.trim())

            if (match != null) {
                val type = match.groupValues[1]
                val code = match.groupValues[2]
                val message = match.groupValues[3]

                return ErrorOrInfoMessage(type, code, message)
            }

            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing error/info message: ${e.message}", e)
            return null
        }
    }

    /**
     * Extracts x402 settlement information from the function result string.
     * Returns a formatted message if settlement info is found, null otherwise.
     */
    private fun extractX402SettlementInfo(functionResult: String): String? {
        try {
            // Look for settlement information in the function result
            // The pattern matches the output from makeSolanaPayment in SolanaWalletFunctions.kt
            val settlementPattern = """Payment settled successfully!\s*Transaction:\s*([^\s]+)\s*Network:\s*([^\s]+)\s*Payer:\s*([^\s]+)""".toRegex()
            val match = settlementPattern.find(functionResult)

            if (match != null) {
                val transaction = match.groupValues[1]
                val network = match.groupValues[2]
                val payer = match.groupValues[3]

                // Also try to extract the premium content/response body
                val responsePattern = """Response:\s*(.+)""".toRegex(RegexOption.DOT_MATCHES_ALL)
                val responseMatch = responsePattern.find(functionResult)
                val premiumContent = responseMatch?.groupValues?.get(1)?.trim() ?: ""

                return buildString {
                    append("━━━━━━━━━━━━━━━━━━━━━━\n")
                    append("💳 x402 Payment Details\n")
                    append("━━━━━━━━━━━━━━━━━━━━━━\n\n")
                    append("✅ Payment Status: Success\n\n")
                    append("🔗 Transaction Hash:\n")
                    append("`$transaction`\n\n")
                    append("🌐 Network: $network\n\n")
                    append("👤 Payer Address:\n")
                    append("`$payer`\n\n")
                    if (premiumContent.isNotEmpty()) {
                        append("━━━━━━━━━━━━━━━━━━━━━━\n")
                        append("📦 Premium Content\n")
                        append("━━━━━━━━━━━━━━━━━━━━━━\n\n")
                        append(premiumContent)
                    }
                }
            }

            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting x402 settlement info: ${e.message}", e)
            return null
        }
    }
}
