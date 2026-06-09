package com.humsong.app.ai

import com.humsong.app.fitness.FitnessEntry
import com.humsong.app.fitness.BodyPhotoAngle
import com.humsong.app.fitness.MealType
import com.humsong.app.fitness.UserProfile
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONObject

class FitnessAiClient {
    fun analyze(entry: FitnessEntry, profile: UserProfile, apiKey: String): String {
        return requestAi(apiKey = apiKey, prompt = buildPrompt(entry, profile), maxTokens = 1200)
    }

    fun analyzeNutrition(
        entry: FitnessEntry,
        profile: UserProfile,
        previousDayPhotoAngles: String,
        apiKey: String
    ): String {
        return requestAi(
            apiKey = apiKey,
            prompt = buildNutritionPrompt(entry, profile, previousDayPhotoAngles),
            maxTokens = 1800,
            systemPrompt = NutritionSystemPrompt
        )
    }

    fun analyzeMealNutrition(
        entry: FitnessEntry,
        mealType: MealType,
        profile: UserProfile,
        apiKey: String
    ): String {
        return requestAi(
            apiKey = apiKey,
            prompt = buildMealNutritionPrompt(entry, mealType, profile),
            maxTokens = 1400,
            systemPrompt = NutritionSystemPrompt
        )
    }

    fun recommendBodyGoal(
        entry: FitnessEntry,
        profile: UserProfile,
        previousDayPhotoAngles: String,
        apiKey: String
    ): String {
        return requestAi(
            apiKey = apiKey,
            prompt = buildBodyGoalRecommendationPrompt(entry, profile, previousDayPhotoAngles),
            maxTokens = 1200,
            systemPrompt = NutritionSystemPrompt
        )
    }

    fun compareBodyPhotos(
        currentEntry: FitnessEntry,
        previousEntry: FitnessEntry,
        angle: BodyPhotoAngle,
        profile: UserProfile,
        apiKey: String
    ): String {
        return requestAiWithImages(
            apiKey = apiKey,
            prompt = buildPhotoComparisonPrompt(currentEntry, previousEntry, angle, profile),
            imagePaths = comparisonImagePaths(previousEntry, currentEntry, angle),
            maxTokens = 1300
        )
    }

    fun recognizeFoodPhoto(
        photoPaths: List<String>,
        mealType: MealType,
        apiKey: String,
        supplementText: String = ""
    ): String {
        return requestAiWithImages(
            apiKey = apiKey,
            prompt = buildFoodPhotoPrompt(mealType, supplementText),
            imagePaths = photoPaths.mapIndexed { index, path -> LabeledImagePath("food photo ${index + 1}", path) },
            maxTokens = 900
        )
    }

    private fun requestAi(
        apiKey: String,
        prompt: String,
        maxTokens: Int,
        systemPrompt: String = DefaultSystemPrompt
    ): String {
        val body = JSONObject()
            .put("model", "mimo-v2.5")
            .put("messages", JSONArray()
                .put(JSONObject()
                    .put("role", "system")
                    .put("content", systemPrompt))
                .put(JSONObject()
                    .put("role", "user")
                    .put("content", prompt)))
            .put("max_completion_tokens", maxTokens)
            .put("temperature", 0.4)
            .put("top_p", 0.9)
            .put("stream", false)
            .put("thinking", JSONObject().put("type", "disabled"))

        val responseText = postWithRetry(
            apiKey = apiKey,
            body = body,
            readTimeoutMs = 120_000,
            errorPrefix = "AI 请求失败"
        )
        return parseAiContent(responseText, "AI 没有返回分析内容。")
    }

