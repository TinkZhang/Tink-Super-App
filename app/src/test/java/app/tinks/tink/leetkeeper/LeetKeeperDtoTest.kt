package app.tinks.tink.leetkeeper

import org.junit.Assert.assertEquals
import org.junit.Test

class LeetKeeperDtoTest {
    @Test
    fun publicPlanDto_mapsModulesAndProblemDifficulty() {
        val dto = LeetKeeperPlanDetailDto(
            id = 7,
            title = "NeetCode 150",
            introduction = "Pattern practice",
            copy = 12,
            totalProblems = 2,
            subModules = listOf(
                LeetKeeperModuleDto(
                    name = "Arrays",
                    totalProblems = 2,
                    progress = 1,
                    problems = listOf(
                        LeetKeeperProblemSummaryDto(
                            id = "1",
                            title = "Two Sum",
                            difficulty = 0,
                            done = true,
                            link = "two-sum",
                        ),
                        LeetKeeperProblemSummaryDto(
                            id = "4",
                            title = "Median of Two Sorted Arrays",
                            difficulty = 2,
                            done = false,
                            link = "median-of-two-sorted-arrays",
                        ),
                    ),
                )
            ),
        )

        val domain = dto.toDomain()

        assertEquals(7, domain.id)
        assertEquals(2, domain.totalProblems)
        assertEquals("Arrays", domain.modules.single().name)
        assertEquals(LeetKeeperDifficulty.Easy, domain.modules.single().problems.first().difficulty)
        assertEquals(LeetKeeperDifficulty.Hard, domain.modules.single().problems.last().difficulty)
    }

    @Test
    fun ongoingPlanDto_mapsLanguageAndTransactions() {
        val dto = LeetKeeperOngoingPlanDto(
            id = "plan-1",
            title = "Top Interview",
            introduction = "Daily practice",
            language = "Python",
            dones = listOf(1),
            totalProblems = 1,
            progress = 1,
            subModules = emptyList(),
        )

        val domain = dto.toDomain()

        assertEquals(LeetKeeperLanguage.Python, domain.language)
        assertEquals(listOf(1), domain.dones)
    }
}
