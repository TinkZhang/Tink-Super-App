package app.tinks.tink.yt

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

enum class YtDownloadKind(val label: String, val mimeType: String) {
    AUDIO("Audio", "audio/mp4"), VIDEO("Video", "video/mp4")
}

enum class YtDownloadStatus {
    QUEUED, DOWNLOADING, PROCESSING, READY, FAILED, CANCELLED;
    val isActive: Boolean get() = this in setOf(QUEUED, DOWNLOADING, PROCESSING)
}

@Entity(tableName = "yt_download", indices = [Index(value = ["videoId", "kind"], unique = true)])
data class YtDownload(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoId: String,
    val kind: YtDownloadKind,
    val attempt: String,
    val title: String = videoId,
    val status: YtDownloadStatus = YtDownloadStatus.QUEUED,
    val progress: Int = 0,
    val filePath: String? = null,
    val sizeBytes: Long = 0,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface YtDownloadDao {
    @Query("SELECT * FROM yt_download ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<YtDownload>>

    @Query("SELECT * FROM yt_download")
    suspend fun all(): List<YtDownload>

    @Query("SELECT * FROM yt_download WHERE id = :id")
    suspend fun get(id: Int): YtDownload?

    @Query("SELECT * FROM yt_download WHERE videoId = :videoId AND kind = :kind")
    suspend fun find(videoId: String, kind: YtDownloadKind): YtDownload?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(download: YtDownload): Long

    @Query("""UPDATE yt_download SET attempt = :attempt, status = 'QUEUED', progress = 0,
        error = NULL, filePath = NULL, sizeBytes = 0 WHERE id = :id AND status IN ('FAILED', 'CANCELLED')""")
    suspend fun retry(id: Int, attempt: String): Int

    // Every worker write is conditional: a stopped or superseded attempt cannot overwrite a retry.
    @Query("""UPDATE yt_download SET status = :status, progress = :progress, error = :error
        WHERE id = :id AND attempt = :attempt AND status IN ('QUEUED', 'DOWNLOADING', 'PROCESSING')""")
    suspend fun updateActive(id: Int, attempt: String, status: YtDownloadStatus, progress: Int, error: String? = null): Int

    @Query("""UPDATE yt_download SET status = 'READY', progress = 100, title = :title,
        filePath = :path, sizeBytes = :size, error = NULL
        WHERE id = :id AND attempt = :attempt AND status IN ('QUEUED', 'DOWNLOADING', 'PROCESSING')""")
    suspend fun complete(id: Int, attempt: String, title: String, path: String, size: Long): Int

    @Query("""UPDATE yt_download SET status = 'FAILED', filePath = NULL, sizeBytes = 0,
        error = 'The downloaded file is missing. Retry to download it again.' WHERE id = :id AND status = 'READY'""")
    suspend fun markMissing(id: Int)
}

@Database(entities = [YtDownload::class], version = 1, exportSchema = false)
abstract class YtDatabase : RoomDatabase() {
    abstract fun downloads(): YtDownloadDao
}

@Module
@InstallIn(SingletonComponent::class)
object YtModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): YtDatabase =
        Room.databaseBuilder(context, YtDatabase::class.java, "yt-downloads.db").build()

    @Provides
    fun downloads(database: YtDatabase): YtDownloadDao = database.downloads()

    @Provides
    fun scheduler(scheduler: AndroidYtDownloadScheduler): YtDownloadScheduler = scheduler

    @Provides
    fun repository(repository: YtDownloadRepository): YtDownloads = repository

    @Provides
    fun engine(engine: YoutubeDlEngine): YtDownloadEngine = engine
}