    private fun requestAiWithImages(
        apiKey: String,
        prompt: String,
        imagePaths: List<LabeledImagePath>,
        maxTokens: Int
    ): String {
        val content = JSONArray()
            .put(JSONObject()
                .put("type", "text")
                .put("text", prompt))
        imagePaths.forEach { image ->
            val file = File(image.path)
            if (file.isFile) {
                content
                    .put(JSONObject()
                        .put("type", "text")
                        .put("text", image.label))
                    .put(JSONObject()
                        .put("type", "image_url")
                        .put("image_url", JSONObject().put("url", file.toDataUrl())))
            }
        }
        val body = JSONObject()
            .put("model", "mimo-v2.5")
            .put("messages", JSONArray()
                .put(JSONObject()
                    .put("role", "system")
                    .put("content", "你是一个谨慎、实用的健身体型变化分析助手。可以根据用户提供的照片做视觉对比，但不要做医疗诊断。"))
                .put(JSONObject()
                    .put("role", "user")
                    .put("content", content)))
            .put("max_completion_tokens", maxTokens)
            .put("temperature", 0.35)
            .put("top_p", 0.9)
            .put("stream", false)
            .put("thinking", JSONObject().put("type", "disabled"))

        val responseText = postWithRetry(
            apiKey = apiKey,
            body = body,
            readTimeoutMs = 180_000,
            errorPrefix = "AI 图片对比请求失败"
        )
        return parseAiContent(responseText, "AI 没有返回照片对比内容。")
    }

    private fun postWithRetry(
        apiKey: String,
        body: JSONObject,
        readTimeoutMs: Int,
        errorPrefix: String
    ): String {
        var lastError: Throwable? = null
        repeat(MaxAttempts) { attempt ->
            try {
                return postOnce(apiKey, body, readTimeoutMs, errorPrefix)
            } catch (error: IOException) {
                lastError = error
            } catch (error: IllegalStateException) {
                lastError = error
                if (!error.message.orEmpty().contains("HTTP 429") &&
                    !error.message.orEmpty().contains("HTTP 500") &&
                    !error.message.orEmpty().contains("HTTP 502") &&
                    !error.message.orEmpty().contains("HTTP 503") &&
                    !error.message.orEmpty().contains("HTTP 504")
                ) {
                    throw error
                }
            }
            if (attempt < MaxAttempts - 1) {
                Thread.sleep(900L * (attempt + 1))
            }
        }
        throw IllegalStateException("${errorPrefix}：${lastError?.message.orEmpty()}")
    }

