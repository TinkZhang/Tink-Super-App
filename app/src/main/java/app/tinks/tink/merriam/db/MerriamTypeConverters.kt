package app.tinks.tink.merriam.db

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class MerriamTypeConverters {
    @TypeConverter
    fun fromList(value: List<String>) = Json.encodeToString(value)

    @TypeConverter
    fun toList(value: String) = Json.decodeFromString<List<String>>(value)
}