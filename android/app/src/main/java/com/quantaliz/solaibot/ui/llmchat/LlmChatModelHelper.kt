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
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.ResponseObserver
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.Session
import com.google.ai.edge.litertlm.SessionConfig
import java.io.ByteArrayOutputStream

private const val TAG = "AGLlmChatModelHelper"

typealias ResultListener = (partialResult: String, done: Boolean) -> Unit

typealias CleanUpListener = () -> Unit

data class LlmModelInstance(val engine: Engine, var session: Session)

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
        Accelerator.CPU.label -> Backend.CPU
        Accelerator.GPU.label -> Backend.GPU
        else -> Backend.GPU
      }
    val engineConfig =
      EngineConfig(
        modelPath = model.getPath(context = context),
        backend = preferredBackend,
        visionBackend = if (shouldEnableImage) Backend.GPU else null, // must be GPU for Gemma 3n
        audioBackend = if (shouldEnableAudio) Backend.CPU else null, // must be CPU for Gemma 3n
        maxNumTokens = maxTokens,
        enableBenchmark = true,
      )

    // Create an instance of the LLM Inference task and session.
    // Try GPU first, fallback to CPU if it fails
    try {
      val engine = Engine(engineConfig)
      engine.initialize()

      val sessionConfig =
        SessionConfig(
          SamplerConfig(topK = topK, topP = topP.toDouble(), temperature = temperature.toDouble())
        )
      val session = engine.createSession(sessionConfig)
      model.instance = LlmModelInstance(engine = engine, session = session)
      Log.d(TAG, "Model initialized successfully with ${if (preferredBackend == Backend.GPU) "GPU" else "CPU"}")
    } catch (e: Exception) {
      // If GPU failed, try CPU fallback
      if (preferredBackend == Backend.GPU) {
        Log.w(TAG, "GPU initialization failed, trying CPU fallback: ${e.message}")
        try {
          val cpuEngineConfig =
            EngineConfig(
              modelPath = model.getPath(context = context),
              backend = Backend.CPU,
              visionBackend = if (shouldEnableImage) Backend.GPU else null,
              audioBackend = if (shouldEnableAudio) Backend.CPU else null,
              maxNumTokens = maxTokens,
              enableBenchmark = true,
            )
          val engine = Engine(cpuEngineConfig)
          engine.initialize()

          val sessionConfig =
            SessionConfig(
              SamplerConfig(topK = topK, topP = topP.toDouble(), temperature = temperature.toDouble())
            )
          val session = engine.createSession(sessionConfig)
          model.instance = LlmModelInstance(engine = engine, session = session)
          Log.d(TAG, "Model initialized successfully with CPU fallback")
        } catch (cpuException: Exception) {
          Log.e(TAG, "CPU fallback also failed: ${cpuException.message}", cpuException)
          onDone(cleanUpMediapipeTaskErrorMessage(cpuException.message ?: "Unknown error"))
          return
        }
      } else {
        onDone(cleanUpMediapipeTaskErrorMessage(e.message ?: "Unknown error"))
        return
      }
    }
    onDone("")
  }

  fun resetSession(model: Model, supportImage: Boolean, supportAudio: Boolean) {
    try {
      Log.d(TAG, "Resetting session for model '${model.name}'")

      val instance = model.instance as LlmModelInstance? ?: return
      instance.session.close()

      val engine = instance.engine
      val topK = model.getIntConfigValue(key = ConfigKeys.TOPK, defaultValue = DEFAULT_TOPK)
      val topP = model.getFloatConfigValue(key = ConfigKeys.TOPP, defaultValue = DEFAULT_TOPP)
      val temperature =
        model.getFloatConfigValue(key = ConfigKeys.TEMPERATURE, defaultValue = DEFAULT_TEMPERATURE)
      val shouldEnableImage = supportImage
      val shouldEnableAudio = supportAudio
      Log.d(TAG, "Enable image: $shouldEnableImage, enable audio: $shouldEnableAudio")

      val sessionConfig =
        SessionConfig(
          SamplerConfig(topK = topK, topP = topP.toDouble(), temperature = temperature.toDouble())
        )
      val newSession = engine.createSession(sessionConfig)
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

    val session = instance.session
    val inputDataList = mutableListOf<InputData>()
    for (image in images) {
      inputDataList.add(InputData.Image(image.toPngByteArray()))
    }
    for (audioClip in audioClips) {
      inputDataList.add(InputData.Audio(audioClip))
    }
    // add the text after image and audio for the accurate last token
    if (input.trim().isNotEmpty()) {
      inputDataList.add(InputData.Text(input))
    }

    session.generateContentStream(
      inputDataList,
      object : ResponseObserver {
        override fun onNext(response: String) {
          resultListener(response, false)
        }

        override fun onDone() {
          resultListener("", true)
        }

        override fun onError(throwable: Throwable) {
          Log.e(TAG, "Failed to run inference: ${throwable.message}", throwable)
          resultListener("Error: ${throwable.message}", true)
        }
      },
    )
  }

  private fun Bitmap.toPngByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
  }
}