    private fun postOnce(
        apiKey: String,
        body: JSONObject,
        readTimeoutMs: Int,
        errorPrefix: String
    ): String {
        val connection = (URL(ApiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = readTimeoutMs
            doOutput = true
            setRequestProperty("api-key", apiKey)
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
        }
        return try {
            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(Charsets.UTF_8))
            }
            if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                val error = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                error("$errorPrefix：HTTP ${connection.responseCode} ${extractApiError(error).ifBlank { error.orEmpty() }}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseAiContent(responseText: String, emptyMessage: String): String {
        val json = JSONObject(responseText)
        val message = json
            .optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?: return extractApiError(responseText).ifBlank {
                "$emptyMessage\n\n接口响应（已隐藏推理字段）：\n${sanitizeDebugResponse(responseText)}"
            }
        val content = message.opt("content")
        val contentText = when (content) {
            is String -> content
            is JSONArray -> (0 until content.length()).joinToString("\n") { index ->
                val item = content.optJSONObject(index)
                item?.optString("text").orEmpty().ifBlank { item?.optString("content").orEmpty() }
            }
            else -> ""
        }
        if (contentText.isNotBlank()) return contentText

        val apiError = extractApiError(responseText)
        if (apiError.isNotBlank()) return apiError

        val hasReasoningOnly = message.optString("reasoning_content").isNotBlank() ||
            message.optString("reasoningContent").isNotBlank()
        val reason = if (hasReasoningOnly) {
            "模型只返回了推理过程，没有返回最终 JSON/正文。"
        } else {
            emptyMessage
        }
        return "$reason\n\n接口响应（已隐藏推理字段）：\n${sanitizeDebugResponse(responseText)}"
    }

    private fun sanitizeDebugResponse(raw: String): String {
        return runCatching {
            val json = JSONObject(raw)
            val choices = json.optJSONArray("choices")
            if (choices != null) {
                for (index in 0 until choices.length()) {
                    val message = choices.optJSONObject(index)?.optJSONObject("message")
                    message?.remove("reasoning_content")
                    message?.remove("reasoningContent")
                }
            }
            json.toString(2).take(MaxDebugResponseChars)
        }.getOrElse {
            raw.take(MaxDebugResponseChars)
        }
    }

    private fun extractApiError(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return runCatching {
            val json = JSONObject(raw)
            val error = json.opt("error")
            when (error) {
                is JSONObject -> error.optString("message").ifBlank { error.toString() }
                is String -> error
                else -> json.optString("message")
            }
        }.getOrDefault("")
    }

    private fun buildPrompt(entry: FitnessEntry, profile: UserProfile): String {
        return """
            请分析这一天的健身记录，输出中文，分为：
            1. 今日概览
            2. 训练评价
            3. 饮食观察
            4. 明天建议
            5. 注意事项

            要求：
            - 语气像私人健身记录教练，具体、温和、可执行。
            - 不要夸大，不做医疗诊断。
            - 如果信息不足，请指出缺失项。
            - 如果有身材照片，只能根据“用户上传了照片”这个事实提醒长期对比，不要声称你看到了照片内容。

            个人资料：
            ${profile.summaryForAi()}

            当天记录：
            ${entry.summaryForAi()}
        """.trimIndent()
    }

    private fun buildNutritionPrompt(
        entry: FitnessEntry,
        profile: UserProfile,
        previousDayPhotoAngles: String
    ): String {
        return """
            请根据用户已经记录的食物名称和克数，估算当天“已摄入”的营养，并结合个人资料、身体数据和当天实际训练给出补充建议。

            请只返回最终 JSON 对象，不要放在 markdown 代码块里，不要输出推理过程、计算草稿、四舍五入说明或任何 JSON 之外的文字。格式如下：
            {
              "calories_kcal": 估算热量数字,
              "carbs_g": 碳水克数数字,
              "protein_g": 蛋白质克数数字,
              "fat_g": 脂肪克数数字,
              "fiber_g": 膳食纤维克数数字,
              "carbs_percent": 碳水克数占比数字,
              "protein_percent": 蛋白质克数占比数字,
              "fat_percent": 脂肪克数占比数字,
              "fiber_percent": 膳食纤维克数占比数字,
              "markdown_advice": "用 Markdown 写建议"
            }

            要求：
            - 必须把最终答案放在 message.content 中；不要只返回 reasoning_content。
            - markdown_advice 只写补充建议，不要重复卡路里、碳水、蛋白质、脂肪、膳食纤维数值。
            - carbs_percent、protein_percent、fat_percent、fiber_percent 用四项克数总和做占比，四项加起来应接近 100，用于前端饼状图展示。
            - 这是估算，不要假装精确。
            - 如果食物名过于笼统，请给出合理区间或提醒补充烹饪方式。
            - 营养分析不受当前页面筛选的早餐/中餐/晚餐影响，只分析下面列出的全部已摄入食物。
            - 必须结合当前设备时间判断今天目前处在早饭前后、午饭前后、晚饭前后、训练前或训练后，再安排建议。
            - 如果三餐没有全部记录，不要默认用户已经吃完整天；要结合当前时间判断“合理未吃”“可能漏记”“接下来该吃什么”。
            - 如果当前时间还没到午饭/晚饭，而对应餐次未记录，只按后续餐次规划建议，不要批评缺失。
            - 如果当前时间已经过了某餐常规时间，而对应餐次未记录，要提醒用户可能漏记，建议补充记录或给出下一餐调整方案。
            - 训练建议只结合当天左滑完成过的实际训练，按时段、类型、实际组数/次数/重量/有氧时长推测训练强度；计划了但实际完成组数为 0 的训练不能当作实际训练。
            - 如果当天没有实际训练，要明确按“目前没有实际训练”来安排，不要编造训练。
            - 前一天身材照片没有传给你做视觉识别。只有在“前一天照片上传角度”不为“无”时，才可以提醒用户长期对比体型变化；不要声称看到了照片内容、体脂或骨架。
            - 不做医疗诊断。

            日期：${entry.date}
            当前设备时间：${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}
            个人资料：
            ${profile.summaryForAi()}

            身高：${entry.heightCm.ifBlank { "未填写" }} cm
            体重：${entry.weightKg.ifBlank { "未填写" }} kg
            当天实际训练：
            ${entry.trainingSummaryForNutrition()}
            三餐记录状态：
            早餐：${if (entry.mealItems.any { it.type == com.humsong.app.fitness.MealType.Breakfast }) "已记录" else "未记录"}
            中餐：${if (entry.mealItems.any { it.type == com.humsong.app.fitness.MealType.Lunch }) "已记录" else "未记录"}
            晚餐：${if (entry.mealItems.any { it.type == com.humsong.app.fitness.MealType.Dinner }) "已记录" else "未记录"}
            前一天照片上传角度：$previousDayPhotoAngles
            饮食记录：
            ${entry.mealsSummaryForNutrition()}
        """.trimIndent()
    }

    private fun buildBodyGoalRecommendationPrompt(
        entry: FitnessEntry,
        profile: UserProfile,
        previousDayPhotoAngles: String
    ): String {
        return """
            请根据用户个人资料、今天填写的身高体重、目标（增肌/减脂）推荐今天的摄入目标。

            请只返回最终 JSON 对象，不要放在 markdown 代码块里，不要输出推理过程或 JSON 之外的文字。格式如下：
            {
              "calories_kcal": 推荐今日总卡路里数字,
              "carbs_g": 推荐今日碳水克数数字,
              "protein_g": 推荐今日蛋白质克数数字,
              "fat_g": 推荐今日脂肪克数数字,
              "fiber_g": 推荐今日膳食纤维克数数字,
              "markdown_advice": "用 Markdown 写 2-4 条执行建议"
            }

            要求：
            - 必须把最终答案放在 message.content 中；不要只返回 reasoning_content。
            - 如果年龄、性别或活动量信息不足，要使用保守估算，并在 markdown_advice 里说明不确定因素。
            - 前一天照片没有传给你做视觉识别。只有在“前一天照片上传角度”不为“无”时，才可以提醒用户可结合长期照片对比调整；不要声称看到了照片内容、体脂或骨架。
            - 不做医疗诊断，不给极端节食建议。

            日期：${entry.date}
            当前设备时间：${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}
            个人资料：
            ${profile.summaryForAi()}

            今日身高：${entry.heightCm.ifBlank { "未填写" }} cm
            今日体重：${entry.weightKg.ifBlank { "未填写" }} kg
            今日目标：${entry.trainingGoal.label}
            前一天照片上传角度：$previousDayPhotoAngles
        """.trimIndent()
    }

    private fun buildMealNutritionPrompt(
        entry: FitnessEntry,
        mealType: MealType,
        profile: UserProfile
    ): String {
        return """
            请只分析用户 ${mealType.label} 已记录食物的营养摄入。

            请只返回最终 JSON 对象，不要放在 markdown 代码块里，不要输出推理过程、计算草稿、四舍五入说明或任何 JSON 之外的文字。格式如下：
            {
              "calories_kcal": 估算热量数字,
              "carbs_g": 碳水克数数字,
              "protein_g": 蛋白质克数数字,
              "fat_g": 脂肪克数数字,
              "fiber_g": 膳食纤维克数数字,
              "carbs_percent": 碳水克数占比数字,
              "protein_percent": 蛋白质克数占比数字,
              "fat_percent": 脂肪克数占比数字,
              "fiber_percent": 膳食纤维克数占比数字,
              "markdown_advice": "可以使用 Markdown 小标题和列表，只针对这一餐给 1-3 条建议"
            }

            要求：
            - 必须把最终答案放在 message.content 中；不要只返回 reasoning_content。
            - 占比按碳水、蛋白质、脂肪、膳食纤维四项克数占比估算，四项加起来应接近 100，用于前端饼状图展示。
            - 常见中文食物按中国日常熟食重量估算；例如“米饭 100克”默认按熟米饭估算，约 116 kcal、碳水约 26g、蛋白质约 2-3g、脂肪接近 0g，不能把三大营养素平均分配。
            - 如果无法确定食物状态，请在建议里说明不确定项，但前四项仍要给出合理估算。
            - 不要分析其他餐，不要输出全天汇总。
            - 这是估算，不要假装精确。

            日期：${entry.date}
            个人资料：
            ${profile.summaryForAi()}

            ${mealType.label}记录：
            ${entry.mealSummaryForNutrition(mealType)}
        """.trimIndent()
    }

    private fun buildPhotoComparisonPrompt(
        currentEntry: FitnessEntry,
        previousEntry: FitnessEntry,
        angle: BodyPhotoAngle,
        profile: UserProfile
    ): String {
        return """
            请对比用户两个日期的身体数据和同角度身材照片，输出中文。
            本次只对比照片角度：${angle.label}。

            输出结构：
            1. 对比区间：从 ${previousEntry.date} 到 ${currentEntry.date}
            2. 身体数据变化：身高、体重变化；如果缺失就说明缺失。
            3. 视觉体型变化：只根据${angle.label}对比，描述该角度可见的肩背、胸腹、腰腹、腿部、整体线条、体脂感变化。只描述照片可见的趋势，不要假装精确测量体脂率。
            4. 可能原因：结合两个日期间隔和训练/饮食记录，给出合理推测。
            5. 下一步建议：结合当前目标给出训练、饮食、拍照记录建议。

            要求：
            - 只分析本次提供的${angle.label}，不要分析其它角度。
            - 不做医疗诊断，不使用羞辱性语言。
            - 尽量具体，但承认光线、姿势、距离、衣着会影响判断。

            个人资料：
            ${profile.summaryForAi()}

            往期记录：
            ${previousEntry.summaryForAi()}

            当前记录：
            ${currentEntry.summaryForAi()}
        """.trimIndent()
    }

    private fun buildFoodPhotoPrompt(mealType: MealType, supplementText: String): String {
        val supplement = supplementText.trim().ifBlank { "无" }
        return """
            You are a food photo nutrition logging assistant.
            Analyze the uploaded meal photo(s) for ${mealType.label}. Identify visible foods and estimate cooked edible grams.
            User supplemental information: $supplement

            Return ONLY one final JSON object in message.content. Do not output markdown, reasoning, explanation, or text outside JSON.
            Required schema:
            {
              "items": [
                {
                  "food_name": "food name in Chinese",
                  "grams": estimated_grams_number
                }
              ],
              "note": "short uncertainty note in Chinese"
            }

            Rules:
            - Split mixed meals into separate food items when possible.
            - If several photos are provided, treat the latest photo and supplemental information as corrections or extra context for the same meal, not as a separate meal unless the user says so.
            - If the user gives a dish name, portion, package size, number of bowls, or missing ingredient in supplemental information, use it to refine grams.
            - Use conservative estimates. If the photo is ambiguous, still return the most likely visible foods with reasonable grams.
            - Use cooked weight for rice, noodles, meat, vegetables and similar foods.
            - If no food can be identified, return {"items":[],"note":"未能识别出明确食物"}.
            - grams must be a number, not a string.
        """.trimIndent()
    }

    private fun comparisonImagePaths(
        previousEntry: FitnessEntry,
        currentEntry: FitnessEntry,
        angle: BodyPhotoAngle
    ): List<LabeledImagePath> {
        val result = mutableListOf<LabeledImagePath>()
        previousEntry.photoPathFor(angle)?.let { path ->
            result += LabeledImagePath("往期 ${previousEntry.date} ${angle.label}", path)
        }
        currentEntry.photoPathFor(angle)?.let { path ->
            result += LabeledImagePath("当前 ${currentEntry.date} ${angle.label}", path)
        }
        return result.take(MaxComparisonImages)
    }

    private fun File.toDataUrl(): String {
        val encoded = Base64.encodeToString(toCompressedJpegBytes(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$encoded"
    }

    private fun File.toCompressedJpegBytes(): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MaxImageSide || bounds.outHeight / sampleSize > MaxImageSide) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeFile(path, options) ?: return readBytes()
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, ImageJpegQuality, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private data class LabeledImagePath(
        val label: String,
        val path: String
    )

    companion object {
        private const val ApiUrl = "https://api.xiaomimimo.com/v1/chat/completions"
        private const val DefaultSystemPrompt = "你是一个谨慎、实用的健身记录分析助手。只提供生活方式和训练建议，不做医疗诊断。"
        private const val NutritionSystemPrompt = "你是营养估算 JSON 输出器。你只能输出最终 JSON 对象，不能输出推理过程、计算草稿、解释文字或 markdown 代码块。"
        private const val MaxAttempts = 2
        private const val MaxComparisonImages = 4
        private const val MaxImageSide = 1280
        private const val ImageJpegQuality = 78
        private const val MaxDebugResponseChars = 5000
    }
}
