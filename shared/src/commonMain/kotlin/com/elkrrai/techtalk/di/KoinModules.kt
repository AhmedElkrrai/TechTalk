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
import com.elkrrai.techtalk.domain.usecase.online.ConnectOnlineBattleUseCase
import com.elkrrai.techtalk.domain.usecase.online.CreateOnlineRoomUseCase
import com.elkrrai.techtalk.domain.usecase.online.DisconnectOnlineBattleUseCase
import com.elkrrai.techtalk.domain.usecase.online.JoinOnlineRoomUseCase
import com.elkrrai.techtalk.domain.usecase.online.LeaveOnlineRoomUseCase
import com.elkrrai.techtalk.domain.usecase.online.ObserveOnlineBattleEventsUseCase
import com.elkrrai.techtalk.domain.usecase.online.ReconnectOnlineBattleUseCase
import com.elkrrai.techtalk.domain.usecase.online.RequestOnlineRematchUseCase
import com.elkrrai.techtalk.domain.usecase.online.SetOnlinePlayerReadyUseCase
import com.elkrrai.techtalk.domain.usecase.online.SubmitOnlineAnswerUseCase
import com.elkrrai.techtalk.presentation.battle.BattleLobbyRoute
import com.elkrrai.techtalk.presentation.battle.BattleResultRoute
import com.elkrrai.techtalk.presentation.battle.OfflineBattleRoute
import com.elkrrai.techtalk.presentation.battle.OnlineBattleRoute
import com.elkrrai.techtalk.presentation.battle.home.BattleHomeViewModel
import com.elkrrai.techtalk.presentation.battle.offline.OfflineBattleViewModel
import com.elkrrai.techtalk.presentation.battle.online.BattleLobbyViewModel
import com.elkrrai.techtalk.presentation.battle.online.OnlineBattleViewModel
import com.elkrrai.techtalk.presentation.battle.result.BattleResultViewModel
import com.elkrrai.techtalk.presentation.feed.FeedViewModel
import com.elkrrai.techtalk.presentation.getmorecontent.GetMoreContentViewModel
import com.elkrrai.techtalk.presentation.technologylist.TechnologyListViewModel
import com.elkrrai.techtalk.presentation.userprofile.UserProfileViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
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

/** All 10 online use cases are registered (harmless, matches the domain layer's own
 * rebuild checklist) even though only 6 are ever injected into a ViewModel — see
 * [viewModelModule]. `SetOnlinePlayerReadyUseCase`, `RequestOnlineRematchUseCase`,
 * `LeaveOnlineRoomUseCase` and `ReconnectOnlineBattleUseCase` have no UI caller: the
 * lobby calls `disconnect()` directly instead of `LeaveOnlineRoomUseCase`, and
 * ready-up/rematch/reconnect have no screen at all. */
fun useCaseModule(): Module = module {
    single { ObserveOnlineBattleEventsUseCase(repository = get()) }
    single { ConnectOnlineBattleUseCase(repository = get()) }
    single { DisconnectOnlineBattleUseCase(repository = get()) }
    single { CreateOnlineRoomUseCase(repository = get()) }
    single { JoinOnlineRoomUseCase(repository = get()) }
    single { SubmitOnlineAnswerUseCase(repository = get()) }
    single { SetOnlinePlayerReadyUseCase(repository = get()) }
    single { RequestOnlineRematchUseCase(repository = get()) }
    single { LeaveOnlineRoomUseCase(repository = get()) }
    single { ReconnectOnlineBattleUseCase(repository = get()) }
}

/** [BattleHomeViewModel] is parameterless (the flow's start destination); every other
 * battle ViewModel is created from the typed nav-route it was navigated to — see
 * `presentation/battle/BattleRoutes.kt` and `BattleScreen.kt`'s `parametersOf(route)`
 * call sites. There's no shared battle session singleton anymore: each screen carries
 * what it needs forward as route arguments instead. */
fun viewModelModule(): Module = module {
    viewModelOf(::FeedViewModel)
    viewModelOf(::TechnologyListViewModel)
    viewModelOf(::BattleHomeViewModel)

    viewModel { (route: OfflineBattleRoute) -> OfflineBattleViewModel(route, get()) }
    viewModel { (route: BattleLobbyRoute) -> BattleLobbyViewModel(route, get(), get(), get(), get(), get()) }
    viewModel { (route: OnlineBattleRoute) -> OnlineBattleViewModel(route, get(), get(), get()) }
    viewModel { (route: BattleResultRoute) -> BattleResultViewModel(route) }

    viewModelOf(::GetMoreContentViewModel)
    viewModelOf(::UserProfileViewModel)
}

/** Single entry point, included from each platform's `startKoin { modules(...) }`. */
fun commonAppModule(): Module = module {
    includes(
        platformModule(),
        databaseModule(),
        repositoryModule(),
        useCaseModule(),
        viewModelModule()
    )
}
