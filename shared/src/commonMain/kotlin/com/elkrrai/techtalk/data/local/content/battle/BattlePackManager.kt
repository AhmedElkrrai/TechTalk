package com.elkrrai.techtalk.data.local.content.battle

import com.elkrrai.techtalk.data.local.content.tip.TipPackFileHandler
import com.elkrrai.techtalk.data.local.dao.QuestionDao
import com.elkrrai.techtalk.data.local.dao.TechnologyDao
import com.elkrrai.techtalk.data.local.dao.TopicDao
import com.elkrrai.techtalk.data.local.entity.AnswerEntity
import com.elkrrai.techtalk.data.local.entity.QuestionEntity
import com.elkrrai.techtalk.domain.model.battle.BattlePack
import com.elkrrai.techtalk.domain.model.battle.BattlePackAnswer
import com.elkrrai.techtalk.domain.model.battle.BattlePackEntry
import com.elkrrai.techtalk.domain.model.battle.BattlePackQuestion
import com.elkrrai.techtalk.domain.model.common.Difficulty
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class BattlePackManager(
    private val technologyDao: TechnologyDao,
    private val topicDao: TopicDao,
    private val questionDao: QuestionDao
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun parse(jsonContent: String): BattlePack = json.decodeFromString(jsonContent)

    /** Requires both technology *and* topic to already exist; otherwise the entry is
     * skipped (battle packs are imported *after* tip packs for that reason). Questions
     * with no correct answer are dropped. Returns the number of questions inserted. */
    suspend fun import(pack: BattlePack): Int {
        var imported = 0
        for (entry in pack.entries) {
            val technology = technologyDao.getByName(entry.technologyName) ?: continue
            val topic = topicDao.getByNameAndTechnology(entry.topicName, technology.id) ?: continue

            for (question in entry.questions) {
                if (question.answers.none { it.isCorrect }) continue
                val difficulty = runCatching { Difficulty.valueOf(question.difficulty) }
                    .getOrDefault(Difficulty.BEGINNER)
                val questionId = questionDao.upsertQuestion(
                    QuestionEntity(
                        topicId = topic.id,
                        questionText = question.questionText,
                        difficulty = difficulty,
                        explanation = question.explanation
                    )
                )
                questionDao.upsertAllAnswers(
                    question.answers.map {
                        AnswerEntity(
                            questionId = questionId,
                            answerText = it.answerText,
                            isCorrect = it.isCorrect
                        )
                    }
                )
                imported++
            }
        }
        return imported
    }

    suspend fun importFromJson(jsonContent: String): Int = import(parse(jsonContent))

    /** One file per technology named `"{Tech}_battle"`, containing one entry per topic
     * that has questions. Returns filename -> json content, ready for a
     * [TipPackFileHandler] to persist. */
    suspend fun exportAllByTechs(author: String = ""): Map<String, String> {
        val result = linkedMapOf<String, String>()
        for (technology in technologyDao.getAll()) {
            val entries = mutableListOf<BattlePackEntry>()
            for (topic in topicDao.getByTechnologyId(technology.id)) {
                val questionIds = questionDao.getQuestionIdsByTopic(topic.id)
                if (questionIds.isEmpty()) continue
                val packQuestions = questionIds.mapNotNull { id ->
                    val question = questionDao.getQuestionById(id) ?: return@mapNotNull null
                    val answers = questionDao.getAnswersByQuestionId(question.id)
                    BattlePackQuestion(
                        questionText = question.questionText,
                        difficulty = question.difficulty.name,
                        explanation = question.explanation,
                        answers = answers.map { BattlePackAnswer(it.answerText, it.isCorrect) }
                    )
                }
                if (packQuestions.isEmpty()) continue
                entries += BattlePackEntry(
                    technologyName = technology.name,
                    topicName = topic.name,
                    topicDescription = topic.description,
                    questions = packQuestions
                )
            }
            if (entries.isEmpty()) continue
            val fileName = technology.name.replace(" ", "_") + "_battle.json"
            result[fileName] = json.encodeToString(BattlePack(author = author, entries = entries))
        }
        return result
    }
}
