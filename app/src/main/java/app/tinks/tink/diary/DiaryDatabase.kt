package app.tinks.tink.diary

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary ORDER BY endDate DESC")
    fun getAllDiaries(): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diary ORDER BY endDate DESC LIMIT 3")
    fun getRecentDiaries(): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM draft ORDER BY updateTime DESC")
    fun getAllDrafts(): Flow<List<DraftEntity>>

    @Query("SELECT * FROM diary WHERE id = :id")
    suspend fun getDiary(id: String): DiaryEntity?

    @Query("SELECT * FROM draft WHERE id = :id")
    suspend fun getDraft(id: String): DraftEntity?

    @Query("SELECT * FROM diary WHERE startDate BETWEEN :start AND :end AND diaryType = :type")
    suspend fun getDiariesBetweenDates(
        start: LocalDate,
        end: LocalDate,
        type: DiaryType,
    ): List<DiaryEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM diary WHERE startDate BETWEEN :start AND :end AND diaryType = :type LIMIT 1)")
    suspend fun hasSummary(
        start: LocalDate,
        end: LocalDate,
        type: DiaryType,
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiary(diary: DiaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftEntity)

    @Query("DELETE FROM diary WHERE id = :id")
    suspend fun deleteDiary(id: String)

    @Query("DELETE FROM draft WHERE id = :id")
    suspend fun deleteDraft(id: String)
}

@Entity(tableName = "diary")
data class DiaryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo val title: String,
    @ColumnInfo val content: String,
    @ColumnInfo val startDate: LocalDate,
    @ColumnInfo val endDate: LocalDate,
    @ColumnInfo val diaryType: DiaryType,
    @ColumnInfo val timeEntryId: Long?,
)

@Entity(tableName = "draft")
data class DraftEntity(
    @PrimaryKey val id: String,
    @ColumnInfo val title: String,
    @ColumnInfo val content: String,
    @ColumnInfo val startDate: LocalDate,
    @ColumnInfo val endDate: LocalDate,
    @ColumnInfo val diaryType: DiaryType,
    @ColumnInfo val updateTime: LocalDateTime,
)

fun Diary.toEntity(): DiaryEntity = DiaryEntity(
    id = id,
    title = title,
    content = content,
    startDate = startDate,
    endDate = endDate,
    diaryType = type,
    timeEntryId = timeEntryId,
)

fun Diary.toDraftEntity(): DraftEntity = DraftEntity(
    id = id,
    title = title,
    content = content,
    startDate = startDate,
    endDate = endDate,
    diaryType = type,
    updateTime = LocalDateTime.now(),
)

fun DiaryEntity.toDiary(): Diary = Diary(
    title = title,
    content = content,
    startDate = startDate,
    endDate = endDate,
    type = diaryType,
    timeEntryId = timeEntryId,
)

fun DraftEntity.toDiary(): Diary = Diary(
    title = title,
    content = content,
    startDate = startDate,
    endDate = endDate,
    type = diaryType,
)

object DiaryDateConverters {
    @TypeConverter
    @JvmStatic
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    @JvmStatic
    fun toLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)
}

object DiaryDateTimeConverters {
    @TypeConverter
    @JvmStatic
    fun fromLocalDateTime(time: LocalDateTime?): Long? =
        time?.atZone(ZoneId.systemDefault())?.toEpochSecond()

    @TypeConverter
    @JvmStatic
    fun toLocalDateTime(epochSecond: Long?): LocalDateTime? =
        epochSecond?.let { LocalDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneId.systemDefault()) }
}

object DiaryTypeConverters {
    @TypeConverter
    @JvmStatic
    fun fromDiaryType(value: DiaryType?): String? = value?.name

    @TypeConverter
    @JvmStatic
    fun toDiaryType(value: String?): DiaryType? = value?.let(DiaryType::valueOf)
}
