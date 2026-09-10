package com.elkrrai.techtalk.data.local.db.utils

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.elkrrai.techtalk.data.local.db.AppDatabase
import kotlinx.coroutines.Dispatchers

/**
 * Shared builder config. No migrations are written — bumping [AppDatabase]'s version
 * wipes the DB and re-triggers seeding. Acceptable because all content is re-seedable
 * from bundled JSON; user profile/history is lost.
 */
fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
