package com.elkrrai.techtalk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.elkrrai.techtalk.data.local.entity.UserTipHistoryEntity

@Dao
interface UserTipHistoryDao {
    /** INSERT-IGNORE — `seenAt` on the stored row reflects the *first* view only. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markSeen(history: UserTipHistoryEntity)

    /** Updates an existing row only — a tip must be marked seen (a row must exist)
     * before it can be toggled interesting. */
    @Query("UPDATE user_tip_history SET isInterested = NOT isInterested WHERE tipId = :tipId")
    suspend fun toggleInterested(tipId: Long)

    @Query("SELECT isInterested FROM user_tip_history WHERE tipId = :tipId")
    suspend fun isInterested(tipId: Long): Boolean?

    @Query("SELECT COUNT(*) FROM user_tip_history WHERE isSeen = 1")
    suspend fun getSeenCount(): Int

    @Query("SELECT tipId FROM user_tip_history WHERE isSeen = 1")
    suspend fun getSeenTipIds(): List<Long>

    @Query("SELECT tipId FROM user_tip_history WHERE isInterested = 1")
    suspend fun getInterestedTipIds(): List<Long>

    @Query("SELECT COUNT(*) FROM user_tip_history WHERE isInterested = 1")
    suspend fun getInterestedCount(): Int

    @Query("DELETE FROM user_tip_history")
    suspend fun clearAll()
}
