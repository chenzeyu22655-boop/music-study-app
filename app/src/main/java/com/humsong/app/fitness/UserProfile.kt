package com.humsong.app.fitness

import java.time.LocalDate
import java.time.Period

data class UserProfile(
    val name: String = "",
    val birthday: String = "",
    val gender: String = "",
    val signature: String = "",
    val avatarPath: String? = null
) {
    fun ageText(today: LocalDate = LocalDate.now()): String {
        val birthDate = runCatching { LocalDate.parse(birthday) }.getOrNull() ?: return ""
        if (birthDate.isAfter(today)) return ""
        return Period.between(birthDate, today).years.toString()
    }

    fun summaryForAi(): String {
        return """
            姓名：${name.ifBlank { "未填写" }}
            年龄：${ageText().ifBlank { "未填写" }}
            性别：${gender.ifBlank { "未填写" }}
            生日：${birthday.ifBlank { "未填写" }}
            个人签名：${signature.ifBlank { "未填写" }}
        """.trimIndent()
    }
}
