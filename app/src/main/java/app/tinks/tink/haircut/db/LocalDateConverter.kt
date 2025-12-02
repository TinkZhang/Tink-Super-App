package app.tinks.tink.haircut.db

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate

class LocalDateConverter {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? {
        return date?.toEpochDays()
    }

    @TypeConverter
    fun toLocalDate(epochDays: Long?): LocalDate? {
        return epochDays?.let { LocalDate.fromEpochDays(it) }
    }
}