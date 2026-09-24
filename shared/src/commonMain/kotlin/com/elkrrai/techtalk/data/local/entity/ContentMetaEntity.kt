package com.elkrrai.techtalk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tiny key/value store for content bookkeeping (currently just the applied bundled
 * content version, see [com.elkrrai.techtalk.data.local.seed.SeedManifest.contentVersion]). */
@Entity(tableName = "content_meta")
data class ContentMetaEntity(
    @PrimaryKey val metaKey: String,
    val metaValue: String
)
