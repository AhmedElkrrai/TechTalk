package com.elkrrai.techtalk.data.local.db.utils

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.elkrrai.techtalk.data.local.db.AppDatabase
import kotlinx.coroutines.Dispatchers

/**
 * Shared builder config. Only schema versions with an explicit migration (currently the
 * 4 -> 5 auto-migration declared on [AppDatabase]) keep user data; bumping the version
 * WITHOUT declaring one falls through to the destructive fallback below, which wipes the
 * DB — including user profile/history — and re-triggers seeding. Bundled content changes
 * don't need a schema bump: bump `SeedManifest.contentVersion` instead.
 *
 * Uses [Dispatchers.Default], not `Dispatchers.IO` — the latter is JVM/Android-only
 * and unavailable from commonMain on Kotlin/Native (iOS) targets.
 */
fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
