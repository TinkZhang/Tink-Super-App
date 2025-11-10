package app.tinks.tink.db

import androidx.room.Database
import androidx.room.RoomDatabase
import app.tinks.tink.weight.db.WeightDao
import app.tinks.tink.weight.db.WeightEntity

@Database(entities = [WeightEntity::class], version = 1)
abstract class TinkDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao
}