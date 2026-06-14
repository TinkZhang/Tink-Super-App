package app.tinks.tink.testing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.tinks.tink.ui.theme.TinkTheme
import app.tinks.tink.time.DEFAULT_TIME_TYPE
import app.tinks.tink.time.TimeDayEntries
import app.tinks.tink.time.TimeEntry
import app.tinks.tink.time.TimeEditorState
import app.tinks.tink.time.TimeLabel
import app.tinks.tink.time.TimeLabelManagerState
import app.tinks.tink.time.TimeScreen
import app.tinks.tink.time.TimeStatistic
import app.tinks.tink.time.TimeUiState
import app.tinks.tink.book.Book
import app.tinks.tink.book.BookDraft
import app.tinks.tink.book.ArchiveStatus
import app.tinks.tink.book.BookListCard
import app.tinks.tink.book.BookSearchResultCard
import app.tinks.tink.book.BookScreen
import app.tinks.tink.book.BookState
import app.tinks.tink.book.BookUiState
import app.tinks.tink.book.BooksScreenState
import app.tinks.tink.lottery.LOTTERY_TYPE_DA_LE_TOU
import app.tinks.tink.lottery.LotteryMatchSummary
import app.tinks.tink.lottery.LotteryNumbers
import app.tinks.tink.lottery.LotteryScreen
import app.tinks.tink.lottery.LotteryStatsUiState
import app.tinks.tink.lottery.LotteryTicket
import app.tinks.tink.lottery.LotteryTicketStatus
import app.tinks.tink.lottery.LotteryTicketUiState
import app.tinks.tink.lottery.LotteryUiState
import app.tinks.tink.weight.TrendChartCardUiState
import app.tinks.tink.weight.WeightControlCardUiState
import app.tinks.tink.weight.WeightScreen
import app.tinks.tink.weight.WeightUiState
import app.tinks.tink.weight.data.Weight
import com.android.tools.screenshot.PreviewTest
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

