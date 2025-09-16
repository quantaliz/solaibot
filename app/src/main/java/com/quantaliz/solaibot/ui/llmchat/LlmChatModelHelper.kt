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
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.AudioModelOptions
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession

private const val TAG = "AGLlmChatModelHelper"

typealias ResultListener = (partialResult: String, done: Boolean) -> Unit

typealias CleanUpListener = () -> Unit

data class LlmModelInstance(val engine: LlmInference, var session: LlmInferenceSession)

object LlmChatModelHelper {
  // Indexed by model name.
  private val cleanUpListeners: MutableMap<String, CleanUpListener> = mutableMapOf()

  fun initialize(
    context: Context,
    model: Model,
    supportImage: Boolean,
    supportAudio: Boolean,
    onDone: (String) -> Unit,
  ) {
    // Prepare options.
    val maxTokens =
      model.getIntConfigValue(key = ConfigKeys.MAX_TOKENS, defaultValue = DEFAULT_MAX_TOKEN)
    val topK = model.getIntConfigValue(key = ConfigKeys.TOPK, defaultValue = DEFAULT_TOPK)
    val topP = model.getFloatConfigValue(key = ConfigKeys.TOPP, defaultValue = DEFAULT_TOPP)
    val temperature =
      model.getFloatConfigValue(key = ConfigKeys.TEMPERATURE, defaultValue = DEFAULT_TEMPERATURE)
    val accelerator =
      model.getStringConfigValue(key = ConfigKeys.ACCELERATOR, defaultValue = Accelerator.GPU.label)
    Log.d(TAG, "Initializing...")
    val shouldEnableImage = supportImage
    val shouldEnableAudio = supportAudio
    Log.d(TAG, "Enable image: $shouldEnableImage, enable audio: $shouldEnableAudio")
    val preferredBackend =
      when (accelerator) {
        Accelerator.CPU.label -> LlmInference.Backend.CPU
        Accelerator.GPU.label -> LlmInference.Backend.GPU
        else -> LlmInference.Backend.GPU
      }
    val optionsBuilder =
      LlmInference.LlmInferenceOptions.builder()
        .setModelPath(model.getPath(context = context))
        .setMaxTokens(maxTokens)
        .setPreferredBackend(preferredBackend)
        .setMaxNumImages(if (shouldEnableImage) MAX_IMAGE_COUNT else 0)
    if (shouldEnableAudio) {
      optionsBuilder.setAudioModelOptions(AudioModelOptions.builder().build())
    }
    val options = optionsBuilder.build()

    // Create an instance of the LLM Inference task and session.
    try {
      val llmInference = LlmInference.createFromOptions(context, options)

      val session =
        LlmInferenceSession.createFromOptions(
          llmInference,
          LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(topK)
            .setTopP(topP)
            .setTemperature(temperature)
            .setGraphOptions(
              GraphOptions.builder()
                .setEnableVisionModality(shouldEnableImage)
                .setEnableAudioModality(shouldEnableAudio)
                .build()
            )
            .build(),
        )
      model.instance = LlmModelInstance(engine = llmInference, session = session)
    } catch (e: Exception) {
      onDone(cleanUpMediapipeTaskErrorMessage(e.message ?: "Unknown error"))
      return
    }
    onDone("")
  }

  fun resetSession(model: Model, supportImage: Boolean, supportAudio: Boolean) {
    try {
      Log.d(TAG, "Resetting session for model '${model.name}'")

      val instance = model.instance as LlmModelInstance? ?: return
      val session = instance.session
      session.close()

      val inference = instance.engine
      val topK = model.getIntConfigValue(key = ConfigKeys.TOPK, defaultValue = DEFAULT_TOPK)
      val topP = model.getFloatConfigValue(key = ConfigKeys.TOPP, defaultValue = DEFAULT_TOPP)
      val temperature =
        model.getFloatConfigValue(key = ConfigKeys.TEMPERATURE, defaultValue = DEFAULT_TEMPERATURE)
      val shouldEnableImage = supportImage
      val shouldEnableAudio = supportAudio
      Log.d(TAG, "Enable image: $shouldEnableImage, enable audio: $shouldEnableAudio")
      val newSession =
        LlmInferenceSession.createFromOptions(
          inference,
          LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(topK)
            .setTopP(topP)
            .setTemperature(temperature)
            .setGraphOptions(
              GraphOptions.builder()
                .setEnableVisionModality(shouldEnableImage)
                .setEnableAudioModality(shouldEnableAudio)
                .build()
            )
            .build(),
        )
      instance.session = newSession
      Log.d(TAG, "Resetting done")
    } catch (e: Exception) {
      Log.d(TAG, "Failed to reset session", e)
    }
  }

  fun cleanUp(model: Model, onDone: () -> Unit) {
    if (model.instance == null) {
      return
    }

    val instance = model.instance as LlmModelInstance

    try {
      instance.session.close()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to close the LLM Inference session: ${e.message}")
    }

    try {
      instance.engine.close()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to close the LLM Inference engine: ${e.message}")
    }

    val onCleanUp = cleanUpListeners.remove(model.name)
    if (onCleanUp != null) {
      onCleanUp()
    }
    model.instance = null

    onDone()
    Log.d(TAG, "Clean up done.")
  }

  fun runInference(
    model: Model,
    input: String,
    resultListener: ResultListener,
    cleanUpListener: CleanUpListener,
    images: List<Bitmap> = listOf(),
    audioClips: List<ByteArray> = listOf(),
  ) {
    val instance = model.instance as LlmModelInstance

    // Set listener.
    if (!cleanUpListeners.containsKey(model.name)) {
      cleanUpListeners[model.name] = cleanUpListener
    }

    // Start async inference.
    //
    // For a model that supports image modality, we need to add the text query chunk before adding
    // image.
    val session = instance.session
    if (input.trim().isNotEmpty()) {
      session.addQueryChunk(input)
    }
    for (image in images) {
      session.addImage(BitmapImageBuilder(image).build())
    }
    for (audioClip in audioClips) {
      session.addAudio(audioClip)
    }
    val unused = session.generateResponseAsync(resultListener)
  }
}
