package com.elkrrai.techtalk.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getAndroidDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val dbFile = context.getDatabasePath("techtalk.db")
    return Room.databaseBuilder<AppDatabase>(context, dbFile.absolutePath)
}
