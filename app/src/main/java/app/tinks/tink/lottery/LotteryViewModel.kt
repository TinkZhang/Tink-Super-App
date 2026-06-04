package app.tinks.tink.lottery

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
import java.time.Instant
import javax.inject.Inject

sealed interface LotteryEvent {
    object Refresh : LotteryEvent
    object UseCapturedTicketPreview : LotteryEvent
    object DismissReview : LotteryEvent
    object RetakePhoto : LotteryEvent
    object SaveDraft : LotteryEvent
    object DismissHero : LotteryEvent
    data class UpdateDraftIssueId(val value: String) : LotteryEvent
    data class UpdateDraftFrontNumbers(val value: String) : LotteryEvent
    data class UpdateDraftBackNumbers(val value: String) : LotteryEvent
    data class UpdateDraftRevealTime(val value: String) : LotteryEvent
    data class CheckLottery(val id: Int) : LotteryEvent
    data class DeleteLottery(val id: Int) : LotteryEvent
}

enum class LotteryTicketStatus {
    Pending,
    Ready,
    Revealed,
}

data class LotteryTicketUiState(
    val ticket: LotteryTicket,
    val status: LotteryTicketStatus,
    val revealTimeText: String,
    val checkedTimeText: String?,
    val matchSummary: LotteryMatchSummary?,
) {
    val canReveal: Boolean get() = status == LotteryTicketStatus.Ready
}

data class LotteryStatsUiState(
    val totalTickets: Int,
    val pendingTickets: Int,
    val revealedTickets: Int,
    val winningTickets: Int,
    val bestPrizeTier: String,
    val prizeDistribution: List<Pair<String, Int>>,
)

data class LotteryUiState(
    val isLoading: Boolean,
    val activeTicket: LotteryTicketUiState?,
    val historyTickets: List<LotteryTicketUiState>,
    val stats: LotteryStatsUiState,
    val draft: LotteryDraft?,
    val luckyOutcome: LotteryCheckOutcome?,
)

data class LotteryState(
    val tickets: List<LotteryTicket> = emptyList(),
    val isLoading: Boolean = false,
    val draft: LotteryDraft? = null,
    val luckyOutcome: LotteryCheckOutcome? = null,
) {
    fun toUiState(now: Instant = Instant.now()): LotteryUiState {
        val ticketStates = tickets
            .sortedWith(compareBy<LotteryTicket> { it.checked }.thenBy { it.revealTime }.thenBy { it.id })
            .map { it.toUiState(now) }
        val activeTicket = ticketStates.firstOrNull { it.status != LotteryTicketStatus.Revealed }
            ?: ticketStates.firstOrNull()

        return LotteryUiState(
            isLoading = isLoading,
            activeTicket = activeTicket,
            historyTickets = tickets.sortedByDescending { it.revealTime }.map { it.toUiState(now) },
            stats = tickets.toStats(now),
            draft = draft,
            luckyOutcome = luckyOutcome,
        )
    }
}

