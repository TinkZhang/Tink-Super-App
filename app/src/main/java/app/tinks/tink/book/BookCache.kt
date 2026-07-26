package app.tinks.tink.book

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "book_cache")
data class BookEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val publisher: String?,
    val author: String?,
    val coverUrl: String?,
    val isbn: String?,
    val description: String?,
    val rating: Double?,
    val pages: Int?,
    val publishYear: Int?,
    val state: String,
    val platform: String?,
    val pageFormat: String,
    val currentPage: Int?,
    val readingDurationMinutes: Int,
    val archivedDate: String?,
    val category: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val hasPendingProgress: Boolean = false,
)

@Entity(tableName = "book_pending_progress")
data class BookPendingProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val currentPage: Int?,
    val readingDurationMinutes: Int,
    val timeType: Int?,
    val timeStart: String?,
    val timeEnd: String?,
    val timeTitle: String?,
    val timeDescription: String?,
    val bookSynced: Boolean = false,
    val timeSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface BookDao {
    @Query("SELECT * FROM book_cache WHERE state IN (:states) ORDER BY COALESCE(updatedAt, createdAt) DESC")
    suspend fun getBooks(states: List<String>): List<BookEntity>

    @Query("SELECT * FROM book_cache WHERE id = :bookId")
    suspend fun getBook(bookId: Long): BookEntity?

    @Query(
        """
        SELECT * FROM book_cache
        WHERE state = 'reading'
        ORDER BY COALESCE(updatedAt, createdAt) DESC
        LIMIT 1
        """
    )
    fun observeLatestReadingBook(): Flow<BookEntity?>

    @Query(
        """
        SELECT * FROM book_cache
        WHERE state = 'reading'
        ORDER BY COALESCE(updatedAt, createdAt) DESC
        LIMIT 1
        """
    )
    suspend fun getLatestReadingBook(): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBooks(books: List<BookEntity>)

    @Query("DELETE FROM book_cache WHERE id = :bookId")
    suspend fun deleteBook(bookId: Long)

    @Query("DELETE FROM book_cache WHERE state = :state AND hasPendingProgress = 0")
    suspend fun deleteSyncedBooksForState(state: String)

    @Query(
        """
        UPDATE book_cache
        SET currentPage = :currentPage,
            readingDurationMinutes = :readingDurationMinutes,
            updatedAt = :updatedAt,
            hasPendingProgress = 1
        WHERE id = :bookId
        """
    )
    suspend fun applyLocalProgress(
        bookId: Long,
        currentPage: Int?,
        readingDurationMinutes: Int,
        updatedAt: String,
    )

    @Query("UPDATE book_cache SET hasPendingProgress = 0 WHERE id = :bookId")
    suspend fun clearPendingProgress(bookId: Long)

    @Insert
    suspend fun insertPendingProgress(action: BookPendingProgressEntity): Long

    @Query("SELECT * FROM book_pending_progress ORDER BY createdAt ASC, id ASC")
    suspend fun getPendingProgress(): List<BookPendingProgressEntity>

    @Query("UPDATE book_pending_progress SET bookSynced = 1 WHERE id = :actionId")
    suspend fun markBookSynced(actionId: Long)

    @Query("UPDATE book_pending_progress SET timeSynced = 1 WHERE id = :actionId")
    suspend fun markTimeSynced(actionId: Long)

    @Query("DELETE FROM book_pending_progress WHERE id = :actionId")
    suspend fun deletePendingProgress(actionId: Long)

    @Query("SELECT COUNT(*) FROM book_pending_progress WHERE bookId = :bookId")
    suspend fun pendingProgressCount(bookId: Long): Int

    @Query("DELETE FROM book_pending_progress WHERE bookId = :bookId")
    suspend fun deletePendingProgressForBook(bookId: Long)

    @Transaction
    suspend fun replaceRemoteBooks(states: List<String>, books: List<BookEntity>) {
        states.forEach { state -> deleteSyncedBooksForState(state) }
        upsertBooks(books.filterNot { remote -> getBook(remote.id)?.hasPendingProgress == true })
    }

    @Transaction
    suspend fun enqueueProgress(
        bookId: Long,
        currentPage: Int?,
        readingDurationMinutes: Int,
        updatedAt: String,
        action: BookPendingProgressEntity,
    ) {
        applyLocalProgress(
            bookId = bookId,
            currentPage = currentPage,
            readingDurationMinutes = readingDurationMinutes,
            updatedAt = updatedAt,
        )
        insertPendingProgress(action)
    }

    @Transaction
    suspend fun deleteBookAndPendingProgress(bookId: Long) {
        deletePendingProgressForBook(bookId)
        deleteBook(bookId)
    }
}

internal fun Book.toEntity(hasPendingProgress: Boolean = false): BookEntity =
    BookEntity(
        id = id,
        title = title,
        publisher = publisher,
        author = author,
        coverUrl = coverUrl,
        isbn = isbn,
        description = description,
        rating = rating,
        pages = pages,
        publishYear = publishYear,
        state = when (archiveStatus) {
            ArchiveStatus.Done -> "done"
            ArchiveStatus.Abandon -> "abandon"
            null -> state.wireValue
        },
        platform = platform,
        pageFormat = pageFormat.wireValue,
        currentPage = currentPage,
        readingDurationMinutes = readingDurationMinutes,
        archivedDate = archivedDate,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt,
        hasPendingProgress = hasPendingProgress,
    )

internal fun BookEntity.toDomain(): Book {
    val format = BookPageFormat.fromWire(pageFormat)
    return Book(
        id = id,
        title = title,
        publisher = publisher,
        author = author,
        coverUrl = coverUrl,
        isbn = isbn,
        description = description,
        rating = rating,
        pages = pages,
        publishYear = publishYear,
        state = when (state) {
            "wish" -> BookState.Wish
            "done", "abandon" -> BookState.Archived
            else -> BookState.Reading
        },
        platform = platform,
        pageFormat = format,
        currentPage = currentPage,
        progressPercentage = currentPage.toProgressPercentage(format, pages),
        readingDurationMinutes = readingDurationMinutes,
        archiveStatus = when (state) {
            "done" -> ArchiveStatus.Done
            "abandon" -> ArchiveStatus.Abandon
            else -> null
        },
        archivedDate = archivedDate,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

internal fun BookState.cacheStates(): List<String> =
    when (this) {
        BookState.Wish -> listOf("wish")
        BookState.Reading -> listOf("reading")
        BookState.Archived -> listOf("done", "abandon")
    }
