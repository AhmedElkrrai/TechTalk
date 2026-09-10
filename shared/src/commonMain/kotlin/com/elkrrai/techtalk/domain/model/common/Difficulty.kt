package com.elkrrai.techtalk.domain.model.common

/**
 * Difficulty level for a tip or battle question.
 *
 * [RANDOM] is a filter/selection sentinel used only in battle setup ("any difficulty")
 * — it is never persisted on a tip or question, and scores 0 points.
 */
enum class Difficulty { RANDOM, BEGINNER, INTERMEDIATE, ADVANCED }

fun Difficulty.getTitle(): String = when (this) {
    Difficulty.RANDOM -> "Random"
    Difficulty.BEGINNER -> "Beginner"
    Difficulty.INTERMEDIATE -> "Intermediate"
    Difficulty.ADVANCED -> "Advanced"
}

fun Difficulty.getPoints(): Int = when (this) {
    Difficulty.RANDOM -> 0
    Difficulty.BEGINNER -> 1
    Difficulty.INTERMEDIATE -> 3
    Difficulty.ADVANCED -> 5
}
