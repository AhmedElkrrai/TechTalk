package com.elkrrai.techtalk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.elkrrai.techtalk.data.local.entity.TechnologyEntity
import com.elkrrai.techtalk.data.local.entity.UserSubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSubscriptionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun subscribe(subscription: UserSubscriptionEntity)

    @Query("DELETE FROM user_subscriptions WHERE technologyId = :technologyId")
    suspend fun unsubscribe(technologyId: Long)

    @Query("SELECT technologyId FROM user_subscriptions")
    fun observeSubscribedIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM user_subscriptions WHERE technologyId = :technologyId)")
    suspend fun isSubscribed(technologyId: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM user_subscriptions WHERE technologyId = :technologyId)")
    fun observeIsSubscribed(technologyId: Long): Flow<Boolean>

    @Query(
        """
        SELECT technologies.* FROM technologies
        INNER JOIN user_subscriptions ON technologies.id = user_subscriptions.technologyId
        """
    )
    fun observeSubscribedTechnologies(): Flow<List<TechnologyEntity>>
}
