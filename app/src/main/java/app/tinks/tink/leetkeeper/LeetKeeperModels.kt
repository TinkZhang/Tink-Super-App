package app.tinks.tink.leetkeeper

import androidx.annotation.DrawableRes
import app.tinks.tink.R

enum class LeetKeeperLanguage(
    val label: String,
    @DrawableRes val icon: Int,
) {
    Kotlin("Kotlin", R.drawable.language_kotlin),
    Python("Python", R.drawable.language_python),
    Java("Java", R.drawable.language_java),
    Swift("Swift", R.drawable.language_swift),
    JavaScript("JavaScript", R.drawable.language_javascript);

    companion object {
        fun fromLabel(label: String?): LeetKeeperLanguage =
            entries.firstOrNull { it.label == label } ?: Kotlin
    }
}

data class LeetKeeperSnapshot(
    val publicPlans: List<LeetKeeperPublicPlan>,
    val ongoingPlan: LeetKeeperOngoingPlan?,
)

data class LeetKeeperPublicPlan(
    val id: Int,
    val title: String,
    val introduction: String,
    val copy: Int,
    val totalProblems: Int,
    val modules: List<LeetKeeperModule>,
)

data class LeetKeeperOngoingPlan(
    val id: String,
    val title: String,
    val introduction: String,
    val language: LeetKeeperLanguage,
    val totalProblems: Int,
    val progress: Int,
    val dones: List<Int>,
    val createdAt: String?,
    val updatedAt: String?,
    val modules: List<LeetKeeperModule>,
)

data class LeetKeeperModule(
    val name: String,
    val totalProblems: Int,
    val progress: Int,
    val problems: List<LeetKeeperProblemSummary>,
)

data class LeetKeeperProblemSummary(
    val id: String,
    val title: String,
    val difficulty: LeetKeeperDifficulty,
    val done: Boolean,
    val link: String?,
)

data class LeetKeeperProblemDetail(
    val id: String,
    val title: String,
    val details: String,
    val difficulty: LeetKeeperDifficulty,
    val link: String?,
    val transactions: List<LeetKeeperTransaction>,
)

data class LeetKeeperTransaction(
    val id: String?,
    val language: LeetKeeperLanguage,
    val createdAt: String?,
    val problemId: String,
    val problemLink: String?,
    val timeMinutes: Int,
    val submission: String?,
)

enum class LeetKeeperDifficulty(val label: String) {
    Easy("Easy"),
    Medium("Medium"),
    Hard("Hard");

    companion object {
        fun fromValue(value: Int): LeetKeeperDifficulty = when (value) {
            0 -> Easy
            1 -> Medium
            else -> Hard
        }
    }
}

fun LeetKeeperPublicPlanDto.toDomain(): LeetKeeperPublicPlan {
    val modules = subModules.orEmpty().map { it.toDomain() }
    return LeetKeeperPublicPlan(
        id = id,
        title = title,
        introduction = introduction,
        copy = copy,
        totalProblems = totalProblems ?: modules.sumOf { it.totalProblems },
        modules = modules,
    )
}

fun LeetKeeperPlanDetailDto.toDomain(): LeetKeeperPublicPlan =
    LeetKeeperPublicPlan(
        id = id,
        title = title,
        introduction = introduction,
        copy = copy,
        totalProblems = totalProblems,
        modules = subModules.map { it.toDomain() },
    )

fun LeetKeeperOngoingPlanDto.toDomain(): LeetKeeperOngoingPlan =
    LeetKeeperOngoingPlan(
        id = id,
        title = title,
        introduction = introduction,
        language = LeetKeeperLanguage.fromLabel(language),
        totalProblems = totalProblems,
        progress = progress,
        dones = dones,
        createdAt = createdAt,
        updatedAt = updatedAt,
        modules = subModules.map { it.toDomain() },
    )

fun LeetKeeperModuleDto.toDomain(): LeetKeeperModule =
    LeetKeeperModule(
        name = name,
        totalProblems = totalProblems,
        progress = progress,
        problems = problems.map { it.toDomain() },
    )

fun LeetKeeperProblemSummaryDto.toDomain(): LeetKeeperProblemSummary =
    LeetKeeperProblemSummary(
        id = id,
        title = title,
        difficulty = LeetKeeperDifficulty.fromValue(difficulty),
        done = done,
        link = link,
    )

fun LeetKeeperProblemDetailDto.toDomain(): LeetKeeperProblemDetail =
    LeetKeeperProblemDetail(
        id = id,
        title = title,
        details = details,
        difficulty = LeetKeeperDifficulty.fromValue(difficulty),
        link = link,
        transactions = transactions.map { it.toDomain() },
    )

fun LeetKeeperTransactionDto.toDomain(): LeetKeeperTransaction =
    LeetKeeperTransaction(
        id = id,
        language = LeetKeeperLanguage.fromLabel(language),
        createdAt = createdAt,
        problemId = problemId,
        problemLink = problemLink,
        timeMinutes = time ?: 0,
        submission = submission,
    )
