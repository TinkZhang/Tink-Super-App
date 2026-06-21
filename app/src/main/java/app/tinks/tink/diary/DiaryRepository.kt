package app.tinks.tink.diary

import app.tinks.tink.time.TimeApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class DiaryRepository @Inject constructor(
    private val dao: DiaryDao,
    private val timeApi: TimeApi,
) {
    open fun getAllDiaries(): Flow<List<Diary>> =
        dao.getAllDiaries().map { rows -> rows.map { it.toDiary() } }

    open fun getRecentDiaries(): Flow<List<Diary>> =
        dao.getRecentDiaries().map { rows -> rows.map { it.toDiary() } }

    open fun getAllDrafts(): Flow<List<Diary>> =
        dao.getAllDrafts().map { rows -> rows.map { it.toDiary() } }

    open suspend fun getDiary(id: String): Diary? = withContext(Dispatchers.IO) {
        dao.getDiary(id)?.toDiary()
    }

    open suspend fun getDraft(id: String): Diary? = withContext(Dispatchers.IO) {
        dao.getDraft(id)?.toDiary()
    }

    open suspend fun saveDraft(diary: Diary, previousId: String? = null) = withContext(Dispatchers.IO) {
        if (previousId != null && previousId != diary.id) {
            dao.deleteDraft(previousId)
        }
        dao.insertDraft(diary.toDraftEntity())
    }

    open suspend fun saveDiary(diary: Diary, previousId: String? = null) = withContext(Dispatchers.IO) {
        if (previousId != null && previousId != diary.id) {
            dao.deleteDraft(previousId)
            dao.deleteDiary(previousId)
        }
        dao.deleteDraft(previousId ?: diary.id)
        dao.insertDiary(diary.toEntity())
    }

    open suspend fun deleteDiary(id: String) = withContext(Dispatchers.IO) {
        dao.deleteDiary(id)
        dao.deleteDraft(id)
    }

    open suspend fun deleteDraft(id: String) = withContext(Dispatchers.IO) {
        dao.deleteDraft(id)
    }

    open suspend fun syncDiaryToTime(diary: Diary): Diary = withContext(Dispatchers.IO) {
        val payload = diary.toTimeRequest()
        val response = if (diary.timeEntryId == null) {
            timeApi.createTimeEntry(payload)
        } else {
            timeApi.updateTimeEntry(diary.timeEntryId, payload)
        }
        val updated = diary.copy(timeEntryId = response.id ?: diary.timeEntryId)
        dao.insertDiary(updated.toEntity())
        updated
    }

    open suspend fun removeTimeEvent(diary: Diary): Diary = withContext(Dispatchers.IO) {
        diary.timeEntryId?.let { timeApi.deleteTimeEntry(it) }
        val updated = diary.copy(timeEntryId = null)
        dao.insertDiary(updated.toEntity())
        updated
    }

    open suspend fun getWeeklyRecord(offset: Int): WeeklyRecordData = withContext(Dispatchers.IO) {
        val today = defaultDiaryDate()
        val start = weekStart(offset, today)
        val end = start.plusDays(6)
        val dayEntries = dao.getDiariesBetweenDates(start, end, DiaryType.Day)
            .associateBy { it.startDate }

        val records = (0..6).map { index ->
            val date = start.plusDays(index.toLong())
            val exists = date in dayEntries
            val status = when {
                date.isBefore(today) && exists -> RecordStatus.DonePast
                date.isBefore(today) && !exists -> RecordStatus.MissedPast
                date.isEqual(today) && exists -> RecordStatus.DoneToday
                date.isEqual(today) && !exists -> RecordStatus.TodoToday
                else -> RecordStatus.Future
            }
            DailyRecord(date, status)
        }

        val hasSummary = if (end.isBefore(today)) {
            val summaryType = DiaryType.Week
            dao.hasSummary(
                start = start,
                end = end,
                type = summaryType,
            )
        } else {
            null
        }

        WeeklyRecordData(hasWeekSummary = hasSummary, records = records)
    }
}
