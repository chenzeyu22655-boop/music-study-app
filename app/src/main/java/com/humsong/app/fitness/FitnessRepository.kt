package com.humsong.app.fitness

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class StorageUsage(
    val recordsBytes: Long = 0L,
    val bodyPhotoBytes: Long = 0L,
    val tempBytes: Long = 0L,
    val profileBytes: Long = 0L
) {
    val totalBytes: Long get() = recordsBytes + bodyPhotoBytes + tempBytes + profileBytes
}

data class StorageCleanupResult(
    val freedBytes: Long = 0L,
    val affectedCount: Int = 0
)

class FitnessRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences("fitness_entries", Context.MODE_PRIVATE)
    private val templatePreferences = context.getSharedPreferences("fitness_templates", Context.MODE_PRIVATE)

    fun ensureDefaultTemplatesSeeded() {
        if (templatePreferences.getBoolean("default_templates_seeded_v1", false)) return
        val hasAnyTemplate = loadTrainingTemplates().isNotEmpty() ||
            loadTrainingTemplateGroups().isNotEmpty() ||
            loadMealTemplates().isNotEmpty() ||
            loadMealTemplateGroups().isNotEmpty()
        if (!hasAnyTemplate) {
            saveTrainingTemplates(defaultTrainingTemplates())
            saveTrainingTemplateGroups(defaultTrainingTemplateGroups())
            saveMealTemplates(defaultMealTemplates())
            saveMealTemplateGroups(defaultMealTemplateGroups())
        }
        templatePreferences.edit().putBoolean("default_templates_seeded_v1", true).apply()
    }

    fun load(date: String): FitnessEntry {
        val raw = preferences.getString(date, null) ?: return FitnessEntry(date = date)
        return runCatching {
            val json = JSONObject(raw)
            FitnessEntry(
                date = date,
                heightCm = json.optString("heightCm"),
                weightKg = json.optString("weightKg"),
                trainingGoal = TrainingGoal.fromKey(json.optString("trainingGoal")),
                trainingItems = readTrainingItems(json),
                mealItems = readMealItems(json),
                frontPhotoPath = json.optString("frontPhotoPath")
                    .ifBlank { json.optString("photoPath") }
                    .ifBlank { null },
                backPhotoPath = json.optString("backPhotoPath").ifBlank { null },
                leftPhotoPath = json.optString("leftPhotoPath").ifBlank { null },
                rightPhotoPath = json.optString("rightPhotoPath").ifBlank { null },
                aiAnalysis = json.optString("aiAnalysis"),
                nutritionAnalysis = json.optString("nutritionAnalysis"),
                breakfastNutritionAnalysis = json.optString("breakfastNutritionAnalysis"),
                lunchNutritionAnalysis = json.optString("lunchNutritionAnalysis"),
                dinnerNutritionAnalysis = json.optString("dinnerNutritionAnalysis"),
                photoComparisonAnalysis = json.optString("photoComparisonAnalysis"),
                bodyGoalRecommendation = json.optString("bodyGoalRecommendation")
            )
        }.getOrElse {
            FitnessEntry(date = date)
        }
    }

    fun save(entry: FitnessEntry) {
        val json = JSONObject()
            .put("heightCm", entry.heightCm)
            .put("weightKg", entry.weightKg)
            .put("trainingGoal", entry.trainingGoal.key)
            .put("trainingItems", writeTrainingItems(entry.trainingItems))
            .put("mealItems", writeMealItems(entry.mealItems))
            .put("frontPhotoPath", entry.frontPhotoPath.orEmpty())
            .put("backPhotoPath", entry.backPhotoPath.orEmpty())
            .put("leftPhotoPath", entry.leftPhotoPath.orEmpty())
            .put("rightPhotoPath", entry.rightPhotoPath.orEmpty())
            .put("aiAnalysis", entry.aiAnalysis)
            .put("nutritionAnalysis", entry.nutritionAnalysis)
            .put("breakfastNutritionAnalysis", entry.breakfastNutritionAnalysis)
            .put("lunchNutritionAnalysis", entry.lunchNutritionAnalysis)
            .put("dinnerNutritionAnalysis", entry.dinnerNutritionAnalysis)
            .put("photoComparisonAnalysis", entry.photoComparisonAnalysis)
            .put("bodyGoalRecommendation", entry.bodyGoalRecommendation)
        preferences.edit().putString(entry.date, json.toString()).apply()
    }

    fun delete(date: String) {
        val entry = load(date)
        deletePhoto(entry.frontPhotoPath)
        deletePhoto(entry.backPhotoPath)
        deletePhoto(entry.leftPhotoPath)
        deletePhoto(entry.rightPhotoPath)
        preferences.edit().remove(date).apply()
    }

    fun datesWithPhotosBefore(date: String): List<String> {
        return preferences.all.keys
            .filter { key -> key < date && runCatching { load(key).hasAnyPhoto() }.getOrDefault(false) }
            .sortedDescending()
    }

    fun recordedDates(): List<String> {
        return preferences.all.keys
            .filter { key -> runCatching { java.time.LocalDate.parse(key) }.isSuccess }
            .sorted()
    }

    fun loadTrainingTemplates(): List<TrainingTemplate> {
        val raw = templatePreferences.getString("training_templates", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val json = array.optJSONObject(index) ?: return@mapNotNull null
                val itemJson = json.optJSONObject("item") ?: JSONObject()
                TrainingTemplate(
                    id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                    templateName = json.optString("templateName"),
                    category = json.optString("category"),
                    item = readTrainingItem(itemJson)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveTrainingTemplates(templates: List<TrainingTemplate>) {
        val array = JSONArray()
        templates.forEach { template ->
            array.put(
                JSONObject()
                    .put("id", template.id)
                    .put("templateName", template.templateName)
                    .put("category", template.category)
                    .put("item", writeTrainingItem(template.item.copy(id = "")))
            )
        }
        templatePreferences.edit().putString("training_templates", array.toString()).apply()
    }

    fun loadTrainingTemplateGroups(): List<TrainingTemplateGroup> {
        val raw = templatePreferences.getString("training_template_groups", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val json = array.optJSONObject(index) ?: return@mapNotNull null
                val itemsArray = json.optJSONArray("items") ?: JSONArray()
                TrainingTemplateGroup(
                    id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                    templateName = json.optString("templateName"),
                    category = json.optString("category"),
                    items = (0 until itemsArray.length()).mapNotNull { itemIndex ->
                        itemsArray.optJSONObject(itemIndex)?.let(::readTrainingItem)
                    }
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveTrainingTemplateGroups(groups: List<TrainingTemplateGroup>) {
        val array = JSONArray()
        groups.forEach { group ->
            val itemsArray = JSONArray()
            group.items.forEach { item -> itemsArray.put(writeTrainingItem(item.copy(id = ""))) }
            array.put(
                JSONObject()
                    .put("id", group.id)
                    .put("templateName", group.templateName)
                    .put("category", group.category)
                    .put("items", itemsArray)
            )
        }
        templatePreferences.edit().putString("training_template_groups", array.toString()).apply()
    }

    fun loadMealTemplates(): List<MealTemplate> {
        val raw = templatePreferences.getString("meal_templates", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val json = array.optJSONObject(index) ?: return@mapNotNull null
                val itemJson = json.optJSONObject("item") ?: JSONObject()
                MealTemplate(
                    id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                    templateName = json.optString("templateName"),
                    category = json.optString("category"),
                    item = readMealItem(itemJson)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveMealTemplates(templates: List<MealTemplate>) {
        val array = JSONArray()
        templates.forEach { template ->
            array.put(
                JSONObject()
                    .put("id", template.id)
                    .put("templateName", template.templateName)
                    .put("category", template.category)
                    .put("item", writeMealItem(template.item.copy(id = "")))
            )
        }
        templatePreferences.edit().putString("meal_templates", array.toString()).apply()
    }

    fun loadMealTemplateGroups(): List<MealTemplateGroup> {
        val raw = templatePreferences.getString("meal_template_groups", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val json = array.optJSONObject(index) ?: return@mapNotNull null
                val itemsArray = json.optJSONArray("items") ?: JSONArray()
                MealTemplateGroup(
                    id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                    templateName = json.optString("templateName"),
                    category = json.optString("category"),
                    items = (0 until itemsArray.length()).mapNotNull { itemIndex ->
                        itemsArray.optJSONObject(itemIndex)?.let(::readMealItem)
                    }
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveMealTemplateGroups(groups: List<MealTemplateGroup>) {
        val array = JSONArray()
        groups.forEach { group ->
            val itemsArray = JSONArray()
            group.items.forEach { item -> itemsArray.put(writeMealItem(item.copy(id = ""))) }
            array.put(
                JSONObject()
                    .put("id", group.id)
                    .put("templateName", group.templateName)
                    .put("category", group.category)
                    .put("items", itemsArray)
            )
        }
        templatePreferences.edit().putString("meal_template_groups", array.toString()).apply()
    }

    private fun readTrainingItems(json: JSONObject): List<TrainingItem> {
        val array = json.optJSONArray("trainingItems")
        if (array != null) {
            return (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                readTrainingItem(item)
            }
        }
        val oldTraining = json.optString("training")
        return if (oldTraining.isBlank()) {
            emptyList()
        } else {
            listOf(TrainingItem(id = UUID.randomUUID().toString(), name = oldTraining))
        }
    }

    private fun readMealItems(json: JSONObject): List<MealItem> {
        val array = json.optJSONArray("mealItems")
        if (array != null) {
            return (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                readMealItem(item)
            }
        }
        return listOf(
            MealType.Breakfast to json.optString("breakfast"),
            MealType.Lunch to json.optString("lunch"),
            MealType.Dinner to json.optString("dinner")
        ).mapNotNull { (type, foodName) ->
            if (foodName.isBlank()) null
            else MealItem(id = UUID.randomUUID().toString(), type = type, foodName = foodName)
        }
    }

    private fun writeTrainingItems(items: List<TrainingItem>): JSONArray {
        val array = JSONArray()
        items.forEach { item ->
            array.put(writeTrainingItem(item))
        }
        return array
    }

    private fun readTrainingItem(item: JSONObject): TrainingItem {
        return TrainingItem(
            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
            period = TrainingPeriod.fromKey(item.optString("period")),
            type = TrainingType.fromKey(item.optString("type")),
            name = item.optString("name"),
            sets = item.optString("sets"),
            reps = item.optString("reps"),
            restSeconds = item.optString("restSeconds"),
            weightMode = TrainingWeightMode.fromKey(item.optString("weightMode")),
            weightKg = item.optString("weightKg"),
            durationMinutes = item.optString("durationMinutes"),
            completedSets = item.optInt(
                "completedSets",
                if (item.optBoolean("completed", false)) {
                    item.optString("sets").toIntOrNull()?.coerceAtLeast(1) ?: 1
                } else {
                    0
                }
            )
        )
    }

    private fun writeTrainingItem(item: TrainingItem): JSONObject {
        return JSONObject()
            .put("id", item.id)
            .put("period", item.period.key)
            .put("type", item.type.key)
            .put("name", item.name)
            .put("sets", item.sets)
            .put("reps", item.reps)
            .put("restSeconds", item.restSeconds)
            .put("weightMode", item.weightMode.key)
            .put("weightKg", item.weightKg)
            .put("durationMinutes", item.durationMinutes)
            .put("completedSets", item.completedSets)
    }

    private fun writeMealItems(items: List<MealItem>): JSONArray {
        val array = JSONArray()
        items.forEach { item ->
            array.put(writeMealItem(item))
        }
        return array
    }

    private fun readMealItem(item: JSONObject): MealItem {
        return MealItem(
            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
            type = MealType.fromKey(item.optString("type")),
            foodName = item.optString("foodName"),
            grams = item.optString("grams")
        )
    }

    private fun writeMealItem(item: MealItem): JSONObject {
        return JSONObject()
            .put("id", item.id)
            .put("type", item.type.key)
            .put("foodName", item.foodName)
            .put("grams", item.grams)
    }

    private fun defaultTrainingTemplates(): List<TrainingTemplate> {
        fun strength(name: String, sets: String, reps: String, rest: String): TrainingTemplate {
            return TrainingTemplate(
                id = UUID.randomUUID().toString(),
                templateName = name,
                category = "新手力量",
                item = TrainingItem(
                    id = "",
                    type = TrainingType.Strength,
                    name = name,
                    sets = sets,
                    reps = reps,
                    restSeconds = rest,
                    weightMode = TrainingWeightMode.Bodyweight
                )
            )
        }
        fun cardio(name: String, minutes: String): TrainingTemplate {
            return TrainingTemplate(
                id = UUID.randomUUID().toString(),
                templateName = name,
                category = "新手有氧",
                item = TrainingItem(
                    id = "",
                    type = TrainingType.Cardio,
                    name = name,
                    durationMinutes = minutes
                )
            )
        }
        return listOf(
            strength("俯卧撑", "3", "8", "90"),
            strength("徒手深蹲", "3", "12", "90"),
            strength("臀桥", "3", "12", "60"),
            strength("卷腹", "3", "12", "60"),
            cardio("快走", "20"),
            cardio("慢跑", "15"),
            cardio("跳绳", "10")
        )
    }

    private fun defaultTrainingTemplateGroups(): List<TrainingTemplateGroup> {
        fun strength(name: String, sets: String, reps: String, rest: String): TrainingItem {
            return TrainingItem(
                id = "",
                type = TrainingType.Strength,
                name = name,
                sets = sets,
                reps = reps,
                restSeconds = rest,
                weightMode = TrainingWeightMode.Bodyweight
            )
        }
        fun cardio(name: String, minutes: String): TrainingItem {
            return TrainingItem(
                id = "",
                type = TrainingType.Cardio,
                name = name,
                durationMinutes = minutes
            )
        }
        return listOf(
            TrainingTemplateGroup(
                id = UUID.randomUUID().toString(),
                templateName = "新手全身训练",
                category = "新手计划",
                items = listOf(
                    strength("俯卧撑", "3", "8", "90"),
                    strength("徒手深蹲", "3", "12", "90"),
                    strength("臀桥", "3", "12", "60"),
                    strength("卷腹", "3", "12", "60")
                )
            ),
            TrainingTemplateGroup(
                id = UUID.randomUUID().toString(),
                templateName = "轻有氧燃脂",
                category = "新手计划",
                items = listOf(
                    cardio("快走", "20"),
                    cardio("跳绳", "10")
                )
            )
        )
    }

    private fun defaultMealTemplates(): List<MealTemplate> {
        fun meal(name: String, grams: String, category: String): MealTemplate {
            return MealTemplate(
                id = UUID.randomUUID().toString(),
                templateName = name,
                category = category,
                item = MealItem(id = "", foodName = name, grams = grams)
            )
        }
        return listOf(
            meal("米饭", "100", "主食"),
            meal("燕麦", "50", "主食"),
            meal("全麦面包", "60", "主食"),
            meal("鸡胸肉", "100", "蛋白质"),
            meal("鸡蛋", "50", "蛋白质"),
            meal("牛奶", "250", "蛋白质"),
            meal("西兰花", "100", "蔬菜水果"),
            meal("苹果", "150", "蔬菜水果")
        )
    }

    private fun defaultMealTemplateGroups(): List<MealTemplateGroup> {
        fun item(name: String, grams: String): MealItem {
            return MealItem(id = "", foodName = name, grams = grams)
        }
        return listOf(
            MealTemplateGroup(
                id = UUID.randomUUID().toString(),
                templateName = "基础早餐",
                category = "新手餐食",
                items = listOf(
                    item("燕麦", "50"),
                    item("牛奶", "250"),
                    item("鸡蛋", "50")
                )
            ),
            MealTemplateGroup(
                id = UUID.randomUUID().toString(),
                templateName = "训练后简餐",
                category = "新手餐食",
                items = listOf(
                    item("米饭", "150"),
                    item("鸡胸肉", "120"),
                    item("西兰花", "100")
                )
            )
        )
    }

    fun importPhoto(date: String, angle: BodyPhotoAngle, uri: Uri, oldPath: String?): String {
        val directory = File(context.filesDir, "body_photos")
        directory.mkdirs()
        val target = File(directory, "body_${date}_${angle.key}_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取选择的图片" }
            val bytes = input.readBytes()
            writeCompressedImage(bytes, target)
        }
        deletePhoto(oldPath)
        return target.absolutePath
    }

    fun deletePhoto(path: String?) {
        if (path.isNullOrBlank()) return
        val photoDirectory = File(context.filesDir, "body_photos").canonicalFile
        val target = File(path).canonicalFile
        if (target.path.startsWith(photoDirectory.path) && target.isFile) {
            target.delete()
        }
    }

    fun storageUsage(): StorageUsage {
        val recordsBytes = preferences.all.values.sumOf { value ->
            (value as? String)?.toByteArray(Charsets.UTF_8)?.size?.toLong() ?: 0L
        }
        return StorageUsage(
            recordsBytes = recordsBytes,
            bodyPhotoBytes = directorySize(File(context.filesDir, "body_photos")),
            tempBytes = directorySize(File(context.cacheDir, "food_recognition")),
            profileBytes = directorySize(File(context.filesDir, "profile"))
        )
    }

    fun clearFoodRecognitionTempFiles(): StorageCleanupResult {
        val directory = File(context.cacheDir, "food_recognition")
        val before = directorySize(directory)
        val count = directory.listFiles()?.count { it.isFile } ?: 0
        directory.deleteRecursively()
        directory.mkdirs()
        return StorageCleanupResult(freedBytes = before, affectedCount = count)
    }

    fun compressAllBodyPhotos(): StorageCleanupResult {
        val dates = recordedDates()
        var freed = 0L
        var count = 0
        dates.forEach { date ->
            val entry = load(date)
            BodyPhotoAngle.entries.forEach { angle ->
                val path = entry.photoPathFor(angle).orEmpty()
                val file = File(path)
                if (file.isFile && file.parentFile?.name == "body_photos") {
                    val before = file.length()
                    val bytes = file.readBytes()
                    writeCompressedImage(bytes, file)
                    val after = file.length()
                    if (after < before) freed += before - after
                    count += 1
                }
            }
        }
        return StorageCleanupResult(freedBytes = freed, affectedCount = count)
    }

    fun archiveLongAiText(maxLength: Int = 700): StorageCleanupResult {
        var freed = 0L
        var count = 0
        recordedDates().forEach { date ->
            val oldRaw = preferences.getString(date, null).orEmpty()
            val entry = load(date)
            val updated = entry.copy(
                aiAnalysis = entry.aiAnalysis.archiveText(maxLength),
                nutritionAnalysis = entry.nutritionAnalysis.archiveText(maxLength),
                breakfastNutritionAnalysis = entry.breakfastNutritionAnalysis.archiveText(maxLength),
                lunchNutritionAnalysis = entry.lunchNutritionAnalysis.archiveText(maxLength),
                dinnerNutritionAnalysis = entry.dinnerNutritionAnalysis.archiveText(maxLength),
                photoComparisonAnalysis = entry.photoComparisonAnalysis.archiveText(maxLength)
            )
            if (updated != entry) {
                save(updated)
                val newRaw = preferences.getString(date, null).orEmpty()
                freed += (oldRaw.toByteArray(Charsets.UTF_8).size - newRaw.toByteArray(Charsets.UTF_8).size).coerceAtLeast(0).toLong()
                count += 1
            }
        }
        return StorageCleanupResult(freedBytes = freed, affectedCount = count)
    }

    fun writeCompressedImage(bytes: ByteArray, target: File) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        val longest = maxOf(options.outWidth, options.outHeight).coerceAtLeast(1)
        val sampleSize = calculateInSampleSize(longest, 1600)
        val bitmap = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        )
        if (bitmap == null) {
            target.writeBytes(bytes)
            return
        }
        val scaled = bitmap.scaleDownToMaxSide(1600)
        target.outputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, output)
        }
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
    }

    private fun calculateInSampleSize(longest: Int, target: Int): Int {
        var sample = 1
        while (longest / sample > target * 2) sample *= 2
        return sample
    }

    private fun Bitmap.scaleDownToMaxSide(maxSide: Int): Bitmap {
        val longest = maxOf(width, height)
        if (longest <= maxSide) return this
        val ratio = maxSide.toFloat() / longest.toFloat()
        val nextWidth = (width * ratio).toInt().coerceAtLeast(1)
        val nextHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, nextWidth, nextHeight, true)
    }

    private fun directorySize(directory: File): Long {
        if (!directory.exists()) return 0L
        return directory.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    private fun String.archiveText(maxLength: Int): String {
        val cleaned = lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
        if (cleaned.length <= maxLength) return this
        return "【已归档摘要】\n" + cleaned.take(maxLength).trimEnd() + "\n..."
    }
}
