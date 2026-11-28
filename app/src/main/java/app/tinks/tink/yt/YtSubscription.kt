package app.tinks.tink.yt

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.net.URI
import java.net.URLDecoder

enum class YtSubscriptionFormat(val label: String, val kinds: List<YtDownloadKind>) {
    AUDIO("Audio", listOf(YtDownloadKind.AUDIO)), VIDEO("Video", listOf(YtDownloadKind.VIDEO))
}

@Entity(tableName = "yt_subscription")
data class YtSubscription(
    @PrimaryKey val playlistId: String,
    val title: String,
    val titleRegex: String = "",
    val enabled: Boolean = true,
    val lastChecked: Long = 0,
    val error: String? = null,
    @ColumnInfo(defaultValue = "'0 8 * * *'") val cron: String = "0 8 * * *",
    // Keep the legacy SQL default for migration compatibility; Room inserts the explicit app default.
    @ColumnInfo(defaultValue = "10") val latestCount: Int = 1,
    @ColumnInfo(defaultValue = "0") val nextCheck: Long = 0,
    @ColumnInfo(defaultValue = "''") val scheduleZone: String = "",
    @ColumnInfo(defaultValue = "1") val autoDelete: Boolean = true,
    @ColumnInfo(defaultValue = "1.0") val playbackSpeed: Float = 1f,
    val artworkUrl: String? = null,
    @ColumnInfo(defaultValue = "'AUDIO'") val format: YtSubscriptionFormat = YtSubscriptionFormat.AUDIO,
    val checkMessage: String? = null,
    @ColumnInfo(defaultValue = "0") val downloadAllOnce: Boolean = false,

)

// Membership is independent of the downloaded file: shared episodes need only one local copy.
// Dismissed entries prevent deleted episodes returning on every daily check.
@Entity(tableName = "yt_playlist_entry", primaryKeys = ["playlistId", "videoId", "kind"])
data class YtPlaylistEntry(
    val playlistId: String,
    val videoId: String,
    val title: String,
    val position: Int,
    val dismissed: Boolean = false,
    @ColumnInfo(defaultValue = "'AUDIO'") val kind: YtDownloadKind = YtDownloadKind.AUDIO,
)

@Entity(tableName = "yt_video_date")
data class YtVideoDate(@PrimaryKey val videoId: String, val uploadDate: Long)

@Dao
interface YtSubscriptionDao {
    @Query("UPDATE yt_subscription SET checkMessage = :message WHERE playlistId = :id") suspend fun checkMessage(id: String, message: String)
    @Query("SELECT * FROM yt_video_date") fun observeDates(): Flow<List<YtVideoDate>>
    @Query("UPDATE yt_subscription SET playbackSpeed = :speed WHERE playlistId = :id") suspend fun setPlaybackSpeed(id: String, speed: Float)
    @Query("UPDATE yt_subscription SET artworkUrl = :url WHERE playlistId = :id") suspend fun setArtwork(id: String, url: String)
    @Query("UPDATE yt_playlist_entry SET dismissed = 1 WHERE playlistId = :id AND videoId = :videoId AND kind = :kind") suspend fun dismissFrom(id: String, videoId: String, kind: YtDownloadKind = YtDownloadKind.AUDIO)
    @Query("SELECT s.* FROM yt_subscription s INNER JOIN yt_playlist_entry e ON s.playlistId = e.playlistId WHERE e.videoId = :videoId AND e.dismissed = 0 AND e.kind = :kind")
    suspend fun memberships(videoId: String, kind: YtDownloadKind = YtDownloadKind.AUDIO): List<YtSubscription>

    @Query("SELECT * FROM yt_video_date WHERE videoId = :id") suspend fun videoDate(id: String): YtVideoDate?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveVideoDate(date: YtVideoDate)