@PreviewTest
@Preview(showBackground = true, widthDp = 400, heightDp = 500)
@Composable
fun TinkScreenshotSmokePreview() {
    TinkTheme(dynamicColor = false) {
        Surface {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Tink test surface")
                Button(onClick = {}) {
                    Text("Ready")
                }
            }
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
fun WeightOverviewScreenshotPreview() {
    TinkTheme(dynamicColor = false) {
        WeightScreen(
            state = WeightUiState(
                isLoading = false,
                weightControlCardUiState = WeightControlCardUiState(
                    isTodayRecorded = false,
                    lastDateText = "2026-05-18",
                    showConfirm = false,
                    newWeight = 141.2,
                ),
                trendChartCardUiState = TrendChartCardUiState(
                    selectedIndex = 0,
                    weightList = listOf(
                        Weight(id = 1, weight = 142.0, createdTime = 1778745600000L),
                        Weight(id = 2, weight = 141.6, createdTime = 1778918400000L),
                        Weight(id = 3, weight = 141.2, createdTime = 1779177600000L),
                    ),
                ),
                allWeights = listOf(
                    Weight(id = 3, weight = 141.2, createdTime = 1779177600000L),
                    Weight(id = 2, weight = 141.6, createdTime = 1778918400000L),
                    Weight(id = 1, weight = 142.0, createdTime = 1778745600000L),
                ),
            ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun LotteryActiveTicketScreenshotPreview() {
    TinkTheme(dynamicColor = false) {
        LotteryScreen(
            state = LotteryUiState(
                isLoading = false,
                activeTicket = lotteryTicketUiState(),
                historyTickets = listOf(lotteryTicketUiState()),
                stats = LotteryStatsUiState(
                    totalTickets = 1,
                    pendingTickets = 0,
                    revealedTickets = 1,
                    winningTickets = 1,
                    bestPrizeTier = "一等奖",
                    prizeDistribution = listOf("一等奖" to 1),
                ),
                draft = null,
                luckyOutcome = null,
            )
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
fun TimeDashboardScreenshotPreview() {
    TinkTheme(dynamicColor = false) {
        TimeScreen(
            state = TimeUiState(
                isLoading = false,
                isSaving = false,
                startDate = LocalDate.parse("2026-05-22"),
                endDate = LocalDate.parse("2026-05-22"),
                statistics = listOf(
                    TimeStatistic(type = 5, duration = 210),
                    TimeStatistic(type = 9, duration = 90),
                    TimeStatistic(type = 2, duration = 45),
                ),
                entriesByDay = listOf(
                    TimeDayEntries(
                        day = LocalDate.parse("2026-05-22"),
                        entries = listOf(
                            TimeEntry(
                                id = 1,
                                type = 5,
                                start = OffsetDateTime.parse("2026-05-22T09:00:00+08:00"),
                                end = OffsetDateTime.parse("2026-05-22T11:30:00+08:00"),
                                title = "Planning: Weekly roadmap",
                                description = "Time feature polish",
                            ),
                            TimeEntry(
                                id = 2,
                                type = 9,
                                start = OffsetDateTime.parse("2026-05-22T14:00:00+08:00"),
                                end = OffsetDateTime.parse("2026-05-22T15:30:00+08:00"),
                                title = "Coding: Appium tests",
                                description = null,
                            ),
                        ),
                    )
                ),
                deletingIds = emptySet(),
                showEditor = false,
                editor = TimeEditorState.defaultNow(),
                labels = listOf(
                    TimeLabel(id = 1, type = DEFAULT_TIME_TYPE, name = "Planning", sortOrder = 0),
                    TimeLabel(id = 2, type = DEFAULT_TIME_TYPE, name = "Coding", sortOrder = 1),
                ),
                showLabelManager = false,
                labelManager = TimeLabelManagerState(),
            ),
            onEvent = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
fun BookReadingTrackerScreenshotPreview() {
    TinkTheme(dynamicColor = false) {
        BookScreen(
            state = BookUiState(
                readingBooks = listOf(
                    Book(
                        id = 1,
                        title = "The Left Hand of Darkness",
                        publisher = "Ace",
                        author = "Ursula K. Le Guin",
                        coverUrl = null,
                        isbn = "9780441478125",
                        description = null,
                        rating = 4.3,
                        amazonLink = null,
                        pages = 304,
                        publishYear = 1969,
                        state = BookState.Reading,
                        platform = "Paper",
                        currentPage = 92,
                        progressPercentage = 30.0,
                        archiveStatus = null,
                        archivedDate = null,
                    )
                ),
                wishlistBooks = emptyList(),
                archivedBooks = emptyList(),
            ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 400, heightDp = 160)
@Composable
fun BookSearchResultCardScreenshotPreview() {
    TinkTheme(dynamicColor = false) {
        Surface(modifier = Modifier.padding(8.dp)) {
            BookSearchResultCard(
                draft = BookDraft(
                    title = "The Left Hand of Darkness",
                    publisher = "Ace",
                    author = "Ursula K. Le Guin",
                    coverUrl = null,
                    rating = 4.3,
                    pages = 304,
                    publishYear = 1969,
                ),
                onEvent = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 400, heightDp = 188)
@Composable
fun BookReadingListCardScreenshotPreview() {
    TinkTheme(dynamicColor = false) {
        Surface(modifier = Modifier.padding(8.dp)) {
            BookListCard(
                book = listCardSample(
                    state = BookState.Reading,
                    platform = "Kindle",
                    currentPage = 92,
                    progressPercentage = 30.0,
                ),
                onEvent = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 400, heightDp = 188)
@Composable
fun BookWishlistCardScreenshotPreview() {
    TinkTheme(dynamicColor = false) {
        Surface(modifier = Modifier.padding(8.dp)) {
            BookListCard(
                book = listCardSample(state = BookState.Wish),
                onEvent = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 400, heightDp = 188)
@Composable
fun BookArchivedCardScreenshotPreview() {
    TinkTheme(dynamicColor = false) {
        Surface(modifier = Modifier.padding(8.dp)) {
            BookListCard(
                book = listCardSample(
                    state = BookState.Archived,
                    currentPage = 304,
                    progressPercentage = 100.0,
                    archiveStatus = ArchiveStatus.Done,
                    archivedDate = "2026-04-21",
                ),
                onEvent = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
fun BookYearlySummaryScreenshotPreview() {
    TinkTheme(dynamicColor = false) {
        BookScreen(
            state = BookUiState(
                screen = BooksScreenState.YearlySummary,
                archivedBooks = listOf(
                    listCardSample(
                        state = BookState.Archived,
                        archiveStatus = ArchiveStatus.Done,
                        archivedDate = "2026-01-18",
                    ).copy(title = "The Left Hand of Darkness"),
                    listCardSample(
                        state = BookState.Archived,
                        archiveStatus = ArchiveStatus.Done,
                        archivedDate = "2026-03-08",
                    ).copy(title = "Designing Data-Intensive Applications", pages = 616),
                    listCardSample(
                        state = BookState.Archived,
                        archiveStatus = ArchiveStatus.Done,
                        archivedDate = "2026-05-22",
                    ).copy(title = "Clean Code", pages = 464),
                ),
            ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 400, heightDp = 360)
@Composable
fun BookFilteredReadingShelfScreenshotPreview() {
    TinkTheme(dynamicColor = false) {
        BookScreen(
            state = BookUiState(
                screen = BooksScreenState.List(BookState.Reading),
                categories = listOf("Science Fiction", "Design", "Work"),
                selectedCategory = "Science Fiction",
                readingBooks = listOf(
                    listCardSample(
                        state = BookState.Reading,
                        platform = "Kindle",
                        currentPage = 92,
                        progressPercentage = 30.0,
                        category = "Science Fiction",
                    )
                ),
            ),
        )
    }
}

private fun listCardSample(
    state: BookState,
    platform: String? = null,
    currentPage: Int? = null,
    progressPercentage: Double? = null,
    archiveStatus: ArchiveStatus? = null,
    archivedDate: String? = null,
    category: String? = null,
) = Book(
    id = 1,
    title = "The Left Hand of Darkness",
    publisher = "Ace",
    author = "Ursula K. Le Guin",
    coverUrl = null,
    isbn = "9780441478125",
    description = null,
    rating = 4.3,
    amazonLink = null,
    pages = 304,
    publishYear = 1969,
    state = state,
    platform = platform,
    currentPage = currentPage,
    progressPercentage = progressPercentage,
    archiveStatus = archiveStatus,
    archivedDate = archivedDate,
    category = category,
)

private fun lotteryTicketUiState(): LotteryTicketUiState {
    val ticket = LotteryTicket(
        id = 1,
        type = LOTTERY_TYPE_DA_LE_TOU,
        issueId = "21126",
        numbers = LotteryNumbers(listOf(1, 11, 12, 34, 35), listOf(9, 12)),
        revealTime = Instant.parse("2021-11-03T12:30:00Z"),
        capturedImageUri = null,
        checked = true,
        checkedAt = Instant.parse("2026-06-04T10:00:00Z"),
        resultId = 2,
        prizeTier = "一等奖",
        frontMatchCount = 5,
        backMatchCount = 2,
        result = null,
    )
    return LotteryTicketUiState(
        ticket = ticket,
        status = LotteryTicketStatus.Revealed,
        revealTimeText = "2021-11-03 12:30:00",
        checkedTimeText = "2026-06-04 10:00:00",
        matchSummary = LotteryMatchSummary(5, 2, "一等奖"),
    )
}
