package com.humsong.app.fitness

enum class BodyPhotoAngle(
    val key: String,
    val label: String
) {
    Front("front", "正面照"),
    Back("back", "后背照"),
    Left("left", "左侧照"),
    Right("right", "右侧照");

    companion object {
        fun fromKey(value: String): BodyPhotoAngle {
            return entries.firstOrNull { it.key == value } ?: Front
        }
    }
}

data class TrainingItem(
    val id: String,
    val period: TrainingPeriod = TrainingPeriod.Evening,
    val type: TrainingType = TrainingType.Strength,
    val name: String = "",
    val sets: String = "",
    val reps: String = "",
    val restSeconds: String = "",
    val weightMode: TrainingWeightMode = TrainingWeightMode.Bodyweight,
    val weightKg: String = "",
    val durationMinutes: String = "",
    val completedSets: Int = 0
) {
    val targetSets: Int
        get() = when (type) {
            TrainingType.Strength -> sets.toIntOrNull()?.coerceAtLeast(1) ?: 1
            TrainingType.Cardio -> 1
        }

    val completed: Boolean
        get() = completedSets >= targetSets

    val performed: Boolean
        get() = completedSets > 0

    fun weightLabel(): String {
        return when (weightMode) {
            TrainingWeightMode.Bodyweight -> "自重"
            TrainingWeightMode.Kg -> weightKg.ifBlank { "?" } + " kg"
        }
    }

    fun summaryLabel(): String {
        return when (type) {
            TrainingType.Strength -> "${name.ifBlank { "未命名力量训练" }}：${sets.ifBlank { "?" }} 组 x ${reps.ifBlank { "?" }} 次，重量 ${weightLabel()}，组间休息 ${restSeconds.ifBlank { "?" }} 秒"
            TrainingType.Cardio -> "${name.ifBlank { "未命名有氧" }}：${durationMinutes.ifBlank { "?" }} 分钟"
        }
    }

    fun actualSummaryLabel(): String {
        return when (type) {
            TrainingType.Strength -> "${name.ifBlank { "未命名力量训练" }}：实际 ${completedSets.coerceAtLeast(0)} 组 x ${reps.ifBlank { "?" }} 次，重量 ${weightLabel()}，计划 ${targetSets} 组"
            TrainingType.Cardio -> "${name.ifBlank { "未命名有氧" }}：实际完成，${durationMinutes.ifBlank { "?" }} 分钟"
        }
    }
}

enum class TrainingPeriod(
    val key: String,
    val label: String
) {
    Morning("morning", "早上"),
    Afternoon("afternoon", "下午"),
    Evening("evening", "晚上");

    companion object {
        fun fromKey(value: String): TrainingPeriod {
            return entries.firstOrNull { it.key == value } ?: Evening
        }
    }
}

enum class TrainingType(
    val key: String,
    val label: String
) {
    Strength("strength", "力量训练"),
    Cardio("cardio", "有氧训练");

    companion object {
        fun fromKey(value: String): TrainingType {
            return entries.firstOrNull { it.key == value } ?: Strength
        }
    }
}

enum class TrainingGoal(
    val key: String,
    val label: String
) {
    MuscleGain("muscle_gain", "增肌"),
    FatLoss("fat_loss", "减脂");

    companion object {
        fun fromKey(value: String): TrainingGoal {
            return entries.firstOrNull { it.key == value } ?: MuscleGain
        }
    }
}

enum class TrainingWeightMode(
    val key: String,
    val label: String
) {
    Bodyweight("bodyweight", "自重"),
    Kg("kg", "kg");

    companion object {
        fun fromKey(value: String): TrainingWeightMode {
            return entries.firstOrNull { it.key == value } ?: Bodyweight
        }
    }
}

enum class MealType(
    val key: String,
    val label: String
) {
    Breakfast("breakfast", "早餐"),
    Lunch("lunch", "中餐"),
    Dinner("dinner", "晚餐");

    companion object {
        fun fromKey(value: String): MealType {
            return entries.firstOrNull { it.key == value } ?: Breakfast
        }
    }
}

data class MealItem(
    val id: String,
    val type: MealType = MealType.Breakfast,
    val foodName: String = "",
    val grams: String = ""
)

data class TrainingTemplate(
    val id: String,
    val templateName: String = "",
    val category: String = "",
    val item: TrainingItem = TrainingItem(id = "")
)

data class TrainingTemplateGroup(
    val id: String,
    val templateName: String = "",
    val category: String = "",
    val items: List<TrainingItem> = emptyList()
)

data class MealTemplate(
    val id: String,
    val templateName: String = "",
    val category: String = "",
    val item: MealItem = MealItem(id = "")
)

data class MealTemplateGroup(
    val id: String,
    val templateName: String = "",
    val category: String = "",
    val items: List<MealItem> = emptyList()
)

