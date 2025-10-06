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

package com.quantaliz.solaibot.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.quantaliz.solaibot.customtasks.common.CustomTaskData
import com.quantaliz.solaibot.customtasks.common.CustomTaskDataForBuiltinTask
import com.quantaliz.solaibot.data.ModelDownloadStatusType
import com.quantaliz.solaibot.data.Task
import com.quantaliz.solaibot.data.isBuiltInTask
import com.quantaliz.solaibot.ui.common.ErrorDialog
import com.quantaliz.solaibot.ui.common.ModelPageAppBar
import com.quantaliz.solaibot.ui.common.chat.ModelDownloadStatusInfoPanel
import com.quantaliz.solaibot.ui.home.HomeScreen
import com.quantaliz.solaibot.ui.modelmanager.ModelInitializationStatusType
import com.quantaliz.solaibot.ui.modelmanager.ModelManager
import com.quantaliz.solaibot.ui.modelmanager.ModelManagerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AGGalleryNavGraph"
private const val ROUTE_PLACEHOLDER = "placeholder"
private const val ROUTE_MODEL = "route_model"
private const val ENTER_ANIMATION_DURATION_MS = 500
private val ENTER_ANIMATION_EASING = EaseOutExpo
private const val ENTER_ANIMATION_DELAY_MS = 100

private const val EXIT_ANIMATION_DURATION_MS = 500
private val EXIT_ANIMATION_EASING = EaseOutExpo

private fun enterTween(): FiniteAnimationSpec<IntOffset> {
  return tween(
    ENTER_ANIMATION_DURATION_MS,
    easing = ENTER_ANIMATION_EASING,
    delayMillis = ENTER_ANIMATION_DELAY_MS,
  )
}

private fun exitTween(): FiniteAnimationSpec<IntOffset> {
  return tween(EXIT_ANIMATION_DURATION_MS, easing = EXIT_ANIMATION_EASING)
}

private fun AnimatedContentTransitionScope<*>.slideEnter(): EnterTransition {
  return slideIntoContainer(
    animationSpec = enterTween(),
    towards = AnimatedContentTransitionScope.SlideDirection.Left,
  )
}

private fun AnimatedContentTransitionScope<*>.slideExit(): ExitTransition {
  return slideOutOfContainer(
    animationSpec = exitTween(),
    towards = AnimatedContentTransitionScope.SlideDirection.Right,
  )
}

