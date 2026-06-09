package com.humsong.app.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton as MaterialFloatingActionButton
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.humsong.app.TrainingTimerAlarmReceiver
import com.humsong.app.KEY_PENDING_TRAINING_HIGHLIGHT_ITEM_ID
import com.humsong.app.TRAINING_TIMER_PREFS
import com.humsong.app.fitness.BodyPhotoAngle
import com.humsong.app.fitness.FitnessEntry
import com.humsong.app.fitness.MealItem
import com.humsong.app.fitness.MealTemplate
import com.humsong.app.fitness.MealTemplateGroup
import com.humsong.app.fitness.MealType
import com.humsong.app.fitness.TrainingItem
import com.humsong.app.fitness.TrainingTemplate
import com.humsong.app.fitness.TrainingTemplateGroup
import com.humsong.app.fitness.TrainingGoal
import com.humsong.app.fitness.TrainingPeriod
import com.humsong.app.fitness.TrainingType
import com.humsong.app.fitness.TrainingWeightMode
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.Year
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FitnessApp(
    themeKey: String = "teal",
    onThemeChange: (String) -> Unit = {},
    viewModel: FitnessViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var drawerOpen by remember { mutableStateOf(false) }
    var activeEditor by remember { mutableStateOf<EditorPanel?>(null) }
    var pendingPhotoAngle by remember { mutableStateOf<BodyPhotoAngle?>(null) }
    var pendingFoodPhotoPath by remember { mutableStateOf<String?>(null) }
    var bodyEditSnapshot by remember { mutableStateOf<Triple<String, String, TrainingGoal>?>(null) }
    var timerItemId by remember { mutableStateOf<String?>(null) }
    var timerRemainingSeconds by remember { mutableStateOf(0) }
    var timerRunning by remember { mutableStateOf(false) }
    var highlightTrainingItemId by remember { mutableStateOf<String?>(null) }
    var exactAlarmPermissionOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(timerRunning, timerRemainingSeconds) {
        if (timerRunning && timerRemainingSeconds > 0) {
            delay(1000)
            timerRemainingSeconds -= 1
        } else if (timerRunning && timerRemainingSeconds == 0) {
            highlightTrainingItemId = timerItemId
            timerRunning = false
            timerItemId = null
        }
    }
    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val preferences = context.getSharedPreferences(TRAINING_TIMER_PREFS, Context.MODE_PRIVATE)
                val pendingItemId = preferences.getString(KEY_PENDING_TRAINING_HIGHLIGHT_ITEM_ID, null)
                if (!pendingItemId.isNullOrBlank()) {
                    highlightTrainingItemId = pendingItemId
                    preferences.edit().remove(KEY_PENDING_TRAINING_HIGHLIGHT_ITEM_ID).apply()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importPhotoForAngle(pendingPhotoAngle ?: state.selectedPhotoAngle, uri)
        }
        pendingPhotoAngle = null
    }
    val foodGalleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.recognizeFoodImageFromGallery(uri)
        }
    }
    val foodCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val path = pendingFoodPhotoPath
        if (success && !path.isNullOrBlank()) {
            viewModel.recognizeFoodPhotoWithAi(path)
        } else if (!path.isNullOrBlank()) {
            File(path).delete()
        }
        pendingFoodPhotoPath = null
    }
    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.importProfileAvatar(uri)
    }
    val homeBackgroundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.importHomeBackground(uri)
    }
    val pages = listOf(
        MainPage("数值", Icons.Filled.MonitorWeight),
        MainPage("训练", Icons.Filled.FitnessCenter),
        MainPage("摄入", Icons.Filled.Restaurant),
        MainPage("照片", Icons.Filled.PhotoCamera),
        MainPage("总结", Icons.Filled.Analytics)
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    fun setBodyEditMode(editing: Boolean) {
        if (editing) {
            bodyEditSnapshot = Triple(state.entry.heightCm, state.entry.weightKg, state.entry.trainingGoal)
            activeEditor = EditorPanel.Body
        } else {
            val snapshot = bodyEditSnapshot
            if (snapshot != null) {
                viewModel.completeBodyDataEditing(snapshot.first, snapshot.second, snapshot.third)
            }
            bodyEditSnapshot = null
            if (activeEditor == EditorPanel.Body) activeEditor = null
        }
    }
    fun returnToFirstPage() {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackground(backgroundPath = state.homeBackgroundPath)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, top = 36.dp, end = 20.dp, bottom = 84.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderProfile(
                    name = state.profileName,
                    signature = state.profileSignature,
                    avatarPath = state.profileAvatarPath,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            shape = CircleShape
                        ),
                    onClick = { drawerOpen = true }
                ) {
                    Icon(imageVector = Icons.Filled.Menu, contentDescription = "设置")
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (page) {
                        0 -> BodyCard(
                            height = state.entry.heightCm,
                            weight = state.entry.weightKg,
                            trainingGoal = state.entry.trainingGoal,
                            recommendation = state.entry.bodyGoalRecommendation,
                            isAnalyzingRecommendation = state.isAnalyzingBodyGoal,
                            editable = state.canEditSelectedDate && (activeEditor == null || activeEditor == EditorPanel.Body),
                            editMode = activeEditor == EditorPanel.Body,
                            onEditModeChange = ::setBodyEditMode,
                            onHeightChange = viewModel::updateHeight,
                            onWeightChange = viewModel::updateWeight,
                            onTrainingGoalChange = viewModel::updateTrainingGoal
                        )
                        1 -> TrainingItemsCard(
                            items = state.entry.trainingItems,
                            templates = state.trainingTemplates,
                            templateGroups = state.trainingTemplateGroups,
                            editable = state.canEditSelectedDate && (activeEditor == null || activeEditor == EditorPanel.Training),
                            onAddItem = viewModel::addTrainingItem,
                            onAddTemplateItem = viewModel::addTrainingItemFromTemplate,
                            onAddTemplateGroup = viewModel::addTrainingItemsFromGroup,
                            onSaveTemplate = viewModel::saveTrainingTemplate,
                            onUpdateItem = viewModel::updateTrainingItem,
                            onUpdateCompletedSets = viewModel::updateTrainingItemCompletedSets,
                            activeTimerItemId = timerItemId,
                            activeTimerRemainingSeconds = timerRemainingSeconds,
                            highlightItemId = highlightTrainingItemId,
                            onHighlightDone = { itemId ->
                                if (highlightTrainingItemId == itemId) {
                                    highlightTrainingItemId = null
                                }
                            },
                            onStartSetTimer = { item ->
                                val restSeconds = item.restSeconds.toIntOrNull()?.coerceAtLeast(1) ?: 60
                                if (!context.canScheduleTrainingTimerAlarm()) {
                                    exactAlarmPermissionOpen = true
                                } else {
                                    context.cancelTrainingTimerAlarm()
                                    timerItemId = item.id
                                    timerRemainingSeconds = restSeconds
                                    context.scheduleTrainingTimerAlarm(restSeconds, item.id)
                                    timerRunning = true
                                }
                            },
                            onResetSetTimer = {
                                context.cancelTrainingTimerAlarm()
                                timerRunning = false
                                timerItemId = null
                                timerRemainingSeconds = 0
                            },
                            onDeleteItem = viewModel::deleteTrainingItem
                        )
                        2 -> MealItemsCard(
                            items = state.entry.mealItems,
                            templates = state.mealTemplates,
                            templateGroups = state.mealTemplateGroups,
                            selectedMealViewType = state.selectedMealViewType,
                            editable = state.canEditSelectedDate && (activeEditor == null || activeEditor == EditorPanel.Meals),
                            editMode = activeEditor == EditorPanel.Meals,
                            isAnalyzingNutrition = state.isAnalyzingNutrition,
                            isAnalyzingMealNutrition = state.isAnalyzingMealNutrition,
                            isRecognizingFoodPhoto = state.isRecognizingFoodPhoto,
                            bodyGoalRecommendation = state.entry.bodyGoalRecommendation,
                            nutritionAnalysis = state.entry.nutritionAnalysis,
                            mealNutritionAnalysis = state.selectedMealViewType.mealType?.let { state.entry.mealNutritionAnalysisFor(it) }.orEmpty(),
                            breakfastNutritionAnalysis = state.entry.breakfastNutritionAnalysis,
                            lunchNutritionAnalysis = state.entry.lunchNutritionAnalysis,
                            dinnerNutritionAnalysis = state.entry.dinnerNutritionAnalysis,
                            onMealViewTypeChange = viewModel::updateMealViewType,
                            onAddItem = viewModel::addMealItem,
                            onAddTemplateItem = viewModel::addMealItemFromTemplate,
                            onAddTemplateGroup = viewModel::addMealItemsFromGroup,
                            onSaveTemplate = viewModel::saveMealTemplate,
                            onUpdateItem = viewModel::updateMealItem,
                            onDeleteItem = viewModel::deleteMealItem,
                            onAnalyzeNutrition = viewModel::analyzeNutritionWithAi,
                            onAnalyzeMealNutrition = viewModel::analyzeSelectedMealNutritionWithAi,
                            onRecognizeFoodPhoto = {
                                val file = context.createFoodRecognitionImageFile()
                                pendingFoodPhotoPath = file.absolutePath
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                foodCameraLauncher.launch(uri)
                            },
                            onPickFoodPhoto = {
                                foodGalleryPicker.launch("image/*")
                            }
                        )
                        3 -> PhotoCard(
                            entry = state.entry,
                            selectedAngle = state.selectedPhotoAngle,
                            comparisonDateOptions = state.photoComparisonDateOptions,
                            selectedComparisonDate = state.selectedPhotoComparisonDate,
                            comparisonResult = state.photoComparisonResult,
                            editable = state.canEditSelectedDate && (activeEditor == null || activeEditor == EditorPanel.Photos),
                            isComparingPhotos = state.isComparingPhotos,
                            onAngleChange = viewModel::updatePhotoAngle,
                            onComparisonDateChange = viewModel::updatePhotoComparisonDate,
                            onComparePhotos = viewModel::comparePhotosWithAi,
                            onDeletePhoto = viewModel::deleteSelectedPhoto,
                            onPickPhoto = { angle ->
                                pendingPhotoAngle = angle
                                photoPicker.launch("image/*")
                            }
                        )
                        4 -> AnalysisCard(
                            analysis = state.entry.aiAnalysis,
                            isAnalyzing = state.isAnalyzing || activeEditor != null,
                            editable = state.canEditSelectedDate,
                            onAnalyze = viewModel::analyzeWithAi
                        )
                    }

                    state.errorMessage?.let {
                        ResultCard(title = "鎻愮ず", content = it)
                    }
                }
            }
        }

        NavigationBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(18.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
            tonalElevation = NavigationBarDefaults.Elevation
        ) {
            pages.forEachIndexed { index, page ->
                val haptics = LocalHapticFeedback.current
                NavigationBarItem(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    icon = {
                        Icon(imageVector = page.icon, contentDescription = page.label)
                    },
                    label = { Text(page.label) }
                )
            }
        }

        SettingsDrawer(
            open = drawerOpen,
            apiKey = state.apiKeyInput,
            selectedDate = state.selectedDate,
            profileName = state.profileName,
            profileBirthday = state.profileBirthday,
            profileGender = state.profileGender,
            profileSignature = state.profileSignature,
            profileAvatarPath = state.profileAvatarPath,
            homeBackgroundPath = state.homeBackgroundPath,
            welcomeMessage = state.welcomeMessage,
            profileAge = state.profileAge,
            themeKey = themeKey,
            recordedDates = state.recordedDates,
            trainingTemplates = state.trainingTemplates,
            trainingTemplateGroups = state.trainingTemplateGroups,
            mealTemplates = state.mealTemplates,
            mealTemplateGroups = state.mealTemplateGroups,
            canEditSelectedDate = state.canEditSelectedDate,
            onApiKeyChange = viewModel::updateApiKey,
            onSaveApiKey = viewModel::saveApiKey,
            onProfileNameChange = viewModel::updateProfileName,
            onProfileBirthdayChange = viewModel::updateProfileBirthday,
            onProfileGenderChange = viewModel::updateProfileGender,
            onProfileSignatureChange = viewModel::updateProfileSignature,
            onThemeChange = onThemeChange,
            onPickAvatar = { avatarPicker.launch("image/*") },
            onWelcomeMessageChange = viewModel::updateWelcomeMessage,
            onPickHomeBackground = { homeBackgroundPicker.launch("image/*") },
            onClearHomeBackground = viewModel::clearHomeBackground,
            onPreviousDate = {
                viewModel.previousDay()
                returnToFirstPage()
                drawerOpen = false
            },
            onNextDate = {
                viewModel.nextDay()
                returnToFirstPage()
                drawerOpen = false
            },
            onTodayDate = {
                viewModel.goToday()
                returnToFirstPage()
                drawerOpen = false
            },
            onDateChange = { date ->
                viewModel.updateDate(date)
                returnToFirstPage()
                drawerOpen = false
            },
            onDeleteRecord = viewModel::deleteRecord,
            onSaveTrainingTemplate = viewModel::saveTrainingTemplate,
            onUpdateTrainingTemplate = viewModel::updateTrainingTemplate,
            onDeleteTrainingTemplate = viewModel::deleteTrainingTemplate,
            onSaveTrainingTemplateGroup = viewModel::saveTrainingTemplateGroup,
            onUpdateTrainingTemplateGroup = viewModel::updateTrainingTemplateGroup,
            onDeleteTrainingTemplateGroup = viewModel::deleteTrainingTemplateGroup,
            onSaveMealTemplate = viewModel::saveMealTemplate,
            onUpdateMealTemplate = viewModel::updateMealTemplate,
            onDeleteMealTemplate = viewModel::deleteMealTemplate,
            onSaveMealTemplateGroup = viewModel::saveMealTemplateGroup,
            onUpdateMealTemplateGroup = viewModel::updateMealTemplateGroup,
            onDeleteMealTemplateGroup = viewModel::deleteMealTemplateGroup,
            onClose = { drawerOpen = false }
        )
        if (state.showProfileOnboarding) {
            ProfileOnboardingDialog(
                name = state.profileName,
                birthday = state.profileBirthday,
                gender = state.profileGender,
                signature = state.profileSignature,
                age = state.profileAge,
                onNameChange = viewModel::updateProfileName,
                onBirthdayChange = viewModel::updateProfileBirthday,
                onGenderChange = viewModel::updateProfileGender,
                onSignatureChange = viewModel::updateProfileSignature,
                onDone = viewModel::finishProfileOnboarding
            )
        } else if (state.showLaunchWelcome) {
            LaunchWelcomeDialog(
                name = state.profileName,
                message = state.welcomeMessage,
                onDone = viewModel::dismissLaunchWelcome
            )
        } else if (state.showDailyBodyCheckIn && state.canEditSelectedDate) {
            DailyBodyCheckInDialog(
                height = state.entry.heightCm,
                weight = state.entry.weightKg,
                goal = state.entry.trainingGoal,
                onHeightChange = viewModel::updateHeight,
                onWeightChange = viewModel::updateWeight,
                onGoalChange = viewModel::updateTrainingGoal,
                onDone = viewModel::dismissDailyBodyCheckIn
            )
        }
        state.foodRecognitionSession?.let { session ->
            FoodRecognitionDialog(
                session = session,
                isRecognizing = state.isRecognizingFoodPhoto,
                onSupplementChange = viewModel::updateFoodRecognitionSupplement,
                onRetry = viewModel::retryFoodRecognitionWithSupplement,
                onAddPhoto = {
                    val file = context.createFoodRecognitionImageFile()
                    pendingFoodPhotoPath = file.absolutePath
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    foodCameraLauncher.launch(uri)
                },
                onPickPhoto = { foodGalleryPicker.launch("image/*") },
                onConfirm = viewModel::confirmFoodRecognitionItems,
                onDismiss = viewModel::dismissFoodRecognitionSession
            )
        }
        if (exactAlarmPermissionOpen) {
            ExactAlarmPermissionDialog(
                onDismiss = { exactAlarmPermissionOpen = false },
                onOpenSettings = {
                    exactAlarmPermissionOpen = false
                    context.openExactAlarmSettings()
                }
            )
        }
    }
}

@Composable
private fun AppBackground(backgroundPath: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        if (!backgroundPath.isNullOrBlank()) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = rememberAsyncImagePainter(File(backgroundPath)),
                contentDescription = "主页背景",
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.62f))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.14f))
        )
    }
}

private data class MainPage(
    val label: String,
    val icon: ImageVector
)

private fun Context.trainingTimerPendingIntent(itemId: String = ""): PendingIntent {
    val intent = Intent(this, TrainingTimerAlarmReceiver::class.java).apply {
        action = TrainingTimerAlarmReceiver.ACTION_TRAINING_TIMER_DONE
        putExtra(TrainingTimerAlarmReceiver.EXTRA_TRAINING_ITEM_ID, itemId)
    }
    return PendingIntent.getBroadcast(
        this,
        20260607,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun Context.scheduleTrainingTimerAlarm(delaySeconds: Int, itemId: String) {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val triggerAt = System.currentTimeMillis() + delaySeconds.coerceAtLeast(1) * 1000L
    val pendingIntent = trainingTimerPendingIntent(itemId)
    alarmManager.setAlarmClock(
        AlarmManager.AlarmClockInfo(triggerAt, pendingIntent),
        pendingIntent
    )
}

private fun Context.cancelTrainingTimerAlarm() {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(trainingTimerPendingIntent())
}

private fun Context.createFoodRecognitionImageFile(): File {
    val directory = File(cacheDir, "food_recognition").apply { mkdirs() }
    return File(directory, "food_${System.currentTimeMillis()}.jpg")
}

private fun Context.canScheduleTrainingTimerAlarm(): Boolean {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
}

private fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}

@Composable
private fun rememberHapticClick(onClick: () -> Unit): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }
}

@Composable
private fun Modifier.hapticClickable(onClick: () -> Unit): Modifier {
    val haptics = LocalHapticFeedback.current
    return clickable {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }
}

@Composable
private fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    content: @Composable RowScope.() -> Unit
) {
    MaterialButton(
        onClick = rememberHapticClick(onClick),
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        content = content
    )
}

@Composable
private fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    MaterialTextButton(
        onClick = rememberHapticClick(onClick),
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}

@Composable
private fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialIconButton(
        onClick = rememberHapticClick(onClick),
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}

@Composable
private fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    content: @Composable () -> Unit
) {
    MaterialFloatingActionButton(
        onClick = rememberHapticClick(onClick),
        modifier = modifier,
        shape = shape,
        content = content
    )
}

private enum class EditorPanel {
    Body,
    Training,
    Meals,
    Photos
}

private enum class TrainingAddFlow {
    Manual,
    Template
}

private sealed class ImportRoute {
    data object Home : ImportRoute()
    data object SingleCategories : ImportRoute()
    data object GroupCategories : ImportRoute()
    data class SingleDetail(val category: String) : ImportRoute()
    data class GroupDetail(val category: String) : ImportRoute()
}

private enum class SettingsDialog {
    Profile,
    Personalization,
    Api,
    Date,
    Records,
    Templates
}

private data class ThemeOption(
    val key: String,
    val label: String,
    val description: String,
    val primary: Color,
    val secondary: Color
)

