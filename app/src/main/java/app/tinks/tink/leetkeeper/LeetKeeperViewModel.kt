package app.tinks.tink.leetkeeper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.network.ApiResult
import app.tinks.tink.ui.components.AppSnackbarBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class LeetKeeperTab {
    Ongoing,
    Popular,
    Done,
}

sealed interface LeetKeeperEvent {
    object Refresh : LeetKeeperEvent
    data class SelectTab(val tab: LeetKeeperTab) : LeetKeeperEvent
    data class SelectPublicPlan(val planId: Int) : LeetKeeperEvent
    object ClosePublicPlanDetail : LeetKeeperEvent
    data class SelectLanguage(val language: LeetKeeperLanguage) : LeetKeeperEvent
    object StartSelectedPlan : LeetKeeperEvent
    data class OpenProblem(val problem: LeetKeeperProblemSummary) : LeetKeeperEvent
    object CloseProblemDetail : LeetKeeperEvent
    data class RequestMarkDone(val problem: LeetKeeperProblemSummary) : LeetKeeperEvent
    data class ChangeDuration(val value: String) : LeetKeeperEvent
    data class ChangeSubmission(val value: String) : LeetKeeperEvent
    object DismissCompletionSheet : LeetKeeperEvent
    object ConfirmCompletion : LeetKeeperEvent
}

data class LeetKeeperUiState(
    val selectedTab: LeetKeeperTab,
    val isLoading: Boolean,
    val publicPlans: List<LeetKeeperPublicPlan>,
    val ongoingPlan: LeetKeeperOngoingPlan?,
    val selectedPublicPlan: LeetKeeperPublicPlan?,
    val selectedLanguage: LeetKeeperLanguage,
    val focusedProblem: LeetKeeperProblemDetail?,
    val completionProblem: LeetKeeperProblemSummary?,
    val durationText: String,
    val submissionText: String,
    val errorMessage: String?,
) {
    val showCompletionSheet: Boolean = completionProblem != null
}

private data class LeetKeeperState(
    val selectedTab: LeetKeeperTab = LeetKeeperTab.Ongoing,
    val isLoading: Boolean = true,
    val publicPlans: List<LeetKeeperPublicPlan> = emptyList(),
    val ongoingPlan: LeetKeeperOngoingPlan? = null,
    val selectedPublicPlan: LeetKeeperPublicPlan? = null,
    val selectedLanguage: LeetKeeperLanguage = LeetKeeperLanguage.Kotlin,
    val focusedProblem: LeetKeeperProblemDetail? = null,
    val completionProblem: LeetKeeperProblemSummary? = null,
    val durationText: String = "",
    val submissionText: String = "",
    val errorMessage: String? = null,
) {
    fun toUiState(): LeetKeeperUiState =
        LeetKeeperUiState(
            selectedTab = selectedTab,
            isLoading = isLoading,
            publicPlans = publicPlans,
            ongoingPlan = ongoingPlan,
            selectedPublicPlan = selectedPublicPlan,
            selectedLanguage = selectedLanguage,
            focusedProblem = focusedProblem,
            completionProblem = completionProblem,
            durationText = durationText,
            submissionText = submissionText,
            errorMessage = errorMessage,
        )
}

