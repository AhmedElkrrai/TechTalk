package com.elkrrai.techtalk.data.local.seed

import com.elkrrai.techtalk.data.local.content.battle.BattlePackManager
import com.elkrrai.techtalk.data.local.content.tip.TipPackManager
import com.elkrrai.techtalk.data.local.dao.ContentMetaDao
import com.elkrrai.techtalk.data.local.dao.TechnologyDao
import com.elkrrai.techtalk.data.local.entity.ContentMetaEntity
import com.elkrrai.techtalk.data.local.entity.TechnologyEntity
import com.elkrrai.techtalk.domain.model.tech.Technology
import com.elkrrai.techtalk.utils.inspect
import org.jetbrains.compose.resources.ExperimentalResourceApi
import techtalk.shared.generated.resources.Res

/**
 * Idempotent content loading. Constraint: technologies must exist before pack import,
 * because packs reference technologies by *name*.
 *
 * - Empty database -> seed everything from the bundled packs, then record
 *   [SeedManifest.contentVersion].
 * - Already seeded, but the recorded version is older than [SeedManifest.contentVersion]
 *   -> [syncContent] merges the bundled packs into the existing rows (user data intact).
 */
class DatabaseSeeder(
    private val technologyDao: TechnologyDao,
    private val tipPackManager: TipPackManager,
    private val battlePackManager: BattlePackManager,
    private val contentMetaDao: ContentMetaDao
) {
    suspend fun seedOrSync() {
        if (technologyDao.getAll().isEmpty()) {
            seedFresh()
            recordContentVersion()
            return
        }
        // Installs that predate content_meta have no row: they hold the original content.
        val applied = contentMetaDao.get(CONTENT_VERSION_KEY)?.toIntOrNull() ?: 1
        if (applied < SeedManifest.contentVersion) {
            val ok = syncContent()
            if (ok) recordContentVersion()
            inspect("DatabaseSeeder: content sync v$applied -> v${SeedManifest.contentVersion} ${if (ok) "applied" else "incomplete, will retry"}")
        }
    }

    private suspend fun recordContentVersion() {
        contentMetaDao.upsert(ContentMetaEntity(CONTENT_VERSION_KEY, SeedManifest.contentVersion.toString()))
    }

    /** Merges every bundled pack into the existing database. Returns true only when every
     * pack applied, so a partial failure leaves the old version recorded and the next
     * launch retries (sync is idempotent). */
    @OptIn(ExperimentalResourceApi::class)
    private suspend fun syncContent(): Boolean {
        var allApplied = true

        for (fileName in SeedManifest.tipPackFileNames) {
            runCatching {
                val bytes = Res.readBytes("files/$fileName")
                tipPackManager.syncFromJson(bytes.decodeToString(), SeedManifest.renamedTips)
            }.onFailure {
                allApplied = false
                inspect("DatabaseSeeder: failed to sync tip pack '$fileName': ${it.message}")
            }
        }

        for (technology in Technology.entries) {
            val fileName = "${technology.title.replace(" ", "_")}_battle.json"
            val bytes = runCatching { Res.readBytes("files/$fileName") }.getOrNull() ?: continue
            runCatching {
                battlePackManager.syncFromJson(bytes.decodeToString(), SeedManifest.renamedQuestions)
            }.onFailure {
                allApplied = false
                inspect("DatabaseSeeder: failed to sync battle pack '$fileName': ${it.message}")
            }
        }
        return allApplied
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun seedFresh() {
        technologyDao.upsertAll(
            listOf(
                TechnologyEntity(
                    id = 1,
                    name = Technology.KOTLIN.title,
                    tagColor = "#7F52FF",
                    description = "A modern, and safe programming language for Android, and multiplatform development."
                ),
                TechnologyEntity(
                    id = 2,
                    name = Technology.ANDROID.title,
                    tagColor = "#3DDC84",
                    description = "Google's mobile platform for building apps with Kotlin, and Compose."
                ),
                TechnologyEntity(
                    id = 3,
                    name = Technology.SWIFT.title,
                    tagColor = "#F05138",
                    description = "Apple's powerful and intuitive programming language for iOS, macOS, and beyond."
                ),
                TechnologyEntity(
                    id = 4,
                    name = Technology.IOS.title,
                    tagColor = "#147EFB",
                    description = "Apple's mobile operating system powering the iPhone and iPad app ecosystem."
                ),
                TechnologyEntity(
                    id = 5,
                    name = Technology.JAVA.title,
                    tagColor = "#ED8B00",
                    description = "A mature, platform-independent language powering enterprise systems and Android app development worldwide."
                )
            )
        )

        for (fileName in SeedManifest.tipPackFileNames) {
            runCatching {
                val bytes = Res.readBytes("files/$fileName")
                tipPackManager.importFromJson(bytes.decodeToString())
            }.onFailure {
                inspect("DatabaseSeeder: failed to import tip pack '$fileName': ${it.message}")
            }
        }

        for (technology in Technology.entries) {
            val fileName = "${technology.title.replace(" ", "_")}_battle.json"
            runCatching {
                val bytes = Res.readBytes("files/$fileName")
                battlePackManager.importFromJson(bytes.decodeToString())
            }.onFailure {
                // Missing battle pack files are expected and skipped quietly.
            }
        }
    }

    private companion object {
        const val CONTENT_VERSION_KEY = "content_version"
    }
}