private val themeOptions = listOf(
    ThemeOption("gundam", "高达", "白蓝红热血机甲风", Color(0xFF1E4FD7), Color(0xFFE51F2F)),
    ThemeOption("teal", "青绿", "清爽运动感", Color(0xFF5EEAD4), Color(0xFFFFB86B)),
    ThemeOption("violet", "紫色", "安静科技感", Color(0xFFC4B5FD), Color(0xFF7DD3FC)),
    ThemeOption("blue", "蓝色", "克制清晰", Color(0xFF93C5FD), Color(0xFFF9A8D4)),
    ThemeOption("rose", "玫瑰", "明亮柔和", Color(0xFFFFA6C1), Color(0xFF67E8F9)),
    ThemeOption("amber", "暖黄", "温暖明亮", Color(0xFFFCD34D), Color(0xFF5EEAD4))
)

private fun MealViewType.mealRatio(): Double {
    return when (this) {
        MealViewType.Breakfast -> 0.30
        MealViewType.Lunch -> 0.40
        MealViewType.Dinner -> 0.30
        MealViewType.All -> 1.0
    }
}

@Composable
private fun HeaderProfile(
    name: String,
    signature: String,
    avatarPath: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape)
                .padding(3.dp)
        ) {
            AvatarImage(avatarPath = avatarPath, sizeDp = 54)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = name.ifBlank { "未设置用户名" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (signature.isNotBlank()) {
                Text(
                    text = signature,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        content()
    }
}

@Composable
private fun PageIntro(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.54f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing?.invoke(this)
    }
}

@Composable
private fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RoundIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    IconButton(
        modifier = modifier
            .clip(CircleShape)
            .background(
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            ),
        enabled = enabled,
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DateCard(
    date: String,
    recordedDates: List<String>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onDateChange: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var visibleMonth by remember(date) {
        mutableStateOf(runCatching { YearMonth.from(LocalDate.parse(date)) }.getOrElse { YearMonth.now() })
    }
    val selectedDate = runCatching { LocalDate.parse(date) }.getOrNull()
    val today = LocalDate.now()

    SectionCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "日期", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onPrevious) { Text("前一天") }
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = { showDatePicker = true }
                ) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (selectedDate?.isBefore(today) == true) {
                    TextButton(onClick = onNext) { Text("后一天") }
                }
            }
            MonthCalendar(
                visibleMonth = visibleMonth,
                selectedDate = selectedDate,
                today = today,
                selectableDates = recordedDates.toSet() + today.toString(),
                onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                onDateSelected = {
                    onDateChange(it.toString())
                    visibleMonth = YearMonth.from(it)
                }
            )
            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = {
                    onToday()
                    visibleMonth = YearMonth.from(today)
                }
            ) {
                Text("鍥炲埌浠婂ぉ")
            }
        }
    }
    if (showDatePicker) {
        DateJumpDialog(
            currentDate = selectedDate ?: today,
            today = today,
            recordedDates = recordedDates,
            onDismiss = { showDatePicker = false },
            onDone = { target ->
                showDatePicker = false
                onDateChange(target.toString())
                visibleMonth = YearMonth.from(target)
            }
        )
    }
}