/** Navigation routes. */
@Composable
fun GalleryNavHost(
  navController: NavHostController,
  modifier: Modifier = Modifier,
  modelManagerViewModel: ModelManagerViewModel,
  activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender,
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  var showModelManager by remember { mutableStateOf(false) }
  var pickedTask by remember { mutableStateOf<Task?>(null) }
  
  val uiState by modelManagerViewModel.uiState.collectAsState()

  // Get the first available LLM chat model
  val firstLlmChatModel by remember(uiState) {
    derivedStateOf {
      val llmChatTask = uiState.tasks.find { it.id == "llm_chat" }
      llmChatTask?.models?.find { model ->
        val downloadStatus = uiState.modelDownloadStatus[model.name]
        downloadStatus?.status == ModelDownloadStatusType.SUCCEEDED
      }
    }
  }

  // Track whether app is in foreground.
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START,
        Lifecycle.Event.ON_RESUME -> {
          modelManagerViewModel.setAppInForeground(foreground = true)
        }
        Lifecycle.Event.ON_STOP,
        Lifecycle.Event.ON_PAUSE -> {
          modelManagerViewModel.setAppInForeground(foreground = false)
        }
        else -> {
          /* Do nothing for other events */
        }
      }
    }

    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  // Check if there are any downloaded models for LLM chat
  val hasDownloadedLlmModel by remember(uiState) {
    derivedStateOf {
      val llmChatTask = uiState.tasks.find { it.id == "llm_chat" }
      llmChatTask?.models?.any { model ->
        val downloadStatus = uiState.modelDownloadStatus[model.name]
        downloadStatus?.status == ModelDownloadStatusType.SUCCEEDED
      } ?: false
    }
  }
  
  var initialCheckDone by remember { mutableStateOf(false) }
  var shouldShowLlmModelManager by remember { mutableStateOf(false) }
  var shouldNavigateToLlmModel by remember { mutableStateOf(false) }
  
  // Perform initial check based on model availability
  LaunchedEffect(uiState.modelDownloadStatus, uiState.loadingModelAllowlist) {
    if (!initialCheckDone && !uiState.loadingModelAllowlist) {
      if (hasDownloadedLlmModel && firstLlmChatModel != null) {
        // Select the model and mark that we should navigate to it
        modelManagerViewModel.selectModel(firstLlmChatModel!!)
        shouldNavigateToLlmModel = true
      } else if (!uiState.loadingModelAllowlist) {
        // Mark that we should show model manager for LLM_CHAT task
        shouldShowLlmModelManager = true
        val llmChatTask = uiState.tasks.find { it.id == "llm_chat" }
        if (llmChatTask != null) {
          pickedTask = llmChatTask
        }
      }
      initialCheckDone = true
    }
  }
  
  // Navigate to the LLM model when marked
  LaunchedEffect(shouldNavigateToLlmModel) {
    if (shouldNavigateToLlmModel && firstLlmChatModel != null) {
      navController.navigate("$ROUTE_MODEL/llm_chat/${firstLlmChatModel!!.name}")
      shouldNavigateToLlmModel = false
    }
  }

  // Show HomeScreen or LLM model manager based on state
  if (showModelManager && !shouldShowLlmModelManager) {
    HomeScreen(
      modelManagerViewModel = modelManagerViewModel,
      tosViewModel = hiltViewModel(),
      activityResultSender = activityResultSender,
      navigateToTaskScreen = { task ->
        pickedTask = task
        showModelManager = true
      },
    )
  } else if (shouldShowLlmModelManager && initialCheckDone) {
    AnimatedVisibility(
      visible = true,
      enter = slideInHorizontally(initialOffsetX = { it }),
      exit = slideOutHorizontally(targetOffsetX = { it }),
    ) {
      val curPickedTask = pickedTask
      if (curPickedTask != null) {
        ModelManager(
          viewModel = modelManagerViewModel,
          task = curPickedTask,
          onModelClicked = { model ->
            navController.navigate("$ROUTE_MODEL/${curPickedTask.id}/${model.name}")
          },
          navigateUp = { 
            shouldShowLlmModelManager = false
          },
        )
      }
    }
  } else if (initialCheckDone && !hasDownloadedLlmModel && !shouldNavigateToLlmModel) {
    // If initial check is done and there are no downloaded models and we're not navigating,
    // show HomeScreen as fallback
    HomeScreen(
      modelManagerViewModel = modelManagerViewModel,
      tosViewModel = hiltViewModel(),
      activityResultSender = activityResultSender,
      navigateToTaskScreen = { task ->
        pickedTask = task
        showModelManager = true
      },
    )
  }

  NavHost(
    navController = navController,
    // Default to open home screen initially, but we will redirect based on model availability
    startDestination = ROUTE_PLACEHOLDER,
    enterTransition = { EnterTransition.None },
    exitTransition = { ExitTransition.None },
  ) {
    // Placeholder root screen
    //
    // Having a non-empty placeholder here is needed to make the exit transition below work.
    // We can't have an empty Text here because it will block TalkBack.
    composable(route = ROUTE_PLACEHOLDER) { 
      // Show a minimal placeholder while checking model availability
      Box(modifier = Modifier.fillMaxSize()) 
    }

    composable(
      route = "$ROUTE_MODEL/{taskId}/{modelName}",
      arguments =
        listOf(
          navArgument("taskId") { type = NavType.StringType },
          navArgument("modelName") { type = NavType.StringType },
        ),
      enterTransition = { slideEnter() },
      exitTransition = { slideExit() },
    ) { backStackEntry ->
      val modelName = backStackEntry.arguments?.getString("modelName") ?: ""
      val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
      modelManagerViewModel.getModelByName(name = modelName)?.let { model ->
        modelManagerViewModel.selectModel(model)

        val customTask = modelManagerViewModel.getCustomTaskByTaskId(id = taskId)
        if (customTask != null) {
          if (isBuiltInTask(customTask.task.id)) {
            customTask.MainScreen(
              data =
                CustomTaskDataForBuiltinTask(
                  modelManagerViewModel = modelManagerViewModel,
                  onNavUp = {
                    // Set state to show model manager
                    pickedTask = customTask.task
                    shouldShowLlmModelManager = true
                    // Navigate back
                    if (navController.previousBackStackEntry != null) {
                      navController.popBackStack()
                    }
                  },
                  activityResultSender = activityResultSender,
                )
            )
          } else {
            var disableAppBarControls by remember { mutableStateOf(false) }
            CustomTaskScreen(
              task = customTask.task,
              modelManagerViewModel = modelManagerViewModel,
              onNavigateUp = { navController.navigateUp() },
              disableAppBarControls = disableAppBarControls,
              activityResultSender = activityResultSender,
            ) { bottomPadding ->
              customTask.MainScreen(
                data =
                  CustomTaskData(
                    modelManagerViewModel = modelManagerViewModel,
                    bottomPadding = bottomPadding,
                    setAppBarControlsDisabled = { disableAppBarControls = it },
                  )
              )
            }
          }
        }
      }
    }
  }

  // Handle incoming intents for deep links
  val intent = androidx.activity.compose.LocalActivity.current?.intent
  val data = intent?.data
  if (data != null) {
    intent.data = null
    Log.d(TAG, "navigation link clicked: $data")
    if (data.toString().startsWith("com.quantaliz.solaibot://model/")) {
      if (data.pathSegments.size >= 2) {
        val taskId = data.pathSegments.get(data.pathSegments.size - 2)
        val modelName = data.pathSegments.last()
        modelManagerViewModel.getModelByName(name = modelName)?.let { model ->
          navController.navigate("$ROUTE_MODEL/${taskId}/${model.name}")
        }
      } else {
        Log.e(TAG, "Malformed deep link URI received: $data")
      }
    }
  }
}

