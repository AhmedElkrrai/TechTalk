package com.elkrrai.techtalk.di

import com.elkrrai.techtalk.data.local.content.battle.BattlePackManager
import com.elkrrai.techtalk.data.local.content.tip.TipPackManager
import com.elkrrai.techtalk.data.local.dao.BattleHistoryDao
import com.elkrrai.techtalk.data.local.dao.QuestionDao
import com.elkrrai.techtalk.data.local.dao.TechnologyDao
import com.elkrrai.techtalk.data.local.dao.TipDao
import com.elkrrai.techtalk.data.local.dao.TopicDao
import com.elkrrai.techtalk.data.local.dao.UserProfileDao
import com.elkrrai.techtalk.data.local.dao.UserSubscriptionDao
import com.elkrrai.techtalk.data.local.dao.UserTipHistoryDao
import com.elkrrai.techtalk.data.local.db.AppDatabase
import com.elkrrai.techtalk.data.local.seed.DatabaseSeeder
import com.elkrrai.techtalk.data.remote.KtorOnlineBattleRepository
import com.elkrrai.techtalk.data.repository.TechTalkRepositoryImpl
import com.elkrrai.techtalk.domain.repository.OnlineBattleRepository
import com.elkrrai.techtalk.domain.repository.TechTalkRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/** Each DAO exposed as its own `single`, delegating to the [AppDatabase] instance,
 * plus [DatabaseSeeder] and the two content pack managers. */
fun databaseModule(): Module = module {
    single<TechnologyDao> { get<AppDatabase>().technologyDao() }
    single<TopicDao> { get<AppDatabase>().topicDao() }
    single<TipDao> { get<AppDatabase>().tipDao() }
    single<QuestionDao> { get<AppDatabase>().questionDao() }
    single<UserProfileDao> { get<AppDatabase>().userProfileDao() }
    single<UserSubscriptionDao> { get<AppDatabase>().userSubscriptionDao() }
    single<UserTipHistoryDao> { get<AppDatabase>().userTipHistoryDao() }
    single<BattleHistoryDao> { get<AppDatabase>().battleHistoryDao() }

    single { TipPackManager(get(), get(), get()) }
    single { BattlePackManager(get(), get(), get()) }
    single { DatabaseSeeder(technologyDao = get(), tipPackManager = get(), battlePackManager = get()) }
}

fun repositoryModule(): Module = module {
    single<TechTalkRepository> {
        TechTalkRepositoryImpl(
            technologyDao = get(),
            topicDao = get(),
            tipDao = get(),
            questionDao = get(),
            userProfileDao = get(),
            userSubscriptionDao = get(),
            userTipHistoryDao = get(),
            battleHistoryDao = get(),
            seeder = get(),
            tipPackManager = get(),
            battlePackManager = get(),
            fileHandler = get()
        )
    }
    single<OnlineBattleRepository> { KtorOnlineBattleRepository(httpClient = get()) }
}
