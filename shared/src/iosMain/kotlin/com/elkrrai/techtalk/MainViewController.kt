package com.elkrrai.techtalk

import androidx.compose.ui.window.ComposeUIViewController
import com.elkrrai.techtalk.di.commonAppModule
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

// Exposed to Swift as `doInitKoin()` — Kotlin/Native's Objective-C header generator
// prefixes top-level functions named `initXxx` with `do` to avoid colliding with
// Swift/ObjC initializer semantics.
fun initKoin() {
    startKoin {
        modules(commonAppModule())
    }
}

fun MainViewController(): UIViewController = ComposeUIViewController { AppContent() }
