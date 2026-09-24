package com.elkrrai.techtalk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.elkrrai.techtalk.data.local.entity.AnswerEntity
import com.elkrrai.techtalk.data.local.entity.QuestionEntity
import com.elkrrai.techtalk.domain.model.common.Difficulty

@Dao
interface QuestionDao {
    @Upsert
    suspend fun upsertQuestion(question: QuestionEntity): Long

    @Upsert
    suspend fun upsertAllQuestions(questions: List<QuestionEntity>)

    @Upsert
    suspend fun upsertAllAnswers(answers: List<AnswerEntity>)

    @Query(
        """
        SELECT questions.id FROM questions
        INNER JOIN topics ON questions.topicId = topics.id
        INNER JOIN user_subscriptions ON topics.technologyId = user_subscriptions.technologyId
        """
    )
    suspend fun getSubscribedQuestionIds(): List<Long>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: Long): QuestionEntity?

    @Query("SELECT * FROM answers WHERE questionId = :questionId")
    suspend fun getAnswersByQuestionId(questionId: Long): List<AnswerEntity>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getQuestionCount(): Int

    @Query(
        """
        SELECT questions.id FROM questions
        INNER JOIN topics ON questions.topicId = topics.id
        WHERE topics.technologyId = :technologyId
        """
    )
    suspend fun getQuestionIdsByTechnology(technologyId: Long): List<Long>

    @Query(
        """
        SELECT questions.id FROM questions
        INNER JOIN topics ON questions.topicId = topics.id
        WHERE topics.technologyId = :technologyId AND questions.difficulty = :difficulty
        """
    )
    suspend fun getQuestionIdsByTechnologyAndDifficulty(
        technologyId: Long,
        difficulty: Difficulty
    ): List<Long>

    @Query("SELECT id FROM questions WHERE topicId = :topicId")
    suspend fun getQuestionIdsByTopic(topicId: Long): List<Long>

    /** Content-sync lookup: questions have no stable key, so (topic, text) identifies one. */
    @Query("SELECT * FROM questions WHERE topicId = :topicId AND questionText = :questionText LIMIT 1")
    suspend fun getQuestionByTopicAndText(topicId: Long, questionText: String): QuestionEntity?

    @Query("UPDATE questions SET questionText = :newText WHERE topicId = :topicId AND questionText = :oldText")
    suspend fun renameQuestion(topicId: Long, oldText: String, newText: String)

    @Query("DELETE FROM answers WHERE questionId = :questionId")
    suspend fun deleteAnswersByQuestionId(questionId: Long)
}
