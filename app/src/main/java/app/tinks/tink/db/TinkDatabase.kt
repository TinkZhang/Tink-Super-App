package app.tinks.tink.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.tinks.tink.haircut.db.HaircutDao
import app.tinks.tink.haircut.db.HaircutEntity
import app.tinks.tink.haircut.db.LocalDateConverter
import app.tinks.tink.merriam.db.MerriamDao
import app.tinks.tink.merriam.db.MerriamTypeConverters
import app.tinks.tink.merriam.db.RootEntity
import app.tinks.tink.weight.db.WeightDao
import app.tinks.tink.weight.db.WeightEntity

@Database(entities = [WeightEntity::class, HaircutEntity::class, RootEntity::class], version = 2)
@TypeConverters(LocalDateConverter::class, MerriamTypeConverters::class)
abstract class TinkDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao
    abstract fun haircutDao(): HaircutDao
    abstract fun merriamDao(): MerriamDao
}