@Composable
private fun CustomTaskScreen(
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  disableAppBarControls: Boolean,
  onNavigateUp: () -> Unit,
  activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender,
  content: @Composable (bottomPadding: Dp) -> Unit,
) {
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val selectedModel = modelManagerUiState.selectedModel
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  var navigatingUp by remember { mutableStateOf(false) }
  var showErrorDialog by remember { mutableStateOf(false) }

  val handleNavigateUp = {
    navigatingUp = true
    onNavigateUp()

    // clean up all models.
    scope.launch(Dispatchers.Default) {
      for (model in task.models) {
        modelManagerViewModel.cleanupModel(context = context, task = task, model = model)
      }
    }
  }

  // Handle system's edge swipe.
  BackHandler { handleNavigateUp() }

  // Initialize model when model/download state changes.
  val curDownloadStatus = modelManagerUiState.modelDownloadStatus[selectedModel.name]
  LaunchedEffect(curDownloadStatus, selectedModel.name) {
    if (!navigatingUp) {
      if (curDownloadStatus?.status == ModelDownloadStatusType.SUCCEEDED) {
        Log.d(
          TAG,
          "Initializing model '${selectedModel.name}' from CustomTaskScreen launched effect",
        )
        modelManagerViewModel.initializeModel(context, task = task, model = selectedModel)
      }
    }
  }

  val modelInitializationStatus = modelManagerUiState.modelInitializationStatus[selectedModel.name]
  LaunchedEffect(modelInitializationStatus) {
    showErrorDialog = modelInitializationStatus?.status == ModelInitializationStatusType.ERROR
  }

  Scaffold(
    topBar = {
      ModelPageAppBar(
        task = task,
        model = selectedModel,
        modelManagerViewModel = modelManagerViewModel,
        inProgress = disableAppBarControls,
        modelPreparing = disableAppBarControls,
        canShowResetSessionButton = false,
        onConfigChanged = { _, _ -> },
        onBackClicked = { handleNavigateUp() },
        onModelSelected = { prevModel, newSelectedModel ->
          scope.launch(Dispatchers.Default) {
            // Clean up prev model.
            if (prevModel.name != newSelectedModel.name) {
              modelManagerViewModel.cleanupModel(context = context, task = task, model = prevModel)
            }

            // Update selected model.
            modelManagerViewModel.selectModel(model = newSelectedModel)
          }
        },
      )
    }
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.padding(
          top = innerPadding.calculateTopPadding(),
          start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
          end = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
        )
    ) {
      val curModelDownloadStatus = modelManagerUiState.modelDownloadStatus[selectedModel.name]
      AnimatedContent(
        targetState = curModelDownloadStatus?.status == ModelDownloadStatusType.SUCCEEDED
      ) { targetState ->
        when (targetState) {
          // Main UI when model is downloaded.
          true -> content(innerPadding.calculateBottomPadding())
          // Model download
          false ->
            ModelDownloadStatusInfoPanel(
              model = selectedModel,
              task = task,
              modelManagerViewModel = modelManagerViewModel,
            )
        }
      }
    }
  }

  if (showErrorDialog) {
    ErrorDialog(
      error = modelInitializationStatus?.error ?: "",
      onDismiss = { showErrorDialog = false },
    )
  }
}
