Drop tip pack / battle pack JSON files here.

These are read at first-launch seeding time via `Res.readBytes("files/<name>.json")`.
See `data/local/seed/SeedManifest.kt` for the list of tip-pack filenames the seeder
looks for, and `domain/model/tech/Technology.kt` for how battle-pack filenames are
derived (`"${title.replace(" ", "_")}_battle.json"`).
