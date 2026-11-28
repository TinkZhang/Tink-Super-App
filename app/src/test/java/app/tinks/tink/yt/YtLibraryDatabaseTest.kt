package app.tinks.tink.yt

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class YtLibraryDatabaseTest {
    private val context: Application = ApplicationProvider.getApplicationContext()

    @Test fun migratesExistingDownloadsWithoutLosingTheirFilesOrMetadata() = runBlocking {
        val name = "yt-migration-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE yt_download (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, videoId TEXT NOT NULL, kind TEXT NOT NULL, attempt TEXT NOT NULL, title TEXT NOT NULL, status TEXT NOT NULL, progress INTEGER NOT NULL, filePath TEXT, sizeBytes INTEGER NOT NULL, error TEXT, createdAt INTEGER NOT NULL)")
            old.execSQL("CREATE UNIQUE INDEX index_yt_download_videoId_kind ON yt_download(videoId, kind)")
            old.execSQL("INSERT INTO yt_download VALUES(1, 'LbCHAQ7HKu4', 'AUDIO', 'old', 'Saved episode', 'READY', 100, '/saved/media.m4a', 123, NULL, 42)")
            old.version = 1
        }
        val db = Room.databaseBuilder(context, YtDatabase::class.java, name).addMigrations(YT_MIGRATION_1_2, YT_MIGRATION_2_3, YT_MIGRATION_3_4, YT_MIGRATION_4_5, YT_MIGRATION_5_6, YT_MIGRATION_6_7, YT_MIGRATION_7_8, YT_MIGRATION_8_9).build()
        try {
            val item = db.downloads().get(1)!!
            assertEquals("Saved episode", item.title)
            assertEquals("/saved/media.m4a", item.filePath)
            assertEquals(YtDownloadStatus.READY, item.status)
            assertTrue(item.standalone)
            assertFalse(item.background)
            assertEquals(0L, item.positionMs)
            assertEquals("", item.labels)
            assertNull(item.downloadedAt)
            assertTrue(db.subscriptions().all().isEmpty())
            db.subscriptions().insert(YtSubscription("playlist", "New group"))
            assertEquals("0 8 * * *", db.subscriptions().get("playlist")!!.cron)
            assertEquals(1, db.subscriptions().get("playlist")!!.latestCount)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun migratesExistingSubscriptionsAndClaimsEachCronOccurrenceOnce() = runBlocking {
        val name = "yt-v2-migration.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE yt_download (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, videoId TEXT NOT NULL, kind TEXT NOT NULL, attempt TEXT NOT NULL, title TEXT NOT NULL, status TEXT NOT NULL, progress INTEGER NOT NULL, filePath TEXT, sizeBytes INTEGER NOT NULL, error TEXT, standalone INTEGER NOT NULL DEFAULT 1, background INTEGER NOT NULL DEFAULT 0, positionMs INTEGER NOT NULL DEFAULT 0, played INTEGER NOT NULL DEFAULT 0, createdAt INTEGER NOT NULL)")
            old.execSQL("CREATE UNIQUE INDEX index_yt_download_videoId_kind ON yt_download(videoId, kind)")
            old.execSQL("CREATE TABLE yt_subscription (playlistId TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, titleRegex TEXT NOT NULL, enabled INTEGER NOT NULL, lastChecked INTEGER NOT NULL, error TEXT)")
            old.execSQL("CREATE TABLE yt_playlist_entry (playlistId TEXT NOT NULL, videoId TEXT NOT NULL, title TEXT NOT NULL, position INTEGER NOT NULL, dismissed INTEGER NOT NULL, PRIMARY KEY(playlistId, videoId))")
            old.execSQL("INSERT INTO yt_subscription VALUES('p', 'Saved playlist', '(?i)history', 1, 123, NULL)")
            old.execSQL("INSERT INTO yt_playlist_entry VALUES('p', 'video', 'Deleted episode', 1, 1)")
            old.version = 2
        }
        val db = Room.databaseBuilder(context, YtDatabase::class.java, name).addMigrations(YT_MIGRATION_2_3, YT_MIGRATION_3_4, YT_MIGRATION_4_5, YT_MIGRATION_5_6, YT_MIGRATION_6_7, YT_MIGRATION_7_8, YT_MIGRATION_8_9).build()
        try {
            val item = db.subscriptions().get("p")!!
            assertEquals("Saved playlist", item.title)
            assertEquals("(?i)history", item.titleRegex)
            assertEquals("0 8 * * *", item.cron)
            assertEquals(10, item.latestCount)
            assertTrue(item.autoDelete)
            assertEquals(YtSubscriptionFormat.AUDIO, item.format)
            assertFalse(item.downloadAllOnce)
            assertEquals(1f, item.playbackSpeed)
            assertTrue(db.subscriptions().entries("p").single().dismissed)
            assertEquals(YtDownloadKind.AUDIO, db.subscriptions().entries("p").single().kind)
            assertEquals(1, db.subscriptions().advanceSchedule("p", 0, 1000, "UTC"))
            assertEquals(0, db.subscriptions().advanceSchedule("p", 0, 1000, "UTC"))
            db.subscriptions().edit("p", "", false, "0 */6 * * *", 5, 2000, "UTC")
            assertEquals(0, db.subscriptions().advanceSchedule("p", 2000, 3000, "UTC"))
            db.subscriptions().setPlaybackSpeed("p", 1.75f)
            db.subscriptions().setArtwork("p", "https://i.ytimg.com/vi/LbCHAQ7HKu4/hqdefault.jpg")
            assertEquals(1.75f, db.subscriptions().get("p")!!.playbackSpeed)
            assertNotNull(db.subscriptions().get("p")!!.artworkUrl)
            db.subscriptions().saveVideoDate(YtVideoDate("video", 20260))
            assertEquals(20260L, db.subscriptions().videoDate("video")!!.uploadDate)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun repeatedChecksAndOverlappingPlaylistsDoNotDownloadDuplicatesOrBackfill() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, YtDatabase::class.java).build()
        try {
            val subscription = YtSubscription("p", "Playlist", latestCount = 2)
            db.subscriptions().insert(subscription)
            val episodes = listOf("new", "middle", "old").mapIndexed { index, id -> YtPlaylistEntry("p", id, id, index) }
            val dates = mapOf("old" to 1L, "middle" to 2L, "new" to 3L)
            repeat(2) { db.withTransaction { enqueueLatestPlaylistEpisodes(db, subscription, episodes, dates) } }
            assertEquals(setOf("middle", "new"), db.downloads().all().map { it.videoId }.toSet())
            val before = db.downloads().all().associate { it.videoId to it.attempt }
            val second = subscription.copy(playlistId = "other")
            db.subscriptions().insert(second)
            db.withTransaction { enqueueLatestPlaylistEpisodes(db, second, episodes.map { it.copy(playlistId = "other") }, dates) }
            assertEquals(before, db.downloads().all().associate { it.videoId to it.attempt })
            db.subscriptions().dismiss("new")
            db.downloads().delete(db.downloads().find("new", YtDownloadKind.AUDIO)!!.id)
            db.withTransaction { enqueueLatestPlaylistEpisodes(db, subscription, episodes, dates) }
            assertEquals(listOf("middle"), db.downloads().all().map { it.videoId })
            val updated = listOf(YtPlaylistEntry("p", "newest", "newest", 0)) + episodes.map { it.copy(position = it.position + 1) }
            db.withTransaction { enqueueLatestPlaylistEpisodes(db, subscription, updated, dates + ("newest" to 4L)) }
            assertEquals(setOf("middle", "newest"), db.downloads().all().map { it.videoId }.toSet())
        } finally { db.close() }
    }

    @Test fun sharedMembershipKeepsDeletionTombstoneAndPlaybackState() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, YtDatabase::class.java).build()
        try {
            db.subscriptions().insert(YtSubscription("a", "A"))
            db.subscriptions().insert(YtSubscription("b", "B"))
            val entry = YtPlaylistEntry("a", "LbCHAQ7HKu4", "Episode", 0)
            db.subscriptions().addEntry(entry)
            db.subscriptions().addEntry(entry.copy(playlistId = "b"))
            val id = db.downloads().insert(YtDownload(videoId = entry.videoId, kind = YtDownloadKind.AUDIO, attempt = "one")).toInt()
            db.downloads().playback(id, 12345, false)
            assertEquals(12345L, db.downloads().get(id)!!.positionMs)
            db.subscriptions().dismiss(entry.videoId)
            db.downloads().delete(id)
            db.subscriptions().addEntry(entry)
            assertTrue(db.subscriptions().entries("a").single().dismissed)
            assertTrue(db.subscriptions().entries("b").single().dismissed)
            assertNull(db.downloads().get(id))
        } finally { db.close() }
    }
    @Test fun migratesLegacyBothToVideoWithoutDeletingAudio() = runBlocking {
        val name = "yt-legacy-both.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, YtDatabase::class.java, name).build()
        try {
            db.subscriptions().insert(YtSubscription("p", "Keep", latestCount = 3))
            db.downloads().insert(YtDownload(videoId = "LbCHAQ7HKu4", kind = YtDownloadKind.AUDIO, attempt = "a"))
            db.close()
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { old ->
                old.execSQL("ALTER TABLE yt_download DROP COLUMN downloadedAt")
                old.execSQL("ALTER TABLE yt_subscription DROP COLUMN downloadAllOnce")
                old.execSQL("UPDATE yt_subscription SET format = 'BOTH'")
                old.version = 6
            }
            db = Room.databaseBuilder(context, YtDatabase::class.java, name).addMigrations(YT_MIGRATION_6_7, YT_MIGRATION_7_8, YT_MIGRATION_8_9).build()
            assertEquals(YtSubscriptionFormat.VIDEO, db.subscriptions().get("p")!!.format)
            assertEquals(3, db.subscriptions().get("p")!!.latestCount)
            assertEquals(YtDownloadKind.AUDIO, db.downloads().all().single().kind)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
