package app.tinks.tink.time

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tinks.tink.ui.theme.TinkTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class TimeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboard_rendersEmptySelectedRange() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                TimeScreen(
                    state = sampleUiState(),
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithText("Start").assertIsDisplayed()
        composeRule.onNodeWithText("End").assertIsDisplayed()
        composeRule.onNodeWithText("No tracked duration in this range.").assertIsDisplayed()
        composeRule.onNodeWithText("No time entries in selected range.").assertIsDisplayed()
    }

    @Test
    fun dateFields_openDatePickerDialog() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                TimeScreen(
                    state = sampleUiState(),
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithTag("time_start_date_field").performClick()

        composeRule.onNodeWithText("Confirm").assertIsDisplayed()
    }

    @Test
    fun editorEditLabelsButton_requestsLabelManager() {
        val events = mutableListOf<TimeEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                TimeScreen(
                    state = sampleUiState(showEditor = true),
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithText("Edit").performClick()

        assertTrue(events.contains(TimeEvent.OpenLabelManager))
    }

    @Test
    fun editorLabelChip_appliesLabel() {
        val label = TimeLabel(id = 1, type = DEFAULT_TIME_TYPE, name = "Planning", sortOrder = 0)
        val events = mutableListOf<TimeEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                TimeScreen(
                    state = sampleUiState(
                        showEditor = true,
                        labels = listOf(label),
                    ),
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithText("Planning").performClick()

        assertTrue(events.contains(TimeEvent.ApplyLabel(label)))
    }

    private fun sampleUiState(
        showEditor: Boolean = false,
        labels: List<TimeLabel> = emptyList(),
    ): TimeUiState = TimeUiState(
        isLoading = false,
        isSaving = false,
        startDate = LocalDate.parse("2026-05-22"),
        endDate = LocalDate.parse("2026-05-22"),
        statistics = emptyList(),
        entriesByDay = emptyList(),
        deletingIds = emptySet(),
        showEditor = showEditor,
        editor = TimeEditorState.defaultNow(),
        labels = labels,
        showLabelManager = false,
        labelManager = TimeLabelManagerState(),
    )
}
