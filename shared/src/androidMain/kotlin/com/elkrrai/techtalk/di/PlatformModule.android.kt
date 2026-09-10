package com.elkrrai.techtalk.di

import android.content.Context
import com.elkrrai.techtalk.data.local.content.tip.AndroidTipPackFileHandler
import com.elkrrai.techtalk.data.local.content.tip.TipPackFileHandler
import com.elkrrai.techtalk.data.local.db.AppDatabase
import com.elkrrai.techtalk.data.local.db.getAndroidDatabaseBuilder
import com.elkrrai.techtalk.data.local.db.utils.getRoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<AppDatabase> { getRoomDatabase(getAndroidDatabaseBuilder(get<Context>())) }
    single<TipPackFileHandler> { AndroidTipPackFileHandler(get()) }
    single<HttpClient> {
        HttpClient(OkHttp) {
            install(WebSockets)
            expectSuccess = false
        }
    }
}
