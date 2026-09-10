package com.elkrrai.techtalk.di

import com.elkrrai.techtalk.data.local.content.tip.IosTipPackFileHandler
import com.elkrrai.techtalk.data.local.content.tip.TipPackFileHandler
import com.elkrrai.techtalk.data.local.db.AppDatabase
import com.elkrrai.techtalk.data.local.db.getIosDatabaseBuilder
import com.elkrrai.techtalk.data.local.db.utils.getRoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.websocket.WebSockets
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<AppDatabase> { getRoomDatabase(getIosDatabaseBuilder()) }
    single<TipPackFileHandler> { IosTipPackFileHandler() }
    single<HttpClient> {
        HttpClient(Darwin) {
            install(WebSockets)
            expectSuccess = false
        }
    }
}