data class FitnessEntry(
    val date: String,
    val heightCm: String = "",
    val weightKg: String = "",
    val trainingGoal: TrainingGoal = TrainingGoal.MuscleGain,
    val trainingItems: List<TrainingItem> = emptyList(),
    val mealItems: List<MealItem> = emptyList(),
    val frontPhotoPath: String? = null,
    val backPhotoPath: String? = null,
    val leftPhotoPath: String? = null,
    val rightPhotoPath: String? = null,
    val aiAnalysis: String = "",
    val nutritionAnalysis: String = "",
    val breakfastNutritionAnalysis: String = "",
    val lunchNutritionAnalysis: String = "",
    val dinnerNutritionAnalysis: String = "",
    val photoComparisonAnalysis: String = "",
    val bodyGoalRecommendation: String = ""
) {
    fun photoPathFor(angle: BodyPhotoAngle): String? {
        return when (angle) {
            BodyPhotoAngle.Front -> frontPhotoPath
            BodyPhotoAngle.Back -> backPhotoPath
            BodyPhotoAngle.Left -> leftPhotoPath
            BodyPhotoAngle.Right -> rightPhotoPath
        }
    }

    fun withPhotoPath(angle: BodyPhotoAngle, path: String?): FitnessEntry {
        return when (angle) {
            BodyPhotoAngle.Front -> copy(frontPhotoPath = path)
            BodyPhotoAngle.Back -> copy(backPhotoPath = path)
            BodyPhotoAngle.Left -> copy(leftPhotoPath = path)
            BodyPhotoAngle.Right -> copy(rightPhotoPath = path)
        }
    }

    fun summaryForAi(): String {
        val uploadedAngles = BodyPhotoAngle.entries
            .filter { !photoPathFor(it).isNullOrBlank() }
            .joinToString("、") { it.label }
            .ifBlank { "无" }
        val performedTrainingItems = trainingItems.filter { it.performed }
        val trainingSummary = performedTrainingItems.joinToString("\n") {
            "- ${it.period.label} ${it.type.label}：${it.actualSummaryLabel()}"
        }.ifBlank { "没有实际完成训练；未左滑完成的训练计划不作为实际训练记录。" }
        val mealsSummary = mealItems.joinToString("\n") {
            "- ${it.type.label}：${it.foodName.ifBlank { "未填写食物" }}，${it.grams.ifBlank { "?" }} 克"
        }.ifBlank { "未填写" }

        return """
            日期：$date
            身高：${heightCm.ifBlank { "未填写" }} cm
            体重：${weightKg.ifBlank { "未填写" }} kg
            当日训练目标：${trainingGoal.label}
            当天实际训练：
            $trainingSummary
            三餐：
            $mealsSummary
            已上传身材照片角度：$uploadedAngles
        """.trimIndent()
    }

    fun mealsSummaryForNutrition(): String {
        return mealItems.joinToString("\n") {
            "- ${it.type.label}：${it.foodName.ifBlank { "未填写食物" }}，${it.grams.ifBlank { "?" }} 克"
        }.ifBlank { "未填写" }
    }

    fun mealSummaryForNutrition(type: MealType): String {
        return mealItems
            .filter { it.type == type }
            .joinToString("\n") {
                "- ${it.foodName.ifBlank { "未填写食物" }}，${it.grams.ifBlank { "?" }} 克"
            }
            .ifBlank { "未填写" }
    }

    fun mealNutritionAnalysisFor(type: MealType): String {
        return when (type) {
            MealType.Breakfast -> breakfastNutritionAnalysis
            MealType.Lunch -> lunchNutritionAnalysis
            MealType.Dinner -> dinnerNutritionAnalysis
        }
    }

    fun withMealNutritionAnalysis(type: MealType, analysis: String): FitnessEntry {
        return when (type) {
            MealType.Breakfast -> copy(breakfastNutritionAnalysis = analysis)
            MealType.Lunch -> copy(lunchNutritionAnalysis = analysis)
            MealType.Dinner -> copy(dinnerNutritionAnalysis = analysis)
        }
    }

    fun trainingSummaryForNutrition(): String {
        val performedTrainingItems = trainingItems.filter { it.performed }
        val itemsSummary = performedTrainingItems.joinToString("\n") {
            "- ${it.period.label} ${it.type.label}：${it.actualSummaryLabel()}"
        }.ifBlank { "当前没有实际完成训练；未左滑完成的训练计划不作为实际训练记录。" }
        return "当日训练目标：${trainingGoal.label}\n当天实际训练：\n$itemsSummary"
    }

    fun uploadedPhotoAnglesLabel(): String {
        return BodyPhotoAngle.entries
            .filter { !photoPathFor(it).isNullOrBlank() }
            .joinToString("、") { it.label }
            .ifBlank { "无" }
    }

    fun hasAnyPhoto(): Boolean {
        return BodyPhotoAngle.entries.any { !photoPathFor(it).isNullOrBlank() }
    }
}
