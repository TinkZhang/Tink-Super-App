package app.tinks.tink.di

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TinkDatabase {
        return Room.databaseBuilder(
            context,
            TinkDatabase::class.java,
            "tink.db"
        ).build()
    }

    @Provides
    fun provideWeightDao(db: TinkDatabase) = db.weightDao()
}