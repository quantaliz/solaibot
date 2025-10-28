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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Mms
import androidx.compose.runtime.Composable
import com.quantaliz.solaibot.R
import com.quantaliz.solaibot.customtasks.common.CustomTask
import com.quantaliz.solaibot.customtasks.common.CustomTaskDataForBuiltinTask
import com.quantaliz.solaibot.data.BuiltInTaskId
import com.quantaliz.solaibot.data.Category
import com.quantaliz.solaibot.data.MODEL_GEMINI_3N_E2B
import com.quantaliz.solaibot.data.MODEL_GEMINI_3N_E4B
import com.quantaliz.solaibot.data.PHI4_MINI_INSTRUCT
import com.quantaliz.solaibot.data.Model
import com.quantaliz.solaibot.data.Task
import com.quantaliz.solaibot.ui.llmchat.LlmFunctionCallingModelHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

////////////////////////////////////////////////////////////////////////////////////////////////////
// AI Chat.

class LlmChatTask @Inject constructor() : CustomTask {
  override val task: Task =
    Task(
      id = BuiltInTaskId.LLM_CHAT,
      label = "SolAIBot",
      category = Category.LLM,
      iconVectorResourceId = R.drawable.ic_robot,
      models = mutableListOf(MODEL_GEMINI_3N_E2B, MODEL_GEMINI_3N_E4B, PHI4_MINI_INSTRUCT),
      description = "On-device Large Language Model that can make x402 payments. Developed for Hackaroo & Cypherpunk",
      docUrl = "https://quantaliz.com/hackaroo",
      sourceCodeUrl =
        "https://github.com/quantaliz/solaibot",
      textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    if (model.llmSupportFunctionCalling) {
      LlmFunctionCallingModelHelper.initialize(
        context = context,
        model = model,
        supportImage = false,
        supportAudio = false,
        onDone = onDone,
      )
    } else {
      LlmChatModelHelper.initialize(
        context = context,
        model = model,
        supportImage = false,
        supportAudio = false,
        onDone = onDone,
      )
    }
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    if (model.llmSupportFunctionCalling) {
      LlmFunctionCallingModelHelper.cleanUp(model = model, onDone = onDone)
    } else {
      LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
    }
  }

  @Composable
  override fun MainScreen(data: Any) {
    val myData = data as CustomTaskDataForBuiltinTask
    LlmChatScreen(
      modelManagerViewModel = myData.modelManagerViewModel,
      navigateUp = myData.onNavUp,
      activityResultSender = myData.activityResultSender
    )
  }
}

@Module
@InstallIn(SingletonComponent::class) // Or another component that fits your scope
internal object LlmChatTaskModule {
  @Provides
  @IntoSet
  fun provideTask(): CustomTask {
    return LlmChatTask()
  }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Ask image.

class LlmAskImageTask @Inject constructor() : CustomTask {
  override val task: Task =
    Task(
      id = BuiltInTaskId.LLM_ASK_IMAGE,
      label = "Ask Image",
      category = Category.LLM,
      icon = Icons.Outlined.Mms,
      models = mutableListOf(),
      description = "Ask questions about images with on-device large language models",
      docUrl = "https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android",
      sourceCodeUrl =
        "https://github.com/google-ai-edge/gallery/blob/main/Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatModelHelper.kt",
      textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    if (model.llmSupportFunctionCalling) {
      LlmFunctionCallingModelHelper.initialize(
        context = context,
        model = model,
        supportImage = true,
        supportAudio = false,
        onDone = onDone,
      )
    } else {
      LlmChatModelHelper.initialize(
        context = context,
        model = model,
        supportImage = true,
        supportAudio = false,
        onDone = onDone,
      )
    }
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    if (model.llmSupportFunctionCalling) {
      LlmFunctionCallingModelHelper.cleanUp(model = model, onDone = onDone)
    } else {
      LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
    }
  }

  @Composable
  override fun MainScreen(data: Any) {
    val myData = data as CustomTaskDataForBuiltinTask
    LlmAskImageScreen(
      modelManagerViewModel = myData.modelManagerViewModel,
      navigateUp = myData.onNavUp,
      activityResultSender = myData.activityResultSender
    )
  }
}

@Module
@InstallIn(SingletonComponent::class) // Or another component that fits your scope
internal object LlmAskImageModule {
  @Provides
  @IntoSet
  fun provideTask(): CustomTask {
    return LlmAskImageTask()
  }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Ask audio.

class LlmAskAudioTask @Inject constructor() : CustomTask {
  override val task: Task =
    Task(
      id = BuiltInTaskId.LLM_ASK_AUDIO,
      label = "Audio Scribe",
      category = Category.LLM,
      icon = Icons.Outlined.Mic,
      models = mutableListOf(),
      description =
        "Instantly transcribe and/or translate audio clips using on-device large language models",
      docUrl = "https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android",
      sourceCodeUrl =
        "https://github.com/google-ai-edge/gallery/blob/main/Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatModelHelper.kt",
      textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    if (model.llmSupportFunctionCalling) {
      LlmFunctionCallingModelHelper.initialize(
        context = context,
        model = model,
        supportImage = false,
        supportAudio = true,
        onDone = onDone,
      )
    } else {
      LlmChatModelHelper.initialize(
        context = context,
        model = model,
        supportImage = false,
        supportAudio = true,
        onDone = onDone,
      )
    }
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    if (model.llmSupportFunctionCalling) {
      LlmFunctionCallingModelHelper.cleanUp(model = model, onDone = onDone)
    } else {
      LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
    }
  }

  @Composable
  override fun MainScreen(data: Any) {
    val myData = data as CustomTaskDataForBuiltinTask
    LlmAskAudioScreen(
      modelManagerViewModel = myData.modelManagerViewModel,
      navigateUp = myData.onNavUp,
      activityResultSender = myData.activityResultSender
    )
  }
}

@Module
@InstallIn(SingletonComponent::class) // Or another component that fits your scope
internal object LlmAskAudioModule {
  @Provides
  @IntoSet
  fun provideTask(): CustomTask {
    return LlmAskAudioTask()
  }
}