@HiltViewModel
class LotteryViewModel @Inject constructor(
    private val repository: LotteryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LotteryState())
    val uiState = _state.map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value.toUiState())

    private var refreshJob: Job? = null

    init {
        refreshHistory()
    }

    fun onEvent(event: LotteryEvent) {
        when (event) {
            LotteryEvent.Refresh -> refreshHistory()
            LotteryEvent.UseCapturedTicketPreview -> startParsedDraft()
            LotteryEvent.DismissReview -> _state.update { it.copy(draft = null) }
            LotteryEvent.RetakePhoto -> _state.update { it.copy(draft = null) }
            LotteryEvent.SaveDraft -> saveDraft()
            LotteryEvent.DismissHero -> _state.update { it.copy(luckyOutcome = null) }
            is LotteryEvent.UpdateDraftIssueId -> updateDraft { it.copy(issueId = event.value, parseError = null) }
            is LotteryEvent.UpdateDraftFrontNumbers -> updateDraft { it.copy(frontNumbersText = event.value, parseError = null) }
            is LotteryEvent.UpdateDraftBackNumbers -> updateDraft { it.copy(backNumbersText = event.value, parseError = null) }
            is LotteryEvent.UpdateDraftRevealTime -> updateDraft { it.copy(revealTimeText = event.value, parseError = null) }
            is LotteryEvent.CheckLottery -> checkLottery(event.id)
            is LotteryEvent.DeleteLottery -> deleteLottery(event.id)
        }
    }

    private fun refreshHistory() {
        refreshJob?.cancel()
        refreshJob = repository.getLotteryHistory()
            .onEach { result ->
                when (result) {
                    is ApiResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is ApiResult.Success -> _state.update {
                        it.copy(tickets = result.data, isLoading = false)
                    }
                    is ApiResult.Error -> _state.update {
                        it.copy(isLoading = false)
                    }.also {
                        AppSnackbarBus.showApiFailure(onRetry = ::refreshHistory)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun startParsedDraft() {
        _state.update {
            it.copy(
                draft = LotteryDraft(
                    issueId = "21126",
                    frontNumbersText = "01 11 12 34 35",
                    backNumbersText = "09 12",
                    revealTimeText = "2021-11-03T12:30:00Z",
                )
            )
        }
    }

    private fun updateDraft(update: (LotteryDraft) -> LotteryDraft) {
        _state.update { state ->
            state.copy(draft = state.draft?.let(update))
        }
    }

    private fun saveDraft() {
        val request = try {
            _state.value.draft?.toCreateRequest() ?: return
        } catch (error: IllegalArgumentException) {
            updateDraft { it.copy(parseError = error.message ?: "Lottery details are invalid.") }
            return
        }

        repository.createLottery(request)
            .onEach { result ->
                when (result) {
                    is ApiResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is ApiResult.Success -> {
                        _state.update { it.copy(draft = null, isLoading = false) }
                        refreshHistory()
                    }
                    is ApiResult.Error -> _state.update {
                        it.copy(isLoading = false)
                    }.also {
                        AppSnackbarBus.showApiFailure(onRetry = ::saveDraft)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun checkLottery(id: Int) {
        repository.checkLottery(id)
            .onEach { result ->
                when (result) {
                    is ApiResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is ApiResult.Success -> _state.update { state ->
                        val checkedTicket = result.data.lottery
                        state.copy(
                            tickets = state.tickets.map { ticket ->
                                if (ticket.id == checkedTicket.id) checkedTicket else ticket
                            },
                            isLoading = false,
                            luckyOutcome = result.data.takeIf { it.prizeTier != "未中奖" },
                        )
                    }
                    is ApiResult.Error -> _state.update {
                        it.copy(isLoading = false)
                    }.also {
                        AppSnackbarBus.showApiFailure(onRetry = { checkLottery(id) })
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun deleteLottery(id: Int) {
        repository.deleteLottery(id)
            .onEach { result ->
                when (result) {
                    is ApiResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is ApiResult.Success -> refreshHistory()
                    is ApiResult.Error -> _state.update {
                        it.copy(isLoading = false)
                    }.also {
                        AppSnackbarBus.showApiFailure(onRetry = { deleteLottery(id) })
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}

private fun LotteryTicket.toUiState(now: Instant): LotteryTicketUiState {
    val status = when {
        checked -> LotteryTicketStatus.Revealed
        now.isBefore(revealTime) -> LotteryTicketStatus.Pending
        else -> LotteryTicketStatus.Ready
    }
    return LotteryTicketUiState(
        ticket = this,
        status = status,
        revealTimeText = revealTime.toLotteryDateTimeText(),
        checkedTimeText = checkedAt?.toLotteryDateTimeText(),
        matchSummary = matchSummary(),
    )
}

private fun List<LotteryTicket>.toStats(now: Instant): LotteryStatsUiState {
    val ticketStates = map { it.toUiState(now) }
    val winningTickets = ticketStates.count { it.matchSummary?.isWinning == true }
    val distribution = ticketStates
        .mapNotNull { it.matchSummary?.prizeTier }
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedBy { prizeRank(it.first) }

    return LotteryStatsUiState(
        totalTickets = size,
        pendingTickets = ticketStates.count { it.status == LotteryTicketStatus.Pending },
        revealedTickets = ticketStates.count { it.status == LotteryTicketStatus.Revealed },
        winningTickets = winningTickets,
        bestPrizeTier = distribution.minByOrNull { prizeRank(it.first) }?.first ?: "暂无",
        prizeDistribution = distribution,
    )
}

private fun prizeRank(tier: String): Int =
    when (tier) {
        "一等奖" -> 1
        "二等奖" -> 2
        "三等奖" -> 3
        "四等奖" -> 4
        "五等奖" -> 5
        "六等奖" -> 6
        "七等奖" -> 7
        "八等奖" -> 8
        "九等奖" -> 9
        "未中奖" -> 10
        else -> 99
    }

private fun Instant.toLotteryDateTimeText(): String = toString()
    .replace("T", " ")
    .removeSuffix("Z")