    @Query("SELECT * FROM yt_subscription ORDER BY title") fun observe(): Flow<List<YtSubscription>>
    @Query("SELECT * FROM yt_playlist_entry ORDER BY position") fun observeEntries(): Flow<List<YtPlaylistEntry>>
    @Query("SELECT * FROM yt_subscription") suspend fun all(): List<YtSubscription>
    @Query("UPDATE yt_subscription SET nextCheck = :next, scheduleZone = :zone WHERE playlistId = :id AND enabled = 1 AND nextCheck = :expected")
    suspend fun advanceSchedule(id: String, expected: Long, next: Long, zone: String): Int
    @Query("SELECT * FROM yt_subscription WHERE playlistId = :id") suspend fun get(id: String): YtSubscription?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(item: YtSubscription)
    @Query("UPDATE yt_subscription SET titleRegex = :regex, enabled = :enabled, cron = :cron, latestCount = :latestCount, nextCheck = :nextCheck, scheduleZone = :zone, autoDelete = :autoDelete, playbackSpeed = :speed, format = :format, downloadAllOnce = :downloadAllOnce WHERE playlistId = :id")
    suspend fun edit(id: String, regex: String, enabled: Boolean, cron: String, latestCount: Int, nextCheck: Long, zone: String, autoDelete: Boolean = true, speed: Float = 1f, format: YtSubscriptionFormat = YtSubscriptionFormat.AUDIO, downloadAllOnce: Boolean = false)
    @Query("UPDATE yt_subscription SET title = :title, lastChecked = :time, error = :error WHERE playlistId = :id")
    suspend fun checked(id: String, title: String, time: Long, error: String?)
    @Query("SELECT * FROM yt_playlist_entry WHERE playlistId = :id") suspend fun entries(id: String): List<YtPlaylistEntry>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addEntry(entry: YtPlaylistEntry)
    @Query("UPDATE yt_playlist_entry SET title = :title, position = :position WHERE playlistId = :id AND videoId = :videoId AND kind = :kind")
    suspend fun refreshEntry(id: String, videoId: String, title: String, position: Int, kind: YtDownloadKind = YtDownloadKind.AUDIO)
    @Query("UPDATE yt_playlist_entry SET dismissed = 1 WHERE videoId = :videoId AND kind = :kind") suspend fun dismiss(videoId: String, kind: YtDownloadKind = YtDownloadKind.AUDIO)
    @Query("DELETE FROM yt_playlist_entry WHERE playlistId = :id") suspend fun deleteEntries(id: String)
    @Query("DELETE FROM yt_subscription WHERE playlistId = :id") suspend fun delete(id: String)
}

val YT_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE yt_download ADD COLUMN standalone INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE yt_download ADD COLUMN background INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE yt_download ADD COLUMN positionMs INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE yt_download ADD COLUMN played INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS yt_subscription (playlistId TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, titleRegex TEXT NOT NULL, enabled INTEGER NOT NULL, lastChecked INTEGER NOT NULL, error TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS yt_playlist_entry (playlistId TEXT NOT NULL, videoId TEXT NOT NULL, title TEXT NOT NULL, position INTEGER NOT NULL, dismissed INTEGER NOT NULL, PRIMARY KEY(playlistId, videoId))")
    }
}

