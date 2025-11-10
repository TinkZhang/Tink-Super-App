package app.tinks.tink.weight.db


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weights")
data class WeightEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val id: Int?, // Supabase ID
    val createdAt: String?,
    val weight: Double,
    val isSynced: Boolean = false // 是否已同步到 Supabase
)