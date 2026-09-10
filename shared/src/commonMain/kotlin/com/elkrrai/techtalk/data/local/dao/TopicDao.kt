package com.elkrrai.techtalk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.elkrrai.techtalk.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Upsert
    suspend fun upsert(topic: TopicEntity): Long

    @Upsert
    suspend fun upsertAll(topics: List<TopicEntity>)

    @Query("SELECT * FROM topics WHERE technologyId = :technologyId")
    fun observeByTechnology(technologyId: Long): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE technologyId = :technologyId")
    suspend fun getByTechnologyId(technologyId: Long): List<TopicEntity>

    @Query("SELECT * FROM topics")
    suspend fun getAll(): List<TopicEntity>

    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun getById(id: Long): TopicEntity?

    @Query("SELECT * FROM topics WHERE name = :name COLLATE NOCASE AND technologyId = :technologyId")
    suspend fun getByNameAndTechnology(name: String, technologyId: Long): TopicEntity?
}
