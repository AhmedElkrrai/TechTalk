package com.elkrrai.techtalk.domain.model.tech

/**
 * The 5 built-in technologies. [title] is the join key used everywhere: seeding inserts
 * rows named after it, and TipPack/BattlePack entries reference technologies by name.
 *
 * Declaration order is NOT the database id order — the seeder assigns ids explicitly
 * (Kotlin=1, Android=2, Swift=3, iOS=4, Go=5). Never assume `ordinal == id`.
 *
 * Battle seed file names are derived as `title.replace(" ", "_") + "_battle.json"`.
 */
enum class Technology(val title: String) {
    ANDROID("Android"),
    IOS("iOS"),
    KOTLIN("Kotlin"),
    SWIFT("Swift"),
    GO_LANG("Go")
}
