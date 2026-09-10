package com.elkrrai.techtalk.data.local.seed

/**
 * List of tip-pack JSON filenames (under `commonMain/composeResources/files/`) that
 * [DatabaseSeeder] imports on first launch. Starts empty — fill in once real tip pack
 * files are supplied. An empty list is not an error: the 5 built-in technologies still
 * seed from code, the feed is simply empty until packs are added here and dropped into
 * that folder.
 *
 * Battle-pack filenames are NOT listed here — they're derived per-technology
 * (`"${title.replace(" ", "_")}_battle.json"`) and missing ones are skipped quietly.
 */
object SeedManifest {
    val tipPackFileNames: List<String> = emptyList()
}
