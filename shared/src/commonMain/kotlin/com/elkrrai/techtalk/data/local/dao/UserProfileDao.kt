package com.elkrrai.techtalk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.elkrrai.techtalk.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/** All queries scoped to the single row, `id = 1` (must match [UserProfileEntity
 * .SINGLE_ROW_ID]). */
@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?

    @Upsert
    suspend fun upsert(profile: UserProfileEntity)
}
