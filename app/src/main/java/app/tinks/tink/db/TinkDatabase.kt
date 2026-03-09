package app.tinks.tink.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.tinks.tink.merriam.db.MerriamDao
import app.tinks.tink.merriam.db.MerriamTypeConverters
import app.tinks.tink.merriam.db.RootEntity

@Database(entities = [RootEntity::class], version = 2)
@TypeConverters(MerriamTypeConverters::class)
abstract class TinkDatabase : RoomDatabase() {
    abstract fun merriamDao(): MerriamDao
}
