package com.elkrrai.techtalk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.elkrrai.techtalk.data.utils.currentEpochMillis
import com.elkrrai.techtalk.domain.model.user.AvatarCatalog

/** Single row, always keyed [SINGLE_ROW_ID] — always read-modify-upsert, never
 * blind-insert. */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val name: String = "",
    val avatarKey: String = AvatarCatalog.defaultAvatarKey,
    val level: Int = 1,
    val currentXp: Int = 0,
    val updatedAt: Long = currentEpochMillis()
) {
    companion object {
        const val SINGLE_ROW_ID = 1
    }
}
