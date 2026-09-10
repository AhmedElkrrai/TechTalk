package com.elkrrai.techtalk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.elkrrai.techtalk.data.local.entity.TechnologyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TechnologyDao {
    @Upsert
    suspend fun upsert(technology: TechnologyEntity): Long

    @Upsert
    suspend fun upsertAll(technologies: List<TechnologyEntity>)

    @Query("SELECT * FROM technologies")
    fun observeAll(): Flow<List<TechnologyEntity>>

    @Query("SELECT * FROM technologies")
    suspend fun getAll(): List<TechnologyEntity>

    @Query("SELECT * FROM technologies WHERE id = :id")
    suspend fun getById(id: Long): TechnologyEntity?

    @Query("SELECT * FROM technologies WHERE name = :name COLLATE NOCASE")
    suspend fun getByName(name: String): TechnologyEntity?

    @Query("DELETE FROM technologies WHERE id = :id")
    suspend fun deleteById(id: Long)
}
