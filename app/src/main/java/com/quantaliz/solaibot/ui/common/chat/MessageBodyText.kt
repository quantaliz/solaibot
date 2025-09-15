/*
 * Copyright 2025 Google LLC
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

package com.quantaliz.solaibot.ui.common.chat

// import com.quantaliz.solaibot.ui.theme.GalleryTheme
// import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quantaliz.solaibot.ui.common.MarkdownText

/** Composable function to display the text content of a ChatMessageText. */
@Composable
fun MessageBodyText(message: ChatMessageText) {
  if (message.side == ChatSide.USER) {
    MarkdownText(
      text = message.content,
      modifier = Modifier.padding(12.dp),
      textColor = Color.White,
    )
  } else if (message.side == ChatSide.AGENT) {
    if (message.isMarkdown) {
      MarkdownText(
        text = message.content,
        modifier =
          Modifier.padding(12.dp).semantics(mergeDescendants = true) {
            contentDescription = "Chat message content text markdown"
          },
      )
    } else {
      Text(
        message.content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(12.dp),
      )
    }
  }
}

// @Preview(showBackground = true)
// @Composable
// fun MessageBodyTextPreview() {
//   GalleryTheme {
//     Column {
//       Row(modifier = Modifier.padding(16.dp).background(MaterialTheme.colorScheme.primary)) {
//         MessageBodyText(ChatMessageText(content = "Hello world", side = ChatSide.USER))
//       }
//       Row(
//         modifier = Modifier.padding(16.dp).background(MaterialTheme.colorScheme.surfaceContainer)
//       ) {
//         MessageBodyText(ChatMessageText(content = "yes hello world", side = ChatSide.AGENT))
//       }
//     }
//   }
// }