@Composable
private fun DateJumpDialog(
    currentDate: LocalDate,
    today: LocalDate,
    recordedDates: List<String>,
    onDismiss: () -> Unit,
    onDone: (LocalDate) -> Unit
) {
    var selectedYear by remember(currentDate) { mutableStateOf(currentDate.year) }
    var selectedMonth by remember(currentDate) { mutableStateOf(currentDate.monthValue) }
    var selectedDay by remember(currentDate) { mutableStateOf(currentDate.dayOfMonth) }
    val selectableDateList = (recordedDates + today.toString())
        .distinct()
        .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        .filter { !it.isAfter(today) }
        .sorted()
    val yearOptions = selectableDateList.map { it.year }.distinct().sortedDescending()
    val monthOptions = selectableDateList
        .filter { it.year == selectedYear }
        .map { it.monthValue }
        .distinct()
        .sorted()
    if (selectedYear !in yearOptions) {
        selectedYear = yearOptions.firstOrNull() ?: today.year
    }
    if (selectedMonth !in monthOptions) {
        selectedMonth = monthOptions.firstOrNull() ?: today.monthValue
    }
    val dayOptions = selectableDateList
        .filter { it.year == selectedYear && it.monthValue == selectedMonth }
        .map { it.dayOfMonth }
        .distinct()
        .sorted()
    if (selectedDay !in dayOptions) {
        selectedDay = dayOptions.firstOrNull() ?: today.dayOfMonth
    }
    val targetDate = LocalDate.of(selectedYear, selectedMonth, selectedDay)
    val isSelectable = targetDate in selectableDateList

    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "选择日期", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "只能跳转到今天或已有记录的日期",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SimpleDropdown(
                        modifier = Modifier.weight(1f),
                        label = "${selectedYear}年",
                        options = yearOptions.map { it.toString() },
                        onSelected = { selectedYear = it.toInt() }
                    )
                    SimpleDropdown(
                        modifier = Modifier.weight(1f),
                        label = "${selectedMonth}月",
                        options = monthOptions.map { it.toString() },
                        onSelected = { selectedMonth = it.toInt() }
                    )
                    SimpleDropdown(
                        modifier = Modifier.weight(1f),
                        label = "${selectedDay}日",
                        options = dayOptions.map { it.toString() },
                        onSelected = { selectedDay = it.toInt() }
                    )
                }
                if (!isSelectable) {
                    Text(
                        text = "这个日期没有记录，不能选择。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        enabled = isSelectable,
                        onClick = { onDone(targetDate) }
                    ) {
                        Text("完成")
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    visibleMonth: YearMonth,
    selectedDate: LocalDate?,
    today: LocalDate,
    selectableDates: Set<String>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val currentMonth = YearMonth.from(today)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPreviousMonth) { Text("‹") }
            Text(
                modifier = Modifier.weight(1f),
                text = "${visibleMonth.year} 年 ${visibleMonth.monthValue} 月",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            if (visibleMonth.isBefore(currentMonth)) {
                TextButton(onClick = onNextMonth) { Text("›") }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(
                    modifier = Modifier.weight(1f),
                    text = it,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        val firstDay = visibleMonth.atDay(1)
        val leadingBlankDays = firstDay.dayOfWeek.weekIndexFromMonday()
        val days = visibleMonth.lengthOfMonth()
        val cells = leadingBlankDays + days
        val rows = maxOf(5, (cells + 6) / 7)
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { column ->
                    val dayNumber = row * 7 + column - leadingBlankDays + 1
                    if (dayNumber in 1..days) {
                        val itemDate = visibleMonth.atDay(dayNumber)
                        val isSelected = itemDate == selectedDate
                        val isFuture = itemDate.isAfter(today)
                        val isUnavailable = !isFuture && itemDate.toString() !in selectableDates
                        val isDisabled = isFuture || isUnavailable
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .background(
                                    when {
                                        isDisabled -> Color.LightGray
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                )
                                .then(
                                    if (isDisabled) Modifier
                                    else Modifier.hapticClickable { onDateSelected(itemDate) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNumber.toString(),
                                textAlign = TextAlign.Center,
                                color = when {
                                    isDisabled -> Color.DarkGray
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    } else {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun DayOfWeek.weekIndexFromMonday(): Int {
    return value - 1
}

private data class ParsedNutritionAnalysis(
    val calories: String,
    val carbs: String,
    val protein: String,
    val fat: String,
    val advice: String
)

private data class ParsedMealNutritionAnalysis(
    val calories: String,
    val carbsGrams: String,
    val carbsPercent: Float,
    val proteinGrams: String,
    val proteinPercent: Float,
    val fatGrams: String,
    val fatPercent: Float,
    val fiberGrams: String,
    val fiberPercent: Float,
    val advice: String
)

private data class ParsedBodyGoalRecommendation(
    val calories: String,
    val carbs: String,
    val protein: String,
    val fat: String,
    val fiber: String,
    val advice: String
)

private data class NutritionNumbers(
    val calories: Double = 0.0,
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0
)

private fun parseNutritionAnalysis(content: String): ParsedNutritionAnalysis {
    val cleaned = content.replace("\r\n", "\n").replace("**", "")

    fun extractValue(vararg keywords: String): String {
        val line = cleaned.lineSequence().firstOrNull { line -> keywords.any { line.contains(it) } }.orEmpty()
        return line.substringAfter('：', line).substringAfter(':', line)
            .replace("*", "")
            .replace("|", "")
            .trim()
    }

    val calories = extractValue("已摄入卡路里", "总热量", "热量", "卡路里")
    val carbs = extractValue("已摄入碳水", "碳水化合物", "碳水")
    val protein = extractValue("已摄入蛋白质", "蛋白质")
    val fat = extractValue("已摄入脂肪", "脂肪")
    val adviceKeywords = listOf("营养补充建议", "补充建议", "建议")
    val adviceStart = adviceKeywords.mapNotNull { keyword -> cleaned.indexOf(keyword).takeIf { it >= 0 } }.minOrNull()
    val advice = if (adviceStart != null) {
        cleaned.substring(adviceStart)
            .replace(Regex("""^#+\s*"""), "")
            .replace(Regex("""^(营养补充建议|补充建议|建议)[\uFF1A:\s]*"""), "")
            .lineSequence()
            .filterNot { line ->
                val trimmed = line.trim()
                trimmed.contains("已摄入卡路里") ||
                    trimmed.contains("已摄入碳水") ||
                    trimmed.contains("已摄入蛋白质") ||
                    trimmed.contains("已摄入脂肪") ||
                    trimmed.contains("总热量") ||
                    trimmed.contains("碳水化合物") ||
                    trimmed.startsWith("|")
            }
            .joinToString("\n")
            .trim()
    } else {
        "这条历史分析没有单独的建议段，建议重新点击 AI 分析营养生成新版格式。"
    }

    return ParsedNutritionAnalysis(calories = calories, carbs = carbs, protein = protein, fat = fat, advice = advice)
}

private fun parseMealNutritionAnalysis(content: String): ParsedMealNutritionAnalysis {
    val cleaned = content.replace("\r\n", "\n")
    parseMealNutritionJson(cleaned)?.let { return it }

    fun lineFor(vararg keywords: String): String {
        return cleaned.lineSequence().firstOrNull { line -> keywords.any { line.contains(it) } }.orEmpty()
    }
    fun percentFrom(line: String): Float {
        return Regex("""(\d+(?:\.\d+)?)\s*%""").find(line)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0f
    }
    fun numberFrom(line: String): Float {
        return Regex("""(\d+(?:\.\d+)?)""").find(line)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0f
    }
    fun gramsFrom(line: String): String {
        val value = numberFrom(line)
        return if (value > 0f) "${formatMacroNumber(value.toDouble())} g" else ""
    }
    fun caloriesFrom(line: String): String {
        val value = numberFrom(line)
        return if (value > 0f) "${formatMacroNumber(value.toDouble())} kcal" else line.substringAfter('：', line).substringAfter(':', line).trim()
    }

    val caloriesLine = lineFor("本餐总卡路里", "已摄入卡路里", "总卡路里", "总热量", "热量")
    val carbsLine = lineFor("碳水", "碳水化合物")
    val proteinLine = lineFor("蛋白质")
    val fatLine = lineFor("脂肪")
    val fiberLine = lineFor("膳食纤维", "纤维")
    val adviceStart = listOf("简短建议", "建议")
        .mapNotNull { keyword -> cleaned.indexOf(keyword).takeIf { it >= 0 } }
        .minOrNull()
    val advice = if (adviceStart != null) {
        cleaned.substring(adviceStart)
            .replace(Regex("""^(简短建议|建议)[\uFF1A:\s]*"""), "")
            .filterNutritionAdviceMarkdown()
    } else {
        ""
    }

    val carbsPercent = percentFrom(carbsLine)
    val proteinPercent = percentFrom(proteinLine)
    val fatPercent = percentFrom(fatLine)
    val fiberPercent = percentFrom(fiberLine)
    val rawPercentTotal = carbsPercent + proteinPercent + fatPercent + fiberPercent
    val carbsGrams = numberFrom(carbsLine)
    val proteinGrams = numberFrom(proteinLine)
    val fatGrams = numberFrom(fatLine)
    val fiberGrams = numberFrom(fiberLine)
    val gramTotal = carbsGrams + proteinGrams + fatGrams + fiberGrams
    val fallback = if (rawPercentTotal <= 0f && gramTotal <= 0f) 25f else 0f

    return ParsedMealNutritionAnalysis(
        calories = caloriesFrom(caloriesLine).ifBlank { "--" },
        carbsGrams = gramsFrom(carbsLine),
        carbsPercent = carbsPercent.takeIf { it > 0f } ?: carbsGrams.takeIf { gramTotal > 0f }?.let { it / gramTotal * 100f } ?: fallback,
        proteinGrams = gramsFrom(proteinLine),
        proteinPercent = proteinPercent.takeIf { it > 0f } ?: proteinGrams.takeIf { gramTotal > 0f }?.let { it / gramTotal * 100f } ?: fallback,
        fatGrams = gramsFrom(fatLine),
        fatPercent = fatPercent.takeIf { it > 0f } ?: fatGrams.takeIf { gramTotal > 0f }?.let { it / gramTotal * 100f } ?: fallback,
        fiberGrams = gramsFrom(fiberLine),
        fiberPercent = fiberPercent.takeIf { it > 0f } ?: fiberGrams.takeIf { gramTotal > 0f }?.let { it / gramTotal * 100f } ?: fallback,
        advice = advice
    )
}

private fun parseBodyGoalRecommendation(content: String): ParsedBodyGoalRecommendation {
    val jsonText = Regex("""\{[\s\S]*\}""").find(content)?.value
    if (jsonText != null) {
        runCatching {
            val json = JSONObject(jsonText)
            return ParsedBodyGoalRecommendation(
                calories = json.numberFor("calories_kcal", "calories", "热量", "总热量")?.let { "${it.roundToInt()} kcal" } ?: "--",
                carbs = json.numberFor("carbs_g", "碳水", "碳水化合物")?.let { "${formatMacroNumber(it)} g" } ?: "--",
                protein = json.numberFor("protein_g", "蛋白质")?.let { "${formatMacroNumber(it)} g" } ?: "--",
                fat = json.numberFor("fat_g", "脂肪")?.let { "${formatMacroNumber(it)} g" } ?: "--",
                fiber = json.numberFor("fiber_g", "膳食纤维", "纤维")?.let { "${formatMacroNumber(it)} g" } ?: "--",
                advice = json.optString("markdown_advice").ifBlank { json.optString("advice") }
            )
        }
    }
    return ParsedBodyGoalRecommendation(
        calories = "--",
        carbs = "--",
        protein = "--",
        fat = "--",
        fiber = "--",
        advice = content
    )
}

private fun ParsedBodyGoalRecommendation.toNumbers(): NutritionNumbers {
    return NutritionNumbers(
        calories = numberFromText(calories),
        carbs = numberFromText(carbs),
        protein = numberFromText(protein),
        fat = numberFromText(fat),
        fiber = numberFromText(fiber)
    )
}

private fun ParsedMealNutritionAnalysis.toNumbers(): NutritionNumbers {
    return NutritionNumbers(
        calories = numberFromText(calories),
        carbs = numberFromText(carbsGrams),
        protein = numberFromText(proteinGrams),
        fat = numberFromText(fatGrams),
        fiber = numberFromText(fiberGrams)
    )
}

private fun numberFromText(value: String): Double {
    return Regex("""-?\d+(?:\.\d+)?""").find(value)?.value?.toDoubleOrNull() ?: 0.0
}

private fun NutritionNumbers.minus(other: NutritionNumbers): NutritionNumbers {
    return NutritionNumbers(
        calories = (calories - other.calories).coerceAtLeast(0.0),
        carbs = (carbs - other.carbs).coerceAtLeast(0.0),
        protein = (protein - other.protein).coerceAtLeast(0.0),
        fat = (fat - other.fat).coerceAtLeast(0.0),
        fiber = (fiber - other.fiber).coerceAtLeast(0.0)
    )
}

private fun NutritionNumbers.plus(other: NutritionNumbers): NutritionNumbers {
    return NutritionNumbers(
        calories = calories + other.calories,
        carbs = carbs + other.carbs,
        protein = protein + other.protein,
        fat = fat + other.fat,
        fiber = fiber + other.fiber
    )
}

private fun NutritionNumbers.times(ratio: Double): NutritionNumbers {
    return NutritionNumbers(
        calories = calories * ratio,
        carbs = carbs * ratio,
        protein = protein * ratio,
        fat = fat * ratio,
        fiber = fiber * ratio
    )
}

private fun parseMealNutritionJson(content: String): ParsedMealNutritionAnalysis? {
    val jsonText = Regex("""\{[\s\S]*\}""").find(content)?.value ?: return null
    return runCatching {
        val json = JSONObject(jsonText)
        val calories = json.numberFor("calories_kcal", "total_calories_kcal", "calories", "kcal", "热量", "总热量", "卡路里")
        val carbs = json.numberFor("carbs_g", "carbohydrates_g", "carb_g", "碳水_g", "碳水", "碳水化合物")
        val protein = json.numberFor("protein_g", "proteins_g", "蛋白质_g", "蛋白质")
        val fat = json.numberFor("fat_g", "fats_g", "脂肪_g", "脂肪")
        val fiber = json.numberFor("fiber_g", "dietary_fiber_g", "膳食纤维_g", "膳食纤维", "纤维")
        val nutrientGramTotal = carbs.orZero() + protein.orZero() + fat.orZero() + fiber.orZero()
        val fallbackPercent = if (nutrientGramTotal > 0) 0.0 else 25.0
        val carbsPercent = json.numberFor("carbs_percent", "carbohydrates_percent", "碳水占比")
            ?: carbs.takeIf { nutrientGramTotal > 0 }?.let { it / nutrientGramTotal * 100.0 }
            ?: fallbackPercent
        val proteinPercent = json.numberFor("protein_percent", "蛋白质占比")
            ?: protein.takeIf { nutrientGramTotal > 0 }?.let { it / nutrientGramTotal * 100.0 }
            ?: fallbackPercent
        val fatPercent = json.numberFor("fat_percent", "脂肪占比")
            ?: fat.takeIf { nutrientGramTotal > 0 }?.let { it / nutrientGramTotal * 100.0 }
            ?: fallbackPercent
        val fiberPercent = json.numberFor("fiber_percent", "dietary_fiber_percent", "膳食纤维占比", "纤维占比")
            ?: fiber.takeIf { nutrientGramTotal > 0 }?.let { it / nutrientGramTotal * 100.0 }
            ?: fallbackPercent
        ParsedMealNutritionAnalysis(
            calories = calories?.let { "${it.roundToInt()} kcal" } ?: "--",
            carbsGrams = carbs?.let { "${formatMacroNumber(it)} g" }.orEmpty(),
            carbsPercent = carbsPercent.toFloat(),
            proteinGrams = protein?.let { "${formatMacroNumber(it)} g" }.orEmpty(),
            proteinPercent = proteinPercent.toFloat(),
            fatGrams = fat?.let { "${formatMacroNumber(it)} g" }.orEmpty(),
            fatPercent = fatPercent.toFloat(),
            fiberGrams = fiber?.let { "${formatMacroNumber(it)} g" }.orEmpty(),
            fiberPercent = fiberPercent.toFloat(),
            advice = json.optString("markdown_advice").ifBlank { json.optString("advice") }
        )
    }.getOrNull()
}

private fun Double?.orZero(): Double = this ?: 0.0

private fun JSONObject.numberFor(vararg keys: String): Double? {
    keys.forEach { key ->
        if (has(key)) {
            val value = opt(key)
            val number = when (value) {
                is Number -> value.toDouble()
                is String -> Regex("""-?\d+(?:\.\d+)?""").find(value)?.value?.toDoubleOrNull()
                else -> null
            }
            if (number != null && !number.isNaN()) return number
        }
    }
    return null
}

private fun formatMacroNumber(value: Double): String {
    return if (value % 1.0 == 0.0) value.roundToInt().toString() else "%.1f".format(value)
}

private fun String.filterNutritionAdviceMarkdown(): String {
    return lineSequence()
        .map { it.trim() }
        .filter { line ->
            line.isNotBlank() &&
                !line.startsWith("|") &&
                !line.all { it == '-' || it == '|' || it.isWhitespace() } &&
                !line.contains("本餐总卡路里") &&
                !line.contains("已摄入卡路里") &&
                !line.contains("总热量") &&
                !line.contains("总卡路里") &&
                !line.contains("碳水") &&
                !line.contains("蛋白质") &&
                !line.contains("脂肪") &&
                !line.contains("膳食纤维") &&
                !line.contains("纤维")
        }
        .joinToString("\n")
        .trim()
}

@Composable
private fun BodyCard(
    height: String,
    weight: String,
    trainingGoal: TrainingGoal,
    recommendation: String,
    isAnalyzingRecommendation: Boolean,
    editable: Boolean,
    editMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onTrainingGoalChange: (TrainingGoal) -> Unit
) {
    val canEdit = editable && editMode

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PageIntro(
            title = "今日数值",
            subtitle = "点击身高、体重或目标即可更新。推荐值会在完成修改后刷新。"
        )
        SectionCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (canEdit) {
                    BodyMetricEditTile(
                        label = "身高",
                        value = height,
                        unit = "cm",
                        onValueChange = onHeightChange
                    )
                    BodyMetricEditTile(
                        label = "体重",
                        value = weight,
                        unit = "kg",
                        onValueChange = onWeightChange
                    )
                    GoalEditTile(
                        goal = trainingGoal,
                        onGoalChange = onTrainingGoalChange
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            enabled = height.isNotBlank() && weight.isNotBlank(),
                            onClick = { onEditModeChange(false) }
                        ) {
                            Text("完成")
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BodyMetricTile(
                        label = "身高",
                            value = height.ifBlank { "--" },
                            unit = "cm",
                            editable = editable,
                            modifier = Modifier.weight(1f),
                            onClick = { onEditModeChange(true) }
                        )
                        BodyMetricTile(
                        label = "体重",
                            value = weight.ifBlank { "--" },
                            unit = "kg",
                            editable = editable,
                            modifier = Modifier.weight(1f),
                            onClick = { onEditModeChange(true) }
                        )
                    }
                    GoalTile(
                        goal = trainingGoal,
                        editable = editable,
                        onClick = { onEditModeChange(true) }
                    )
                }
                BodyGoalRecommendationCard(
                    content = recommendation,
                    isAnalyzing = isAnalyzingRecommendation
                )
            }
        }
    }
}

@Composable
private fun ProfileOnboardingDialog(
    name: String,
    birthday: String,
    gender: String,
    signature: String,
    age: String,
    onNameChange: (String) -> Unit,
    onBirthdayChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onSignatureChange: (String) -> Unit,
    onDone: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        PopDialogSurface {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = "完善个人资料", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "这些信息会用于每日摄入推荐。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("用户名") },
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = signature,
                    onValueChange = onSignatureChange,
                    label = { Text("个人签名") },
                    singleLine = true
                )
                BirthdaySelector(birthday = birthday, onBirthdayChange = onBirthdayChange)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "当前年龄", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(text = age.ifBlank { "--" }, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "性别", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    SimpleDropdown(
                        modifier = Modifier.width(136.dp),
                        label = gender.ifBlank { "未选择" },
                        options = listOf("男", "女", "未填写"),
                        onSelected = onGenderChange
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        enabled = name.isNotBlank() && birthday.isNotBlank() && gender.isNotBlank(),
                        onClick = onDone
                    ) {
                            Text("完成")
                    }
                }
            }
        }
    }
}

@Composable
private fun LaunchWelcomeDialog(
    name: String,
    message: String,
    onDone: () -> Unit
) {
    var secondsLeft by remember { mutableStateOf(3) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
        onDone()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .hapticClickable(onClick = onDone)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.18f)))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.18f), RoundedCornerShape(28.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 22.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(99.dp))
                )
                Text(
                    text = message.ifBlank { "今天也请努力" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = name.ifBlank { "你" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "${secondsLeft.coerceAtLeast(0)} 秒后进入",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DailyBodyCheckInDialog(
    height: String,
    weight: String,
    goal: TrainingGoal,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onGoalChange: (TrainingGoal) -> Unit,
    onDone: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        PopDialogSurface {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = "开始今天的记录", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "填写今天的身体数据和目标后，AI 会生成今日摄入推荐。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NumberField(
                    modifier = Modifier.fillMaxWidth(),
                    value = height,
                    label = "身高 cm",
                    editable = true,
                    onValueChange = onHeightChange
                )
                NumberField(
                    modifier = Modifier.fillMaxWidth(),
                    value = weight,
                    label = "体重 kg",
                    editable = true,
                    onValueChange = onWeightChange
                )
                SimpleDropdown(
                    modifier = Modifier.fillMaxWidth(),
                    label = goal.label,
                    options = TrainingGoal.entries.map { it.label },
                    onSelected = { label ->
                        TrainingGoal.entries.firstOrNull { it.label == label }?.let(onGoalChange)
                    }
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        enabled = height.isNotBlank() && weight.isNotBlank(),
                        onClick = onDone
                    ) {
                            Text("完成")
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalTile(
    goal: TrainingGoal,
    editable: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (editable) Modifier.hapticClickable(onClick = onClick) else Modifier)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "今日目标", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = goal.label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        TextPill(text = "可调整")
    }
}

@Composable
private fun GoalEditTile(
    goal: TrainingGoal,
    onGoalChange: (TrainingGoal) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "今日目标", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SimpleDropdown(
            modifier = Modifier.fillMaxWidth(),
            label = goal.label,
            options = TrainingGoal.entries.map { it.label },
            onSelected = { label ->
                TrainingGoal.entries.firstOrNull { it.label == label }?.let(onGoalChange)
            }
        )
    }
}

@Composable
private fun BodyMetricTile(
    label: String,
    value: String,
    unit: String,
    editable: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .then(if (editable) Modifier.hapticClickable(onClick = onClick) else Modifier)
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = unit, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BodyMetricEditTile(
    label: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(text = unit, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BodyGoalRecommendationCard(
    content: String,
    isAnalyzing: Boolean
) {
    val parsed = remember(content) { parseBodyGoalRecommendation(content) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "今日摄入推荐",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (isAnalyzing) {
                TextPill(text = "生成中")
            }
        }
        if (content.isBlank() && !isAnalyzing) {
            Text(
                text = "填写身高、体重和目标后，会在这里显示 AI 推荐摄入。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "推荐总热量", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = parsed.calories.ifBlank { "--" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip(label = "碳水", value = parsed.carbs, modifier = Modifier.weight(1f))
            InfoChip(label = "蛋白质", value = parsed.protein, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip(label = "脂肪", value = parsed.fat, modifier = Modifier.weight(1f))
            InfoChip(label = "膳食纤维", value = parsed.fiber, modifier = Modifier.weight(1f))
        }
        if (parsed.advice.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.56f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "建议", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                MarkdownText(content = parsed.advice)
            }
        }
    }
}

@Composable
private fun EditableSectionHeader(
    title: String,
    editable: Boolean,
    editMode: Boolean,
    onToggle: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (editable) {
            Button(onClick = onToggle) {
                Text(if (editMode) "完成" else "编辑")
            }
        }
    }
}

@Composable
private fun TextPill(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun InfoChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TrainingItemsCard(
    items: List<TrainingItem>,
    templates: List<TrainingTemplate>,
    templateGroups: List<TrainingTemplateGroup>,
    editable: Boolean,
    onAddItem: (TrainingItem) -> Unit,
    onAddTemplateItem: (TrainingTemplate, TrainingPeriod) -> Unit,
    onAddTemplateGroup: (TrainingTemplateGroup, TrainingPeriod) -> Unit,
    onSaveTemplate: (String, String, TrainingItem) -> Unit,
    onUpdateItem: (String, TrainingItem) -> Unit,
    onUpdateCompletedSets: (String, Int) -> Unit,
    activeTimerItemId: String?,
    activeTimerRemainingSeconds: Int,
    highlightItemId: String?,
    onHighlightDone: (String) -> Unit,
    onStartSetTimer: (TrainingItem) -> Unit,
    onResetSetTimer: () -> Unit,
    onDeleteItem: (String) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    var choosingType by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(TrainingItem(id = "draft")) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var actionItem by remember { mutableStateOf<TrainingItem?>(null) }
    var choosingAddSource by remember { mutableStateOf(false) }
    var choosingTemplate by remember { mutableStateOf(false) }
    var choosingPeriodFor by remember { mutableStateOf<TrainingAddFlow?>(null) }
    var selectedAddPeriod by remember { mutableStateOf(TrainingPeriod.Evening) }
    var savingTemplateItem by remember { mutableStateOf<TrainingItem?>(null) }
    val canEdit = editable
    val visibleItems = if (canEdit) items else items.filter { it.performed }

    fun resetDraft() {
        adding = false
        choosingAddSource = false
        choosingType = false
        choosingPeriodFor = null
        editingId = null
        draft = TrainingItem(id = "draft")
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PageIntro(
                title = if (canEdit) "训练计划" else "训练记录",
                subtitle = if (canEdit) "左滑完成一组，组间休息会自动开始；完成过的组数会作为实际训练记录。" else "这里只展示当天实际完成过的训练。",
                trailing = {
                    if (canEdit) {
                        RoundIconAction(
                            icon = Icons.Filled.Add,
                            contentDescription = "添加训练",
                            modifier = Modifier.width(48.dp).height(48.dp),
                            onClick = {
                                editingId = null
                                adding = false
                                choosingAddSource = true
                            }
                        )
                    }
                }
            )
            SectionCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (visibleItems.isEmpty()) {
                        EmptyState(
                            title = if (canEdit) "还没有训练计划" else "这一天没有实际训练记录",
                            subtitle = if (canEdit) "点击右上角 + 添加训练，或从模板导入。" else "没有左滑完成过的项目不会进入历史训练记录。"
                        )
                    }
                    TrainingPeriod.entries.forEach { period ->
                        val periodItems = visibleItems.filter { it.period == period }
                        if (periodItems.isNotEmpty()) {
                            TrainingPeriodSection(
                                period = period,
                                items = periodItems,
                                editable = canEdit,
                                onLongPress = { item -> actionItem = item },
                                activeTimerItemId = activeTimerItemId,
                                activeTimerRemainingSeconds = activeTimerRemainingSeconds,
                                highlightItemId = highlightItemId,
                                onHighlightDone = onHighlightDone,
                                onCompleteSetChange = { item, completedSets ->
                                    onUpdateCompletedSets(item.id, completedSets)
                                    if (completedSets > item.completedSets) {
                                        if (completedSets < item.targetSets) {
                                            onStartSetTimer(item)
                                        }
                                    } else if (completedSets < item.completedSets) {
                                        onResetSetTimer()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    if (canEdit && choosingAddSource) {
        AddSourceDialog(
            title = "添加训练",
            manualTitle = "新建训练计划",
            manualSubtitle = "手动填写力量训练或有氧训练",
            templateTitle = "导入训练模板",
            templateSubtitle = "从单项或集合模板快速添加",
            templateEnabled = templates.isNotEmpty() || templateGroups.isNotEmpty(),
            onManual = {
                choosingAddSource = false
                choosingPeriodFor = TrainingAddFlow.Manual
            },
            onTemplate = {
                choosingAddSource = false
                choosingPeriodFor = TrainingAddFlow.Template
            },
            onDismiss = { choosingAddSource = false }
        )
    }
    choosingPeriodFor?.let { flow ->
        TrainingPeriodChooserDialog(
            onChoose = { period ->
                selectedAddPeriod = period
                choosingPeriodFor = null
                if (flow == TrainingAddFlow.Manual) {
                    draft = TrainingItem(id = "draft", period = period)
                    choosingType = true
                } else {
                    choosingTemplate = true
                }
            },
            onDismiss = { choosingPeriodFor = null }
        )
    }
    if (canEdit && choosingType) {
        TrainingTypeChooserDialog(
            onChoose = { type ->
                draft = draft.copy(type = type)
                choosingType = false
                adding = true
            },
            onCancel = { resetDraft() }
        )
    }
    if (canEdit && adding) {
        TrainingDraftDialog(
            title = if (editingId == null) "添加训练" else "修改训练",
            draft = draft,
            onDraftChange = { draft = it },
            onDone = {
                val currentEditingId = editingId
                if (currentEditingId == null) {
                    onAddItem(draft)
                } else {
                    onUpdateItem(currentEditingId, draft)
                }
                resetDraft()
            },
            onCancel = { resetDraft() }
        )
    }
    if (canEdit && choosingTemplate) {
        TrainingTemplateImportDialog(
            templates = templates,
            groups = templateGroups,
            onDismiss = { choosingTemplate = false },
            onSelect = { template ->
                onAddTemplateItem(template, selectedAddPeriod)
                choosingTemplate = false
            },
            onSelectGroup = { group ->
                onAddTemplateGroup(group, selectedAddPeriod)
                choosingTemplate = false
            }
        )
    }
    actionItem?.let { item ->
        TrainingActionDialog(
            item = item,
            onDismiss = { actionItem = null },
            onEdit = {
                draft = item
                editingId = item.id
                choosingType = false
                adding = true
                actionItem = null
            },
            onSaveTemplate = {
                savingTemplateItem = item
                actionItem = null
            },
            onDelete = {
                onDeleteItem(item.id)
                actionItem = null
            }
        )
    }
    savingTemplateItem?.let { item ->
        SaveTrainingTemplateDialog(
            item = item,
            onDismiss = { savingTemplateItem = null },
            onSave = { category ->
                onSaveTemplate(item.name, category, item)
                savingTemplateItem = null
            }
        )
    }
}

@Composable
private fun TrainingPeriodSection(
    period: TrainingPeriod,
    items: List<TrainingItem>,
    editable: Boolean,
    onLongPress: (TrainingItem) -> Unit,
    activeTimerItemId: String?,
    activeTimerRemainingSeconds: Int,
    highlightItemId: String?,
    onHighlightDone: (String) -> Unit,
    onCompleteSetChange: (TrainingItem, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = period.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            TextPill(text = "${items.size} 项")
        }
        items.forEach { item ->
            TrainingItemRow(
                item = item,
                editable = editable,
                canSwipeLeft = editable && activeTimerItemId == null,
                canSwipeRight = editable && (activeTimerItemId == null || activeTimerItemId == item.id),
                highlight = highlightItemId == item.id,
                timerRemainingSeconds = if (activeTimerItemId == item.id) activeTimerRemainingSeconds else null,
                onLongPress = { onLongPress(item) },
                onHighlightDone = { onHighlightDone(item.id) },
                onCompleteSetChange = onCompleteSetChange
            )
        }
    }
}

@Composable
private fun ExactAlarmPermissionDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = "需要闹钟权限", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "为了在软件切到后台后仍能按时震动提醒，需要允许系统的“闹钟和提醒”权限。授权后再点击计时器开始。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onOpenSettings) {
                        Text("去授权")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrainingItemRow(
    item: TrainingItem,
    editable: Boolean,
    canSwipeLeft: Boolean,
    canSwipeRight: Boolean,
    highlight: Boolean,
    timerRemainingSeconds: Int?,
    onLongPress: () -> Unit,
    onHighlightDone: () -> Unit,
    onCompleteSetChange: (TrainingItem, Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var dragTotal by remember(item.id, item.completedSets) { mutableStateOf(0f) }
    val highlightPulse = remember(item.id) { Animatable(0f) }
    LaunchedEffect(highlight) {
        if (highlight) {
            repeat(3) {
                highlightPulse.animateTo(1f, animationSpec = tween(durationMillis = 180))
                highlightPulse.animateTo(0f, animationSpec = tween(durationMillis = 260))
            }
            onHighlightDone()
        } else {
            highlightPulse.snapTo(0f)
        }
    }
    val pulse = highlightPulse.value
    val baseBackground =
        if (item.completed) Color.LightGray.copy(alpha = 0.62f) else MaterialTheme.colorScheme.surface
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f * pulse)
    val backgroundColor =
        if (pulse > 0f) Color(
            red = baseBackground.red * (1f - pulse) + highlightColor.red * pulse,
            green = baseBackground.green * (1f - pulse) + highlightColor.green * pulse,
            blue = baseBackground.blue * (1f - pulse) + highlightColor.blue * pulse,
            alpha = maxOf(baseBackground.alpha, highlightColor.alpha)
        ) else baseBackground
    val borderColor =
        if (pulse > 0f) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
        } else if (item.completed) {
            Color.Red.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(item.id, item.completedSets, canSwipeLeft, canSwipeRight) {
                if (!canSwipeLeft && !canSwipeRight) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragTotal += dragAmount },
                    onDragEnd = {
                        when {
                            canSwipeLeft && dragTotal < -96f && item.completedSets < item.targetSets -> {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCompleteSetChange(item, item.completedSets + 1)
                            }
                            canSwipeRight && dragTotal > 96f && item.completedSets > 0 -> {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCompleteSetChange(item, item.completedSets - 1)
                            }
                        }
                        dragTotal = 0f
                    }
                )
            }
            .combinedClickable(
                enabled = editable,
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onLongPress()
                }
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name.ifBlank { if (item.type == TrainingType.Cardio) "未命名有氧" else "未命名力量训练" },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (item.completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f) else MaterialTheme.colorScheme.onSurface
                )
                TextPill(text = item.type.label)
            }
            if (item.type == TrainingType.Cardio) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(label = "时长", value = "${item.durationMinutes.ifBlank { "--" }} 分钟", modifier = Modifier.weight(1f))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(
                        label = if (editable) "组数" else "实际组数",
                        value = if (editable) "${item.completedSets}/${item.targetSets}" else "${item.completedSets} 组",
                        modifier = Modifier.weight(1f)
                    )
                    InfoChip(label = "次数", value = item.reps.ifBlank { "--" }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(label = "重量", value = item.weightLabel(), modifier = Modifier.weight(1f))
                    InfoChip(label = "休息", value = "${item.restSeconds.ifBlank { "--" }} 秒", modifier = Modifier.weight(1f))
                }
            }
            if (item.type == TrainingType.Cardio) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(label = "完成", value = "${item.completedSets}/${item.targetSets}", modifier = Modifier.weight(1f))
                }
            }
        }
        if (timerRemainingSeconds != null) {
            val minutes = timerRemainingSeconds / 60
            val seconds = timerRemainingSeconds % 60
            Text(
                text = "REST\n%02d:%02d".format(minutes, seconds),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Red.copy(alpha = 0.76f),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        } else if (item.completed) {
            Text(
                text = "COMPLETE",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Red.copy(alpha = 0.72f),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AddSourceDialog(
    title: String,
    manualTitle: String,
    manualSubtitle: String,
    templateTitle: String,
    templateSubtitle: String,
    templateEnabled: Boolean,
    aiTitle: String? = null,
    aiSubtitle: String = "",
    aiEnabled: Boolean = true,
    secondAiTitle: String? = null,
    secondAiSubtitle: String = "",
    secondAiEnabled: Boolean = true,
    onManual: () -> Unit,
    onTemplate: () -> Unit,
    onAi: (() -> Unit)? = null,
    onSecondAi: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SettingsMenuItem(
                    title = manualTitle,
                    subtitle = manualSubtitle,
                    onClick = onManual
                )
                SettingsMenuItem(
                    title = templateTitle,
                    subtitle = if (templateEnabled) templateSubtitle else "还没有模板，请先在设置的数据模板里添加",
                    enabled = templateEnabled,
                    onClick = onTemplate
                )
                if (aiTitle != null && onAi != null) {
                    SettingsMenuItem(
                        title = aiTitle,
                        subtitle = aiSubtitle,
                        enabled = aiEnabled,
                        onClick = onAi
                    )
                }
                if (secondAiTitle != null && onSecondAi != null) {
                    SettingsMenuItem(
                        title = secondAiTitle,
                        subtitle = secondAiSubtitle,
                        enabled = secondAiEnabled,
                        onClick = onSecondAi
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainingTypeChooser(
    onChoose: (TrainingType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "添加训练计划", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = "选择今天要记录的训练类型",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingsMenuItem(
            title = "力量训练",
            subtitle = "记录训练名称、组数、次数、重量和组间休息",
            onClick = { onChoose(TrainingType.Strength) }
        )
        SettingsMenuItem(
            title = "有氧训练",
            subtitle = "记录运动名称和持续时间",
            onClick = { onChoose(TrainingType.Cardio) }
        )
    }
}

@Composable
private fun TrainingTypeChooserDialog(
    onChoose: (TrainingType) -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        PopDialogSurface {
            TrainingTypeChooser(onChoose = onChoose)
        }
    }
}

@Composable
private fun TrainingPeriodChooserDialog(
    onChoose: (TrainingPeriod) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "选择训练时段", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TrainingPeriod.entries.forEach { period ->
                    SettingsMenuItem(
                        title = period.label,
                        subtitle = "添加到${period.label}训练视图",
                        onClick = { onChoose(period) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainingDraftDialog(
    title: String,
    draft: TrainingItem,
    onDraftChange: (TrainingItem) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        PopDialogSurface {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                TrainingDraftEditor(
                    draft = draft,
                    onDraftChange = onDraftChange,
                    onDone = onDone,
                    onCancel = onCancel
                )
            }
        }
    }
}

@Composable
private fun TrainingActionDialog(
    item: TrainingItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSaveTemplate: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = item.name.ifBlank { if (item.type == TrainingType.Cardio) "未命名有氧" else "未命名力量训练" },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = item.summaryLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onEdit) {
                        Text("修改")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onSaveTemplate) {
                        Text("存为模板")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onDelete) {
                        Text("删除")
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingTemplateImportDialog(
    templates: List<TrainingTemplate>,
    groups: List<TrainingTemplateGroup>,
    onDismiss: () -> Unit,
    onSelect: (TrainingTemplate) -> Unit,
    onSelectGroup: (TrainingTemplateGroup) -> Unit
) {
    var route by remember { mutableStateOf<ImportRoute>(ImportRoute.Home) }
    var dragTotal by remember { mutableStateOf(0f) }
    val singleCategories = remember(templates) { templates.map { it.category.normalizedCategory() }.distinct().sorted() }
    val groupCategories = remember(groups) { groups.map { it.category.normalizedCategory() }.distinct().sorted() }
    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .height(460.dp)
                    .verticalScroll(rememberScrollState())
                    .pointerInput(route) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onHorizontalDrag = { _, dragAmount -> dragTotal += dragAmount },
                            onDragEnd = {
                                if (dragTotal > 96f && route != ImportRoute.Home) {
                                    route = when (route) {
                                        ImportRoute.SingleCategories,
                                        ImportRoute.GroupCategories -> ImportRoute.Home
                                        is ImportRoute.SingleDetail -> ImportRoute.SingleCategories
                                        is ImportRoute.GroupDetail -> ImportRoute.GroupCategories
                                        ImportRoute.Home -> ImportRoute.Home
                                    }
                                }
                                dragTotal = 0f
                            }
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "导入训练模板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        val forward = targetState.importDepth() >= initialState.importDepth()
                        val enter = slideInHorizontally { if (forward) it else -it } + fadeIn()
                        val exit = slideOutHorizontally { if (forward) -it else it } + fadeOut()
                        enter togetherWith exit using SizeTransform(clip = false)
                    },
                    label = "TrainingTemplateImportRoute"
                ) { current ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (current) {
                            ImportRoute.Home -> {
                                SettingsMenuItem(
                                    title = "单项模板",
                                    subtitle = "${templates.size} 个模板，${singleCategories.size} 个分类",
                                    enabled = templates.isNotEmpty(),
                                    onClick = { route = ImportRoute.SingleCategories }
                                )
                                SettingsMenuItem(
                                    title = "集合模板",
                                    subtitle = "${groups.size} 个集合，${groupCategories.size} 个分类",
                                    enabled = groups.isNotEmpty(),
                                    onClick = { route = ImportRoute.GroupCategories }
                                )
                            }
                            ImportRoute.SingleCategories -> {
                                ImportCategoryList(
                                    categories = singleCategories,
                                    emptyText = "还没有训练单项模板。",
                                    onCategoryClick = { route = ImportRoute.SingleDetail(it) }
                                )
                            }
                            ImportRoute.GroupCategories -> {
                                ImportCategoryList(
                                    categories = groupCategories,
                                    emptyText = "还没有训练集合模板。",
                                    onCategoryClick = { route = ImportRoute.GroupDetail(it) }
                                )
                            }
                            is ImportRoute.SingleDetail -> {
                                templates.filter { it.category.normalizedCategory() == current.category }.forEach { template ->
                                    SettingsMenuItem(
                                        title = template.templateName.ifBlank { template.item.name.ifBlank { "训练模板" } },
                                        subtitle = template.item.summaryLabel(),
                                        onClick = { onSelect(template) }
                                    )
                                }
                            }
                            is ImportRoute.GroupDetail -> {
                                groups.filter { it.category.normalizedCategory() == current.category }.forEach { group ->
                                    SettingsMenuItem(
                                        title = group.templateName.ifBlank { "训练集合模板" },
                                        subtitle = "${group.items.size} 项训练",
                                        onClick = { onSelectGroup(group) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveTrainingTemplateDialog(
    item: TrainingItem,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var category by remember(item) { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "保存训练模板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = item.summaryLabel(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类") },
                    singleLine = true
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(enabled = item.name.isNotBlank(), onClick = { onSave(category) }) {
                        Text("保存模板")
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingDraftEditor(
    draft: TrainingItem,
    onDraftChange: (TrainingItem) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    showPeriod: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextPill(text = draft.type.label)
            Spacer(modifier = Modifier.weight(1f))
            if (showPeriod) {
                TextPill(text = draft.period.label)
            }
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = draft.name,
            onValueChange = { onDraftChange(draft.copy(name = it)) },
            label = { Text(if (draft.type == TrainingType.Cardio) "运动名" else "训练名称") },
            singleLine = true
        )
        if (draft.type == TrainingType.Cardio) {
            FormPanel(title = "有氧信息") {
                NumberField(
                    modifier = Modifier.fillMaxWidth(),
                    value = draft.durationMinutes,
                    label = "持续时间 分钟",
                    editable = true,
                    onValueChange = { onDraftChange(draft.copy(durationMinutes = it)) }
                )
            }
        } else {
            FormPanel(title = "训练容量") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        modifier = Modifier.weight(1f),
                        value = draft.sets,
                        label = "组数",
                        editable = true,
                        onValueChange = { onDraftChange(draft.copy(sets = it)) }
                    )
                    NumberField(
                        modifier = Modifier.weight(1f),
                        value = draft.reps,
                        label = "每组次数",
                        editable = true,
                        onValueChange = { onDraftChange(draft.copy(reps = it)) }
                    )
                }
            }
            FormPanel(title = "负重与休息") {
                SimpleDropdown(
                    modifier = Modifier.fillMaxWidth(),
                    label = draft.weightMode.label,
                    options = TrainingWeightMode.entries.map { it.label },
                    onSelected = { label ->
                        val mode = TrainingWeightMode.entries.firstOrNull { it.label == label } ?: TrainingWeightMode.Bodyweight
                        onDraftChange(
                            draft.copy(
                                weightMode = mode,
                                weightKg = if (mode == TrainingWeightMode.Bodyweight) "" else draft.weightKg
                            )
                        )
                    }
                )
                if (draft.weightMode == TrainingWeightMode.Kg) {
                    NumberField(
                        modifier = Modifier.fillMaxWidth(),
                        value = draft.weightKg,
                        label = "重量 kg",
                        editable = true,
                        onValueChange = { onDraftChange(draft.copy(weightKg = it)) }
                    )
                }
                NumberField(
                    modifier = Modifier.fillMaxWidth(),
                    value = draft.restSeconds,
                    label = "组间休息 秒",
                    editable = true,
                    onValueChange = { onDraftChange(draft.copy(restSeconds = it)) }
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                enabled = draft.name.isNotBlank() ||
                    draft.sets.isNotBlank() ||
                    draft.reps.isNotBlank() ||
                    draft.restSeconds.isNotBlank() ||
                    draft.weightKg.isNotBlank() ||
                    draft.durationMinutes.isNotBlank(),
                onClick = onDone
            ) {
                Text("完成")
            }
        }
    }
}

@Composable
private fun FormPanel(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun MealItemsCard(
    items: List<MealItem>,
    templates: List<MealTemplate>,
    templateGroups: List<MealTemplateGroup>,
    selectedMealViewType: MealViewType,
    editable: Boolean,
    editMode: Boolean,
    isAnalyzingNutrition: Boolean,
    isAnalyzingMealNutrition: Boolean,
    isRecognizingFoodPhoto: Boolean,
    bodyGoalRecommendation: String,
    nutritionAnalysis: String,
    mealNutritionAnalysis: String,
    breakfastNutritionAnalysis: String,
    lunchNutritionAnalysis: String,
    dinnerNutritionAnalysis: String,
    onMealViewTypeChange: (MealViewType) -> Unit,
    onAddItem: (MealItem) -> Unit,
    onAddTemplateItem: (MealTemplate) -> Unit,
    onAddTemplateGroup: (MealTemplateGroup) -> Unit,
    onSaveTemplate: (String, String, MealItem) -> Unit,
    onUpdateItem: (String, MealItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    onAnalyzeNutrition: () -> Unit,
    onAnalyzeMealNutrition: () -> Unit,
    onRecognizeFoodPhoto: () -> Unit,
    onPickFoodPhoto: () -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    val selectedMealType = selectedMealViewType.mealType
    val draftMealType = selectedMealType ?: MealType.Breakfast
    var draft by remember(selectedMealViewType) { mutableStateOf(MealItem(id = "draft", type = draftMealType)) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var actionItem by remember { mutableStateOf<MealItem?>(null) }
    var choosingAddSource by remember { mutableStateOf(false) }
    var choosingTemplate by remember { mutableStateOf(false) }
    var savingTemplateItem by remember { mutableStateOf<MealItem?>(null) }
    val canEdit = editable
    val isAllDay = selectedMealViewType == MealViewType.All
    val selectedItems = selectedMealType?.let { type -> items.filter { it.type == type } } ?: items
    val analysisContent = if (isAllDay) nutritionAnalysis else mealNutritionAnalysis
    val dailyTarget = remember(bodyGoalRecommendation) { parseBodyGoalRecommendation(bodyGoalRecommendation).toNumbers() }
    val breakfastNumbers = remember(breakfastNutritionAnalysis) {
        if (breakfastNutritionAnalysis.isBlank()) NutritionNumbers() else parseMealNutritionAnalysis(breakfastNutritionAnalysis).toNumbers()
    }
    val lunchNumbers = remember(lunchNutritionAnalysis) {
        if (lunchNutritionAnalysis.isBlank()) NutritionNumbers() else parseMealNutritionAnalysis(lunchNutritionAnalysis).toNumbers()
    }
    val dinnerNumbers = remember(dinnerNutritionAnalysis) {
        if (dinnerNutritionAnalysis.isBlank()) NutritionNumbers() else parseMealNutritionAnalysis(dinnerNutritionAnalysis).toNumbers()
    }
    val analyzedAllDayNumbers = breakfastNumbers.plus(lunchNumbers).plus(dinnerNumbers)
    val selectedMealNumbers = when (selectedMealType) {
        MealType.Breakfast -> breakfastNumbers
        MealType.Lunch -> lunchNumbers
        MealType.Dinner -> dinnerNumbers
        null -> analyzedAllDayNumbers
    }
    val selectedTarget = if (isAllDay) dailyTarget else dailyTarget.times(selectedMealViewType.mealRatio())
    val selectedConsumed = if (isAllDay) analyzedAllDayNumbers else selectedMealNumbers

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PageIntro(
            title = "摄入",
            subtitle = if (isAllDay) "查看全天摄入进度和 AI 营养建议。" else "记录${selectedMealViewType.label}食物，分析本餐营养结构。",
            trailing = {
                SimpleDropdown(
                    modifier = Modifier.width(112.dp),
                    label = selectedMealViewType.label,
                    options = MealViewType.entries.map { it.label },
                    onSelected = { label ->
                        MealViewType.fromLabel(label).let { type ->
                            onMealViewTypeChange(type)
                            adding = false
                            editingId = null
                            draft = MealItem(id = "draft", type = type.mealType ?: MealType.Breakfast)
                        }
                    }
                )
            }
        )
        SectionCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAllDay) "全天营养" else "${selectedMealViewType.label}记录",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    if (canEdit) {
                        RoundIconAction(
                            icon = Icons.Filled.AutoAwesome,
                            contentDescription = "AI 分析",
                            modifier = Modifier.width(48.dp).height(48.dp),
                            enabled = if (isAllDay) !isAnalyzingNutrition && items.isNotEmpty() else !isAnalyzingMealNutrition && selectedItems.isNotEmpty(),
                            onClick = if (isAllDay) onAnalyzeNutrition else onAnalyzeMealNutrition
                        )
                    }
                }
                if ((isAllDay && isAnalyzingNutrition) || (!isAllDay && isAnalyzingMealNutrition)) {
                    TextPill(text = "AI 分析中")
                }
                if (analysisContent.isNotBlank()) {
                    MealNutritionChart(content = analysisContent, mealLabel = selectedMealViewType.label)
                }
                MealTargetSummaryCard(
                    label = selectedMealViewType.label,
                    target = selectedTarget,
                    consumed = selectedConsumed,
                    hasTarget = bodyGoalRecommendation.isNotBlank(),
                    isAllDay = isAllDay
                )
                if (!isAllDay) {
                    MealTableHeader(
                        count = selectedItems.size,
                        editable = canEdit,
                        onAdd = {
                            editingId = null
                            adding = false
                            choosingAddSource = true
                        }
                    )
                    if (selectedItems.isEmpty()) {
                        EmptyState(
                            title = "还没有食物记录",
                            subtitle = if (canEdit) "点击右侧 + 添加${selectedMealViewType.label}食物。" else "这一天没有${selectedMealViewType.label}记录。"
                        )
                    }
                    selectedItems.forEach { item ->
                        MealTableRow(
                            item = item,
                            editable = canEdit,
                            onLongPress = { actionItem = item }
                        )
                    }
                }
            }
        }
    }
    if (canEdit && choosingAddSource) {
        AddSourceDialog(
            title = "添加食物",
            manualTitle = "新建食物记录",
            manualSubtitle = "手动填写食物名和克数",
            templateTitle = "导入食物模板",
            templateSubtitle = "从单项或集合模板快速添加",
            templateEnabled = templates.isNotEmpty() || templateGroups.isNotEmpty(),
            aiTitle = if (isRecognizingFoodPhoto) "AI 正在识别..." else "AI 拍照识别",
            aiSubtitle = "拍一张餐食照片，自动拆成食物和克数",
            aiEnabled = !isRecognizingFoodPhoto && selectedMealType != null,
            secondAiTitle = "AI 相册识别",
            secondAiSubtitle = "从相册选择一张食物照片进行识别",
            secondAiEnabled = !isRecognizingFoodPhoto && selectedMealType != null,
            onManual = {
                choosingAddSource = false
                draft = MealItem(id = "draft", type = draftMealType)
                adding = true
            },
            onTemplate = {
                choosingAddSource = false
                choosingTemplate = true
            },
            onAi = {
                choosingAddSource = false
                onRecognizeFoodPhoto()
            },
            onSecondAi = {
                choosingAddSource = false
                onPickFoodPhoto()
            },
            onDismiss = { choosingAddSource = false }
        )
    }
    if (canEdit && adding) {
        MealDraftDialog(
            title = if (editingId == null) "添加食物" else "修改食物",
            draft = draft,
            onDraftChange = { draft = it.copy(type = draftMealType) },
            onDone = {
                val currentEditingId = editingId
                if (currentEditingId == null) {
                    onAddItem(draft.copy(type = draftMealType))
                } else {
                    onUpdateItem(currentEditingId, draft.copy(type = draftMealType))
                }
                draft = MealItem(id = "draft", type = draftMealType)
                editingId = null
                adding = false
            },
            onCancel = {
                draft = MealItem(id = "draft", type = draftMealType)
                editingId = null
                adding = false
            }
        )
    }
    if (canEdit && choosingTemplate) {
        MealTemplateImportDialog(
            templates = templates,
            groups = templateGroups,
            selectedMealType = draftMealType,
            onDismiss = { choosingTemplate = false },
            onSelect = { template ->
                onAddTemplateItem(template)
                choosingTemplate = false
            },
            onSelectGroup = { group ->
                onAddTemplateGroup(group)
                choosingTemplate = false
            }
        )
    }
    actionItem?.let { item ->
        MealActionDialog(
            item = item,
            onDismiss = { actionItem = null },
            onEdit = {
                draft = item
                editingId = item.id
                adding = true
                actionItem = null
            },
            onSaveTemplate = {
                savingTemplateItem = item
                actionItem = null
            },
            onDelete = {
                onDeleteItem(item.id)
                actionItem = null
            }
        )
    }
    savingTemplateItem?.let { item ->
        SaveMealTemplateDialog(
            item = item,
            onDismiss = { savingTemplateItem = null },
            onSave = { name, category ->
                onSaveTemplate(name, category, item)
                savingTemplateItem = null
            }
        )
    }
}

@Composable
private fun AnalysisCard(
    analysis: String,
    isAnalyzing: Boolean,
    editable: Boolean,
    onAnalyze: () -> Unit
) {
    SectionCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "全天分析",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (editable) {
                    Button(
                        enabled = !isAnalyzing,
                        onClick = onAnalyze
                    ) {
                        Text(if (isAnalyzing) "分析中..." else "AI 分析")
                    }
                }
            }
            if (analysis.isNotBlank()) {
                AiReportCard(content = analysis)
            } else {
                Text(text = "分析会综合当天身体数据、训练、三餐和照片上传情况。")
            }
        }
    }
}

@Composable
private fun AiReportCard(content: String) {
    val sections = remember(content) { parseReportTree(content) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "AI 鎶ュ憡", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (sections.isEmpty()) {
            MarkdownText(content = content)
        } else {
            ReportSectionList(sections = sections)
        }
    }
}

@Composable
private fun MealTableHeader(
    count: Int,
    editable: Boolean,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = "食物记录",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        TextPill(text = "$count 项")
        if (editable) {
            RoundIconAction(
                icon = Icons.Filled.Add,
                contentDescription = "添加食物",
                onClick = onAdd,
                modifier = Modifier.width(44.dp).height(44.dp)
            )
        }
    }
}

@Composable
private fun MealTargetSummaryCard(
    label: String,
    target: NutritionNumbers,
    consumed: NutritionNumbers,
    hasTarget: Boolean,
    isAllDay: Boolean
) {
    val remaining = target.minus(consumed)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isAllDay) "全天摄入进度" else "$label 推荐分配",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            TextPill(text = if (isAllDay) "全天" else "${(MealViewType.fromLabel(label).mealRatio() * 100).roundToInt()}%")
        }
        if (!hasTarget) {
            Text(
                text = "身体数据页生成今日摄入推荐后，这里会显示已摄入和剩余可摄入。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }
        NutritionProgressRow(name = "总热量", target = target.calories, consumed = consumed.calories, remaining = remaining.calories, unit = "kcal")
        NutritionProgressRow(name = "碳水", target = target.carbs, consumed = consumed.carbs, remaining = remaining.carbs, unit = "g")
        NutritionProgressRow(name = "蛋白质", target = target.protein, consumed = consumed.protein, remaining = remaining.protein, unit = "g")
        NutritionProgressRow(name = "脂肪", target = target.fat, consumed = consumed.fat, remaining = remaining.fat, unit = "g")
        NutritionProgressRow(name = "膳食纤维", target = target.fiber, consumed = consumed.fiber, remaining = remaining.fiber, unit = "g")
    }
}

@Composable
private fun NutritionProgressRow(
    name: String,
    target: Double,
    consumed: Double,
    remaining: Double,
    unit: String
) {
    val progress = if (target > 0.0) (consumed / target).toFloat().coerceIn(0f, 1.2f) else 0f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.50f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "${formatNutritionValue(consumed, unit)} / ${formatNutritionValue(target, unit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp)),
            color = if (progress > 1f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = "剩余 ${formatNutritionValue(remaining, unit)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatNutritionValue(value: Double, unit: String): String {
    val number = if (value % 1.0 == 0.0) value.roundToInt().toString() else "%.1f".format(value)
    return "$number $unit"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MealTableRow(
    item: MealItem,
    editable: Boolean,
    onLongPress: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = editable,
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onLongPress()
                }
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.foodName.ifBlank { "未命名食物" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "记录重量",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextPill(text = "${item.grams.ifBlank { "--" }} g")
    }
}

@Composable
private fun MealActionDialog(
    item: MealItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSaveTemplate: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        SectionCard {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = item.foodName.ifBlank { "食物记录" }, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${item.grams.ifBlank { "--" }} g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onEdit) {
                        Text("修改")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onSaveTemplate) {
                        Text("存为模板")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onDelete) {
                        Text("删除")
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodRecognitionDialog(
    session: FoodRecognitionSession,
    isRecognizing: Boolean,
    onSupplementChange: (String) -> Unit,
    onRetry: () -> Unit,
    onAddPhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PopDialogSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.88f)
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "AI 食物识别", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${session.mealType.label} · ${session.photoPaths.size} 张照片",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("退出")
                    }
                }
                val latestPhoto = session.photoPaths.lastOrNull()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!latestPhoto.isNullOrBlank()) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            painter = rememberAsyncImagePainter(File(latestPhoto)),
                            contentDescription = "待识别食物照片",
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f))))
                        )
                        Text(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp),
                            text = if (isRecognizing) "AI 正在分析这张照片..." else "当前预览照片",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(text = "还没有选择照片", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (isRecognizing) {
                    SectionCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "正在等待 AI 结果", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = "识别完成后会先给你确认，不会直接写入食物表。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    SectionCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "识别结果", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (session.recognizedItems.isEmpty()) {
                                EmptyState(
                                    title = "暂时没有可添加的食物",
                                    subtitle = session.note.ifBlank { "可以补充说明或再上传一张更清晰的照片继续识别。" }
                                )
                            } else {
                                session.recognizedItems.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.foodName.ifBlank { "未命名食物" },
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        TextPill(text = "${item.grams.ifBlank { "--" }} g")
                                    }
                                }
                                if (session.note.isNotBlank()) {
                                    Text(
                                        text = session.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = session.supplementText,
                    onValueChange = onSupplementChange,
                    label = { Text("补充信息") },
                    placeholder = { Text("例如：这是半碗米饭，鸡胸肉大概一掌心，还有少量酱汁") },
                    minLines = 2
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(modifier = Modifier.weight(1f), enabled = !isRecognizing, onClick = onAddPhoto) {
                        Text("继续拍照")
                    }
                    Button(modifier = Modifier.weight(1f), enabled = !isRecognizing, onClick = onPickPhoto) {
                        Text("上传相册")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(enabled = !isRecognizing && session.photoPaths.isNotEmpty(), onClick = onRetry) {
                        Text("继续识别")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(enabled = !isRecognizing && session.recognizedItems.isNotEmpty(), onClick = onConfirm) {
                        Text("满意，添加到表格")
                    }
                }
            }
        }
    }
}

@Composable
private fun MealTemplateImportDialog(
    templates: List<MealTemplate>,
    groups: List<MealTemplateGroup>,
    selectedMealType: MealType,
    onDismiss: () -> Unit,
    onSelect: (MealTemplate) -> Unit,
    onSelectGroup: (MealTemplateGroup) -> Unit
) {
    var route by remember { mutableStateOf<ImportRoute>(ImportRoute.Home) }
    var dragTotal by remember { mutableStateOf(0f) }
    val singleCategories = remember(templates) { templates.map { it.category.normalizedCategory() }.distinct().sorted() }
    val groupCategories = remember(groups) { groups.map { it.category.normalizedCategory() }.distinct().sorted() }
    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .height(460.dp)
                    .verticalScroll(rememberScrollState())
                    .pointerInput(route) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onHorizontalDrag = { _, dragAmount -> dragTotal += dragAmount },
                            onDragEnd = {
                                if (dragTotal > 96f && route != ImportRoute.Home) {
                                    route = when (route) {
                                        ImportRoute.SingleCategories,
                                        ImportRoute.GroupCategories -> ImportRoute.Home
                                        is ImportRoute.SingleDetail -> ImportRoute.SingleCategories
                                        is ImportRoute.GroupDetail -> ImportRoute.GroupCategories
                                        ImportRoute.Home -> ImportRoute.Home
                                    }
                                }
                                dragTotal = 0f
                            }
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "导入食物模板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        val forward = targetState.importDepth() >= initialState.importDepth()
                        val enter = slideInHorizontally { if (forward) it else -it } + fadeIn()
                        val exit = slideOutHorizontally { if (forward) -it else it } + fadeOut()
                        enter togetherWith exit using SizeTransform(clip = false)
                    },
                    label = "MealTemplateImportRoute"
                ) { current ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (current) {
                            ImportRoute.Home -> {
                                SettingsMenuItem(
                                    title = "单项模板",
                                    subtitle = "${templates.size} 个模板，${singleCategories.size} 个分类",
                                    enabled = templates.isNotEmpty(),
                                    onClick = { route = ImportRoute.SingleCategories }
                                )
                                SettingsMenuItem(
                                    title = "集合模板",
                                    subtitle = "${groups.size} 个集合，${groupCategories.size} 个分类",
                                    enabled = groups.isNotEmpty(),
                                    onClick = { route = ImportRoute.GroupCategories }
                                )
                            }
                            ImportRoute.SingleCategories -> {
                                ImportCategoryList(
                                    categories = singleCategories,
                                    emptyText = "还没有食物单项模板。",
                                    onCategoryClick = { route = ImportRoute.SingleDetail(it) }
                                )
                            }
                            ImportRoute.GroupCategories -> {
                                ImportCategoryList(
                                    categories = groupCategories,
                                    emptyText = "还没有食物集合模板。",
                                    onCategoryClick = { route = ImportRoute.GroupDetail(it) }
                                )
                            }
                            is ImportRoute.SingleDetail -> {
                                templates.filter { it.category.normalizedCategory() == current.category }.forEach { template ->
                                    val item = template.item.copy(type = selectedMealType)
                                    SettingsMenuItem(
                                        title = item.foodName.ifBlank { "食物模板" },
                                        subtitle = "${item.foodName.ifBlank { "未命名食物" }}：${item.grams.ifBlank { "--" }} g",
                                        onClick = { onSelect(template.copy(item = item)) }
                                    )
                                }
                            }
                            is ImportRoute.GroupDetail -> {
                                groups.filter { it.category.normalizedCategory() == current.category }.forEach { group ->
                                    SettingsMenuItem(
                                        title = group.templateName.ifBlank { "食物集合模板" },
                                        subtitle = "${group.items.size} 项食物",
                                        onClick = { onSelectGroup(group) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveMealTemplateDialog(
    item: MealItem,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var category by remember(item) { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "保存食物模板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "${item.foodName.ifBlank { "未命名食物" }}：${item.grams.ifBlank { "--" }} g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类") },
                    singleLine = true
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(enabled = item.foodName.isNotBlank(), onClick = { onSave(item.foodName, category) }) {
                        Text("保存模板")
                    }
                }
            }
        }
    }
}

@Composable
private fun PopDialogSurface(
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(90)) + scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            initialScale = 0.86f
        ),
        exit = fadeOut(animationSpec = tween(90)) + scaleOut(
            animationSpec = tween(110),
            targetScale = 0.94f
        )
    ) {
        SectionCard {
            content()
        }
    }
}

