package com.elkrrai.techtalk.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.elkrrai.techtalk.data.local.dao.BattleHistoryDao
import com.elkrrai.techtalk.data.local.dao.QuestionDao
import com.elkrrai.techtalk.data.local.dao.TechnologyDao
import com.elkrrai.techtalk.data.local.dao.TipDao
import com.elkrrai.techtalk.data.local.dao.TopicDao
import com.elkrrai.techtalk.data.local.dao.UserProfileDao
import com.elkrrai.techtalk.data.local.dao.UserSubscriptionDao
import com.elkrrai.techtalk.data.local.dao.UserTipHistoryDao
import com.elkrrai.techtalk.data.local.db.converter.DifficultyConverter
import com.elkrrai.techtalk.data.local.db.utils.AppDatabaseConstructor
import com.elkrrai.techtalk.data.local.entity.AnswerEntity
import com.elkrrai.techtalk.data.local.entity.BattleHistoryEntity
import com.elkrrai.techtalk.data.local.entity.QuestionEntity
import com.elkrrai.techtalk.data.local.entity.TechnologyEntity
import com.elkrrai.techtalk.data.local.entity.TipEntity
import com.elkrrai.techtalk.data.local.entity.TopicEntity
import com.elkrrai.techtalk.data.local.entity.UserProfileEntity
import com.elkrrai.techtalk.data.local.entity.UserSubscriptionEntity
import com.elkrrai.techtalk.data.local.entity.UserTipHistoryEntity

@Database(
    entities = [
        TechnologyEntity::class,
        TopicEntity::class,
        TipEntity::class,
        BattleHistoryEntity::class,
        UserProfileEntity::class,
        UserSubscriptionEntity::class,
        UserTipHistoryEntity::class,
        QuestionEntity::class,
        AnswerEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(DifficultyConverter::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun technologyDao(): TechnologyDao
    abstract fun topicDao(): TopicDao
    abstract fun tipDao(): TipDao
    abstract fun questionDao(): QuestionDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userSubscriptionDao(): UserSubscriptionDao
    abstract fun userTipHistoryDao(): UserTipHistoryDao
    abstract fun battleHistoryDao(): BattleHistoryDao
}
