package app.tinks.tink.ui.components

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class AppSnackbarEvent(
    val message: String,
    val actionLabel: String? = "Retry",
    val onAction: () -> Unit = {}
)

object AppSnackbarBus {
    private val _events = MutableSharedFlow<AppSnackbarEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<AppSnackbarEvent> = _events

    fun showApiFailure(onRetry: () -> Unit) {
        _events.tryEmit(
            AppSnackbarEvent(
                message = "Request failed",
                onAction = onRetry
            )
        )
    }

    fun showMessage(
        message: String,
        actionLabel: String? = null,
        onAction: () -> Unit = {},
    ) {
        _events.tryEmit(
            AppSnackbarEvent(
                message = message,
                actionLabel = actionLabel,
                onAction = onAction,
            )
        )
    }
}