@Composable
private fun MealDraftDialog(
    title: String,
    draft: MealItem,
    onDraftChange: (MealItem) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        PopDialogSurface {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                MealDraftEditor(
                    draft = draft,
                    onDraftChange = onDraftChange,
                    onDone = onDone,
                    onCancel = onCancel
                )
            }
        }
    }
}

@Composable
private fun MealDraftEditor(
    draft: MealItem,
    onDraftChange: (MealItem) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = draft.foodName,
            onValueChange = { onDraftChange(draft.copy(foodName = it)) },
            label = { Text("食物名") },
            singleLine = true
        )
        NumberField(
            modifier = Modifier.fillMaxWidth(),
            value = draft.grams,
            label = "克数",
            editable = true,
            onValueChange = { onDraftChange(draft.copy(grams = it)) }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                enabled = draft.foodName.isNotBlank() || draft.grams.isNotBlank(),
                onClick = onDone
            ) {
                Text("完成")
            }
        }
    }
}

@Composable
private fun MealNutritionChart(
    content: String,
    mealLabel: String
) {
    val analysis = remember(content) { parseMealNutritionAnalysis(content) }
    val carbsColor = Color(0xFF2F80ED)
    val proteinColor = Color(0xFFFF4D6D)
    val fatColor = Color(0xFFFFC857)
    val fiberColor = Color(0xFF23D18B)
    val values = listOf(
        analysis.carbsPercent,
        analysis.proteinPercent,
        analysis.fatPercent,
        analysis.fiberPercent
    )
    val total = max(values.sum(), 1f)
    var adviceExpanded by remember(content) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$mealLabel 营养占比",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextPill(text = analysis.calories)
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
            ) {
                val pieSize = minOf(size.width, size.height) * 0.58f
                val left = (this.size.width - pieSize) / 2f
                val top = (this.size.height - pieSize) * 0.56f
                val centerX = left + pieSize / 2f
                val centerY = top + pieSize / 2f
                val labels = listOf(
                    "碳水\n${analysis.carbsGrams.ifBlank { "--" }}\n${analysis.carbsPercent.roundToInt()}%",
                    "蛋白质\n${analysis.proteinGrams.ifBlank { "--" }}\n${analysis.proteinPercent.roundToInt()}%",
                    "脂肪\n${analysis.fatGrams.ifBlank { "--" }}\n${analysis.fatPercent.roundToInt()}%",
                    "纤维\n${analysis.fiberGrams.ifBlank { "--" }}\n${analysis.fiberPercent.roundToInt()}%"
                )
                val colors = listOf(carbsColor, proteinColor, fatColor, fiberColor)
                val arcs = values.mapIndexed { index, _ ->
                    val sweep = values[index] / total * 360f
                    val startAngle = -90f + values.take(index).sumOf { (it / total * 360f).toDouble() }.toFloat()
                    Triple(startAngle, sweep, colors[index])
                }
                arcs.forEachIndexed { index, arc ->
                    val startAngle = arc.first
                    val sweep = arc.second
                    val color = arc.third
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(pieSize, pieSize)
                    )
                }
                val insidePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 24f
                    isFakeBoldText = true
                    isAntiAlias = true
                    setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
                }
                val outsidePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 21f
                    isFakeBoldText = true
                    isAntiAlias = true
                    setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
                }
                val smallIndices = arcs.mapIndexedNotNull { index, arc -> index.takeIf { arc.second < 58f } }
                val leftSlots = mutableListOf<androidx.compose.ui.geometry.Offset>()
                val rightSlots = mutableListOf<androidx.compose.ui.geometry.Offset>()
                smallIndices.forEach { index ->
                    val arc = arcs[index]
                    val midAngle = Math.toRadians((arc.first + arc.second / 2f).toDouble())
                    val goesRight = cos(midAngle) >= 0.0
                    val slotY = 34f + (if (goesRight) rightSlots.size else leftSlots.size) * 58f
                    val slotX = if (goesRight) size.width - 78f else 78f
                    val offset = androidx.compose.ui.geometry.Offset(slotX, slotY)
                    if (goesRight) rightSlots += offset else leftSlots += offset
                }
                val smallTargetByIndex = mutableMapOf<Int, androidx.compose.ui.geometry.Offset>()
                var leftCursor = 0
                var rightCursor = 0
                smallIndices.forEach { index ->
                    val arc = arcs[index]
                    val midAngle = Math.toRadians((arc.first + arc.second / 2f).toDouble())
                    if (cos(midAngle) >= 0.0) {
                        smallTargetByIndex[index] = rightSlots[rightCursor++]
                    } else {
                        smallTargetByIndex[index] = leftSlots[leftCursor++]
                    }
                }
                arcs.forEachIndexed { index, arc ->
                    val startAngle = arc.first
                    val sweep = arc.second
                    val color = arc.third
                    val midAngle = Math.toRadians((startAngle + sweep / 2f).toDouble())
                    val target = smallTargetByIndex[index]
                    val textX: Float
                    val textY: Float
                    val paint: android.graphics.Paint
                    if (target != null) {
                        val edgeX = centerX + cos(midAngle).toFloat() * pieSize * 0.5f
                        val edgeY = centerY + sin(midAngle).toFloat() * pieSize * 0.5f
                        textX = target.x
                        textY = target.y
                        paint = outsidePaint
                        drawLine(
                            color = color,
                            start = androidx.compose.ui.geometry.Offset(edgeX, edgeY),
                            end = androidx.compose.ui.geometry.Offset(textX, textY + 12f),
                            strokeWidth = 3f
                        )
                    } else {
                        val labelRadius = pieSize * 0.27f
                        textX = centerX + cos(midAngle).toFloat() * labelRadius
                        textY = centerY + sin(midAngle).toFloat() * labelRadius
                        paint = insidePaint
                    }
                    labels[index].lineSequence().forEachIndexed { lineIndex, line ->
                        drawContext.canvas.nativeCanvas.drawText(
                            line,
                            textX,
                            textY + (lineIndex - 1) * if (target != null) 23f else 28f,
                            paint
                        )
                    }
                }
            }
            Text(
                text = "${if (mealLabel == "全天") "全天总卡路里" else "本餐总卡路里"}：${analysis.calories}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (analysis.advice.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "寤鸿",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { adviceExpanded = !adviceExpanded }) {
                            Text(if (adviceExpanded) "鏀惰捣" else "灞曞紑")
                        }
                    }
                    if (adviceExpanded) {
                        MarkdownText(content = analysis.advice)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownText(content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { rawLine ->
                val headingLevel = rawLine.takeWhile { it == '#' }.length
                val withoutHeading = rawLine.drop(headingLevel).trim()
                val cleaned = cleanMarkdownTextLine(withoutHeading)
                val style = when (headingLevel) {
                    1 -> MaterialTheme.typography.titleMedium
                    2 -> MaterialTheme.typography.titleSmall
                    3 -> MaterialTheme.typography.bodyLarge
                    else -> MaterialTheme.typography.bodyMedium
                }
                Text(
                    text = cleaned,
                    style = style,
                    fontWeight = if (headingLevel > 0) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth()
                )
            }
    }
}

