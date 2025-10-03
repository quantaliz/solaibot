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
import com.quantaliz.solaibot.data.MAX_IMAGE_COUNT
import com.quantaliz.solaibot.data.Model
import com.quantaliz.solaibot.data.executeFunction
import com.quantaliz.solaibot.data.hammerTool
import com.google.ai.edge.localagents.Chat
import com.google.ai.edge.localagents.Content
import com.google.ai.edge.localagents.FunctionResponse
import com.google.ai.edge.localagents.GenerativeModel
import com.google.ai.edge.localagents.HammerFormatter
import com.google.ai.edge.localagents.LlmInference
import com.google.ai.edge.localagents.LlmInferenceBackend
import com.google.ai.edge.localagents.LlmInferenceOptions
import com.google.ai.edge.localagents.Part
import com.google.ai.edge.localagents.Struct
import com.google.ai.edge.localagents.Value
import java.io.ByteArrayOutputStream

private const val TAG = "AGLlmFunctionCallingModelHelper"

typealias FunctionCallingResultListener = (partialResult: String, done: Boolean) -> Unit

typealias FunctionCallingCleanUpListener = () -> Unit

data class FunctionCallingModelInstance(
    val generativeModel: GenerativeModel,
    var chat: Chat
)

object LlmFunctionCallingModelHelper {
    // Indexed by model name.
    private val cleanUpListeners: MutableMap<String, FunctionCallingCleanUpListener> = mutableMapOf()

    fun initialize(
        context: Context,
        model: Model,
        supportImage: Boolean,
        supportAudio: Boolean,
        onDone: (String) -> Unit,
    ) {
        try {
            Log.d(TAG, "Initializing function calling model...")

            // Create LLM Inference backend
            val llmInferenceOptions = LlmInferenceOptions.builder()
                .setModelPath(model.getPath(context = context))
                .build()

            val llmInference = LlmInference.createFromOptions(context, llmInferenceOptions)
            val llmInferenceBackend = LlmInferenceBackend(llmInference, HammerFormatter())

            // Create system instruction
            val systemInstruction = Content.newBuilder()
                .setRole("system")
                .addParts(Part.newBuilder().setText("You are a helpful assistant with access to various tools and functions. Use function calling when appropriate to provide accurate information."))
                .build()

            // Create generative model with function calling capabilities
            val generativeModel = GenerativeModel(
                llmInferenceBackend,
                systemInstruction,
                listOf(hammerTool)
            )

            // Start chat session
            val chat = generativeModel.startChat()

            model.instance = FunctionCallingModelInstance(
                generativeModel = generativeModel,
                chat = chat
            )

            Log.d(TAG, "Function calling model initialized successfully")
            onDone("")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize function calling model: ${e.message}", e)
            onDone(cleanUpMediapipeTaskErrorMessage(e.message ?: "Unknown error"))
        }
    }

    fun resetSession(model: Model, supportImage: Boolean, supportAudio: Boolean) {
        try {
            Log.d(TAG, "Resetting function calling session for model '${model.name}'")

            val instance = model.instance as FunctionCallingModelInstance?

            if (instance != null) {
                // Create a new chat session
                val newChat = instance.generativeModel.startChat()
                instance.chat = newChat
                Log.d(TAG, "Function calling session reset done")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to reset function calling session", e)
        }
    }

    fun cleanUp(model: Model, onDone: () -> Unit) {
        if (model.instance == null) {
            onDone()
            return
        }

        val instance = model.instance as FunctionCallingModelInstance

        try {
            // Close the LLM inference backend
            instance.generativeModel.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close function calling model: ${e.message}")
        }

        val onCleanUp = cleanUpListeners.remove(model.name)
        if (onCleanUp != null) {
            onCleanUp()
        }
        model.instance = null

        onDone()
        Log.d(TAG, "Function calling model cleanup done.")
    }

    fun runInference(
        model: Model,
        input: String,
        resultListener: FunctionCallingResultListener,
        cleanUpListener: FunctionCallingCleanUpListener,
        images: List<Bitmap> = listOf(),
        audioClips: List<ByteArray> = listOf(),
    ) {
        val instance = model.instance as FunctionCallingModelInstance

        // Set listener.
        if (!cleanUpListeners.containsKey(model.name)) {
            cleanUpListeners[model.name] = cleanUpListener
        }

        try {
            // Send message to the model
            val response = instance.chat.sendMessage(input)

            // Process the response
            val candidates = response.candidates
            if (candidates.isNotEmpty()) {
                val content = candidates[0].content
                val parts = content.parts
                for (part in parts) {
                    when {
                        part.hasText() -> {
                            // Regular text response
                            resultListener(part.text, false)
                        }
                        part.hasFunctionCall() -> {
                        // Function call detected
                        val functionCall = part.functionCall
                        val functionName = functionCall.name
                        val args = mutableMapOf<String, String>()
                        val fieldsMap = functionCall.args.fieldsMap
                        for (key in fieldsMap.keys) {
                            args[key] = fieldsMap[key]?.stringValue ?: ""
                        }

                        Log.d(TAG, "Function call detected: $functionName with args: $args")

                        // Execute the function
                        val functionResult = executeFunction(functionName, args)

                        // Send function response back to the model
                        val functionResponse = FunctionResponse.newBuilder()
                            .setName(functionName)
                            .setResponse(
                                Struct.newBuilder()
                                    .putFields("result", Value.newBuilder().setStringValue(functionResult).build())
                            )
                            .build()

                        val functionResponseContent = Content.newBuilder()
                            .setRole("user")
                            .addParts(Part.newBuilder().setFunctionResponse(functionResponse))
                            .build()

                        // Get the model's response to the function result
                        val followUpResponse = instance.chat.sendMessage(functionResponseContent)

                        // Return the follow-up response
                        val candidates = followUpResponse.candidates
                        if (candidates.isNotEmpty()) {
                            val content = candidates[0].content
                            val parts = content.parts
                            for (followUpPart in parts) {
                                if (followUpPart.hasText()) {
                                    resultListener(followUpPart.text, false)
                                }
                            }
                        }
                    }
                }
            }

            // Mark as done
            resultListener("", true)

        } catch (e: Exception) {
            Log.e(TAG, "Error during function calling inference: ${e.message}", e)
            resultListener("Error: ${e.message}", true)
        }
    }

    private fun Bitmap.toPngByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}