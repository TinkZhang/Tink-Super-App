package app.tinks.tink.home

import app.tinks.tink.book.Book
import app.tinks.tink.book.BookApi
import app.tinks.tink.book.BookPageFormat
import app.tinks.tink.book.BookUpdateRequest
import app.tinks.tink.book.toDomain as toBookDomain
import app.tinks.tink.haircut.HaircutApi
import app.tinks.tink.haircut.toDomain as toHaircutDomain
import app.tinks.tink.merriam.db.MerriamDao
import app.tinks.tink.merriam.network.MerriamApi
import app.tinks.tink.merriam.network.RootPostDto
import app.tinks.tink.time.TimeApi
import app.tinks.tink.time.TimeUpsertRequest
import app.tinks.tink.weight.WeightApi
import app.tinks.tink.weight.WeightCreateRequest
import app.tinks.tink.weight.toDomain as toWeightDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class HomeSnapshot(
    val merriamLatest: Int? = null,
    val readKeeperBook: HomeReadKeeperBook? = null,
    val haircutDays: Int? = null,
    val weightValue: Double? = null,
    val weightRecordedAt: Long? = null,
) {
    val showHaircutReminder: Boolean
        get() = haircutDays != null && haircutDays >= 35
}

data class HomeReadKeeperBook(
    val id: Long,
    val title: String,
    val coverUrl: String?,
    val pageFormat: BookPageFormat,
    val currentPage: Int?,
    val progressPercentage: Double?,
    val pages: Int?,
    val sessionStartedAt: Long?,
    val sessionStartPage: Int?,
    val sessionStartProgressPercentage: Double?,
) {
    val hasActiveSession: Boolean
        get() = sessionStartedAt != null

    val progressTargetLabel: String
        get() = if (pageFormat.usesPages) "Page" else "Progress %"
}

