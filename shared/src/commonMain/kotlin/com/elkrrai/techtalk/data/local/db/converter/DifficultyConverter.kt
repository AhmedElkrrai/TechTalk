package com.elkrrai.techtalk.data.local.db.converter

import androidx.room.TypeConverter
import com.elkrrai.techtalk.domain.model.common.Difficulty

class DifficultyConverter {
    @TypeConverter
    fun fromDifficulty(difficulty: Difficulty): String = difficulty.name

    @TypeConverter
    fun toDifficulty(value: String): Difficulty =
        runCatching { Difficulty.valueOf(value) }.getOrDefault(Difficulty.BEGINNER)
}
