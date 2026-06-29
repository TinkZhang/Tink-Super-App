package app.tinks.tink.leetkeeper

import app.tinks.tink.network.ApiResult
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeetKeeperRepositoryTest {
    @Test
    fun getSnapshot_loadsPublicAndOngoingPlans() = runTest {
        val api = FakeLeetKeeperApi()
        val repository = LeetKeeperRepository(api)

        val emissions = repository.getSnapshot().take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        val success = emissions[1] as ApiResult.Success
        assertEquals(listOf(1), success.data.publicPlans.map { it.id })
        assertEquals("plan-1", success.data.ongoingPlan?.id)
    }

    @Test
    fun startPlan_postsPublicPlanAndLanguage() = runTest {
        val api = FakeLeetKeeperApi()
        val repository = LeetKeeperRepository(api)

        val emissions = repository.startPlan(1, LeetKeeperLanguage.Java).take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        assertTrue(emissions[1] is ApiResult.Success)
        assertEquals(LeetKeeperStartPlanRequest(1, "Java"), api.startRequests.single())
    }

    @Test
    fun markProblemDone_createsTransactionAndPatchesDones() = runTest {
        val api = FakeLeetKeeperApi()
        val repository = LeetKeeperRepository(api)

        val emissions = repository.markProblemDone(
            plan = ongoingPlanDto(dones = listOf(1)).toDomain(),
            problem = LeetKeeperProblemSummary(
                id = "2",
                title = "Add Two Numbers",
                difficulty = LeetKeeperDifficulty.Medium,
                done = false,
                link = "add-two-numbers",
            ),
            durationMinutes = 30,
            submission = "12345",
        ).take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        val success = emissions[1] as ApiResult.Success
        assertEquals(listOf(1, 2), api.updateRequests.single().dones)
        assertEquals("2", api.transactionRequests.single().problemId)
        assertEquals(30, api.transactionRequests.single().time)
        assertEquals(listOf(1, 2), success.data.dones)
    }

    private class FakeLeetKeeperApi : LeetKeeperApi {
        val startRequests = mutableListOf<LeetKeeperStartPlanRequest>()
        val updateRequests = mutableListOf<LeetKeeperUpdateOngoingPlanRequest>()
        val transactionRequests = mutableListOf<LeetKeeperCreateTransactionRequest>()

        override suspend fun getPublicPlans(): List<LeetKeeperPublicPlanDto> =
            listOf(publicPlanDto())

        override suspend fun getPublicPlanDetail(planId: Int): LeetKeeperPlanDetailDto =
            planDetailDto(planId)

        override suspend fun getOngoingPlan(): LeetKeeperOngoingPlanDto =
            ongoingPlanDto()

        override suspend fun createOngoingPlan(payload: LeetKeeperStartPlanRequest): LeetKeeperOngoingPlanDto {
            startRequests.add(payload)
            return ongoingPlanDto(language = payload.language)
        }

        override suspend fun updateOngoingPlan(
            planId: String,
            payload: LeetKeeperUpdateOngoingPlanRequest,
        ): LeetKeeperOngoingPlanDto {
            updateRequests.add(payload)
            return ongoingPlanDto(dones = payload.dones, progress = payload.dones.size)
        }

        override suspend fun getProblemDetail(problemId: String): LeetKeeperProblemDetailDto =
            LeetKeeperProblemDetailDto(
                id = problemId,
                title = "Two Sum",
                details = "<p>Find a pair.</p>",
                difficulty = 0,
                link = "two-sum",
                transactions = emptyList(),
            )

        override suspend fun createTransaction(
            payload: LeetKeeperCreateTransactionRequest,
        ): LeetKeeperTransactionDto {
            transactionRequests.add(payload)
            return LeetKeeperTransactionDto(
                id = "transaction-10",
                language = payload.language,
                problemId = payload.problemId,
                problemLink = payload.problemLink,
                time = payload.time,
                submission = payload.submission,
            )
        }
    }
}

private fun publicPlanDto(): LeetKeeperPublicPlanDto =
    LeetKeeperPublicPlanDto(
        id = 1,
        title = "Top Interview",
        introduction = "Daily practice",
        copy = 4,
        totalProblems = 2,
        subModules = listOf(moduleDto()),
    )

private fun planDetailDto(id: Int = 1): LeetKeeperPlanDetailDto =
    LeetKeeperPlanDetailDto(
        id = id,
        title = "Top Interview",
        introduction = "Daily practice",
        copy = 4,
        totalProblems = 2,
        subModules = listOf(moduleDto()),
    )

private fun ongoingPlanDto(
    language: String = "Kotlin",
    dones: List<Int> = listOf(1),
    progress: Int = dones.size,
): LeetKeeperOngoingPlanDto =
    LeetKeeperOngoingPlanDto(
        id = "plan-1",
        title = "Top Interview",
        introduction = "Daily practice",
        language = language,
        dones = dones,
        totalProblems = 2,
        progress = progress,
        subModules = listOf(moduleDto(doneIds = dones)),
    )

private fun moduleDto(doneIds: List<Int> = listOf(1)): LeetKeeperModuleDto =
    LeetKeeperModuleDto(
        name = "Arrays",
        totalProblems = 2,
        progress = doneIds.size,
        problems = listOf(
            LeetKeeperProblemSummaryDto(
                id = "1",
                title = "Two Sum",
                difficulty = 0,
                done = 1 in doneIds,
                link = "two-sum",
            ),
            LeetKeeperProblemSummaryDto(
                id = "2",
                title = "Add Two Numbers",
                difficulty = 1,
                done = 2 in doneIds,
                link = "add-two-numbers",
            ),
        ),
    )
