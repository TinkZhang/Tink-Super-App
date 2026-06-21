package app.tinks.tink.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.tinks.tink.db.TinkDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `zi`")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `salesforce_question` (
                  `id` INTEGER NOT NULL,
                  `prompt` TEXT NOT NULL,
                  `answer_labels` TEXT NOT NULL,
                  `explanation` TEXT,
                  PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `salesforce_choice` (
                  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                  `question_id` INTEGER NOT NULL,
                  `label` TEXT NOT NULL,
                  `text` TEXT NOT NULL,
                  FOREIGN KEY(`question_id`) REFERENCES `salesforce_question`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_salesforce_choice_question_id` ON `salesforce_choice` (`question_id`)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_salesforce_choice_question_id_label` ON `salesforce_choice` (`question_id`, `label`)"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `salesforce_local_progress` (
                  `question_id` INTEGER NOT NULL,
                  `done` INTEGER NOT NULL DEFAULT 0,
                  `correct_count` INTEGER NOT NULL DEFAULT 0,
                  `incorrect_count` INTEGER NOT NULL DEFAULT 0,
                  `last_answered_at` TEXT,
                  PRIMARY KEY(`question_id`)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `diary` (
                  `id` TEXT NOT NULL,
                  `title` TEXT NOT NULL,
                  `content` TEXT NOT NULL,
                  `startDate` INTEGER NOT NULL,
                  `endDate` INTEGER NOT NULL,
                  `diaryType` TEXT NOT NULL,
                  `timeEntryId` INTEGER,
                  PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `draft` (
                  `id` TEXT NOT NULL,
                  `title` TEXT NOT NULL,
                  `content` TEXT NOT NULL,
                  `startDate` INTEGER NOT NULL,
                  `endDate` INTEGER NOT NULL,
                  `diaryType` TEXT NOT NULL,
                  `updateTime` INTEGER NOT NULL,
                  PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `home_snapshot` (
                  `id` INTEGER NOT NULL,
                  `merriamLatest` INTEGER,
                  `readKeeperBookId` INTEGER,
                  `readKeeperTitle` TEXT,
                  `readKeeperCoverUrl` TEXT,
                  `readKeeperPageFormat` TEXT,
                  `readKeeperCurrentPage` INTEGER,
                  `readKeeperProgressPercentage` REAL,
                  `readKeeperPages` INTEGER,
                  `readKeeperSessionStartedAt` INTEGER,
                  `readKeeperSessionStartPage` INTEGER,
                  `readKeeperSessionStartProgressPercentage` REAL,
                  `haircutDays` INTEGER,
                  `weightValue` REAL,
                  `weightRecordedAt` INTEGER,
                  `updatedAt` INTEGER NOT NULL,
                  PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `home_pending_action` (
                  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                  `type` TEXT NOT NULL,
                  `startIntValue` INTEGER,
                  `targetIntValue` INTEGER,
                  `bookId` INTEGER,
                  `pageValue` INTEGER,
                  `progressValue` REAL,
                  `weightValue` REAL,
                  `startTimeMillis` INTEGER,
                  `endTimeMillis` INTEGER,
                  `textValue` TEXT,
                  `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TinkDatabase {
        return Room.databaseBuilder(
            context,
            TinkDatabase::class.java,
            "tink.db"
        )
            .createFromAsset("database/tink.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    @Provides
    fun provideMerriamDao(db: TinkDatabase) = db.merriamDao()

    @Provides
    fun provideSalesforceDao(db: TinkDatabase) = db.salesforceDao()

    @Provides
    fun provideDiaryDao(db: TinkDatabase) = db.diaryDao()

    @Provides
    fun provideHomeDao(db: TinkDatabase) = db.homeDao()
}
