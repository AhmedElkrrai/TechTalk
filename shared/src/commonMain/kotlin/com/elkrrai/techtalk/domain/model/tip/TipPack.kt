package com.elkrrai.techtalk.domain.model.tip

import kotlinx.serialization.Serializable

/**
 * Portable tip content format, shared between users and used for first-launch seeding.
 * References technologies/topics **by name** so a file works across databases with
 * different ids.
 */
@Serializable
data class TipPack(
    val version: Int = 1,
    val author: String = "",
    val description: String = "",
    val entries: List<TipPackEntry>
)

@Serializable
data class TipPackEntry(
    val technologyName: String,
    val topicName: String,
    val topicDescription: String = "",
    val tips: List<TipPackTip>
)

@Serializable
data class TipPackTip(
    val title: String,
    val content: String,
    val codeSnippet: String? = null,
    val codeLang: String? = null,
    // String, not the Difficulty enum — unknown values fall back to BEGINNER on
    // import, keeping old pack files loadable across enum changes.
    val difficulty: String = "BEGINNER"
)