@HiltViewModel
class LeetKeeperViewModel @Inject constructor(
    private val repository: LeetKeeperRepository,
) : ViewModel() {
    private val state = MutableStateFlow(LeetKeeperState())
    val uiState = state.map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, state.value.toUiState())

    private var refreshJob: Job? = null
    private var detailJob: Job? = null

    init {
        refresh()
    }

    fun onEvent(event: LeetKeeperEvent) {
        when (event) {
            LeetKeeperEvent.Refresh -> refresh()
            is LeetKeeperEvent.SelectTab -> state.update {
                it.copy(selectedTab = event.tab, selectedPublicPlan = null, focusedProblem = null)
            }

            is LeetKeeperEvent.SelectPublicPlan -> loadPublicPlanDetail(event.planId)
            LeetKeeperEvent.ClosePublicPlanDetail -> state.update { it.copy(selectedPublicPlan = null) }
            is LeetKeeperEvent.SelectLanguage -> state.update { it.copy(selectedLanguage = event.language) }
            LeetKeeperEvent.StartSelectedPlan -> startSelectedPlan()
            is LeetKeeperEvent.OpenProblem -> loadProblemDetail(event.problem.id)
            LeetKeeperEvent.CloseProblemDetail -> state.update { it.copy(focusedProblem = null) }
            is LeetKeeperEvent.RequestMarkDone -> state.update {
                it.copy(
                    completionProblem = event.problem,
                    durationText = "",
                    submissionText = "",
                )
            }

            is LeetKeeperEvent.ChangeDuration -> state.update {
                it.copy(durationText = event.value.filter(Char::isDigit))
            }

            is LeetKeeperEvent.ChangeSubmission -> state.update { it.copy(submissionText = event.value) }
            LeetKeeperEvent.DismissCompletionSheet -> dismissCompletionSheet()
            LeetKeeperEvent.ConfirmCompletion -> confirmCompletion()
        }
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = repository.getSnapshot()
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> state.update { it.copy(isLoading = true, errorMessage = null) }
                    is ApiResult.Success -> state.update {
                        it.copy(
                            isLoading = false,
                            publicPlans = result.data.publicPlans,
                            ongoingPlan = result.data.ongoingPlan,
                            errorMessage = null,
                        )
                    }

                    is ApiResult.Error -> showApiError(result, ::refresh)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadPublicPlanDetail(planId: Int) {
        detailJob?.cancel()
        detailJob = repository.getPublicPlanDetail(planId)
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> state.update { it.copy(isLoading = true, errorMessage = null) }
                    is ApiResult.Success -> state.update {
                        it.copy(
                            isLoading = false,
                            selectedPublicPlan = result.data,
                            selectedLanguage = LeetKeeperLanguage.Kotlin,
                            errorMessage = null,
                        )
                    }

                    is ApiResult.Error -> showApiError(result) { loadPublicPlanDetail(planId) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun startSelectedPlan() {
        val plan = state.value.selectedPublicPlan ?: return
        val language = state.value.selectedLanguage
        repository.startPlan(plan.id, language)
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> state.update { it.copy(isLoading = true, errorMessage = null) }
                    is ApiResult.Success -> state.update {
                        it.copy(
                            isLoading = false,
                            ongoingPlan = result.data,
                            selectedPublicPlan = null,
                            selectedTab = LeetKeeperTab.Ongoing,
                            errorMessage = null,
                        )
                    }

                    is ApiResult.Error -> showApiError(result, ::startSelectedPlan)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadProblemDetail(problemId: String) {
        detailJob?.cancel()
        detailJob = repository.getProblemDetail(problemId)
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> state.update { it.copy(isLoading = true, errorMessage = null) }
                    is ApiResult.Success -> state.update {
                        it.copy(
                            isLoading = false,
                            focusedProblem = result.data,
                            errorMessage = null,
                        )
                    }

                    is ApiResult.Error -> showApiError(result) { loadProblemDetail(problemId) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun confirmCompletion() {
        val plan = state.value.ongoingPlan ?: return
        val problem = state.value.completionProblem ?: return
        val duration = state.value.durationText.toIntOrNull()
        if (duration == null || duration <= 0) {
            state.update { it.copy(errorMessage = "Enter a valid duration.") }
            return
        }

        repository.markProblemDone(
            plan = plan,
            problem = problem,
            durationMinutes = duration,
            submission = state.value.submissionText,
        ).onEach { result ->
            when (result) {
                ApiResult.Loading -> state.update { it.copy(isLoading = true, errorMessage = null) }
                is ApiResult.Success -> state.update {
                    it.copy(
                        isLoading = false,
                        ongoingPlan = result.data,
                        completionProblem = null,
                        durationText = "",
                        submissionText = "",
                        errorMessage = null,
                    )
                }

                is ApiResult.Error -> showApiError(result, ::confirmCompletion)
            }
        }.launchIn(viewModelScope)
    }

    private fun dismissCompletionSheet() {
        state.update {
            it.copy(
                completionProblem = null,
                durationText = "",
                submissionText = "",
            )
        }
    }

    private fun showApiError(
        error: ApiResult.Error,
        onRetry: () -> Unit = ::refresh,
    ) {
        state.update {
            it.copy(
                isLoading = false,
                errorMessage = error.message,
            )
        }
        AppSnackbarBus.showApiFailure(onRetry = onRetry)
    }
}
