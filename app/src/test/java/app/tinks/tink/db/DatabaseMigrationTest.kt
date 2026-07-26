package app.tinks.tink.db

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.tinks.tink.di.DatabaseModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class DatabaseMigrationTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private var database: TinkDatabase? = null

    @Before
    fun setUp() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun bundledVersionThreeDatabase_migratesToReadKeeperOfflineSchema() {
        val db = DatabaseModule.provideDatabase(context).also { database = it }
        val sqlite = db.openHelper.writableDatabase

        assertEquals(6, sqlite.version)
        assertTrue(sqlite.tableColumns("book_cache").contains("hasPendingProgress"))
        assertTrue(sqlite.tableColumns("book_pending_progress").contains("timeSynced"))
        assertTrue(sqlite.tableColumns("home_snapshot").contains("readKeeperBookId"))
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.tableColumns(table: String): Set<String> {
        query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow("name")
            return buildSet {
                while (cursor.moveToNext()) add(cursor.getString(nameColumn))
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "tink.db"
    }
}
