package app.tinks.tink.haircut

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.haircut.data.Haircut
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

sealed interface HaircutEvent {
    object AddHaircutFabClick : HaircutEvent
    object DismissDialog : HaircutEvent
    data class SubmitHaircut(val price: Int, val shopName: String, val date: LocalDate) :
        HaircutEvent

    data class DeleteHaircut(val id: Int) : HaircutEvent
    object RefreshHaircutList : HaircutEvent
}

data class HaircutUiState(
    val isLoading: Boolean,
    val history: List<Haircut>,
    val days: Int,
    val showDialog: Boolean = false,
)

data class HaircutState(
    val history: List<Haircut> = emptyList(),
    val days: Int = 0,
    val isLoading: Boolean = true,
    val showDialog: Boolean = false,
) {
    fun toUiState(): HaircutUiState = HaircutUiState(
        isLoading = isLoading,
        history = history,
        days = days,
        showDialog = showDialog,
    )
}

@OptIn(ExperimentalTime::class)
@HiltViewModel
class HaircutViewModel @Inject constructor(
    private val repository: HaircutRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HaircutState())
    val uiState = _state.map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value.toUiState())

    private var refreshJob: Job? = null

    init {
        refreshHaircuts()
    }

    fun onEvent(event: HaircutEvent) {
        when (event) {
            is HaircutEvent.AddHaircutFabClick -> _state.update { it.copy(showDialog = true) }
            is HaircutEvent.DismissDialog -> _state.update { it.copy(showDialog = false) }
            is HaircutEvent.SubmitHaircut -> submitHaircut(event.price, event.shopName, event.date)
            is HaircutEvent.DeleteHaircut -> deleteHaircut(event.id)
            HaircutEvent.RefreshHaircutList -> refreshHaircuts()
        }
    }

    private fun submitHaircut(price: Int, shopName: String, date: LocalDate) {
        repository.addHaircut(
            price = price,
            shopName = shopName,
            date = date,
        ).onEach { result ->
            when (result) {
                is ApiResult.Loading -> _state.update {
                    it.copy(isLoading = true, showDialog = false)
                }

                is ApiResult.Success -> refreshHaircuts()

                is ApiResult.Error -> _state.update {
                    it.copy(isLoading = false)
                }.also {
                    AppSnackbarBus.showApiFailure(
                        onRetry = { submitHaircut(price, shopName, date) }
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun deleteHaircut(id: Int) {
        repository.deleteHaircut(id)
            .onEach { result ->
                when (result) {
                    is ApiResult.Loading -> _state.update {
                        it.copy(isLoading = true)
                    }

                    is ApiResult.Success -> refreshHaircuts()

                    is ApiResult.Error -> _state.update {
                        it.copy(isLoading = false)
                    }.also {
                        AppSnackbarBus.showApiFailure(onRetry = { deleteHaircut(id) })
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun refreshHaircuts() {
        refreshJob?.cancel()
        refreshJob = repository.getHaircuts()
            .onEach { result ->
                when (result) {
                    is ApiResult.Loading -> _state.update {
                        it.copy(isLoading = true)
                    }

                    is ApiResult.Success -> _state.update {
                        it.copy(
                            history = result.data,
                            days = calculateDaysSinceLatestHaircut(result.data),
                            isLoading = false,
                        )
                    }

                    is ApiResult.Error -> _state.update {
                        it.copy(isLoading = false)
                    }.also {
                        AppSnackbarBus.showApiFailure(onRetry = ::refreshHaircuts)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun calculateDaysSinceLatestHaircut(haircuts: List<Haircut>): Int {
        if (haircuts.isEmpty()) {
            return -1
        }

        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        return (today.toEpochDays() - haircuts.first().date.toEpochDays()).toInt()
    }
}
