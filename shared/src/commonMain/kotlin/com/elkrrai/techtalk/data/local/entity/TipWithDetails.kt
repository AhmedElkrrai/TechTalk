package com.elkrrai.techtalk.data.local.entity

import androidx.room.Embedded
import com.elkrrai.techtalk.domain.model.common.Difficulty

/** JOIN projection returned by feed queries — NOT a table, do not register it in
 * `@Database`. */
data class TipWithDetails(
    @Embedded val tip: TipEntity,
    val topicName: String,
    val technologyName: String,
    val technologyTagColor: String
)

// Flattened accessors mirroring the embedded tip's columns, for call-site convenience.
val TipWithDetails.id: Long get() = tip.id
val TipWithDetails.topicId: Long get() = tip.topicId
val TipWithDetails.title: String get() = tip.title
val TipWithDetails.content: String get() = tip.content
val TipWithDetails.codeSnippet: String? get() = tip.codeSnippet
val TipWithDetails.codeLang: String? get() = tip.codeLang
val TipWithDetails.difficulty: Difficulty get() = tip.difficulty
val TipWithDetails.interestCount: Int get() = tip.interestCount
val TipWithDetails.createdAt: Long get() = tip.createdAt