fun youtubePlaylistId(input: String): String? = runCatching {
    val uri = URI(input.trim().trimEnd(','))
    if (uri.scheme !in setOf("http", "https") || uri.userInfo != null || uri.port != -1 ||
        uri.host?.lowercase() !in setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be")) return null
    if (uri.host != "youtu.be" && uri.path !in setOf("/playlist", "/watch")) return null
    uri.rawQuery.orEmpty().split('&').mapNotNull {
        val pair = it.split('=', limit = 2)
        if (pair.size == 2 && pair[0] == "list") URLDecoder.decode(pair[1], "UTF-8") else null
    }.singleOrNull()?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{10,100}")) }
}.getOrNull()

fun titleFilter(pattern: String): Regex? {
    require(pattern.length <= 300) { "Keep the title filter under 300 characters." }
    return if (pattern.isBlank()) null else try { Regex(pattern) }
    catch (_: IllegalArgumentException) { throw IllegalArgumentException("Invalid title regex. Check brackets and special characters.") }
}

fun playlistMatches(entries: List<YtPlaylistEntry>, pattern: String): List<YtPlaylistEntry> {
    val regex = titleFilter(pattern)
    return entries.filter { !it.dismissed && (regex == null || regex.containsMatchIn(it.title)) }.distinctBy { it.videoId }
}

val YT_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE yt_subscription ADD COLUMN cron TEXT NOT NULL DEFAULT '0 8 * * *'")
        db.execSQL("ALTER TABLE yt_subscription ADD COLUMN latestCount INTEGER NOT NULL DEFAULT 10")
        db.execSQL("ALTER TABLE yt_subscription ADD COLUMN nextCheck INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE yt_subscription ADD COLUMN scheduleZone TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE TABLE IF NOT EXISTS yt_video_date (videoId TEXT NOT NULL PRIMARY KEY, uploadDate INTEGER NOT NULL)")
    }
}

/** Limit before deduplication/dismissal so repeated checks never backfill older episodes. */
@Suppress("UNUSED_PARAMETER") // Retain source compatibility; cached dates never affect selection.
fun latestPlaylistEpisodes(entries: List<YtPlaylistEntry>, pattern: String, count: Int, dates: Map<String, Long> = emptyMap()): List<YtPlaylistEntry> {
    require(count in 1..500) { "Latest episodes must be between 1 and 500." }
    return playlistMatches(entries.take(count).map { it.copy(dismissed = false) }, pattern)
}

val YT_MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE yt_subscription ADD COLUMN autoDelete INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE yt_subscription ADD COLUMN playbackSpeed REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE yt_subscription ADD COLUMN artworkUrl TEXT")
    }
}

fun validatePlaybackSpeed(speed: Float) {
    require(speed.isFinite() && speed in 0.5f..3f) { "Playback speed must be between 0.5× and 3×." }
}

val YT_MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE yt_subscription ADD COLUMN format TEXT NOT NULL DEFAULT 'AUDIO'")
        db.execSQL("ALTER TABLE yt_subscription ADD COLUMN checkMessage TEXT")
        db.execSQL("CREATE TABLE yt_playlist_entry_new (playlistId TEXT NOT NULL, videoId TEXT NOT NULL, title TEXT NOT NULL, position INTEGER NOT NULL, dismissed INTEGER NOT NULL, kind TEXT NOT NULL DEFAULT 'AUDIO', PRIMARY KEY(playlistId, videoId, kind))")
        db.execSQL("INSERT INTO yt_playlist_entry_new (playlistId, videoId, title, position, dismissed) SELECT playlistId, videoId, title, position, dismissed FROM yt_playlist_entry")
        db.execSQL("DROP TABLE yt_playlist_entry")
        db.execSQL("ALTER TABLE yt_playlist_entry_new RENAME TO yt_playlist_entry")
    }
}

val YT_MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE yt_subscription SET format = 'VIDEO' WHERE format = 'BOTH'")
    }
}

val YT_MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE yt_subscription ADD COLUMN downloadAllOnce INTEGER NOT NULL DEFAULT 0")
    }
}

/** Inspect the head of regular playlists; an import intentionally has no entry limit. */
internal fun playlistScanLimit(subscription: YtSubscription): Int? =
    if (subscription.downloadAllOnce) null else subscription.latestCount

@Suppress("UNUSED_PARAMETER")
internal fun selectedPlaylistEpisodes(subscription: YtSubscription, fetched: List<YtPlaylistEntry>, dates: Map<String, Long> = emptyMap()): List<YtPlaylistEntry> =
    if (subscription.downloadAllOnce) playlistMatches(fetched, subscription.titleRegex)
    else latestPlaylistEpisodes(fetched, subscription.titleRegex, subscription.latestCount)