@Singleton
open class HomeRepository @Inject constructor(
    private val homeDao: HomeDao,
    private val merriamDao: MerriamDao,
    private val merriamApi: MerriamApi,
    private val bookApi: BookApi,
    private val timeApi: TimeApi,
    private val haircutApi: HaircutApi,
    private val weightApi: WeightApi,
) {
    open fun observeSnapshot(): Flow<HomeSnapshot> =
        homeDao.observeSnapshot().map { it.toHomeSnapshot() }

    open suspend fun refreshHome() = withContext(Dispatchers.IO) {
        ensureSnapshot()
        coroutineScope {
            listOf(
                async { runCatching { refreshMerriam() } },
                async { runCatching { refreshReadKeeper() } },
                async { runCatching { refreshHaircut() } },
                async { runCatching { refreshWeight() } },
            ).forEach { it.await() }
        }
        syncPendingActions()
    }

    open suspend fun completeMerriamWords(count: Int): Boolean = withContext(Dispatchers.IO) {
        val current = ensureSnapshot().merriamLatest ?: return@withContext false
        val target = current + count
        updateSnapshot { it.copy(merriamLatest = target) }
        homeDao.insertPendingAction(
            HomePendingActionEntity(
                type = HOME_ACTION_MERRIAM_COMPLETE,
                startIntValue = current,
                targetIntValue = target,
            )
        )
        syncPendingActions()
        true
    }

    open suspend fun addLatestWeight(): Boolean = withContext(Dispatchers.IO) {
        val weight = ensureSnapshot().weightValue ?: return@withContext false
        homeDao.insertPendingAction(
            HomePendingActionEntity(
                type = HOME_ACTION_WEIGHT_ADD,
                weightValue = weight,
            )
        )
        syncPendingActions()
        true
    }

    open suspend fun toggleReadKeeperSession(): Boolean = withContext(Dispatchers.IO) {
        val snapshot = ensureSnapshot()
        val book = snapshot.toHomeSnapshot().readKeeperBook ?: return@withContext false
        if (book.hasActiveSession) {
            val stopTime = System.currentTimeMillis()
            homeDao.insertPendingAction(
                HomePendingActionEntity(
                    type = HOME_ACTION_BOOK_SESSION,
                    bookId = book.id,
                    startTimeMillis = book.sessionStartedAt,
                    endTimeMillis = stopTime,
                    textValue = book.title,
                )
            )
            updateSnapshot {
                it.copy(
                    readKeeperSessionStartedAt = null,
                    readKeeperSessionStartPage = null,
                    readKeeperSessionStartProgressPercentage = null,
                )
            }
            syncPendingActions()
        } else {
            updateSnapshot {
                it.copy(
                    readKeeperSessionStartedAt = System.currentTimeMillis(),
                    readKeeperSessionStartPage = book.currentPage,
                    readKeeperSessionStartProgressPercentage = book.progressPercentage,
                )
            }
        }
        true
    }

    open suspend fun updateReadKeeperProgress(page: Int?, progressPercentage: Double?): Boolean =
        withContext(Dispatchers.IO) {
            val snapshot = ensureSnapshot()
            val bookId = snapshot.readKeeperBookId ?: return@withContext false
            updateSnapshot {
                it.copy(
                    readKeeperCurrentPage = page ?: it.readKeeperCurrentPage,
                    readKeeperProgressPercentage = progressPercentage ?: it.readKeeperProgressPercentage,
                )
            }
            homeDao.insertPendingAction(
                HomePendingActionEntity(
                    type = HOME_ACTION_BOOK_PROGRESS,
                    bookId = bookId,
                    pageValue = page,
                    progressValue = progressPercentage,
                )
            )
            syncPendingActions()
            true
        }

    open suspend fun syncPendingActions() = withContext(Dispatchers.IO) {
        homeDao.getPendingActions().forEach { action ->
            runCatching { executePendingAction(action) }
                .onSuccess { homeDao.deletePendingAction(action.id) }
        }
    }

    private suspend fun executePendingAction(action: HomePendingActionEntity) {
        when (action.type) {
            HOME_ACTION_MERRIAM_COMPLETE -> {
                val start = action.startIntValue ?: return
                val target = action.targetIntValue ?: return
                val roots = merriamDao.getRootsBetween(start, target)
                check(roots.isNotEmpty()) { "Missing local Merriam roots for pending Home action" }
                merriamApi.postMerriam(
                    roots.map { root ->
                        RootPostDto(rootId = root.id, root = root.root)
                    }
                )
            }
            HOME_ACTION_WEIGHT_ADD -> {
                val weight = action.weightValue ?: return
                val created = weightApi.createWeight(WeightCreateRequest(weight)).toWeightDomain()
                updateSnapshot {
                    it.copy(
                        weightValue = created.weight,
                        weightRecordedAt = created.createdTime,
                    )
                }
            }
            HOME_ACTION_BOOK_PROGRESS -> {
                val bookId = action.bookId ?: return
                val updated = bookApi.updateBook(
                    bookId,
                    BookUpdateRequest(
                        currentPage = action.pageValue,
                        progressPercentage = action.progressValue,
                    )
                ).toBookDomain()
                updateSnapshot { it.withReadKeeperBook(updated) }
            }
            HOME_ACTION_BOOK_SESSION -> {
                val start = action.startTimeMillis ?: return
                val end = action.endTimeMillis ?: return
                val title = action.textValue ?: "ReadKeeper"
                timeApi.createTimeEntry(
                    TimeUpsertRequest(
                        type = READING_TIME_TYPE,
                        start = Instant.ofEpochMilli(start).toString(),
                        end = Instant.ofEpochMilli(maxOf(start + 1, end)).toString(),
                        title = title,
                        description = "ReadKeeper reading session",
                    )
                )
            }
        }
    }

    private suspend fun refreshMerriam() {
        val stat = merriamApi.getStat()
        updateSnapshot { it.copy(merriamLatest = stat.latest) }
    }

    private suspend fun refreshReadKeeper() {
        val latestBook = bookApi.getReading(size = 1)
            .map { it.toBookDomain() }
            .firstOrNull()
        updateSnapshot { snapshot ->
            if (latestBook == null) {
                snapshot.copy(
                    readKeeperBookId = null,
                    readKeeperTitle = null,
                    readKeeperCoverUrl = null,
                    readKeeperPageFormat = null,
                    readKeeperCurrentPage = null,
                    readKeeperProgressPercentage = null,
                    readKeeperPages = null,
                    readKeeperSessionStartedAt = null,
                    readKeeperSessionStartPage = null,
                    readKeeperSessionStartProgressPercentage = null,
                )
            } else {
                snapshot.withReadKeeperBook(latestBook)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun refreshHaircut() {
        val haircuts = haircutApi.getHaircuts()
            .map { it.toHaircutDomain() }
            .sortedByDescending { it.date }
        val days = haircuts.firstOrNull()?.date?.let { latestDate ->
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
            (today.toEpochDays() - latestDate.toEpochDays()).toInt()
        }
        updateSnapshot { it.copy(haircutDays = days) }
    }

    private suspend fun refreshWeight() {
        val latestWeight = weightApi.getWeights()
            .map { it.toWeightDomain() }
            .maxByOrNull { it.createdTime }
        updateSnapshot {
            it.copy(
                weightValue = latestWeight?.weight ?: it.weightValue,
                weightRecordedAt = latestWeight?.createdTime ?: it.weightRecordedAt,
            )
        }
    }

    private suspend fun ensureSnapshot(): HomeSnapshotEntity =
        homeDao.getSnapshot() ?: HomeSnapshotEntity().also { homeDao.upsertSnapshot(it) }

    private suspend fun updateSnapshot(transform: (HomeSnapshotEntity) -> HomeSnapshotEntity) {
        val current = ensureSnapshot()
        homeDao.upsertSnapshot(transform(current).copy(updatedAt = System.currentTimeMillis()))
    }

    private fun HomeSnapshotEntity.withReadKeeperBook(book: Book): HomeSnapshotEntity =
        copy(
            readKeeperBookId = book.id,
            readKeeperTitle = book.title,
            readKeeperCoverUrl = book.coverUrl,
            readKeeperPageFormat = book.pageFormat.wireValue,
            readKeeperCurrentPage = book.currentPage,
            readKeeperProgressPercentage = book.progressPercentage,
            readKeeperPages = book.pages,
            readKeeperSessionStartedAt = readKeeperSessionStartedAt?.takeIf { readKeeperBookId == book.id },
            readKeeperSessionStartPage = readKeeperSessionStartPage?.takeIf { readKeeperBookId == book.id },
            readKeeperSessionStartProgressPercentage = readKeeperSessionStartProgressPercentage
                ?.takeIf { readKeeperBookId == book.id },
        )

    private fun HomeSnapshotEntity?.toHomeSnapshot(): HomeSnapshot {
        if (this == null) return HomeSnapshot()
        val readKeeper = readKeeperBookId?.let { bookId ->
            HomeReadKeeperBook(
                id = bookId,
                title = readKeeperTitle.orEmpty(),
                coverUrl = readKeeperCoverUrl,
                pageFormat = BookPageFormat.fromWire(readKeeperPageFormat),
                currentPage = readKeeperCurrentPage,
                progressPercentage = readKeeperProgressPercentage,
                pages = readKeeperPages,
                sessionStartedAt = readKeeperSessionStartedAt,
                sessionStartPage = readKeeperSessionStartPage,
                sessionStartProgressPercentage = readKeeperSessionStartProgressPercentage,
            )
        }
        return HomeSnapshot(
            merriamLatest = merriamLatest,
            readKeeperBook = readKeeper,
            haircutDays = haircutDays,
            weightValue = weightValue,
            weightRecordedAt = weightRecordedAt,
        )
    }
}

private const val READING_TIME_TYPE = 2
