package app.tinks.tink.salesforce

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesforceDao {
    @Transaction
    @Query("SELECT * FROM salesforce_question ORDER BY id ASC")
    fun observeQuestions(): Flow<List<SalesforceQuestionWithChoices>>

    @Query("SELECT COUNT(*) FROM salesforce_question")
    suspend fun questionCount(): Int

    @Query("SELECT * FROM salesforce_local_progress ORDER BY question_id ASC")
    fun observeProgress(): Flow<List<SalesforceLocalProgressEntity>>

    @Query("SELECT * FROM salesforce_local_progress WHERE question_id = :questionId")
    suspend fun getProgress(questionId: Int): SalesforceLocalProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: SalesforceLocalProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: List<SalesforceLocalProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuestions(questions: List<SalesforceQuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChoices(choices: List<SalesforceChoiceEntity>)

    @Query("DELETE FROM salesforce_choice")
    suspend fun deleteAllChoices()

    @Query("DELETE FROM salesforce_question")
    suspend fun deleteAllQuestions()

    @Transaction
    suspend fun replaceQuestionBank(
        questions: List<SalesforceQuestionEntity>,
        choices: List<SalesforceChoiceEntity>,
    ) {
        deleteAllChoices()
        deleteAllQuestions()
        upsertQuestions(questions)
        insertChoices(choices)
    }
}
