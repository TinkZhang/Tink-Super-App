package app.tinks.tink.haircut.db


import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Entity(tableName = "haircut")
@Serializable
data class HaircutEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Int? = null,
    val remoteId: Int? = null,
    val price: Int = 0,
    val shopName: String,
    val date: LocalDate,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
