package app.tinks.tink.weight.db


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight")
data class WeightEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,
    val remoteId: Int? = null,
    val weight: Double,
    val createdAt: Long,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