@Composable
private fun NutritionAnalysisList(content: String) {
    val analysis = remember(content) { parseNutritionAnalysis(content) }
    SectionCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "营养分析", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            NutritionAnalysisRow(label = "已摄入卡路里", value = analysis.calories)
            NutritionAnalysisRow(label = "已摄入碳水", value = analysis.carbs)
            NutritionAnalysisRow(label = "已摄入蛋白质", value = analysis.protein)
            NutritionAnalysisRow(label = "已摄入脂肪", value = analysis.fat)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "寤鸿", fontWeight = FontWeight.Bold)
                Text(text = analysis.advice.ifBlank { "--" }, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun NutritionAnalysisRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = label, fontWeight = FontWeight.Bold)
        Text(text = value.ifBlank { "--" }, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SwipeActionRow(
    editable: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    if (!editable) {
        content()
        return
    }

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val revealWidth = with(density) { 168.dp.toPx() }
    var offsetX by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                offsetX = 0f
                onEdit()
            }) {
                Text("修改")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                offsetX = 0f
                onDelete()
            }) {
                Text("删除")
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val target = if (offsetX < -revealWidth / 2f) -revealWidth else 0f
                            if (target != offsetX) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            offsetX = target
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-revealWidth, 0f)
                        }
                    )
                }
                .hapticClickable {
                    if (offsetX < 0f) {
                        offsetX = 0f
                    }
                }
        ) {
            content()
        }
    }
}

