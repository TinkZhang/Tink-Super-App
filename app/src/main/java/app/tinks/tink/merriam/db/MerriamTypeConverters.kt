package app.tinks.tink.merriam.db

import androidx.room.TypeConverter

class MerriamTypeConverters {
    @TypeConverter
    fun fromList(value: List<String>) = value.joinToString(separator = ",")

    @TypeConverter
    fun toList(value: String) = value.split(",")
}