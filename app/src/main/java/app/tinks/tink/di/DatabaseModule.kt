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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TinkDatabase {
        return Room.databaseBuilder(
            context,
            TinkDatabase::class.java,
            "tink.db"
        )
            .createFromAsset("database/tink.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideMerriamDao(db: TinkDatabase) = db.merriamDao()
}
