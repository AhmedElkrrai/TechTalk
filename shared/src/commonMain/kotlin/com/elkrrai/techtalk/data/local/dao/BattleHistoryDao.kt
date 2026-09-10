package com.elkrrai.techtalk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.elkrrai.techtalk.data.local.entity.BattleHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BattleHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: BattleHistoryEntity): Long

    @Query("SELECT * FROM battle_history ORDER BY playedAt DESC, id DESC")
    fun observeAll(): Flow<List<BattleHistoryEntity>>

    @Query("SELECT * FROM battle_history ORDER BY playedAt DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<BattleHistoryEntity>>
}