@Composable
private fun NumberField(
    modifier: Modifier,
    value: String,
    label: String,
    editable: Boolean,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        readOnly = !editable,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoCard(
    entry: FitnessEntry,
    selectedAngle: BodyPhotoAngle,
    comparisonDateOptions: List<String>,
    selectedComparisonDate: String,
    comparisonResult: PhotoComparisonResult?,
    editable: Boolean,
    isComparingPhotos: Boolean,
    onAngleChange: (BodyPhotoAngle) -> Unit,
    onComparisonDateChange: (String) -> Unit,
    onComparePhotos: () -> Unit,
    onDeletePhoto: () -> Unit,
    onPickPhoto: (BodyPhotoAngle) -> Unit
) {
    var actionDialogOpen by remember { mutableStateOf(false) }
    var previewOpen by remember { mutableStateOf(false) }
    var dragTotal by remember { mutableStateOf(0f) }
    val haptics = LocalHapticFeedback.current
    val photoPath = entry.photoPathFor(selectedAngle)

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .pointerInput(selectedAngle) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onHorizontalDrag = { _, dragAmount -> dragTotal += dragAmount },
                            onDragEnd = {
                                if (dragTotal > 80f) {
                                    selectedAngle.previousAngle()?.let {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAngleChange(it)
                                    }
                                } else if (dragTotal < -80f) {
                                    selectedAngle.nextAngle()?.let {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAngleChange(it)
                                    }
                                }
                                dragTotal = 0f
                            }
                        )
                    }
                    .combinedClickable(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (photoPath.isNullOrBlank()) {
                                if (editable) onPickPhoto(selectedAngle)
                            } else {
                                previewOpen = true
                            }
                        },
                        onLongClick = {
                            if (editable && !photoPath.isNullOrBlank()) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                actionDialogOpen = true
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!photoPath.isNullOrBlank()) {
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        painter = rememberAsyncImagePainter(File(photoPath)),
                        contentDescription = "身材照片",
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Text(
                text = selectedAngle.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (editable) {
                PhotoComparisonControls(
                    comparisonDateOptions = comparisonDateOptions,
                    selectedComparisonDate = selectedComparisonDate,
                    isComparingPhotos = isComparingPhotos,
                    hasCurrentPhoto = !photoPath.isNullOrBlank(),
                    onComparisonDateChange = onComparisonDateChange,
                    onComparePhotos = onComparePhotos
                )
            }
            comparisonResult
                ?.takeIf { it.currentDate == entry.date && it.previousDate == selectedComparisonDate && it.angle == selectedAngle }
                ?.let { result ->
                PhotoComparisonResultCard(result = result)
            }
        }
    }
    if (actionDialogOpen) {
        PhotoActionDialog(
            angle = selectedAngle,
            onDismiss = { actionDialogOpen = false },
            onReplace = {
                actionDialogOpen = false
                onPickPhoto(selectedAngle)
            },
            onDelete = {
                actionDialogOpen = false
                onDeletePhoto()
            }
        )
    }
    if (previewOpen && !photoPath.isNullOrBlank()) {
        Dialog(
            onDismissRequest = { previewOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ZoomableFullscreenBox(onDismiss = { previewOpen = false }) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = rememberAsyncImagePainter(File(photoPath)),
                    contentDescription = "全屏身材照片",
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

private fun BodyPhotoAngle.nextAngle(): BodyPhotoAngle? {
    val index = BodyPhotoAngle.entries.indexOf(this)
    return BodyPhotoAngle.entries.getOrNull(index + 1)
}

private fun BodyPhotoAngle.previousAngle(): BodyPhotoAngle? {
    val index = BodyPhotoAngle.entries.indexOf(this)
    return BodyPhotoAngle.entries.getOrNull(index - 1)
}

@Composable
private fun PhotoAngleDialog(
    entry: FitnessEntry,
    title: String,
    onDismiss: () -> Unit,
    onSelect: (BodyPhotoAngle) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                BodyPhotoAngle.entries.forEach { angle ->
                    SettingsMenuItem(
                        title = angle.label,
                        subtitle = if (entry.photoPathFor(angle).isNullOrBlank()) "未上传" else "已上传，选择后可更换",
                        onClick = { onSelect(angle) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoActionDialog(
    angle: BodyPhotoAngle,
    onDismiss: () -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = angle.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "选择要对这张照片执行的操作。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onReplace) {
                        Text("更换")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onDelete) {
                        Text("删除")
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoComparisonControls(
    comparisonDateOptions: List<String>,
    selectedComparisonDate: String,
    isComparingPhotos: Boolean,
    hasCurrentPhoto: Boolean,
    onComparisonDateChange: (String) -> Unit,
    onComparePhotos: () -> Unit
) {
    val selectableDateList: List<LocalDate> = remember(comparisonDateOptions) {
        comparisonDateOptions
            .distinct()
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .sorted()
    }
    val initialDate = remember(selectableDateList, selectedComparisonDate) {
        runCatching { LocalDate.parse(selectedComparisonDate) }.getOrNull()
            ?.takeIf { it in selectableDateList }
            ?: selectableDateList.lastOrNull()
    }
    var selectedYear by remember(initialDate) { mutableStateOf(initialDate?.year) }
    var selectedMonth by remember(initialDate) { mutableStateOf(initialDate?.monthValue) }
    var selectedDay by remember(initialDate) { mutableStateOf(initialDate?.dayOfMonth) }
    val yearOptions = selectableDateList.map { it.year }.distinct().sortedDescending()
    if (selectedYear == null || selectedYear !in yearOptions) {
        selectedYear = yearOptions.firstOrNull()
    }
    val monthOptions = selectableDateList
        .filter { it.year == selectedYear }
        .map { it.monthValue }
        .distinct()
        .sorted()
    if (selectedMonth == null || selectedMonth !in monthOptions) {
        selectedMonth = monthOptions.firstOrNull()
    }
    val dayOptions = selectableDateList
        .filter { it.year == selectedYear && it.monthValue == selectedMonth }
        .map { it.dayOfMonth }
        .distinct()
        .sorted()
    if (selectedDay == null || selectedDay !in dayOptions) {
        selectedDay = dayOptions.firstOrNull()
    }
    fun emitSelectedDate(year: Int? = selectedYear, month: Int? = selectedMonth, day: Int? = selectedDay) {
        val targetYear = year ?: return
        val targetMonth = month ?: return
        val targetDay = day ?: return
        val date = runCatching { LocalDate.of(targetYear, targetMonth, targetDay) }.getOrNull() ?: return
        if (date in selectableDateList) {
            onComparisonDateChange(date.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "往期对比", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(
                    text = "选择同角度历史照片，与当前照片并排分析。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (comparisonDateOptions.isNotEmpty()) {
                RoundIconAction(
                    icon = Icons.Filled.AutoAwesome,
                    contentDescription = "AI 对比",
                    modifier = Modifier.width(48.dp).height(48.dp),
                    enabled = !isComparingPhotos && hasCurrentPhoto && selectedComparisonDate.isNotBlank(),
                    onClick = onComparePhotos
                )
            }
        }
        if (comparisonDateOptions.isEmpty()) {
            EmptyState(
                title = "暂无可对比照片",
                subtitle = "有往期同角度照片后，这里会显示日期选择和 AI 对比入口。"
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SimpleDropdown(
                    modifier = Modifier.weight(1f),
                    label = selectedYear?.let { "${it}年" } ?: "年",
                    options = yearOptions.map { it.toString() },
                    onSelected = {
                        val year = it.toIntOrNull()
                        selectedYear = year
                        val nextMonthOptions = selectableDateList
                            .filter { date -> date.year == year }
                            .map { date -> date.monthValue }
                            .distinct()
                            .sorted()
                        val month = nextMonthOptions.firstOrNull()
                        selectedMonth = month
                        val nextDayOptions = selectableDateList
                            .filter { date -> date.year == year && date.monthValue == month }
                            .map { date -> date.dayOfMonth }
                            .distinct()
                            .sorted()
                        val day = nextDayOptions.firstOrNull()
                        selectedDay = day
                        emitSelectedDate(year, month, day)
                    }
                )
                SimpleDropdown(
                    modifier = Modifier.weight(1f),
                    label = selectedMonth?.let { "${it}月" } ?: "月",
                    options = monthOptions.map { it.toString() },
                    onSelected = {
                        val month = it.toIntOrNull()
                        selectedMonth = month
                        val nextDayOptions = selectableDateList
                            .filter { date -> date.year == selectedYear && date.monthValue == month }
                            .map { date -> date.dayOfMonth }
                            .distinct()
                            .sorted()
                        val day = nextDayOptions.firstOrNull()
                        selectedDay = day
                        emitSelectedDate(month = month, day = day)
                    }
                )
                SimpleDropdown(
                    modifier = Modifier.weight(1f),
                    label = selectedDay?.let { "${it}日" } ?: "日",
                    options = dayOptions.map { it.toString() },
                    onSelected = {
                        val day = it.toIntOrNull()
                        selectedDay = day
                        emitSelectedDate(day = day)
                    }
                )
            }
            if (isComparingPhotos) {
                TextPill(text = "AI 对比中")
            }
        }
    }
}

@Composable
private fun SettingsDrawer(
    open: Boolean,
    apiKey: String,
    selectedDate: String,
    profileName: String,
    profileBirthday: String,
    profileGender: String,
    profileSignature: String,
    profileAvatarPath: String?,
    homeBackgroundPath: String?,
    welcomeMessage: String,
    profileAge: String,
    themeKey: String,
    recordedDates: List<String>,
    trainingTemplates: List<TrainingTemplate>,
    trainingTemplateGroups: List<TrainingTemplateGroup>,
    mealTemplates: List<MealTemplate>,
    mealTemplateGroups: List<MealTemplateGroup>,
    canEditSelectedDate: Boolean,
    onApiKeyChange: (String) -> Unit,
    onSaveApiKey: () -> Unit,
    onProfileNameChange: (String) -> Unit,
    onProfileBirthdayChange: (String) -> Unit,
    onProfileGenderChange: (String) -> Unit,
    onProfileSignatureChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onPickAvatar: () -> Unit,
    onWelcomeMessageChange: (String) -> Unit,
    onPickHomeBackground: () -> Unit,
    onClearHomeBackground: () -> Unit,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onTodayDate: () -> Unit,
    onDateChange: (String) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onSaveTrainingTemplate: (String, String, TrainingItem) -> Unit,
    onUpdateTrainingTemplate: (TrainingTemplate) -> Unit,
    onDeleteTrainingTemplate: (String) -> Unit,
    onSaveTrainingTemplateGroup: (String, String, List<TrainingItem>) -> Unit,
    onUpdateTrainingTemplateGroup: (TrainingTemplateGroup) -> Unit,
    onDeleteTrainingTemplateGroup: (String) -> Unit,
    onSaveMealTemplate: (String, String, MealItem) -> Unit,
    onUpdateMealTemplate: (MealTemplate) -> Unit,
    onDeleteMealTemplate: (String) -> Unit,
    onSaveMealTemplateGroup: (String, String, List<MealItem>) -> Unit,
    onUpdateMealTemplateGroup: (MealTemplateGroup) -> Unit,
    onDeleteMealTemplateGroup: (String) -> Unit,
    onClose: () -> Unit
) {
    var activeDialog by remember { mutableStateOf<SettingsDialog?>(null) }

    if (open) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .hapticClickable(onClick = onClose)
        )
    }
    AnimatedVisibility(
        visible = open,
        enter = slideInHorizontally(animationSpec = tween(220), initialOffsetX = { it }),
        exit = slideOutHorizontally(animationSpec = tween(180), targetOffsetX = { it })
    ) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End) {
            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(340.dp)
                    .hapticClickable(onClick = {}),
                shape = RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "设置",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onClose) {
                            Text(text = "脳", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    SettingsMenuItem(title = "个人资料", subtitle = "头像、姓名、生日、性别", onClick = { activeDialog = SettingsDialog.Profile })
                    SettingsMenuItem(title = "个性化", subtitle = "主题颜色、主页背景、欢迎标语", onClick = { activeDialog = SettingsDialog.Personalization })
                    SettingsMenuItem(title = "API 设置", subtitle = if (apiKey.isBlank()) "未填写 MiMo API Key" else "已保存 API Key", onClick = { activeDialog = SettingsDialog.Api })
                    SettingsMenuItem(title = "日期选择", subtitle = selectedDate, onClick = { activeDialog = SettingsDialog.Date })
                    SettingsMenuItem(title = "数据模板", subtitle = "训练模板、食物模板", onClick = { activeDialog = SettingsDialog.Templates })
                    SettingsMenuItem(title = "记录管理", subtitle = "删除某一天的完整记录", onClick = { activeDialog = SettingsDialog.Records })
                }
            }
        }
    }
    activeDialog?.let { dialog ->
        SettingsCenterDialog(
            title = when (dialog) {
                SettingsDialog.Profile -> "个人资料"
                SettingsDialog.Personalization -> "个性化"
                SettingsDialog.Api -> "API 设置"
                SettingsDialog.Date -> "日期选择"
                SettingsDialog.Records -> "记录管理"
                SettingsDialog.Templates -> "数据模板"
            },
            subtitle = when (dialog) {
                SettingsDialog.Profile -> "用于 AI 推荐和欢迎页显示"
                SettingsDialog.Personalization -> "调整主题颜色、主页背景和欢迎页标语"
                SettingsDialog.Api -> "保存你自己的 MiMo API Key"
                SettingsDialog.Date -> "查看过去记录，未来日期不可选择"
                SettingsDialog.Records -> "删除某一天的身体、训练、饮食、照片和 AI 信息"
                SettingsDialog.Templates -> "管理常用训练和食物记录"
            },
            onDismiss = { activeDialog = null }
        ) {
            when (dialog) {
                SettingsDialog.Profile -> ProfileEditor(
                    name = profileName,
                    birthday = profileBirthday,
                    gender = profileGender,
                    signature = profileSignature,
                    avatarPath = profileAvatarPath,
                    age = profileAge,
                    onPickAvatar = onPickAvatar,
                    onNameChange = onProfileNameChange,
                    onBirthdayChange = onProfileBirthdayChange,
                    onGenderChange = onProfileGenderChange,
                    onSignatureChange = onProfileSignatureChange
                )
                SettingsDialog.Personalization -> PersonalizationSettings(
                    themeKey = themeKey,
                    homeBackgroundPath = homeBackgroundPath,
                    welcomeMessage = welcomeMessage,
                    onThemeChange = onThemeChange,
                    onWelcomeMessageChange = onWelcomeMessageChange,
                    onPickHomeBackground = onPickHomeBackground,
                    onClearHomeBackground = onClearHomeBackground
                )
                SettingsDialog.Api -> ApiSettingsPanel(
                    apiKey = apiKey,
                    onApiKeyChange = onApiKeyChange,
                    onSaveApiKey = onSaveApiKey
                )
                SettingsDialog.Date -> DateCard(
                    date = selectedDate,
                    recordedDates = recordedDates,
                    onPrevious = onPreviousDate,
                    onNext = onNextDate,
                    onToday = onTodayDate,
                    onDateChange = onDateChange
                )
                SettingsDialog.Records -> RecordManagementPanel(
                    selectedDate = selectedDate,
                    recordedDates = recordedDates,
                    canEditSelectedDate = canEditSelectedDate,
                    onDeleteRecord = { date ->
                        onDeleteRecord(date)
                        activeDialog = null
                    }
                )
                SettingsDialog.Templates -> DataTemplatesPanel(
                    trainingTemplates = trainingTemplates,
                    trainingTemplateGroups = trainingTemplateGroups,
                    mealTemplates = mealTemplates,
                    mealTemplateGroups = mealTemplateGroups,
                    onSaveTrainingTemplate = onSaveTrainingTemplate,
                    onUpdateTrainingTemplate = onUpdateTrainingTemplate,
                    onDeleteTrainingTemplate = onDeleteTrainingTemplate,
                    onSaveTrainingTemplateGroup = onSaveTrainingTemplateGroup,
                    onUpdateTrainingTemplateGroup = onUpdateTrainingTemplateGroup,
                    onDeleteTrainingTemplateGroup = onDeleteTrainingTemplateGroup,
                    onSaveMealTemplate = onSaveMealTemplate,
                    onUpdateMealTemplate = onUpdateMealTemplate,
                    onDeleteMealTemplate = onDeleteMealTemplate,
                    onSaveMealTemplateGroup = onSaveMealTemplateGroup,
                    onUpdateMealTemplateGroup = onUpdateMealTemplateGroup,
                    onDeleteMealTemplateGroup = onDeleteMealTemplateGroup
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.hapticClickable(onClick = onClick) else Modifier)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.62f else 0.32f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.48f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.58f)
            )
        }
        Text(
            text = "‹",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.35f)
        )
    }
}

@Composable
private fun RecordManagementPanel(
    selectedDate: String,
    recordedDates: List<String>,
    canEditSelectedDate: Boolean,
    onDeleteRecord: (String) -> Unit
) {
    val today = LocalDate.now().toString()
    val selectableDateList = remember(recordedDates) {
        recordedDates
            .distinct()
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .sorted()
    }
    val initialDate = remember(selectableDateList, selectedDate) {
        runCatching { LocalDate.parse(selectedDate) }.getOrNull()
            ?.takeIf { it in selectableDateList }
            ?: runCatching { LocalDate.parse(today) }.getOrNull()?.takeIf { it in selectableDateList }
            ?: selectableDateList.lastOrNull()
    }
    var selectedYear by remember(initialDate) { mutableStateOf(initialDate?.year) }
    var selectedMonth by remember(initialDate) { mutableStateOf(initialDate?.monthValue) }
    var selectedDay by remember(initialDate) { mutableStateOf(initialDate?.dayOfMonth) }
    var targetDate by remember(initialDate) { mutableStateOf(initialDate?.toString().orEmpty()) }
    var confirming by remember { mutableStateOf(false) }
    val yearOptions = selectableDateList.map { it.year }.distinct().sortedDescending()
    if (selectedYear == null || selectedYear !in yearOptions) {
        selectedYear = yearOptions.firstOrNull()
    }
    val monthOptions = selectableDateList
        .filter { it.year == selectedYear }
        .map { it.monthValue }
        .distinct()
        .sorted()
    if (selectedMonth == null || selectedMonth !in monthOptions) {
        selectedMonth = monthOptions.firstOrNull()
    }
    val dayOptions = selectableDateList
        .filter { it.year == selectedYear && it.monthValue == selectedMonth }
        .map { it.dayOfMonth }
        .distinct()
        .sorted()
    if (selectedDay == null || selectedDay !in dayOptions) {
        selectedDay = dayOptions.firstOrNull()
    }
    fun updateTargetDate(year: Int? = selectedYear, month: Int? = selectedMonth, day: Int? = selectedDay) {
        val targetYear = year ?: return
        val targetMonth = month ?: return
        val targetDay = day ?: return
        val date = runCatching { LocalDate.of(targetYear, targetMonth, targetDay) }.getOrNull() ?: return
        if (date in selectableDateList) {
            targetDate = date.toString()
            confirming = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsPanel(
            title = "记录管理",
            subtitle = "删除某一天的完整记录，包括用户数据、照片和 AI 分析"
        ) {
            if (selectableDateList.isEmpty()) {
                Text(
                    text = "当前还没有可以删除的记录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@SettingsPanel
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SimpleDropdown(
                    modifier = Modifier.weight(1f),
                    label = selectedYear?.let { "${it}年" } ?: "年",
                    options = yearOptions.map { it.toString() },
                    onSelected = {
                        val year = it.toIntOrNull()
                        selectedYear = year
                        val nextMonthOptions = selectableDateList
                            .filter { date -> date.year == year }
                            .map { date -> date.monthValue }
                            .distinct()
                            .sorted()
                        val month = nextMonthOptions.firstOrNull()
                        selectedMonth = month
                        val nextDayOptions = selectableDateList
                            .filter { date -> date.year == year && date.monthValue == month }
                            .map { date -> date.dayOfMonth }
                            .distinct()
                            .sorted()
                        val day = nextDayOptions.firstOrNull()
                        selectedDay = day
                        updateTargetDate(year, month, day)
                    }
                )
                SimpleDropdown(
                    modifier = Modifier.weight(1f),
                    label = selectedMonth?.let { "${it}月" } ?: "月",
                    options = monthOptions.map { it.toString() },
                    onSelected = {
                        val month = it.toIntOrNull()
                        selectedMonth = month
                        val nextDayOptions = selectableDateList
                            .filter { date -> date.year == selectedYear && date.monthValue == month }
                            .map { date -> date.dayOfMonth }
                            .distinct()
                            .sorted()
                        val day = nextDayOptions.firstOrNull()
                        selectedDay = day
                        updateTargetDate(month = month, day = day)
                    }
                )
                SimpleDropdown(
                    modifier = Modifier.weight(1f),
                    label = selectedDay?.let { "${it}日" } ?: "日",
                    options = dayOptions.map { it.toString() },
                    onSelected = {
                        val day = it.toIntOrNull()
                        selectedDay = day
                        updateTargetDate(day = day)
                    }
                )
            }
            Text(
                text = "已选择日期：${targetDate.ifBlank { "--" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (targetDate == selectedDate && !canEditSelectedDate) {
                    "删除后会回到今天；历史页面本身仍然只允许查看。"
                } else {
                    "删除后无法从软件内恢复。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    enabled = targetDate.isNotBlank(),
                    onClick = { confirming = true }
                ) {
                    Text("删除这一天记录")
                }
            }
        }
    }

    if (confirming) {
        Dialog(onDismissRequest = { confirming = false }) {
            PopDialogSurface {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "确认删除 $targetDate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "这一天的用户数据、照片文件和 AI 分析都会被删除。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = { onDeleteRecord(targetDate) }) {
                            Text("确认删除")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DataTemplatesPanel(
    trainingTemplates: List<TrainingTemplate>,
    trainingTemplateGroups: List<TrainingTemplateGroup>,
    mealTemplates: List<MealTemplate>,
    mealTemplateGroups: List<MealTemplateGroup>,
    onSaveTrainingTemplate: (String, String, TrainingItem) -> Unit,
    onUpdateTrainingTemplate: (TrainingTemplate) -> Unit,
    onDeleteTrainingTemplate: (String) -> Unit,
    onSaveTrainingTemplateGroup: (String, String, List<TrainingItem>) -> Unit,
    onUpdateTrainingTemplateGroup: (TrainingTemplateGroup) -> Unit,
    onDeleteTrainingTemplateGroup: (String) -> Unit,
    onSaveMealTemplate: (String, String, MealItem) -> Unit,
    onUpdateMealTemplate: (MealTemplate) -> Unit,
    onDeleteMealTemplate: (String) -> Unit,
    onSaveMealTemplateGroup: (String, String, List<MealItem>) -> Unit,
    onUpdateMealTemplateGroup: (MealTemplateGroup) -> Unit,
    onDeleteMealTemplateGroup: (String) -> Unit
) {
    var editingTraining by remember { mutableStateOf<TrainingTemplate?>(null) }
    var editingTrainingGroup by remember { mutableStateOf<TrainingTemplateGroup?>(null) }
    var editingMeal by remember { mutableStateOf<MealTemplate?>(null) }
    var editingMealGroup by remember { mutableStateOf<MealTemplateGroup?>(null) }
    var route by remember { mutableStateOf<TemplateRoute>(TemplateRoute.Home) }
    val trainingSingleCategories = trainingTemplates.map { it.category.normalizedCategory() }.distinct().sorted()
    val trainingGroupCategories = trainingTemplateGroups.map { it.category.normalizedCategory() }.distinct().sorted()
    val mealSingleCategories = mealTemplates.map { it.category.normalizedCategory() }.distinct().sorted()
    val mealGroupCategories = mealTemplateGroups.map { it.category.normalizedCategory() }.distinct().sorted()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val haptics = LocalHapticFeedback.current
        var routeDragTotal by remember(route) { mutableStateOf(0f) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(route) {
                    detectHorizontalDragGestures(
                        onDragStart = { routeDragTotal = 0f },
                        onHorizontalDrag = { _, dragAmount -> routeDragTotal += dragAmount },
                        onDragEnd = {
                            if (routeDragTotal > 96f && route !is TemplateRoute.Home) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                route = route.parent()
                            }
                            routeDragTotal = 0f
                        }
                    )
                }
        ) {
            AnimatedContent(
                targetState = route,
                transitionSpec = {
                    val forward = targetState.depth() >= initialState.depth()
                    val enterOffset: (Int) -> Int = { width -> if (forward) width else -width }
                    val exitOffset: (Int) -> Int = { width -> if (forward) -width / 3 else width / 3 }
                    (slideInHorizontally(animationSpec = tween(240), initialOffsetX = enterOffset) + fadeIn(animationSpec = tween(150)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(190), targetOffsetX = exitOffset) + fadeOut(animationSpec = tween(120)))
                },
                label = "TemplateRouteContent"
            ) { current ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 74.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (current) {
                    TemplateRoute.Home -> {
                        SettingsMenuItem(
                            title = "训练模板",
                            subtitle = "单项 ${trainingTemplates.size} 个，集合 ${trainingTemplateGroups.size} 个",
                            onClick = { route = TemplateRoute.TrainingHome }
                        )
                        SettingsMenuItem(
                            title = "食物模板",
                            subtitle = "单项 ${mealTemplates.size} 个，集合 ${mealTemplateGroups.size} 个",
                            onClick = { route = TemplateRoute.MealHome }
                        )
                    }
                    TemplateRoute.TrainingHome -> {
                        SettingsMenuItem(
                            title = "单项模板",
                            subtitle = "${trainingSingleCategories.size} 个分类",
                            onClick = { route = TemplateRoute.TrainingSingleCategories }
                        )
                        SettingsMenuItem(
                            title = "集合模板",
                            subtitle = "${trainingGroupCategories.size} 个分类",
                            onClick = { route = TemplateRoute.TrainingGroupCategories }
                        )
                    }
                    TemplateRoute.TrainingSingleCategories -> {
                        TemplateCategoryList(
                            categories = trainingSingleCategories,
                            emptyText = "还没有训练单项模板，点右下角 + 添加后会自动生成分类。",
                            onCategoryClick = { route = TemplateRoute.TrainingSingleDetail(it) }
                        )
                    }
                    TemplateRoute.TrainingGroupCategories -> {
                        TemplateCategoryList(
                            categories = trainingGroupCategories,
                            emptyText = "还没有训练集合模板，点右下角 + 添加后会自动生成分类。",
                            onCategoryClick = { route = TemplateRoute.TrainingGroupDetail(it) }
                        )
                    }
                    TemplateRoute.MealHome -> {
                        SettingsMenuItem(
                            title = "单项模板",
                            subtitle = "${mealSingleCategories.size} 个分类",
                            onClick = { route = TemplateRoute.MealSingleCategories }
                        )
                        SettingsMenuItem(
                            title = "集合模板",
                            subtitle = "${mealGroupCategories.size} 个分类",
                            onClick = { route = TemplateRoute.MealGroupCategories }
                        )
                    }
                    TemplateRoute.MealSingleCategories -> {
                        TemplateCategoryList(
                            categories = mealSingleCategories,
                            emptyText = "还没有食物单项模板，点右下角 + 添加后会自动生成分类。",
                            onCategoryClick = { route = TemplateRoute.MealSingleDetail(it) }
                        )
                    }
                    TemplateRoute.MealGroupCategories -> {
                        TemplateCategoryList(
                            categories = mealGroupCategories,
                            emptyText = "还没有食物集合模板，点右下角 + 添加后会自动生成分类。",
                            onCategoryClick = { route = TemplateRoute.MealGroupDetail(it) }
                        )
                    }
                    is TemplateRoute.TrainingSingleDetail -> {
                        TemplateInlineDetail(title = "训练单项 - ${current.category}") {
                            val items = trainingTemplates.filter { it.category.normalizedCategory() == current.category }
                            if (items.isEmpty()) Text("这个分类下还没有模板。")
                            items.forEach { template ->
                                TemplateRow(
                                    title = template.item.name.ifBlank { "训练模板" },
                                    subtitle = template.item.summaryLabel(),
                                    onEdit = { editingTraining = template },
                                    onDelete = { onDeleteTrainingTemplate(template.id) }
                                )
                            }
                        }
                    }
                    is TemplateRoute.TrainingGroupDetail -> {
                        TemplateInlineDetail(title = "训练集合 - ${current.category}") {
                            val groups = trainingTemplateGroups.filter { it.category.normalizedCategory() == current.category }
                            if (groups.isEmpty()) Text("这个分类下还没有集合模板。")
                            groups.forEach { group ->
                                TemplateRow(
                                    title = group.templateName.ifBlank { "训练集合模板" },
                                    subtitle = "${group.items.size} 项训练",
                                    onEdit = { editingTrainingGroup = group },
                                    onDelete = { onDeleteTrainingTemplateGroup(group.id) }
                                )
                            }
                        }
                    }
                    is TemplateRoute.MealSingleDetail -> {
                        TemplateInlineDetail(title = "食物单项 - ${current.category}") {
                            val items = mealTemplates.filter { it.category.normalizedCategory() == current.category }
                            if (items.isEmpty()) Text("这个分类下还没有模板。")
                            items.forEach { template ->
                                TemplateRow(
                                    title = template.item.foodName.ifBlank { "食物模板" },
                                    subtitle = "${template.item.foodName.ifBlank { "未命名食物" }}：${template.item.grams.ifBlank { "--" }} g",
                                    onEdit = { editingMeal = template },
                                    onDelete = { onDeleteMealTemplate(template.id) }
                                )
                            }
                        }
                    }
                    is TemplateRoute.MealGroupDetail -> {
                        TemplateInlineDetail(title = "食物集合 - ${current.category}") {
                            val groups = mealTemplateGroups.filter { it.category.normalizedCategory() == current.category }
                            if (groups.isEmpty()) Text("这个分类下还没有集合模板。")
                            groups.forEach { group ->
                                TemplateRow(
                                    title = group.templateName.ifBlank { "食物集合模板" },
                                    subtitle = "${group.items.size} 项食物",
                                    onEdit = { editingMealGroup = group },
                                    onDelete = { onDeleteMealTemplateGroup(group.id) }
                                )
                            }
                        }
                    }
                    }
                }
            }
            route.addAction()?.let { action ->
                FloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .width(54.dp)
                        .height(54.dp),
                    onClick = {
                        when (action) {
                            TemplateAddAction.TrainingSingle -> editingTraining = TrainingTemplate(id = "", item = TrainingItem(id = ""))
                            TemplateAddAction.TrainingGroup -> editingTrainingGroup = TrainingTemplateGroup(id = "")
                            TemplateAddAction.Meal -> editingMeal = MealTemplate(id = "", item = MealItem(id = ""))
                            TemplateAddAction.MealGroup -> editingMealGroup = MealTemplateGroup(id = "")
                            is TemplateAddAction.TrainingSingleInCategory -> editingTraining = TrainingTemplate(id = "", category = action.category.toStoredCategory(), item = TrainingItem(id = ""))
                            is TemplateAddAction.TrainingGroupInCategory -> editingTrainingGroup = TrainingTemplateGroup(id = "", category = action.category.toStoredCategory())
                            is TemplateAddAction.MealInCategory -> editingMeal = MealTemplate(id = "", category = action.category.toStoredCategory(), item = MealItem(id = ""))
                            is TemplateAddAction.MealGroupInCategory -> editingMealGroup = MealTemplateGroup(id = "", category = action.category.toStoredCategory())
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "新增模板")
                }
            }
        }
    }

    editingTraining?.let { template ->
        TrainingTemplateEditorDialog(
            template = template,
            onDismiss = { editingTraining = null },
            onDone = { name, category, item ->
                if (template.id.isBlank()) onSaveTrainingTemplate(name, category, item)
                else onUpdateTrainingTemplate(template.copy(templateName = name, category = category, item = item))
                editingTraining = null
            }
        )
    }
    editingTrainingGroup?.let { group ->
        TrainingGroupTemplateEditorDialog(
            group = group,
            singleTemplates = trainingTemplates,
            onDismiss = { editingTrainingGroup = null },
            onDone = { name, category, items ->
                if (group.id.isBlank()) onSaveTrainingTemplateGroup(name, category, items)
                else onUpdateTrainingTemplateGroup(group.copy(templateName = name, category = category, items = items))
                editingTrainingGroup = null
            }
        )
    }
    editingMeal?.let { template ->
        MealTemplateEditorDialog(
            template = template,
            onDismiss = { editingMeal = null },
            onDone = { name, category, item ->
                if (template.id.isBlank()) onSaveMealTemplate(name, category, item)
                else onUpdateMealTemplate(template.copy(templateName = name, category = category, item = item))
                editingMeal = null
            }
        )
    }
    editingMealGroup?.let { group ->
        MealGroupTemplateEditorDialog(
            group = group,
            singleTemplates = mealTemplates,
            onDismiss = { editingMealGroup = null },
            onDone = { name, category, items ->
                if (group.id.isBlank()) onSaveMealTemplateGroup(name, category, items)
                else onUpdateMealTemplateGroup(group.copy(templateName = name, category = category, items = items))
                editingMealGroup = null
            }
        )
    }
}

private sealed class TemplateRoute {
    data object Home : TemplateRoute()
    data object TrainingHome : TemplateRoute()
    data object TrainingSingleCategories : TemplateRoute()
    data object TrainingGroupCategories : TemplateRoute()
    data object MealHome : TemplateRoute()
    data object MealSingleCategories : TemplateRoute()
    data object MealGroupCategories : TemplateRoute()
    data class TrainingSingleDetail(val category: String) : TemplateRoute()
    data class TrainingGroupDetail(val category: String) : TemplateRoute()
    data class MealSingleDetail(val category: String) : TemplateRoute()
    data class MealGroupDetail(val category: String) : TemplateRoute()
}

private sealed class TemplateAddAction {
    data object TrainingSingle : TemplateAddAction()
    data object TrainingGroup : TemplateAddAction()
    data object Meal : TemplateAddAction()
    data object MealGroup : TemplateAddAction()
    data class TrainingSingleInCategory(val category: String) : TemplateAddAction()
    data class TrainingGroupInCategory(val category: String) : TemplateAddAction()
    data class MealInCategory(val category: String) : TemplateAddAction()
    data class MealGroupInCategory(val category: String) : TemplateAddAction()
}

private fun TemplateRoute.addAction(): TemplateAddAction? {
    return when (this) {
        TemplateRoute.Home -> null
        TemplateRoute.TrainingHome -> null
        TemplateRoute.MealHome -> null
        TemplateRoute.TrainingSingleCategories -> TemplateAddAction.TrainingSingle
        TemplateRoute.TrainingGroupCategories -> TemplateAddAction.TrainingGroup
        TemplateRoute.MealSingleCategories -> TemplateAddAction.Meal
        TemplateRoute.MealGroupCategories -> TemplateAddAction.MealGroup
        is TemplateRoute.TrainingSingleDetail -> TemplateAddAction.TrainingSingleInCategory(category)
        is TemplateRoute.TrainingGroupDetail -> TemplateAddAction.TrainingGroupInCategory(category)
        is TemplateRoute.MealSingleDetail -> TemplateAddAction.MealInCategory(category)
        is TemplateRoute.MealGroupDetail -> TemplateAddAction.MealGroupInCategory(category)
    }
}

private fun TemplateRoute.depth(): Int {
    return when (this) {
        TemplateRoute.Home -> 0
        TemplateRoute.TrainingHome,
        TemplateRoute.MealHome -> 1
        TemplateRoute.TrainingSingleCategories,
        TemplateRoute.TrainingGroupCategories,
        TemplateRoute.MealSingleCategories,
        TemplateRoute.MealGroupCategories -> 2
        is TemplateRoute.TrainingSingleDetail,
        is TemplateRoute.TrainingGroupDetail,
        is TemplateRoute.MealSingleDetail,
        is TemplateRoute.MealGroupDetail -> 3
    }
}

private fun TemplateRoute.parent(): TemplateRoute {
    return when (this) {
        TemplateRoute.Home -> TemplateRoute.Home
        TemplateRoute.TrainingHome -> TemplateRoute.Home
        TemplateRoute.TrainingSingleCategories -> TemplateRoute.TrainingHome
        TemplateRoute.TrainingGroupCategories -> TemplateRoute.TrainingHome
        TemplateRoute.MealHome -> TemplateRoute.Home
        TemplateRoute.MealSingleCategories -> TemplateRoute.MealHome
        TemplateRoute.MealGroupCategories -> TemplateRoute.MealHome
        is TemplateRoute.TrainingSingleDetail -> TemplateRoute.TrainingSingleCategories
        is TemplateRoute.TrainingGroupDetail -> TemplateRoute.TrainingGroupCategories
        is TemplateRoute.MealSingleDetail -> TemplateRoute.MealSingleCategories
        is TemplateRoute.MealGroupDetail -> TemplateRoute.MealGroupCategories
    }
}

private fun String.normalizedCategory(): String = trim().ifBlank { "未分类" }

private fun String.toStoredCategory(): String = if (this == "未分类") "" else this

private fun ImportRoute.importDepth(): Int {
    return when (this) {
        ImportRoute.Home -> 0
        ImportRoute.SingleCategories,
        ImportRoute.GroupCategories -> 1
        is ImportRoute.SingleDetail,
        is ImportRoute.GroupDetail -> 2
    }
}

@Composable
private fun ImportCategoryList(
    categories: List<String>,
    emptyText: String,
    onCategoryClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (categories.isEmpty()) {
            Text(text = emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        categories.forEach { category ->
            SettingsMenuItem(
                title = category,
                subtitle = "查看该分类下的模板",
                onClick = { onCategoryClick(category) }
            )
        }
    }
}

@Composable
private fun TemplateCategoryList(
    categories: List<String>,
    emptyText: String,
    onCategoryClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (categories.isEmpty()) {
            Text(text = emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        categories.forEach { category ->
            TemplateCategoryRow(
                title = category,
                subtitle = "查看该分类下的模板",
                onClick = { onCategoryClick(category) }
            )
        }
    }
}

@Composable
private fun TemplateInlineDetail(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun TemplateCategoryRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .hapticClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = "›", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TemplateRow(
    title: String,
    subtitle: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = onEdit) {
                Text("修改")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onDelete) {
                Text("删除")
            }
        }
    }
}

@Composable
private fun TrainingTemplateEditorDialog(
    template: TrainingTemplate,
    onDismiss: () -> Unit,
    onDone: (String, String, TrainingItem) -> Unit
) {
    var category by remember(template) { mutableStateOf(template.category) }
    var draft by remember(template) {
        mutableStateOf(
            template.item.copy(
                id = "template_draft",
                type = template.item.type
            )
        )
    }
    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (template.id.isBlank()) "新增训练模板" else "修改训练模板",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类") },
                    singleLine = true
                )
                FormPanel(title = "训练类型") {
                    SimpleDropdown(
                        modifier = Modifier.fillMaxWidth(),
                        label = draft.type.label,
                        options = TrainingType.entries.map { it.label },
                        onSelected = { label ->
                            val type = TrainingType.entries.firstOrNull { it.label == label } ?: TrainingType.Strength
                            draft = draft.copy(type = type)
                        }
                    )
                }
                TrainingDraftEditor(
                    draft = draft,
                    onDraftChange = { draft = it },
                    onDone = { onDone(draft.name.ifBlank { draft.type.label }, category, draft) },
                    onCancel = onDismiss,
                    showPeriod = false
                )
            }
        }
    }
}

