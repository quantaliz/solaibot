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

package com.quantaliz.solaibot.ui.common.modelitem

// import androidx.compose.ui.tooling.preview.Preview
// import com.quantaliz.solaibot.ui.theme.GalleryTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.quantaliz.solaibot.data.MODEL_INFO_ICON_SIZE
import com.quantaliz.solaibot.data.Model
import com.quantaliz.solaibot.data.ModelDownloadStatus
import com.quantaliz.solaibot.data.ModelDownloadStatusType
import com.quantaliz.solaibot.data.Task
import com.quantaliz.solaibot.ui.common.getTaskBgGradientColors
import com.quantaliz.solaibot.ui.theme.customColors

/** Composable function to display an icon representing the download status of a model. */
@Composable
fun StatusIcon(
  task: Task,
  model: Model,
  downloadStatus: ModelDownloadStatus?,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
    modifier = modifier,
  ) {
    if (model.localFileRelativeDirPathOverride.isNotEmpty()) {
      Icon(
        Icons.Filled.DownloadForOffline,
        tint = getTaskBgGradientColors(task = task)[1],
        contentDescription = "",
        modifier = Modifier.size(MODEL_INFO_ICON_SIZE),
      )
    } else {
      when (downloadStatus?.status) {
        ModelDownloadStatusType.NOT_DOWNLOADED ->
          Icon(
            Icons.AutoMirrored.Outlined.HelpOutline,
            tint = MaterialTheme.customColors.modelInfoIconColor,
            contentDescription = "",
            modifier = Modifier.size(MODEL_INFO_ICON_SIZE),
          )

        ModelDownloadStatusType.SUCCEEDED -> {
          Icon(
            Icons.Filled.DownloadForOffline,
            tint = getTaskBgGradientColors(task = task)[1],
            contentDescription = "",
            modifier = Modifier.size(MODEL_INFO_ICON_SIZE),
          )
        }

        ModelDownloadStatusType.FAILED ->
          Icon(
            Icons.Rounded.Error,
            tint = Color(0xFFAA0000),
            contentDescription = "",
            modifier = Modifier.size(MODEL_INFO_ICON_SIZE),
          )

        ModelDownloadStatusType.IN_PROGRESS ->
          Icon(
            Icons.Rounded.Downloading,
            contentDescription = "",
            modifier = Modifier.size(MODEL_INFO_ICON_SIZE),
          )

        else -> {}
      }
    }
  }
}

// @Preview(showBackground = true)
// @Composable
// fun StatusIconPreview() {
//   GalleryTheme {
//     Column {
//       for (downloadStatus in
//         listOf(
//           ModelDownloadStatus(status = ModelDownloadStatusType.NOT_DOWNLOADED),
//           ModelDownloadStatus(status = ModelDownloadStatusType.IN_PROGRESS),
//           ModelDownloadStatus(status = ModelDownloadStatusType.SUCCEEDED),
//           ModelDownloadStatus(status = ModelDownloadStatusType.FAILED),
//           ModelDownloadStatus(status = ModelDownloadStatusType.UNZIPPING),
//           ModelDownloadStatus(status = ModelDownloadStatusType.PARTIALLY_DOWNLOADED),
//         )) {
//         StatusIcon(downloadStatus = downloadStatus)
//       }
//     }
//   }
// }
