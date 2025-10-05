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

import androidx.hilt.navigation.compose.hiltViewModel

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.bundleOf
import com.quantaliz.solaibot.data.BuiltInTaskId
import com.quantaliz.solaibot.firebaseAnalytics
import com.quantaliz.solaibot.ui.common.chat.ChatMessageAudioClip
import com.quantaliz.solaibot.ui.common.chat.ChatMessageImage
import com.quantaliz.solaibot.ui.common.chat.ChatMessageText
import com.quantaliz.solaibot.ui.common.chat.ChatView
import com.quantaliz.solaibot.ui.modelmanager.ModelManagerViewModel

@Composable
fun LlmChatScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: LlmChatViewModel = hiltViewModel(),
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    taskId = BuiltInTaskId.LLM_CHAT,
    navigateUp = navigateUp,
    modifier = modifier,
  )
}

@Composable
fun LlmAskImageScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: LlmAskImageViewModel = hiltViewModel(),
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    taskId = BuiltInTaskId.LLM_ASK_IMAGE,
    navigateUp = navigateUp,
    modifier = modifier,
  )
}

@Composable
fun LlmAskAudioScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: LlmAskAudioViewModel = hiltViewModel(),
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    taskId = BuiltInTaskId.LLM_ASK_AUDIO,
    navigateUp = navigateUp,
    modifier = modifier,
  )
}

@Composable
fun ChatViewWrapper(
  viewModel: LlmChatViewModelBase,
  modelManagerViewModel: ModelManagerViewModel,
  taskId: String,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val task = modelManagerViewModel.getTaskById(id = taskId)!!

  ChatView(
    task = task,
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    onSendMessage = { model, messages ->
      for (message in messages) {
        viewModel.addMessage(model = model, message = message)
      }

      var text = ""
      val images: MutableList<Bitmap> = mutableListOf()
      val audioMessages: MutableList<ChatMessageAudioClip> = mutableListOf()
      var chatMessageText: ChatMessageText? = null
      for (message in messages) {
        if (message is ChatMessageText) {
          chatMessageText = message
          text = message.content
        } else if (message is ChatMessageImage) {
          images.addAll(message.bitmaps)
        } else if (message is ChatMessageAudioClip) {
          audioMessages.add(message)
        }
      }
      if ((text.isNotEmpty() && chatMessageText != null) || audioMessages.isNotEmpty()) {
        modelManagerViewModel.addTextInputHistory(text)
        if (model.llmSupportFunctionCalling) {
          viewModel.generateResponseWithContext(
            context = context,
            model = model,
            input = text,
            images = images,
            audioMessages = audioMessages,
            onError = {
              viewModel.handleError(
                context = context,
                task = task,
                model = model,
                modelManagerViewModel = modelManagerViewModel,
                triggeredMessage = chatMessageText,
              )
            },
          )
        } else {
          viewModel.generateResponse(
            model = model,
            input = text,
            images = images,
            audioMessages = audioMessages,
            onError = {
              viewModel.handleError(
                context = context,
                task = task,
                model = model,
                modelManagerViewModel = modelManagerViewModel,
                triggeredMessage = chatMessageText,
              )
            },
          )
        }

        firebaseAnalytics?.logEvent(
          "generate_action",
          bundleOf("capability_name" to task.id, "model_id" to model.name),
        )
      }
    },
    onRunAgainClicked = { model, message ->
      if (message is ChatMessageText) {
        viewModel.runAgain(
          model = model,
          message = message,
          onError = {
            viewModel.handleError(
              context = context,
              task = task,
              model = model,
              modelManagerViewModel = modelManagerViewModel,
              triggeredMessage = message,
            )
          },
        )
      }
    },
    onBenchmarkClicked = { _, _, _, _ -> },
    onResetSessionClicked = { model -> viewModel.resetSession(task = task, model = model) },
    showStopButtonInInputWhenInProgress = true,
    onStopButtonClicked = { model -> viewModel.stopResponse(model = model) },
    navigateUp = navigateUp,
    modifier = modifier,
  )
}