@Composable
private fun TrainingGroupTemplateEditorDialog(
    group: TrainingTemplateGroup,
    singleTemplates: List<TrainingTemplate>,
    onDismiss: () -> Unit,
    onDone: (String, String, List<TrainingItem>) -> Unit
) {
    var name by remember(group) { mutableStateOf(group.templateName) }
    var category by remember(group) { mutableStateOf(group.category) }
    var selectedIds by remember(group, singleTemplates) {
        mutableStateOf(
            singleTemplates.filter { template ->
                group.items.any { item -> item.summaryLabel() == template.item.summaryLabel() }
            }.map { it.id }
        )
    }
    var selectedTemplateLabel by remember(singleTemplates) { mutableStateOf("") }
    var selectedSingleCategory by remember(singleTemplates) { mutableStateOf("") }
    val singleCategories = singleTemplates.map { it.category.normalizedCategory() }.distinct().sorted()
    val visibleTemplates = if (selectedSingleCategory.isBlank()) emptyList() else singleTemplates.filter { it.category.normalizedCategory() == selectedSingleCategory }
    val templateOptions = visibleTemplates.mapIndexed { index, template ->
        "${template.item.name.ifBlank { "训练模板" }} #${index + 1}"
    }
    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = if (group.id.isBlank()) "新增训练集合模板" else "修改训练集合模板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("闆嗗悎鍚嶇О") },
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类") },
                    singleLine = true
                )
                SettingsPanel(
                    title = "选择训练单项",
                    subtitle = if (singleTemplates.isEmpty()) "请先创建训练单项模板" else "已选择 ${selectedIds.size} 项"
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.56f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "添加单项", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        SimpleDropdown(
                            modifier = Modifier.fillMaxWidth(),
                            label = selectedSingleCategory.ifBlank { "选择单项分类" },
                            options = singleCategories,
                            onSelected = {
                                selectedSingleCategory = it
                                selectedTemplateLabel = ""
                            }
                        )
                        SimpleDropdown(
                            modifier = Modifier.fillMaxWidth(),
                            label = selectedTemplateLabel.ifBlank { "选择单项名称" },
                            options = templateOptions,
                            onSelected = { selectedTemplateLabel = it }
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                enabled = selectedTemplateLabel.isNotBlank(),
                                onClick = {
                                    val index = templateOptions.indexOf(selectedTemplateLabel)
                                    val template = visibleTemplates.getOrNull(index)
                                    if (template != null && template.id !in selectedIds) {
                                        selectedIds = selectedIds + template.id
                                    }
                                    selectedTemplateLabel = ""
                                }
                            ) {
                                Text("加入")
                            }
                        }
                    }
                    if (selectedIds.isNotEmpty()) {
                        Text(text = "已加入单项", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    selectedIds.mapNotNull { id -> singleTemplates.firstOrNull { it.id == id } }.forEach { template ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = template.item.name.ifBlank { "训练模板" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(text = "${template.category.ifBlank { "未分类" }} - ${template.item.summaryLabel()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { selectedIds = selectedIds - template.id }) {
                                Text("绉婚櫎")
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        enabled = name.isNotBlank() && selectedIds.isNotEmpty(),
                        onClick = {
                            val items = singleTemplates.filter { it.id in selectedIds }.map { it.item.copy(id = "") }
                            onDone(name, category, items)
                        }
                    ) {
                        Text("完成")
                    }
                }
            }
        }
    }
}

