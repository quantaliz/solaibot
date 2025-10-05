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

package com.quantaliz.solaibot.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quantaliz.solaibot.data.Task
import com.quantaliz.solaibot.ui.theme.customColors

@Composable
fun getTaskBgColor(task: Task): Color {
  val colorIndex: Int = if (task.index >= 0) positiveMod(task.index, MaterialTheme.customColors.taskBgColors.size) else if (task.id == "llm_chat") 2 else 0
  return MaterialTheme.customColors.taskBgColors[colorIndex]
}

@Composable
fun getTaskBgGradientColors(task: Task): List<Color> {
  val colorIndex: Int = if (task.index >= 0) positiveMod(task.index, MaterialTheme.customColors.taskBgGradientColors.size) else if (task.id == "llm_chat") 2 else 0
  return MaterialTheme.customColors.taskBgGradientColors[colorIndex]
}

@Composable
fun getTaskIconColor(task: Task): Color {
  val colorIndex: Int = if (task.index >= 0) positiveMod(task.index, MaterialTheme.customColors.taskIconColors.size) else if (task.id == "llm_chat") 2 else 0
  return MaterialTheme.customColors.taskIconColors[colorIndex]
}

@Composable
fun getTaskIconColor(index: Int): Color {
  val colorIndex: Int = if (index >= 0) positiveMod(index, MaterialTheme.customColors.taskIconColors.size) else 0
  return MaterialTheme.customColors.taskIconColors[colorIndex]
}

private fun positiveMod(x: Int, y: Int): Int {
  val result = x % y
  return if (result < 0) result + y else result
}
