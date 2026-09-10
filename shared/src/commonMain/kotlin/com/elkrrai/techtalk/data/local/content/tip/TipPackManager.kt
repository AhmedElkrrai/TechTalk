package com.elkrrai.techtalk.data.local.content.tip

import com.elkrrai.techtalk.data.local.dao.TechnologyDao
import com.elkrrai.techtalk.data.local.dao.TipDao
import com.elkrrai.techtalk.data.local.dao.TopicDao
import com.elkrrai.techtalk.data.local.entity.TipEntity
import com.elkrrai.techtalk.data.local.entity.TopicEntity
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.content.ImportResult
import com.elkrrai.techtalk.domain.model.tip.TipPack
import com.elkrrai.techtalk.domain.model.tip.TipPackEntry
import com.elkrrai.techtalk.domain.model.tip.TipPackTip
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class TipPackManager(
    private val technologyDao: TechnologyDao,
    private val topicDao: TopicDao,
    private val tipDao: TipDao
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun export(pack: TipPack): String = json.encodeToString(pack)

    fun parse(jsonContent: String): TipPack = json.decodeFromString(jsonContent)

    /**
     * Technology resolved by name; unknown technology -> entry skipped (recorded in
     * [ImportResult.unknownTechnologies] + [ImportResult.errors]). Topic matched by
     * (name, technologyId); created if absent, else reused. Tips always inserted with
     * id = 0 (auto-generate) -> import never collides, but repeated imports duplicate
     * tips. Unknown difficulty string falls back to [Difficulty.BEGINNER].
     */
    suspend fun import(pack: TipPack): ImportResult {
        var topicsCreated = 0
        var topicsMatched = 0
        var tipsImported = 0
        val unknownTechnologies = mutableListOf<String>()
        val errors = mutableListOf<String>()

        for (entry in pack.entries) {
            val technology = technologyDao.getByName(entry.technologyName)
            if (technology == null) {
                unknownTechnologies += entry.technologyName
                errors += "Unknown technology: ${entry.technologyName}"
                continue
            }

            val existingTopic = topicDao.getByNameAndTechnology(entry.topicName, technology.id)
            val topicId = if (existingTopic != null) {
                topicsMatched++
                existingTopic.id
            } else {
                topicsCreated++
                topicDao.upsert(
                    TopicEntity(
                        technologyId = technology.id,
                        name = entry.topicName,
                        description = entry.topicDescription
                    )
                )
            }

            for (tip in entry.tips) {
                val difficulty = runCatching { Difficulty.valueOf(tip.difficulty) }
                    .getOrDefault(Difficulty.BEGINNER)
                tipDao.upsert(
                    TipEntity(
                        id = 0,
                        topicId = topicId,
                        title = tip.title,
                        content = tip.content,
                        codeSnippet = tip.codeSnippet,
                        codeLang = tip.codeLang,
                        difficulty = difficulty
                    )
                )
                tipsImported++
            }
        }

        return ImportResult(
            topicsCreated = topicsCreated,
            topicsMatched = topicsMatched,
            tipsImported = tipsImported,
            unknownTechnologies = unknownTechnologies,
            errors = errors
        )
    }

    suspend fun importFromJson(jsonContent: String): ImportResult = import(parse(jsonContent))

    /** Resolves tip -> topic -> technology, groups by (technologyName, topicName). */
    suspend fun exportFromDatabase(
        tipIds: List<Long>,
        author: String = "",
        description: String = ""
    ): String = export(TipPack(author = author, description = description, entries = buildEntries(tipIds)))

    private suspend fun buildEntries(tipIds: List<Long>): List<TipPackEntry> {
        data class GroupKey(val technologyName: String, val topicName: String, val topicDescription: String)

        val grouped = linkedMapOf<GroupKey, MutableList<TipPackTip>>()
        for (tipId in tipIds) {
            val tip = tipDao.getById(tipId) ?: continue
            val topic = topicDao.getById(tip.topicId) ?: continue
            val technology = technologyDao.getById(topic.technologyId) ?: continue
            val key = GroupKey(technology.name, topic.name, topic.description)
            grouped.getOrPut(key) { mutableListOf() } += TipPackTip(
                title = tip.title,
                content = tip.content,
                codeSnippet = tip.codeSnippet,
                codeLang = tip.codeLang,
                difficulty = tip.difficulty.name
            )
        }
        return grouped.map { (key, tips) ->
            TipPackEntry(
                technologyName = key.technologyName,
                topicName = key.topicName,
                topicDescription = key.topicDescription,
                tips = tips
            )
        }
    }

    /** One file per topic, filename `"{Tech}_{Topic}"` sanitized. Returns filename ->
     * json content, ready for a [TipPackFileHandler] to persist. */
    suspend fun exportAllByTopic(author: String = ""): Map<String, String> {
        val result = linkedMapOf<String, String>()
        for (technology in technologyDao.getAll()) {
            for (topic in topicDao.getByTechnologyId(technology.id)) {
                val tips = tipDao.getByTopicId(topic.id)
                if (tips.isEmpty()) continue
                val pack = TipPack(
                    author = author,
                    entries = listOf(
                        TipPackEntry(
                            technologyName = technology.name,
                            topicName = topic.name,
                            topicDescription = topic.description,
                            tips = tips.map {
                                TipPackTip(
                                    title = it.title,
                                    content = it.content,
                                    codeSnippet = it.codeSnippet,
                                    codeLang = it.codeLang,
                                    difficulty = it.difficulty.name
                                )
                            }
                        )
                    )
                )
                val fileName = sanitizeFileName("${technology.name}_${topic.name}") + ".json"
                result[fileName] = export(pack)
            }
        }
        return result
    }

    private fun sanitizeFileName(name: String): String = name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
}