@Composable
private fun MealTemplateEditorDialog(
    template: MealTemplate,
    onDismiss: () -> Unit,
    onDone: (String, String, MealItem) -> Unit
) {
    var category by remember(template) { mutableStateOf(template.category) }
    var draft by remember(template) { mutableStateOf(template.item.copy(id = "template_draft")) }
    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = if (template.id.isBlank()) "新增食物模板" else "修改食物模板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类") },
                    singleLine = true
                )
                MealDraftEditor(
                    draft = draft,
                    onDraftChange = { draft = it },
                    onDone = { onDone(draft.foodName.ifBlank { "食物模板" }, category, draft) },
                    onCancel = onDismiss
                )
            }
        }
    }
}

@Composable
private fun MealGroupTemplateEditorDialog(
    group: MealTemplateGroup,
    singleTemplates: List<MealTemplate>,
    onDismiss: () -> Unit,
    onDone: (String, String, List<MealItem>) -> Unit
) {
    var name by remember(group) { mutableStateOf(group.templateName) }
    var category by remember(group) { mutableStateOf(group.category) }
    var selectedIds by remember(group, singleTemplates) {
        mutableStateOf(
            singleTemplates.filter { template ->
                group.items.any { item ->
                    item.foodName == template.item.foodName && item.grams == template.item.grams
                }
            }.map { it.id }
        )
    }
    var selectedTemplateLabel by remember(singleTemplates) { mutableStateOf("") }
    var selectedSingleCategory by remember(singleTemplates) { mutableStateOf("") }
    val singleCategories = singleTemplates.map { it.category.normalizedCategory() }.distinct().sorted()
    val visibleTemplates = if (selectedSingleCategory.isBlank()) emptyList() else singleTemplates.filter { it.category.normalizedCategory() == selectedSingleCategory }
    val templateOptions = visibleTemplates.mapIndexed { index, template ->
        "${template.item.foodName.ifBlank { "食物模板" }} - ${template.item.grams.ifBlank { "--" }} g #${index + 1}"
    }
    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .height(520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = if (group.id.isBlank()) "新增食物集合模板" else "修改食物集合模板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("闆嗗悎鍚嶇О") },
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类") },
                    singleLine = true
                )
                SettingsPanel(
                    title = "选择食物单项",
                    subtitle = if (singleTemplates.isEmpty()) "请先创建食物单项模板" else "已选择 ${selectedIds.size} 项"
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.56f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "添加单项", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        SimpleDropdown(
                            modifier = Modifier.fillMaxWidth(),
                            label = selectedSingleCategory.ifBlank { "选择单项分类" },
                            options = singleCategories,
                            onSelected = {
                                selectedSingleCategory = it
                                selectedTemplateLabel = ""
                            }
                        )
                        SimpleDropdown(
                            modifier = Modifier.fillMaxWidth(),
                            label = selectedTemplateLabel.ifBlank { "选择单项名称" },
                            options = templateOptions,
                            onSelected = { selectedTemplateLabel = it }
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                enabled = selectedTemplateLabel.isNotBlank(),
                                onClick = {
                                    val index = templateOptions.indexOf(selectedTemplateLabel)
                                    val template = visibleTemplates.getOrNull(index)
                                    if (template != null && template.id !in selectedIds) {
                                        selectedIds = selectedIds + template.id
                                    }
                                    selectedTemplateLabel = ""
                                }
                            ) {
                                Text("加入")
                            }
                        }
                    }
                    if (selectedIds.isNotEmpty()) {
                        Text(text = "已加入单项", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    selectedIds.mapNotNull { id -> singleTemplates.firstOrNull { it.id == id } }.forEach { template ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = template.item.foodName.ifBlank { "食物模板" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${template.category.ifBlank { "未分类" }} - ${template.item.grams.ifBlank { "--" }} g",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { selectedIds = selectedIds - template.id }) {
                                Text("绉婚櫎")
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        enabled = name.isNotBlank() && selectedIds.isNotEmpty(),
                        onClick = {
                            val items = singleTemplates.filter { it.id in selectedIds }.map { it.item.copy(id = "") }
                            onDone(name, category, items)
                        }
                    ) {
                        Text("完成")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCenterDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        PopDialogSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

@Composable
private fun ApiSettingsPanel(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onSaveApiKey: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsPanel(
            title = "MiMo 鎺ュ彛",
            subtitle = "API Key 只保存在本机，用于 AI 分析请求"
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("MiMo API Key") },
                singleLine = true
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onSaveApiKey) {
                    Text("保存 API Key")
                }
            }
        }
    }
}

@Composable
private fun ProfileEditor(
    name: String,
    birthday: String,
    gender: String,
    signature: String,
    avatarPath: String?,
    age: String,
    onPickAvatar: () -> Unit,
    onNameChange: (String) -> Unit,
    onBirthdayChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onSignatureChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsPanel(
            title = "显示资料",
            subtitle = "头像、姓名和签名会显示在首页左上角"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.hapticClickable(onClick = onPickAvatar)) {
                    AvatarImage(avatarPath = avatarPath, sizeDp = 96)
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onPickAvatar) {
                    Text(if (avatarPath.isNullOrBlank()) "上传头像" else "修改头像")
                }
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = onNameChange,
                label = { Text("姓名") },
                singleLine = true
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = signature,
                onValueChange = onSignatureChange,
                label = { Text("个人签名") },
                minLines = 2
            )
        }
        SettingsPanel(
            title = "基础信息",
            subtitle = "年龄和性别会参与 AI 饮食与训练建议"
        ) {
            BirthdaySelector(birthday = birthday, onBirthdayChange = onBirthdayChange)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "当前年龄", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(text = age.ifBlank { "--" }, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "性别", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                SimpleDropdown(
                    modifier = Modifier.width(120.dp),
                    label = gender.ifBlank { "选择" },
                    options = listOf("男", "女", "未填写"),
                    onSelected = { onGenderChange(if (it == "未填写") "" else it) }
                )
            }
        }
    }
}

@Composable
private fun PersonalizationSettings(
    themeKey: String,
    homeBackgroundPath: String?,
    welcomeMessage: String,
    onThemeChange: (String) -> Unit,
    onWelcomeMessageChange: (String) -> Unit,
    onPickHomeBackground: () -> Unit,
    onClearHomeBackground: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsPanel(
            title = "主题颜色",
            subtitle = "选择后会立即应用到整个软件"
        ) {
            ThemeColorChooser(
                selectedKey = themeKey,
                onSelected = onThemeChange
            )
        }
        SettingsPanel(
            title = "欢迎标语",
            subtitle = "每次打开软件时显示，3 秒后自动进入"
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = welcomeMessage,
                onValueChange = onWelcomeMessageChange,
                label = { Text("欢迎标语") },
                singleLine = true
            )
        }
        HomeBackgroundSettings(
            path = homeBackgroundPath,
            onPick = onPickHomeBackground,
            onClear = onClearHomeBackground
        )
    }
}

@Composable
private fun ThemeColorChooser(
    selectedKey: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        themeOptions.forEach { option ->
            val selected = option.key == selectedKey
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) option.primary.copy(alpha = 0.20f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) option.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .hapticClickable { onSelected(option.key) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ColorSwatch(option.primary)
                    ColorSwatch(option.secondary)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = option.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(text = option.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = if (selected) "已选" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = option.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color) {
    Box(
        modifier = Modifier
            .width(28.dp)
            .height(28.dp)
            .background(color, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape)
    )
}

@Composable
private fun HomeBackgroundSettings(
    path: String?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    SettingsPanel(
        title = "主页背景图",
        subtitle = "用于主页界面背景，欢迎页跟随主题纯色"
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!path.isNullOrBlank()) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = rememberAsyncImagePainter(File(path)),
                    contentDescription = "主页背景",
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.48f))
                            )
                        )
                )
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                    text = "当前主页背景",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "+", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f))
                    Text(text = "未设置主页背景", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!path.isNullOrBlank()) {
                TextButton(onClick = onClear) {
                    Text("清除")
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Button(onClick = onPick) {
                Text(if (path.isNullOrBlank()) "上传背景" else "更换背景")
            }
        }
    }
}

@Composable
private fun BirthdaySelector(
    birthday: String,
    onBirthdayChange: (String) -> Unit
) {
    val todayYear = Year.now().value
    val parsedBirthday = runCatching { LocalDate.parse(birthday) }.getOrNull()
    val selectedYear = parsedBirthday?.year
    val selectedMonth = parsedBirthday?.monthValue
    val selectedDay = parsedBirthday?.dayOfMonth

    fun updateBirthday(year: Int?, month: Int?, day: Int?) {
        if (year == null || month == null || day == null) return
        val validDay = day.coerceAtMost(YearMonth.of(year, month).lengthOfMonth())
        onBirthdayChange("%04d-%02d-%02d".format(year, month, validDay))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "生日", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SimpleDropdown(
                modifier = Modifier.weight(1f),
                label = selectedYear?.toString() ?: "年",
                options = (1900..todayYear).map { it.toString() }.reversed(),
                onSelected = { updateBirthday(it.toInt(), selectedMonth ?: 1, selectedDay ?: 1) }
            )
            SimpleDropdown(
                modifier = Modifier.weight(1f),
                label = selectedMonth?.toString() ?: "月",
                options = (1..12).map { it.toString() },
                onSelected = { updateBirthday(selectedYear ?: todayYear, it.toInt(), selectedDay ?: 1) }
            )
            val dayOptions = (1..YearMonth.of(selectedYear ?: todayYear, selectedMonth ?: 1).lengthOfMonth()).map { it.toString() }
            SimpleDropdown(
                modifier = Modifier.weight(1f),
                label = selectedDay?.toString() ?: "日",
                options = dayOptions,
                onSelected = { updateBirthday(selectedYear ?: todayYear, selectedMonth ?: 1, it.toInt()) }
            )
        }
    }
}

@Composable
private fun SimpleDropdown(
    modifier: Modifier = Modifier,
    label: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(12.dp)
                )
                .hapticClickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "↓",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun AvatarImage(
    avatarPath: String?,
    sizeDp: Int
) {
    Box(
        modifier = Modifier
            .width(sizeDp.dp)
            .height(sizeDp.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarPath.isNullOrBlank()) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = rememberAsyncImagePainter(File(avatarPath)),
                contentDescription = "澶村儚",
                contentScale = ContentScale.Crop
            )
        } else {
            Text(text = "澶村儚")
        }
    }
}

@Composable
private fun DrawerSectionHeader(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hapticClickable(onClick = onClick)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(text = if (expanded) "鏀惰捣" else "灞曞紑")
    }
}

@Composable
private fun ResultCard(title: String, content: String) {
    SectionCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = content, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private data class ReportNode(
    val title: String,
    val level: Int,
    val body: String,
    val children: List<ReportNode> = emptyList()
)

@Composable
private fun PhotoComparisonResultCard(result: PhotoComparisonResult) {
    val sections = remember(result.analysis) { parseReportTree(result.analysis) }
    var previewOpen by remember { mutableStateOf(false) }
    SectionCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "${result.angle.label} AI 对比",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            ComparisonImagePair(
                result = result,
                modifier = Modifier
                    .fillMaxWidth()
                    .hapticClickable { previewOpen = true }
            )
            if (sections.isEmpty()) {
                MarkdownText(content = result.analysis)
            } else {
                ReportSectionList(sections = sections)
            }
        }
    }
    if (previewOpen) {
        Dialog(
            onDismissRequest = { previewOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ZoomableFullscreenBox(onDismiss = { previewOpen = false }) {
                ComparisonImagePair(
                    result = result,
                    modifier = Modifier.fillMaxSize(),
                    fullscreen = true
                )
            }
        }
    }
}

@Composable
private fun ZoomableFullscreenBox(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                    scale = newScale
                    if (newScale == 1f) {
                        offsetX = 0f
                        offsetY = 0f
                    } else {
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun ComparisonImagePair(
    result: PhotoComparisonResult,
    modifier: Modifier = Modifier,
    fullscreen: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(if (fullscreen) 0.dp else 14.dp))
            .background(if (fullscreen) Color.Black else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .border(
                width = if (fullscreen) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(if (fullscreen) 0.dp else 14.dp)
            )
            .padding(if (fullscreen) 0.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(if (fullscreen) 2.dp else 8.dp)
    ) {
        ComparisonImagePane(
            label = "往期${result.previousDate}",
            path = result.previousPhotoPath,
            fullscreen = fullscreen,
            modifier = Modifier.weight(1f)
        )
        ComparisonImagePane(
            label = "浠婃棩 ${result.currentDate}",
            path = result.currentPhotoPath,
            fullscreen = fullscreen,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ComparisonImagePane(
    label: String,
    path: String,
    fullscreen: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .height(if (fullscreen) 560.dp else 260.dp)
            .clip(RoundedCornerShape(if (fullscreen) 0.dp else 10.dp))
            .background(Color.Black),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = rememberAsyncImagePainter(File(path)),
            contentDescription = label,
            contentScale = if (fullscreen) ContentScale.Fit else ContentScale.Crop
        )
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.48f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            style = if (fullscreen) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReportSectionList(sections: List<ReportNode>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sections.forEach { section ->
            ReportNodeView(node = section, depth = 0)
        }
    }
}

@Composable
private fun ReportNodeView(node: ReportNode, depth: Int) {
    val shape = RoundedCornerShape(if (depth == 0) 14.dp else 10.dp)
    val background = if (depth == 0) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 10).dp)
            .background(background, shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = if (depth == 0) 0.14f else 0.08f),
                shape = shape
            )
            .padding(if (depth == 0) 12.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = node.title,
            style = when (depth) {
                0 -> MaterialTheme.typography.titleSmall
                1 -> MaterialTheme.typography.bodyLarge
                else -> MaterialTheme.typography.bodyMedium
            },
            fontWeight = FontWeight.Bold
        )
        if (node.body.isNotBlank()) {
            MarkdownText(content = node.body)
        }
        node.children.forEach { child ->
            ReportNodeView(node = child, depth = depth + 1)
        }
    }
}

private data class MutableReportNode(
    val title: String,
    val level: Int,
    val body: MutableList<String> = mutableListOf(),
    val children: MutableList<MutableReportNode> = mutableListOf()
)

private data class ParsedReportHeading(
    val title: String,
    val level: Int,
    val inlineBody: String
)

private fun parseReportTree(content: String): List<ReportNode> {
    val roots = mutableListOf<MutableReportNode>()
    val stack = mutableListOf<MutableReportNode>()

    content.lines().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isBlank()) return@forEach
        val heading = parseReportHeading(line)
        if (heading != null) {
            val node = MutableReportNode(title = heading.title, level = heading.level)
            if (heading.inlineBody.isNotBlank()) node.body += heading.inlineBody
            while (stack.isNotEmpty() && stack.last().level >= node.level) {
                stack.removeAt(stack.lastIndex)
            }
            if (stack.isEmpty()) {
                roots += node
            } else {
                stack.last().children += node
            }
            stack += node
        } else {
            val cleaned = cleanMarkdownTextLine(rawLine)
            if (cleaned.isNotBlank()) {
                if (stack.isEmpty()) {
                    roots += MutableReportNode(title = "概览", level = 1, body = mutableListOf(cleaned))
                    stack += roots.last()
                } else {
                    stack.last().body += cleaned
                }
            }
        }
    }
    return roots.map { it.toReportNode() }
}

private fun MutableReportNode.toReportNode(): ReportNode {
    return ReportNode(
        title = title,
        level = level,
        body = body.joinToString("\n").trim(),
        children = children.map { it.toReportNode() }
    )
}

private fun parseReportHeading(line: String): ParsedReportHeading? {
    val markdownLevel = line.takeWhile { it == '#' }.length
    if (markdownLevel > 0) {
        val title = cleanMarkdownTextLine(line.drop(markdownLevel))
        return title.takeIf { it.isNotBlank() }?.let {
            ParsedReportHeading(title = it, level = markdownLevel.coerceIn(1, 6), inlineBody = "")
        }
    }

    val numbered = Regex("""^(\d+(?:\.\d+)*|[一二三四五六七八九十]+)[.、]\s*(.+?)(?:[:：]\s*(.*))?$""").matchEntire(line)
    if (numbered != null) {
        val marker = numbered.groupValues[1]
        val level = if (marker.contains(".")) marker.count { it == '.' } + 1 else 1
        return ParsedReportHeading(
            title = cleanMarkdownTextLine(numbered.groupValues[2]),
            level = level.coerceIn(1, 4),
            inlineBody = cleanMarkdownTextLine(numbered.groupValues.getOrNull(3).orEmpty())
        )
    }

    val bold = Regex("""^\*\*(.+?)\*\*(?:[:：]\s*(.*))?$""").matchEntire(line)
    if (bold != null) {
        return ParsedReportHeading(
            title = cleanMarkdownTextLine(bold.groupValues[1]),
            level = 2,
            inlineBody = cleanMarkdownTextLine(bold.groupValues.getOrNull(2).orEmpty())
        )
    }

    val plain = Regex("""^([^:：]{2,18})[:：]\s*(.*)$""").matchEntire(line)
    if (
        plain != null &&
        !line.startsWith("•") &&
        !line.startsWith("-") &&
        !line.startsWith("*") &&
        line.substringBefore("：").substringBefore(":").length <= 12
    ) {
        return ParsedReportHeading(
            title = cleanMarkdownTextLine(plain.groupValues[1]),
            level = 2,
            inlineBody = cleanMarkdownTextLine(plain.groupValues.getOrNull(2).orEmpty())
        )
    }
    return null
}

private fun cleanMarkdownTextLine(line: String): String {
    return line
        .trim()
        .replace(Regex("""^#{1,6}\s*"""), "")
        .replace(Regex("""^\d+[.、]\s*"""), "")
        .replace(Regex("""^[-*+]\s+"""), "• ")
        .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
        .replace(Regex("""__(.*?)__"""), "$1")
        .replace(Regex("""`([^`]*)`"""), "$1")
        .replace(Regex("""^\s*>\s*"""), "")
        .replace(Regex("""\[(.*?)]\((.*?)\)"""), "$1")
        .trim()
}
