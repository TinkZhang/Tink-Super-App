package app.tinks.tink.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    ],
    version = 3,
)
@TypeConverters(MerriamTypeConverters::class)
abstract class TinkDatabase : RoomDatabase() {
    abstract fun merriamDao(): MerriamDao
    abstract fun salesforceDao(): SalesforceDao
}
