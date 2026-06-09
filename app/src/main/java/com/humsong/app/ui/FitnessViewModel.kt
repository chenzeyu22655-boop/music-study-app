package com.humsong.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humsong.app.ai.FitnessAiClient
import com.humsong.app.fitness.BodyPhotoAngle
import com.humsong.app.fitness.FitnessEntry
import com.humsong.app.fitness.FitnessRepository
import com.humsong.app.fitness.MealItem
import com.humsong.app.fitness.MealTemplate
import com.humsong.app.fitness.MealTemplateGroup
import com.humsong.app.fitness.MealType
import com.humsong.app.fitness.TrainingGoal
import com.humsong.app.fitness.TrainingItem
import com.humsong.app.fitness.TrainingPeriod
import com.humsong.app.fitness.TrainingTemplate
import com.humsong.app.fitness.TrainingTemplateGroup
import com.humsong.app.fitness.UserProfile
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class MealViewType(
    val key: String,
    val label: String,
    val mealType: MealType?
) {
    All("all", "全天", null),
    Breakfast("breakfast", "早餐", MealType.Breakfast),
    Lunch("lunch", "中餐", MealType.Lunch),
    Dinner("dinner", "晚餐", MealType.Dinner);

    companion object {
        fun fromLabel(label: String): MealViewType {
            return entries.firstOrNull { it.label == label } ?: Breakfast
        }
    }
}

data class PhotoComparisonResult(
    val previousDate: String,
    val currentDate: String,
    val angle: BodyPhotoAngle,
    val previousPhotoPath: String,
    val currentPhotoPath: String,
    val analysis: String
)

data class FoodRecognitionSession(
    val mealType: MealType,
    val photoPaths: List<String> = emptyList(),
    val supplementText: String = "",
    val rawResponse: String = "",
    val recognizedItems: List<MealItem> = emptyList(),
    val note: String = ""
)

data class FitnessUiState(
    val selectedDate: String = LocalDate.now().toString(),
    val entry: FitnessEntry = FitnessEntry(date = LocalDate.now().toString()),
    val apiKeyInput: String = "",
    val selectedPhotoAngle: BodyPhotoAngle = BodyPhotoAngle.Front,
    val selectedMealViewType: MealViewType = MealViewType.Breakfast,
    val profileName: String = "",
    val profileBirthday: String = "",
    val profileGender: String = "",
    val profileSignature: String = "",
    val profileAvatarPath: String? = null,
    val homeBackgroundPath: String? = null,
    val welcomeMessage: String = "今天也请努力",
    val profileAge: String = "",
    val isAnalyzing: Boolean = false,
    val isAnalyzingBodyGoal: Boolean = false,
    val isAnalyzingNutrition: Boolean = false,
    val isAnalyzingMealNutrition: Boolean = false,
    val isRecognizingFoodPhoto: Boolean = false,
    val foodRecognitionSession: FoodRecognitionSession? = null,
    val isComparingPhotos: Boolean = false,
    val recordedDates: List<String> = emptyList(),
    val trainingTemplates: List<TrainingTemplate> = emptyList(),
    val trainingTemplateGroups: List<TrainingTemplateGroup> = emptyList(),
    val mealTemplates: List<MealTemplate> = emptyList(),
    val mealTemplateGroups: List<MealTemplateGroup> = emptyList(),
    val photoComparisonDateOptions: List<String> = emptyList(),
    val selectedPhotoComparisonDate: String = "",
    val photoComparisonResult: PhotoComparisonResult? = null,
    val canEditSelectedDate: Boolean = true,
    val showProfileOnboarding: Boolean = false,
    val showLaunchWelcome: Boolean = true,
    val showDailyBodyCheckIn: Boolean = false,
    val isFirstOpenToday: Boolean = false,
    val statusText: String = "记录今天的训练、饮食和身体变化。",
    val errorMessage: String? = null
)

class FitnessViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FitnessRepository(application)
    private val aiClient = FitnessAiClient()
    private val preferences = application.getSharedPreferences("fitness_settings", Application.MODE_PRIVATE)
    private val today = LocalDate.now().toString()

    init {
        repository.ensureDefaultTemplatesSeeded()
    }

    private val _state = MutableStateFlow(
        FitnessUiState(
            selectedDate = today,
            entry = repository.load(today),
            apiKeyInput = preferences.getString("api_key", "").orEmpty(),
            profileName = preferences.getString("profile_name", "").orEmpty(),
            profileBirthday = preferences.getString("profile_birthday", "").orEmpty(),
            profileGender = preferences.getString("profile_gender", "").orEmpty(),
            profileSignature = preferences.getString("profile_signature", "").orEmpty(),
            profileAvatarPath = preferences.getString("profile_avatar_path", null),
            homeBackgroundPath = preferences.getString("home_background_path", null)
                ?: preferences.getString("welcome_background_path", null),
            welcomeMessage = preferences.getString("welcome_message", "今天也请努力").orEmpty().ifBlank { "今天也请努力" },
            profileAge = UserProfile(
                birthday = preferences.getString("profile_birthday", "").orEmpty()
            ).ageText(),
            recordedDates = repository.recordedDates(),
            trainingTemplates = repository.loadTrainingTemplates(),
            trainingTemplateGroups = repository.loadTrainingTemplateGroups(),
            mealTemplates = repository.loadMealTemplates(),
            mealTemplateGroups = repository.loadMealTemplateGroups(),
            photoComparisonDateOptions = repository.datesWithPhotosBefore(today),
            selectedPhotoComparisonDate = repository.datesWithPhotosBefore(today).firstOrNull().orEmpty(),
            showProfileOnboarding = preferences.getString("profile_name", "").orEmpty().isBlank() ||
                preferences.getString("profile_birthday", "").orEmpty().isBlank() ||
                preferences.getString("profile_gender", "").orEmpty().isBlank(),
            showLaunchWelcome = preferences.getString("profile_name", "").orEmpty().isNotBlank() &&
                preferences.getString("profile_birthday", "").orEmpty().isNotBlank() &&
                preferences.getString("profile_gender", "").orEmpty().isNotBlank(),
            isFirstOpenToday = preferences.getString("last_welcome_date", "") != today
        )
    )
    val state: StateFlow<FitnessUiState> = _state

    fun previousDay() {
        val currentDate = LocalDate.parse(state.value.selectedDate)
        val previousSelectableDate = repository.recordedDates()
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .filter { it.isBefore(currentDate) }
            .maxOrNull()
        if (previousSelectableDate != null) {
            changeDate(previousSelectableDate.toString())
        } else {
            _state.update { it.copy(errorMessage = "前面没有可查看的记录。") }
        }
    }

    fun nextDay() {
        val currentDate = LocalDate.parse(state.value.selectedDate)
        val selectableDates = (repository.recordedDates() + today)
            .distinct()
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .filter { !it.isAfter(LocalDate.now()) && it.isAfter(currentDate) }
            .sorted()
        val nextSelectableDate = selectableDates.firstOrNull()
        if (nextSelectableDate != null) {
            changeDate(nextSelectableDate.toString())
        } else {
            _state.update { it.copy(errorMessage = "后面没有可查看的记录。") }
        }
    }

    fun updateDate(value: String) {
        val parsedDate = runCatching { LocalDate.parse(value) }.getOrNull()
        when {
            parsedDate == null -> {
                _state.update { it.copy(selectedDate = value, errorMessage = "日期格式请使用 YYYY-MM-DD。") }
            }
            parsedDate.isAfter(LocalDate.now()) -> {
                _state.update { it.copy(errorMessage = "未来日期还不能选择。") }
            }
            value != today && value !in repository.recordedDates() -> {
                _state.update { it.copy(errorMessage = "这个日期还没有记录，不能选择。") }
            }
            else -> changeDate(value)
        }
    }

    fun goToday() {
        changeDate(today)
    }

    fun deleteRecord(date: String) {
        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
        if (parsedDate == null || date !in repository.recordedDates()) {
            _state.update { it.copy(errorMessage = "这个日期没有可删除的记录。") }
            return
        }
        repository.delete(date)
        val nextDate = if (date == today) today else today
        _state.update {
            it.copy(
                selectedDate = nextDate,
                entry = repository.load(nextDate),
                canEditSelectedDate = true,
                recordedDates = repository.recordedDates(),
                photoComparisonDateOptions = repository.datesWithPhotosBefore(nextDate),
                selectedPhotoComparisonDate = repository.datesWithPhotosBefore(nextDate).firstOrNull().orEmpty(),
                statusText = "$date 的记录已删除。",
                errorMessage = null
            )
        }
    }

    fun updateHeight(value: String) = updateEntryAndSave("身体数据已保存。") { it.copy(heightCm = value) }
    fun updateWeight(value: String) = updateEntryAndSave("身体数据已保存。") { it.copy(weightKg = value) }
    fun updateTrainingGoal(goal: TrainingGoal) = updateEntryAndSave("身体数据已保存。") { it.copy(trainingGoal = goal) }
    fun completeBodyDataEditing(
        originalHeight: String,
        originalWeight: String,
        originalGoal: TrainingGoal
    ) {
        val current = state.value.entry
        val changed = current.heightCm != originalHeight ||
            current.weightKg != originalWeight ||
            current.trainingGoal != originalGoal
        if (changed) {
            analyzeBodyGoalRecommendationIfReady()
        }
    }
    fun finishProfileOnboarding() {
        _state.update { it.copy(showProfileOnboarding = false, showLaunchWelcome = true) }
    }

    fun dismissLaunchWelcome() {
        _state.update {
            it.copy(
                showLaunchWelcome = false,
                showDailyBodyCheckIn = it.isFirstOpenToday
            )
        }
    }

    fun dismissDailyBodyCheckIn() {
        preferences.edit().putString("last_welcome_date", today).apply()
        _state.update { it.copy(showDailyBodyCheckIn = false, isFirstOpenToday = false) }
        analyzeBodyGoalRecommendationIfReady()
    }
    fun updateApiKey(value: String) = _state.update { it.copy(apiKeyInput = value) }
    fun updatePhotoAngle(angle: BodyPhotoAngle) = _state.update { it.copy(selectedPhotoAngle = angle) }
    fun updatePhotoComparisonDate(date: String) = _state.update { it.copy(selectedPhotoComparisonDate = date) }
    fun updateMealViewType(type: MealViewType) = _state.update { it.copy(selectedMealViewType = type) }
    fun updateProfileName(value: String) {
        preferences.edit().putString("profile_name", value).apply()
        _state.update { it.copy(profileName = value, statusText = "个人资料已保存。", errorMessage = null) }
    }

    fun updateProfileBirthday(value: String) {
        preferences.edit().putString("profile_birthday", value).apply()
        _state.update {
            it.copy(
                profileBirthday = value,
                profileAge = UserProfile(birthday = value).ageText(),
                statusText = "个人资料已保存。",
                errorMessage = null
            )
        }
    }

    fun updateProfileGender(value: String) {
        preferences.edit().putString("profile_gender", value).apply()
        _state.update { it.copy(profileGender = value, statusText = "个人资料已保存。", errorMessage = null) }
    }

    fun updateProfileSignature(value: String) {
        preferences.edit().putString("profile_signature", value).apply()
        _state.update { it.copy(profileSignature = value, statusText = "个人资料已保存。", errorMessage = null) }
    }

    fun addTrainingItem(item: TrainingItem) {
        if (!state.value.canEditSelectedDate) {
            _state.update { it.copy(errorMessage = "以前的记录只能查看，不能修改。") }
            return
        }
        val normalized = item.copy(id = UUID.randomUUID().toString(), completedSets = 0)
        val updated = state.value.entry.copy(trainingItems = state.value.entry.trainingItems + normalized)
        repository.save(updated)
        _state.update {
            it.copy(entry = updated, recordedDates = repository.recordedDates(), statusText = "训练计划已添加。", errorMessage = null)
        }
    }

    fun addTrainingItemFromTemplate(template: TrainingTemplate, period: TrainingPeriod) {
        addTrainingItem(template.item.copy(id = UUID.randomUUID().toString(), period = period, completedSets = 0))
    }

    fun addTrainingItemsFromGroup(group: TrainingTemplateGroup, period: TrainingPeriod) {
        if (!state.value.canEditSelectedDate) {
            _state.update { it.copy(errorMessage = "以前的记录只能查看，不能修改。") }
            return
        }
        if (group.items.isEmpty()) {
            _state.update { it.copy(errorMessage = "这个集合模板还没有训练计划。") }
            return
        }
        val normalized = group.items.map { it.copy(id = UUID.randomUUID().toString(), period = period, completedSets = 0) }
        val updated = state.value.entry.copy(trainingItems = state.value.entry.trainingItems + normalized)
        repository.save(updated)
        _state.update {
            it.copy(entry = updated, recordedDates = repository.recordedDates(), statusText = "${group.templateName.ifBlank { "集合模板" }}已导入。", errorMessage = null)
        }
    }

    fun saveTrainingTemplate(templateName: String, category: String, item: TrainingItem) {
        val normalizedName = item.name.trim().ifBlank { item.type.label }
        val templates = state.value.trainingTemplates + TrainingTemplate(
            id = UUID.randomUUID().toString(),
            templateName = normalizedName,
            category = category.trim(),
            item = item.copy(id = "", period = TrainingPeriod.Evening, completedSets = 0)
        )
        repository.saveTrainingTemplates(templates)
        _state.update { it.copy(trainingTemplates = templates, statusText = "训练模板已保存。", errorMessage = null) }
    }

    fun updateTrainingTemplate(template: TrainingTemplate) {
        val templates = state.value.trainingTemplates.map {
            if (it.id == template.id) template.copy(templateName = template.item.name, item = template.item.copy(id = "", period = TrainingPeriod.Evening, completedSets = 0)) else it
        }
        repository.saveTrainingTemplates(templates)
        _state.update { it.copy(trainingTemplates = templates, statusText = "训练模板已修改。", errorMessage = null) }
    }

    fun deleteTrainingTemplate(id: String) {
        val templates = state.value.trainingTemplates.filterNot { it.id == id }
        repository.saveTrainingTemplates(templates)
        _state.update { it.copy(trainingTemplates = templates, statusText = "训练模板已删除。", errorMessage = null) }
    }

    fun saveTrainingTemplateGroup(templateName: String, category: String, items: List<TrainingItem>) {
        val normalizedName = templateName.trim().ifBlank { "训练集合模板" }
        val groups = state.value.trainingTemplateGroups + TrainingTemplateGroup(
            id = UUID.randomUUID().toString(),
            templateName = normalizedName,
            category = category.trim(),
            items = items.map { it.copy(id = "", period = TrainingPeriod.Evening, completedSets = 0) }
        )
        repository.saveTrainingTemplateGroups(groups)
        _state.update { it.copy(trainingTemplateGroups = groups, statusText = "训练集合模板已保存。", errorMessage = null) }
    }

    fun updateTrainingTemplateGroup(group: TrainingTemplateGroup) {
        val groups = state.value.trainingTemplateGroups.map {
            if (it.id == group.id) group.copy(items = group.items.map { item -> item.copy(id = "", period = TrainingPeriod.Evening, completedSets = 0) }) else it
        }
        repository.saveTrainingTemplateGroups(groups)
        _state.update { it.copy(trainingTemplateGroups = groups, statusText = "训练集合模板已修改。", errorMessage = null) }
    }

    fun deleteTrainingTemplateGroup(id: String) {
        val groups = state.value.trainingTemplateGroups.filterNot { it.id == id }
        repository.saveTrainingTemplateGroups(groups)
        _state.update { it.copy(trainingTemplateGroups = groups, statusText = "训练集合模板已删除。", errorMessage = null) }
    }

    fun updateTrainingItem(id: String, item: TrainingItem) {
        if (!state.value.canEditSelectedDate) {
            _state.update { it.copy(errorMessage = "以前的记录只能查看，不能修改。") }
            return
        }
        val updated = state.value.entry.copy(
            trainingItems = state.value.entry.trainingItems.map { current ->
                if (current.id == id) item.copy(id = id, completedSets = current.completedSets.coerceAtMost(item.targetSets)) else current
            }
        )
        repository.save(updated)
        _state.update {
            it.copy(entry = updated, recordedDates = repository.recordedDates(), statusText = "训练计划已修改。", errorMessage = null)
        }
    }

    fun updateTrainingItemCompletedSets(id: String, completedSets: Int) {
        if (!state.value.canEditSelectedDate) {
            _state.update { it.copy(errorMessage = "以前的记录只能查看，不能修改。") }
            return
        }
        var updatedItem: TrainingItem? = null
        val updated = state.value.entry.copy(
            trainingItems = state.value.entry.trainingItems.map { current ->
                if (current.id == id) {
                    current.copy(completedSets = completedSets.coerceIn(0, current.targetSets)).also { updatedItem = it }
                } else {
                    current
                }
            }
        )
        repository.save(updated)
        val item = updatedItem
        _state.update {
            it.copy(
                entry = updated,
                recordedDates = repository.recordedDates(),
                statusText = when {
                    item == null -> "训练计划已更新。"
                    item.completed -> "训练计划已完成。"
                    item.completedSets == 0 -> "训练计划已恢复为未完成。"
                    else -> "已完成 ${item.completedSets}/${item.targetSets} 组。"
                },
                errorMessage = null
            )
        }
    }

    fun deleteTrainingItem(id: String) {
        if (!state.value.canEditSelectedDate) {
            _state.update { it.copy(errorMessage = "以前的记录只能查看，不能修改。") }
            return
        }
        val updated = state.value.entry.copy(
            trainingItems = state.value.entry.trainingItems.filterNot { item -> item.id == id }
        )
        repository.save(updated)
        _state.update {
            it.copy(entry = updated, recordedDates = repository.recordedDates(), statusText = "训练计划已删除。", errorMessage = null)
        }
    }

    fun addMealItem(item: MealItem) {
        if (!state.value.canEditSelectedDate) {
            _state.update { it.copy(errorMessage = "以前的记录只能查看，不能修改。") }
            return
        }
        val normalized = item.copy(
            id = UUID.randomUUID().toString(),
            type = state.value.selectedMealViewType.mealType ?: MealType.Breakfast
        )
        val updated = state.value.entry.copy(mealItems = state.value.entry.mealItems + normalized)
        repository.save(updated)
        _state.update {
            it.copy(entry = updated, recordedDates = repository.recordedDates(), statusText = "${normalized.type.label}食物已添加。", errorMessage = null)
        }
    }

    fun addMealItemFromTemplate(template: MealTemplate) {
        addMealItem(template.item.copy(id = UUID.randomUUID().toString()))
    }

    fun addMealItemsFromGroup(group: MealTemplateGroup) {
        if (!state.value.canEditSelectedDate) {
            _state.update { it.copy(errorMessage = "以前的记录只能查看，不能修改。") }
            return
        }
        if (group.items.isEmpty()) {
            _state.update { it.copy(errorMessage = "这个集合模板还没有食物。") }
            return
        }
        val mealType = state.value.selectedMealViewType.mealType ?: MealType.Breakfast
        val normalized = group.items.map { item ->
            item.copy(id = UUID.randomUUID().toString(), type = mealType)
        }
        val updated = state.value.entry.copy(mealItems = state.value.entry.mealItems + normalized)
        repository.save(updated)
        _state.update {
            it.copy(entry = updated, recordedDates = repository.recordedDates(), statusText = "${group.templateName.ifBlank { "集合模板" }}已导入。", errorMessage = null)
        }
    }

    fun saveMealTemplate(templateName: String, category: String, item: MealItem) {
        val normalizedName = item.foodName.trim().ifBlank { "食物模板" }
        val templates = state.value.mealTemplates + MealTemplate(
            id = UUID.randomUUID().toString(),
            templateName = normalizedName,
            category = category.trim(),
            item = item.copy(id = "")
        )
        repository.saveMealTemplates(templates)
        _state.update { it.copy(mealTemplates = templates, statusText = "食物模板已保存。", errorMessage = null) }
    }

    fun recognizeFoodPhotoWithAi(photoPath: String, supplementText: String = "") {
        val currentState = state.value
        if (!currentState.canEditSelectedDate) {
            File(photoPath).delete()
            _state.update { it.copy(errorMessage = "以前的记录只能查看，不能修改。") }
            return
        }
        val apiKey = currentState.apiKeyInput.trim()
        if (apiKey.isBlank()) {
            File(photoPath).delete()
            _state.update { it.copy(errorMessage = "请先在右上角设置里填写 API Key。") }
            return
        }
        val mealType = currentState.selectedMealViewType.mealType
        if (mealType == null) {
            File(photoPath).delete()
            _state.update { it.copy(errorMessage = "请先切换到早餐、午餐或晚餐，再使用拍照识别。") }
            return
        }
        val previousSession = currentState.foodRecognitionSession?.takeIf { it.mealType == mealType }
        val photoPaths = (previousSession?.photoPaths.orEmpty() + photoPath).distinct()
        val combinedSupplement = supplementText.trim()
            .ifBlank { previousSession?.supplementText.orEmpty() }
        _state.update {
            it.copy(
                isRecognizingFoodPhoto = true,
                foodRecognitionSession = FoodRecognitionSession(
                    mealType = mealType,
                    photoPaths = photoPaths,
                    supplementText = combinedSupplement,
                    rawResponse = previousSession?.rawResponse.orEmpty(),
                    recognizedItems = previousSession?.recognizedItems.orEmpty(),
                    note = previousSession?.note.orEmpty()
                ),
                statusText = "AI 正在识别照片里的食物...",
                errorMessage = null
            )
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { aiClient.recognizeFoodPhoto(photoPaths, mealType, apiKey, combinedSupplement) }
            }
            result.onSuccess { response ->
                val recognizedItems = parseFoodPhotoItems(response, mealType)
                val note = parseFoodPhotoNote(response)
                if (recognizedItems.isEmpty()) {
                    _state.update {
                        it.copy(
                            isRecognizingFoodPhoto = false,
                            foodRecognitionSession = FoodRecognitionSession(
                                mealType = mealType,
                                photoPaths = photoPaths,
                                supplementText = combinedSupplement,
                                rawResponse = response,
                                recognizedItems = emptyList(),
                                note = note.ifBlank { "没有得到可添加的食物，请补充说明或重新上传照片。" }
                            ),
                            statusText = "AI 识别完成，但没有得到可添加的食物。",
                            errorMessage = null
                        )
                    }
                    return@onSuccess
                }
                _state.update {
                    it.copy(
                        isRecognizingFoodPhoto = false,
                        foodRecognitionSession = FoodRecognitionSession(
                            mealType = mealType,
                            photoPaths = photoPaths,
                            supplementText = combinedSupplement,
                            rawResponse = response,
                            recognizedItems = recognizedItems,
                            note = note
                        ),
                        statusText = "AI 已识别 ${recognizedItems.size} 项食物，请确认后添加。",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isRecognizingFoodPhoto = false,
                        foodRecognitionSession = FoodRecognitionSession(
                            mealType = mealType,
                            photoPaths = photoPaths,
                            supplementText = combinedSupplement,
                            rawResponse = error.message.orEmpty().take(1200),
                            recognizedItems = previousSession?.recognizedItems.orEmpty(),
                            note = "识别失败，可以补充信息后重试。"
                        ),
                        statusText = "AI 拍照识别失败。",
                        errorMessage = error.message.orEmpty().take(1200)
                    )
                }
            }
        }
    }

    fun updateFoodRecognitionSupplement(value: String) {
        _state.update {
            val session = it.foodRecognitionSession ?: return@update it
            it.copy(foodRecognitionSession = session.copy(supplementText = value))
        }
    }

    fun retryFoodRecognitionWithSupplement() {
        val session = state.value.foodRecognitionSession ?: return
        if (session.photoPaths.isEmpty()) {
            _state.update { it.copy(errorMessage = "请先拍照或上传一张食物照片。") }
            return
        }
        runFoodRecognitionSession(session.photoPaths, session.mealType, session.supplementText)
    }

    fun confirmFoodRecognitionItems() {
        val current = state.value
        val session = current.foodRecognitionSession ?: return
        if (!current.canEditSelectedDate) {
            _state.update { it.copy(errorMessage = "以前的记录只能查看，不能修改。") }
            return
        }
        if (session.recognizedItems.isEmpty()) {
            _state.update { it.copy(errorMessage = "当前没有可添加的识别结果。") }
            return
        }
        val normalized = session.recognizedItems.map {
            it.copy(id = UUID.randomUUID().toString(), type = session.mealType)
        }
        val updated = current.entry.copy(mealItems = current.entry.mealItems + normalized)
        repository.save(updated)
        _state.update {
            it.copy(
                entry = updated,
                recordedDates = repository.recordedDates(),
                foodRecognitionSession = null,
                isRecognizingFoodPhoto = false,
                statusText = "已添加 ${normalized.size} 项食物。",
                errorMessage = null
            )
        }
        cleanupFoodRecognitionFiles(session.photoPaths)
    }

    fun dismissFoodRecognitionSession() {
        val session = state.value.foodRecognitionSession
        _state.update { it.copy(foodRecognitionSession = null, isRecognizingFoodPhoto = false) }
        cleanupFoodRecognitionFiles(session?.photoPaths.orEmpty())
    }

    private fun runFoodRecognitionSession(
        photoPaths: List<String>,
        mealType: MealType,
        supplementText: String
    ) {
        val apiKey = state.value.apiKeyInput.trim()
        val previousSession = state.value.foodRecognitionSession?.takeIf { it.mealType == mealType }
        _state.update {
            it.copy(
                isRecognizingFoodPhoto = true,
                foodRecognitionSession = FoodRecognitionSession(
                    mealType = mealType,
                    photoPaths = photoPaths.distinct(),
                    supplementText = supplementText,
                    rawResponse = previousSession?.rawResponse.orEmpty(),
                    recognizedItems = previousSession?.recognizedItems.orEmpty(),
                    note = previousSession?.note.orEmpty()
                ),
                statusText = "AI 正在识别照片里的食物...",
                errorMessage = null
            )
        }
        viewModelScope.launch {
            val requestPhotoPaths = photoPaths.distinct()
            val result = withContext(Dispatchers.IO) {
                runCatching { aiClient.recognizeFoodPhoto(requestPhotoPaths, mealType, apiKey, supplementText) }
            }
            result.onSuccess { response ->
                val recognizedItems = parseFoodPhotoItems(response, mealType)
                val note = parseFoodPhotoNote(response)
                if (recognizedItems.isEmpty()) {
                    _state.update {
                        it.copy(
                            isRecognizingFoodPhoto = false,
                            foodRecognitionSession = FoodRecognitionSession(
                                mealType = mealType,
                                photoPaths = requestPhotoPaths,
                                supplementText = supplementText,
                                rawResponse = response,
                                recognizedItems = emptyList(),
                                note = note.ifBlank { "没有得到可添加的食物，请补充说明或重新上传照片。" }
                            ),
                            statusText = "AI 识别完成，但没有得到可添加的食物。",
                            errorMessage = null
                        )
                    }
                    return@onSuccess
                }
                _state.update {
                    it.copy(
                        isRecognizingFoodPhoto = false,
                        foodRecognitionSession = FoodRecognitionSession(
                            mealType = mealType,
                            photoPaths = requestPhotoPaths,
                            supplementText = supplementText,
                            rawResponse = response,
                            recognizedItems = recognizedItems,
                            note = note
                        ),
                        statusText = "AI 已识别 ${recognizedItems.size} 项食物，请确认后添加。",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isRecognizingFoodPhoto = false,
                        foodRecognitionSession = FoodRecognitionSession(
                            mealType = mealType,
                            photoPaths = requestPhotoPaths,
                            supplementText = supplementText,
                            rawResponse = error.message.orEmpty().take(1200),
                            recognizedItems = previousSession?.recognizedItems.orEmpty(),
                            note = "识别失败，可以补充信息后重试。"
                        ),
                        statusText = "AI 拍照识别失败。",
                        errorMessage = error.message.orEmpty().take(1200)
                    )
                }
            }
        }
    }

    fun updateMealTemplate(template: MealTemplate) {
        val templates = state.value.mealTemplates.map {
            if (it.id == template.id) template.copy(templateName = template.item.foodName, item = template.item.copy(id = "")) else it
        }
        repository.saveMealTemplates(templates)
        _state.update { it.copy(mealTemplates = templates, statusText = "食物模板已修改。", errorMessage = null) }
    }

    fun deleteMealTemplate(id: String) {
        val templates = state.value.mealTemplates.filterNot { it.id == id }
        repository.saveMealTemplates(templates)
        _state.update { it.copy(mealTemplates = templates, statusText = "食物模板已删除。", errorMessage = null) }
    }

    fun saveMealTemplateGroup(templateName: String, category: String, items: List<MealItem>) {
        val normalizedName = templateName.trim().ifBlank { "食物集合模板" }
        val groups = state.value.mealTemplateGroups + MealTemplateGroup(
            id = UUID.randomUUID().toString(),
            templateName = normalizedName,
            category = category.trim(),
            items = items.map { it.copy(id = "") }
        )
        repository.saveMealTemplateGroups(groups)
        _state.update { it.copy(mealTemplateGroups = groups, statusText = "食物集合模板已保存。", errorMessage = null) }
    }

    fun updateMealTemplateGroup(group: MealTemplateGroup) {
        val groups = state.value.mealTemplateGroups.map {
            if (it.id == group.id) group.copy(items = group.items.map { item -> item.copy(id = "") }) else it
        }
        repository.saveMealTemplateGroups(groups)
        _state.update { it.copy(mealTemplateGroups = groups, statusText = "食物集合模板已修改。", errorMessage = null) }
    }

    fun deleteMealTemplateGroup(id: String) {
        val groups = state.value.mealTemplateGroups.filterNot { it.id == id }
        repository.saveMealTemplateGroups(groups)
        _state.update { it.copy(mealTemplateGroups = groups, statusText = "食物集合模板已删除。", errorMessage = null) }
    }

    fun updateMealItem(id: String, item: MealItem) {
        if (!state.value.canEditSelectedDate) {
            _state.update { it.copy(errorMessage = "以前的记录只能查看，不能修改。") }
            return
        }
        val updated = state.value.entry.copy(
            mealItems = state.value.entry.mealItems.map { current ->
                if (current.id == id) item.copy(id = id) else current
            }
        )
        repository.save(updated)
        _state.update {
            it.copy(entry = updated, recordedDates = repository.recordedDates(), statusText = "食物记录已修改。", errorMessage = null)
        }
    }

    fun deleteMealItem(id: String) {
        if (!state.value.canEditSelectedDate) {
            _state.update { it.copy(errorMessage = "以前的记录只能查看，不能修改。") }
            return
        }
        val updated = state.value.entry.copy(
            mealItems = state.value.entry.mealItems.filterNot { item -> item.id == id }
        )
        repository.save(updated)
        _state.update {
            it.copy(entry = updated, recordedDates = repository.recordedDates(), statusText = "食物记录已删除。", errorMessage = null)
        }
    }

    fun saveApiKey() {
        preferences.edit().putString("api_key", state.value.apiKeyInput.trim()).apply()
        _state.update { it.copy(statusText = "API Key 已保存。", errorMessage = null) }
    }

    fun saveEntry() {
        if (!state.value.canEditSelectedDate) {
            _state.update { it.copy(errorMessage = "以前的记录只能查看，不能修改。") }
            return
        }
        repository.save(state.value.entry)
        _state.update { it.copy(recordedDates = repository.recordedDates(), statusText = "${it.selectedDate} 已保存。", errorMessage = null) }
    }

    fun importPhoto(uri: Uri) {
        importPhotoForAngle(state.value.selectedPhotoAngle, uri)
    }

    fun recognizeFoodImageFromGallery(uri: Uri) {
        val currentState = state.value
        if (!currentState.canEditSelectedDate) {
            _state.update { it.copy(errorMessage = "以前的记录只能查看，不能修改。") }
            return
        }
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                val directory = File(getApplication<Application>().cacheDir, "food_recognition")
                directory.mkdirs()
                val target = File(directory, "food_gallery_${UUID.randomUUID()}.jpg")
                getApplication<Application>().contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "无法读取选择的食物照片" }
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                target.absolutePath
            }
            recognizeFoodPhotoWithAi(path, state.value.foodRecognitionSession?.supplementText.orEmpty())
        }
    }

    fun importPhotoForAngle(angle: BodyPhotoAngle, uri: Uri) {
        if (!state.value.canEditSelectedDate) {
            _state.update { it.copy(errorMessage = "以前的记录只能查看，不能修改照片。") }
            return
        }
        viewModelScope.launch {
            val current = state.value
            val path = withContext(Dispatchers.IO) {
                repository.importPhoto(
                    date = current.selectedDate,
                    angle = angle,
                    uri = uri,
                    oldPath = current.entry.photoPathFor(angle)
                )
            }
            val updated = current.entry.withPhotoPath(angle, path)
            repository.save(updated)
            _state.update {
                it.copy(
                    entry = updated,
                    selectedPhotoAngle = angle,
                    recordedDates = repository.recordedDates(),
                    photoComparisonDateOptions = repository.datesWithPhotosBefore(current.selectedDate),
                    statusText = "${angle.label} 已保存。",
                    errorMessage = null
                )
            }
        }
    }

    fun importProfileAvatar(uri: Uri) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                val directory = File(getApplication<Application>().filesDir, "profile")
                directory.mkdirs()
                val target = File(directory, "avatar_${UUID.randomUUID()}.jpg")
                getApplication<Application>().contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "无法读取选择的头像" }
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                state.value.profileAvatarPath?.let { oldPath ->
                    val oldFile = File(oldPath)
                    if (oldFile.parentFile?.name == "profile") oldFile.delete()
                }
                target.absolutePath
            }
            preferences.edit().putString("profile_avatar_path", path).apply()
            _state.update { it.copy(profileAvatarPath = path, statusText = "头像已保存。", errorMessage = null) }
        }
    }

    fun updateWelcomeMessage(value: String) {
        preferences.edit().putString("welcome_message", value).apply()
        _state.update { it.copy(welcomeMessage = value, statusText = "欢迎标语已保存。", errorMessage = null) }
    }

    fun importHomeBackground(uri: Uri) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                val directory = File(getApplication<Application>().filesDir, "profile")
                directory.mkdirs()
                val target = File(directory, "home_bg_${UUID.randomUUID()}.jpg")
                getApplication<Application>().contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "无法读取选择的主页背景" }
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                state.value.homeBackgroundPath?.let { oldPath ->
                    val oldFile = File(oldPath)
                    if (oldFile.parentFile?.name == "profile") oldFile.delete()
                }
                target.absolutePath
            }
            preferences.edit().putString("home_background_path", path).apply()
            _state.update { it.copy(homeBackgroundPath = path, statusText = "主页背景已保存。", errorMessage = null) }
        }
    }

    fun clearHomeBackground() {
        state.value.homeBackgroundPath?.let { path ->
            val file = File(path)
            if (file.parentFile?.name == "profile") file.delete()
        }
        preferences.edit().remove("home_background_path").remove("welcome_background_path").apply()
        _state.update { it.copy(homeBackgroundPath = null, statusText = "主页背景已清除。", errorMessage = null) }
    }

    fun deleteSelectedPhoto() {
        if (!state.value.canEditSelectedDate) {
            _state.update { it.copy(errorMessage = "以前的记录只能查看，不能删除照片。") }
            return
        }
        val current = state.value
        val oldPath = current.entry.photoPathFor(current.selectedPhotoAngle)
        if (oldPath.isNullOrBlank()) return
        repository.deletePhoto(oldPath)
        val updated = current.entry.withPhotoPath(current.selectedPhotoAngle, null)
        repository.save(updated)
            _state.update {
                it.copy(
                    entry = updated,
                    recordedDates = repository.recordedDates(),
                    photoComparisonDateOptions = repository.datesWithPhotosBefore(current.selectedDate),
                statusText = "${current.selectedPhotoAngle.label} 已删除。",
                errorMessage = null
            )
        }
    }

    fun comparePhotosWithAi() {
        val apiKey = state.value.apiKeyInput.trim()
        if (apiKey.isBlank()) {
            _state.update { it.copy(errorMessage = "请先在右上角设置里填写 API Key。") }
            return
        }
        val comparisonDate = state.value.selectedPhotoComparisonDate
        if (comparisonDate.isBlank()) {
            _state.update { it.copy(errorMessage = "请先选择一个有身材照片的往期日期。") }
            return
        }
        val current = state.value.entry
        val angle = state.value.selectedPhotoAngle
        val currentPhotoPath = current.photoPathFor(angle)
        if (currentPhotoPath.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "当前日期还没有${angle.label}，无法对比。") }
            return
        }
        val previous = repository.load(comparisonDate)
        val previousPhotoPath = previous.photoPathFor(angle)
        if (previousPhotoPath.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "$comparisonDate 没有${angle.label}，无法对比。") }
            return
        }
        if (state.value.canEditSelectedDate) {
            repository.save(current)
        }
        _state.update { it.copy(isComparingPhotos = true, statusText = "AI 正在对比身材照片...", errorMessage = null) }
        viewModelScope.launch {
            val profile = currentProfile()
            val result = withContext(Dispatchers.IO) {
                runCatching { aiClient.compareBodyPhotos(current, previous, angle, profile, apiKey) }
            }
            result.onSuccess { analysis ->
                val comparisonResult = PhotoComparisonResult(
                    previousDate = previous.date,
                    currentDate = current.date,
                    angle = angle,
                    previousPhotoPath = previousPhotoPath,
                    currentPhotoPath = currentPhotoPath,
                    analysis = analysis
                )
                _state.update {
                    it.copy(
                        photoComparisonResult = comparisonResult,
                        isComparingPhotos = false,
                        statusText = "身材照片对比完成。",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isComparingPhotos = false,
                        statusText = "身材照片对比失败。",
                        errorMessage = error.message.orEmpty().take(500)
                    )
                }
            }
        }
    }

    fun analyzeWithAi() {
        val apiKey = state.value.apiKeyInput.trim()
        if (apiKey.isBlank()) {
            _state.update { it.copy(errorMessage = "请先在右上角设置里填写 API Key。") }
            return
        }
        if (state.value.canEditSelectedDate) {
            repository.save(state.value.entry)
        }
        _state.update { it.copy(isAnalyzing = true, statusText = "AI 正在分析当天记录...", errorMessage = null) }
        viewModelScope.launch {
            val current = state.value.entry
            val profile = currentProfile()
            val result = withContext(Dispatchers.IO) {
                runCatching { aiClient.analyze(current, profile, apiKey) }
            }
            result.onSuccess { analysis ->
                val updated = current.copy(aiAnalysis = analysis)
                repository.save(updated)
                _state.update {
                    it.copy(
                        entry = updated,
                        recordedDates = repository.recordedDates(),
                        isAnalyzing = false,
                        statusText = "AI 分析完成。",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isAnalyzing = false,
                        statusText = "AI 分析失败。",
                        errorMessage = error.message.orEmpty().take(500)
                    )
                }
            }
        }
    }

    fun analyzeBodyGoalRecommendationIfReady() {
        val currentState = state.value
        val current = currentState.entry
        if (!currentState.canEditSelectedDate) return
        if (current.heightCm.isBlank() || current.weightKg.isBlank()) return
        val apiKey = currentState.apiKeyInput.trim()
        if (apiKey.isBlank()) {
            _state.update {
                it.copy(
                    statusText = "身体数据已保存。填写 API Key 后可生成今日摄入推荐。",
                    errorMessage = null
                )
            }
            return
        }
        repository.save(current)
        _state.update { it.copy(isAnalyzingBodyGoal = true, statusText = "AI 正在生成今日摄入目标...", errorMessage = null) }
        viewModelScope.launch {
            val profile = currentProfile()
            val previousDayPhotoAngles = runCatching {
                val previousDate = LocalDate.parse(current.date).minusDays(1).toString()
                repository.load(previousDate).uploadedPhotoAnglesLabel()
            }.getOrElse { "无" }
            val result = withContext(Dispatchers.IO) {
                runCatching { aiClient.recommendBodyGoal(current, profile, previousDayPhotoAngles, apiKey) }
            }
            result.onSuccess { recommendation ->
                val updated = state.value.entry.copy(bodyGoalRecommendation = recommendation)
                repository.save(updated)
                _state.update {
                    it.copy(
                        entry = updated,
                        recordedDates = repository.recordedDates(),
                        isAnalyzingBodyGoal = false,
                        statusText = "今日摄入目标已更新。",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isAnalyzingBodyGoal = false,
                        statusText = "今日摄入目标生成失败。",
                        errorMessage = error.message.orEmpty().take(5000)
                    )
                }
            }
        }
    }

    fun analyzeNutritionWithAi() {
        val apiKey = state.value.apiKeyInput.trim()
        if (apiKey.isBlank()) {
            _state.update { it.copy(errorMessage = "请先在右上角设置里填写 API Key。") }
            return
        }
        if (state.value.entry.mealItems.isEmpty()) {
            _state.update { it.copy(errorMessage = "请先添加至少一条饮食记录。") }
            return
        }
        if (state.value.canEditSelectedDate) {
            repository.save(state.value.entry)
        }
        _state.update { it.copy(isAnalyzingNutrition = true, statusText = "AI 正在估算当日营养摄入...", errorMessage = null) }
        viewModelScope.launch {
            val current = state.value.entry
            val profile = currentProfile()
            val previousDayPhotoAngles = runCatching {
                val previousDate = LocalDate.parse(current.date).minusDays(1).toString()
                repository.load(previousDate).uploadedPhotoAnglesLabel()
            }.getOrElse { "无" }
            val result = withContext(Dispatchers.IO) {
                runCatching { aiClient.analyzeNutrition(current, profile, previousDayPhotoAngles, apiKey) }
            }
            result.onSuccess { analysis ->
                val updated = current.copy(nutritionAnalysis = analysis)
                repository.save(updated)
                _state.update {
                    it.copy(
                        entry = updated,
                        recordedDates = repository.recordedDates(),
                        isAnalyzingNutrition = false,
                        statusText = "营养分析完成。",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isAnalyzingNutrition = false,
                        statusText = "营养分析失败。",
                        errorMessage = error.message.orEmpty().take(5000)
                    )
                }
            }
        }
    }

    fun analyzeSelectedMealNutritionWithAi() {
        val apiKey = state.value.apiKeyInput.trim()
        if (apiKey.isBlank()) {
            _state.update { it.copy(errorMessage = "请先在右上角设置里填写 API Key。") }
            return
        }
        val mealType = state.value.selectedMealViewType.mealType ?: MealType.Breakfast
        val current = state.value.entry
        if (current.mealItems.none { it.type == mealType }) {
            _state.update { it.copy(errorMessage = "请先添加${mealType.label}的食物记录。") }
            return
        }
        if (state.value.canEditSelectedDate) {
            repository.save(current)
        }
        _state.update { it.copy(isAnalyzingMealNutrition = true, statusText = "AI 正在分析${mealType.label}营养...", errorMessage = null) }
        viewModelScope.launch {
            val profile = currentProfile()
            val result = withContext(Dispatchers.IO) {
                runCatching { aiClient.analyzeMealNutrition(current, mealType, profile, apiKey) }
            }
            result.onSuccess { analysis ->
                val updated = current.withMealNutritionAnalysis(mealType, analysis)
                repository.save(updated)
                _state.update {
                    it.copy(
                        entry = updated,
                        recordedDates = repository.recordedDates(),
                        isAnalyzingMealNutrition = false,
                        statusText = "${mealType.label}营养分析完成。",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isAnalyzingMealNutrition = false,
                        statusText = "${mealType.label}营养分析失败。",
                        errorMessage = error.message.orEmpty().take(5000)
                    )
                }
            }
        }
    }

    private fun updateEntry(transform: (FitnessEntry) -> FitnessEntry) {
        _state.update { current ->
            if (!current.canEditSelectedDate) {
                return@update current.copy(errorMessage = "以前的记录只能查看，不能修改。")
            }
            current.copy(entry = transform(current.entry), errorMessage = null)
        }
    }

    private fun updateEntryAndSave(statusText: String, transform: (FitnessEntry) -> FitnessEntry) {
        _state.update { current ->
            if (!current.canEditSelectedDate) {
                return@update current.copy(errorMessage = "以前的记录只能查看，不能修改。")
            }
            val updatedEntry = transform(current.entry)
            repository.save(updatedEntry)
            current.copy(entry = updatedEntry, recordedDates = repository.recordedDates(), statusText = statusText, errorMessage = null)
        }
    }

    private fun updateBodyData(transform: (FitnessEntry) -> FitnessEntry) {
        _state.update { current ->
            if (!current.canEditSelectedDate) {
                return@update current.copy(errorMessage = "以前的记录只能查看，不能修改。")
            }
            val updatedEntry = transform(current.entry)
            repository.save(updatedEntry)
            current.copy(entry = updatedEntry, recordedDates = repository.recordedDates(), statusText = "身体数据已保存。", errorMessage = null)
        }
        analyzeBodyGoalRecommendationIfReady()
    }

    private fun currentProfile(): UserProfile {
        val current = state.value
        return UserProfile(
            name = current.profileName,
            birthday = current.profileBirthday,
            gender = current.profileGender,
            signature = current.profileSignature,
            avatarPath = current.profileAvatarPath
        )
    }

    private fun parseFoodPhotoItems(response: String, mealType: MealType): List<MealItem> {
        val root = runCatching { JSONObject(response.extractJsonObjectText()) }.getOrNull() ?: return emptyList()
        val items = root.optJSONArray("items") ?: JSONArray()
        val result = mutableListOf<MealItem>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val name = item.optString("food_name")
                .ifBlank { item.optString("foodName") }
                .ifBlank { item.optString("name") }
                .ifBlank { item.optString("食物名") }
                .trim()
            val gramsValue = when (val raw = item.opt("grams")) {
                is Number -> raw.toDouble()
                is String -> raw.filter { char -> char.isDigit() || char == '.' }.toDoubleOrNull()
                else -> null
            } ?: item.optString("克数")
                .filter { char -> char.isDigit() || char == '.' }
                .toDoubleOrNull()
            val grams = gramsValue
                ?.takeIf { it > 0.0 }
                ?.roundToInt()
                ?.toString()
                .orEmpty()
            if (name.isNotBlank() && grams.isNotBlank()) {
                result += MealItem(
                    id = UUID.randomUUID().toString(),
                    type = mealType,
                    foodName = name,
                    grams = grams
                )
            }
        }
        return result
    }

    private fun parseFoodPhotoNote(response: String): String {
        val root = runCatching { JSONObject(response.extractJsonObjectText()) }.getOrNull() ?: return ""
        return root.optString("note")
            .ifBlank { root.optString("说明") }
            .ifBlank { root.optString("uncertainty_note") }
            .trim()
    }

    private fun cleanupFoodRecognitionFiles(paths: List<String>) {
        paths.forEach { path ->
            val file = File(path)
            if (file.parentFile?.name == "food_recognition") {
                file.delete()
            }
        }
    }

    private fun String.extractJsonObjectText(): String {
        val trimmed = trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
    }

    private fun changeDate(date: String) {
        val canEdit = date == LocalDate.now().toString()
        _state.update {
            it.copy(
                selectedDate = date,
                entry = repository.load(date),
                canEditSelectedDate = canEdit,
                recordedDates = repository.recordedDates(),
                photoComparisonDateOptions = repository.datesWithPhotosBefore(date),
                selectedPhotoComparisonDate = repository.datesWithPhotosBefore(date).firstOrNull().orEmpty(),
                statusText = if (canEdit) "记录今天的训练、饮食和身体变化。" else "正在查看 $date 的历史记录。",
                errorMessage = null
            )
        }
    }
}
