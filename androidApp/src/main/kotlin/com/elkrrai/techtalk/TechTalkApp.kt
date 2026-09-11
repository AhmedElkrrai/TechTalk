package com.elkrrai.techtalk

import android.app.Application
import com.elkrrai.techtalk.di.commonAppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TechTalkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TechTalkApp)
            modules(commonAppModule())
        }
    }
}
