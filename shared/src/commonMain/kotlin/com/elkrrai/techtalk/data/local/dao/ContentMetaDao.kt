package com.elkrrai.techtalk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.elkrrai.techtalk.data.local.entity.ContentMetaEntity

@Dao
interface ContentMetaDao {
    @Query("SELECT metaValue FROM content_meta WHERE metaKey = :key")
    suspend fun get(key: String): String?

    @Upsert
    suspend fun upsert(meta: ContentMetaEntity)
}
