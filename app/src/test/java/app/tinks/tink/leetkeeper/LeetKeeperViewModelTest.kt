package app.tinks.tink.leetkeeper

import app.tinks.tink.network.ApiResult
import app.tinks.tink.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LeetKeeperViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsPublicAndOngoingPlans() = runTest {
        val repository = FakeLeetKeeperRepository()

        val viewModel = LeetKeeperViewModel(repository)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(1), state.publicPlans.map { it.id })
        assertEquals("plan-1", state.ongoingPlan?.id)
        assertEquals(1, repository.snapshotCalls)
    }

    @Test
    fun selectPublicPlan_loadsPlanDetail() = runTest {
        val repository = FakeLeetKeeperRepository()
        val viewModel = LeetKeeperViewModel(repository)

        viewModel.onEvent(LeetKeeperEvent.SelectPublicPlan(1))

        val state = viewModel.uiState.value
        assertEquals("Top Interview", state.selectedPublicPlan?.title)
        assertEquals(1, repository.detailRequests.single())
    }

    @Test
    fun startSelectedPlan_postsLanguageAndShowsOngoingTab() = runTest {
        val repository = FakeLeetKeeperRepository()
        val viewModel = LeetKeeperViewModel(repository)

        viewModel.onEvent(LeetKeeperEvent.SelectPublicPlan(1))
        viewModel.onEvent(LeetKeeperEvent.SelectLanguage(LeetKeeperLanguage.Python))
        viewModel.onEvent(LeetKeeperEvent.StartSelectedPlan)

        val state = viewModel.uiState.value
        assertEquals(LeetKeeperTab.Ongoing, state.selectedTab)
        assertNull(state.selectedPublicPlan)
        assertEquals(LeetKeeperLanguage.Python, state.ongoingPlan?.language)
        assertEquals(LeetKeeperLanguage.Python, repository.startedLanguages.single())
    }

    @Test
    fun confirmCompletion_marksProblemDoneAndClearsSheet() = runTest {
        val repository = FakeLeetKeeperRepository()
        val viewModel = LeetKeeperViewModel(repository)
        val problem = samplePlan().modules.single().problems.last()

        viewModel.onEvent(LeetKeeperEvent.RequestMarkDone(problem))
        viewModel.onEvent(LeetKeeperEvent.ChangeDuration("30m"))
        viewModel.onEvent(LeetKeeperEvent.ChangeSubmission("https://leetcode.cn/submissions/12345"))
        viewModel.onEvent(LeetKeeperEvent.ConfirmCompletion)

        val state = viewModel.uiState.value
        assertEquals(listOf(1, 2), state.ongoingPlan?.dones)
        assertNull(state.completionProblem)
        assertEquals(listOf("2"), repository.completedProblemIds)
        assertEquals("30", repository.completedDurations.single().toString())
    }

    @Test
    fun confirmCompletion_keepsSheetForInvalidDuration() = runTest {
        val repository = FakeLeetKeeperRepository()
        val viewModel = LeetKeeperViewModel(repository)
        val problem = samplePlan().modules.single().problems.last()

        viewModel.onEvent(LeetKeeperEvent.RequestMarkDone(problem))
        viewModel.onEvent(LeetKeeperEvent.ConfirmCompletion)

        assertTrue(viewModel.uiState.value.showCompletionSheet)
        assertEquals("Enter a valid duration.", viewModel.uiState.value.errorMessage)
        assertTrue(repository.completedProblemIds.isEmpty())
    }

    private class FakeLeetKeeperRepository : LeetKeeperRepository(NoopLeetKeeperApi) {
        var snapshotCalls = 0
        val detailRequests = mutableListOf<Int>()
        val startedLanguages = mutableListOf<LeetKeeperLanguage>()
        val completedProblemIds = mutableListOf<String>()
        val completedDurations = mutableListOf<Int>()

        private var ongoingPlan = sampleOngoingPlan()

        override fun getSnapshot(): Flow<ApiResult<LeetKeeperSnapshot>> {
            snapshotCalls += 1
            return flowOf(
                ApiResult.Loading,
                ApiResult.Success(
                    LeetKeeperSnapshot(
                        publicPlans = listOf(samplePlan()),
                        ongoingPlan = ongoingPlan,
                    )
                )
            )
        }

        override fun getPublicPlanDetail(planId: Int): Flow<ApiResult<LeetKeeperPublicPlan>> {
            detailRequests.add(planId)
            return flowOf(ApiResult.Loading, ApiResult.Success(samplePlan()))
        }

        override fun startPlan(
            publicPlanId: Int,
            language: LeetKeeperLanguage,
        ): Flow<ApiResult<LeetKeeperOngoingPlan>> {
            startedLanguages.add(language)
            ongoingPlan = sampleOngoingPlan(language = language)
            return flowOf(ApiResult.Loading, ApiResult.Success(ongoingPlan))
        }

        override fun markProblemDone(
            plan: LeetKeeperOngoingPlan,
            problem: LeetKeeperProblemSummary,
            durationMinutes: Int,
            submission: String?,
        ): Flow<ApiResult<LeetKeeperOngoingPlan>> {
            completedProblemIds.add(problem.id)
            completedDurations.add(durationMinutes)
            ongoingPlan = plan.copy(
                dones = (plan.dones + problem.id.toInt()).distinct(),
                progress = plan.progress + 1,
            )
            return flowOf(ApiResult.Loading, ApiResult.Success(ongoingPlan))
        }
    }

    private object NoopLeetKeeperApi : LeetKeeperApi {
        override suspend fun getPublicPlans(): List<LeetKeeperPublicPlanDto> = emptyList()
        override suspend fun getPublicPlanDetail(planId: Int): LeetKeeperPlanDetailDto = error("not used")
        override suspend fun getOngoingPlan(): LeetKeeperOngoingPlanDto? = null
        override suspend fun createOngoingPlan(payload: LeetKeeperStartPlanRequest): LeetKeeperOngoingPlanDto = error("not used")
        override suspend fun updateOngoingPlan(
            planId: String,
            payload: LeetKeeperUpdateOngoingPlanRequest,
        ): LeetKeeperOngoingPlanDto = error("not used")

        override suspend fun getProblemDetail(problemId: String): LeetKeeperProblemDetailDto = error("not used")
        override suspend fun createTransaction(
            payload: LeetKeeperCreateTransactionRequest,
        ): LeetKeeperTransactionDto = error("not used")
    }
}

private fun samplePlan(): LeetKeeperPublicPlan {
    val problems = listOf(
        LeetKeeperProblemSummary("1", "Two Sum", LeetKeeperDifficulty.Easy, true, "two-sum"),
        LeetKeeperProblemSummary("2", "Add Two Numbers", LeetKeeperDifficulty.Medium, false, "add-two-numbers"),
    )
    return LeetKeeperPublicPlan(
        id = 1,
        title = "Top Interview",
        introduction = "Daily practice",
        copy = 4,
        totalProblems = 2,
        modules = listOf(
            LeetKeeperModule(
                name = "Arrays",
                totalProblems = 2,
                progress = 1,
                problems = problems,
            )
        ),
    )
}

private fun sampleOngoingPlan(
    language: LeetKeeperLanguage = LeetKeeperLanguage.Kotlin,
): LeetKeeperOngoingPlan =
    LeetKeeperOngoingPlan(
        id = "plan-1",
        title = samplePlan().title,
        introduction = samplePlan().introduction,
        language = language,
        totalProblems = 2,
        progress = 1,
        dones = listOf(1),
        createdAt = null,
        updatedAt = null,
        modules = samplePlan().modules,
    )
