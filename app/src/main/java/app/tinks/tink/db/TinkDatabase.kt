package app.tinks.tink.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.tinks.tink.diary.DiaryDao
import app.tinks.tink.diary.DiaryDateConverters
import app.tinks.tink.diary.DiaryDateTimeConverters
import app.tinks.tink.diary.DiaryEntity
import app.tinks.tink.diary.DiaryTypeConverters
import app.tinks.tink.diary.DraftEntity
import app.tinks.tink.home.HomeDao
import app.tinks.tink.home.HomePendingActionEntity
import app.tinks.tink.home.HomeSnapshotEntity
import app.tinks.tink.merriam.db.MerriamDao
import app.tinks.tink.merriam.db.MerriamTypeConverters
import app.tinks.tink.merriam.db.RootEntity
import app.tinks.tink.salesforce.SalesforceChoiceEntity
import app.tinks.tink.salesforce.SalesforceDao
import app.tinks.tink.salesforce.SalesforceLocalProgressEntity
import app.tinks.tink.salesforce.SalesforceQuestionEntity

@Database(
    entities = [
        RootEntity::class,
        SalesforceQuestionEntity::class,
        SalesforceChoiceEntity::class,
        SalesforceLocalProgressEntity::class,
        DiaryEntity::class,
        DraftEntity::class,
        HomeSnapshotEntity::class,
        HomePendingActionEntity::class,
    ],
    version = 5,
)
@TypeConverters(
    MerriamTypeConverters::class,
    DiaryDateConverters::class,
    DiaryDateTimeConverters::class,
    DiaryTypeConverters::class,
)
abstract class TinkDatabase : RoomDatabase() {
    abstract fun merriamDao(): MerriamDao
    abstract fun salesforceDao(): SalesforceDao
    abstract fun diaryDao(): DiaryDao
    abstract fun homeDao(): HomeDao
}
