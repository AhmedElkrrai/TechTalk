package com.elkrrai.techtalk.data.local.seed

import com.elkrrai.techtalk.data.local.content.battle.BattlePackManager
import com.elkrrai.techtalk.data.local.content.tip.TipPackManager
import com.elkrrai.techtalk.data.local.dao.TechnologyDao
import com.elkrrai.techtalk.data.local.entity.TechnologyEntity
import com.elkrrai.techtalk.domain.model.tech.Technology
import org.jetbrains.compose.resources.ExperimentalResourceApi
import techtalk.shared.generated.resources.Res

/**
 * Idempotent first-launch seeding. Constraint: technologies must exist before pack
 * import, because packs reference technologies by *name*.
 */
class DatabaseSeeder(
    private val technologyDao: TechnologyDao,
    private val tipPackManager: TipPackManager,
    private val battlePackManager: BattlePackManager
) {
    @OptIn(ExperimentalResourceApi::class)
    suspend fun seedIfEmpty() {
        if (technologyDao.getAll().isNotEmpty()) return

        technologyDao.upsertAll(
            listOf(
                TechnologyEntity(id = 1, name = Technology.KOTLIN.title, tagColor = "#7F52FF"),
                TechnologyEntity(id = 2, name = Technology.ANDROID.title, tagColor = "#3DDC84"),
                TechnologyEntity(id = 3, name = Technology.SWIFT.title, tagColor = "#F05138"),
                TechnologyEntity(id = 4, name = Technology.IOS.title, tagColor = "#147EFB"),
                TechnologyEntity(id = 5, name = Technology.GO_LANG.title, tagColor = "#00ADD8")
            )
        )

        for (fileName in SeedManifest.tipPackFileNames) {
            runCatching {
                val bytes = Res.readBytes("files/$fileName")
                tipPackManager.importFromJson(bytes.decodeToString())
            }.onFailure {
                println("DatabaseSeeder: failed to import tip pack '$fileName': ${it.message}")
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
}
