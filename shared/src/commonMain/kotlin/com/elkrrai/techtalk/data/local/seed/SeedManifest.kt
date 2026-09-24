package com.elkrrai.techtalk.data.local.seed

/**
 * List of tip-pack JSON filenames (under `commonMain/composeResources/files/`) that
 * [DatabaseSeeder] imports on first launch.
 *
 * Battle-pack filenames are NOT listed here — they're derived per-technology
 * (`"${title.replace(" ", "_")}_battle.json"`) and missing ones are skipped quietly.
 */
object SeedManifest {
    /**
     * Version of the bundled content. Bump it whenever any tip/battle JSON changes so
     * installs that already seeded their database pick the changes up (see
     * [DatabaseSeeder.seedOrSync]). Installs that never stored a version count as 1.
     */
    const val contentVersion: Int = 4

    /** A bundled tip retitled (or question reworded) since the content shipped earlier.
     * Sync renames the existing row first so its user history survives instead of the
     * new text being inserted next to a stale copy. Keep entries here forever. */
    data class ContentRename(
        val technology: String,
        val topic: String,
        val oldText: String,
        val newText: String
    )

    val renamedTips: List<ContentRename> = listOf(
        ContentRename(
            "Kotlin", "Coroutines",
            "Cancelling a parent Job cancels every child, but not the other way around",
            "Cancelling a parent Job cancels every child, while a failing child cancels its parent and siblings"
        ),
        ContentRename(
            "Kotlin", "Design Patterns",
            "Decorator pattern via Kotlin extension functions on interfaces",
            "Decorator pattern via class delegation (by) instead of wrapper boilerplate"
        ),
        ContentRename(
            "Java", "Generics",
            "PECS: Producer Extends, Consumer Extends — reading wildcards",
            "PECS: Producer Extends, Consumer Super — reading wildcards"
        ),
        ContentRename(
            "Kotlin", "Fundamentals",
            "when as an expression forces exhaustiveness, unlike when as a statement",
            "when over enums and sealed types must be exhaustive — and an expression must always cover every case"
        ),
        ContentRename(
            "Kotlin", "Fundamentals",
            "A private constructor on a sealed class fully closes instantiation",
            "Sealed class constructors are protected by default — make one private to allow only nested subclasses"
        ),
        ContentRename(
            "Kotlin", "Generics",
            "Generic constructors can introduce type parameters independent of the class",
            "Generic factory functions can introduce type parameters independent of the class"
        ),
        ContentRename(
            "Kotlin", "Null Safety & Types",
            "A smart cast can be invalidated across a suspension point",
            "Capture a mutable property in a local val before a suspension point"
        ),
        ContentRename(
            "Android", "Security",
            "report-uri gives you visibility into pin failures happening in the field",
            "Network Security Config has no pin-failure reporting — build your own"
        ),
        ContentRename(
            "Android", "Security",
            "Network Security Config pinning also covers WebView traffic; OkHttp's CertificatePinner does not",
            "OkHttp's CertificatePinner never covers WebView traffic — WebView needs separate handling"
        )
    )

    val renamedQuestions: List<ContentRename> = listOf(
        ContentRename(
            "iOS", "SwiftUI",
            "What property wrapper makes a value observable in SwiftUI?",
            "Which property wrapper declares view-owned value-type state that makes SwiftUI re-render the view when it changes?"
        )
    )

    /** A bundled tip deleted because it duplicated another (its useful content was folded
     * into the tip that stays). Sync deletes exactly these rows, never anything else, so
     * tips the user imported into the same topic are safe. Keep entries here forever. */
    data class ContentRemoval(val technology: String, val topic: String, val title: String)

    val removedTips: List<ContentRemoval> = listOf(
        ContentRemoval("Kotlin", "Coroutines", "SupervisorJob prevents cascading failure"),
        ContentRemoval("Kotlin", "Delegation", "lazy — compute once, use forever"),
        ContentRemoval("Kotlin", "Delegation", "observable & vetoable delegates"),
        ContentRemoval("Kotlin", "Delegation", "Interface delegation with 'by'"),
        ContentRemoval("Kotlin", "Null Safety & Types", "Safe call operator (?.)"),
        ContentRemoval("Kotlin", "Null Safety & Types", "Elvis operator with return/throw for early-exit null handling"),
        ContentRemoval("Kotlin", "Null Safety & Types", "Combine as? with the Elvis operator for a safe-cast-with-default idiom"),
        ContentRemoval("Kotlin", "Null Safety & Types", "Extension functions can be defined on nullable receivers"),
        ContentRemoval("Kotlin", "Fundamentals", "data class auto-generates equals, hashCode, toString, and copy"),
        ContentRemoval("Kotlin", "Fundamentals", "@DslMarker prevents accidental access to an outer DSL receiver"),
        ContentRemoval("Kotlin", "Fundamentals", "Lambdas with receiver are the foundation of Kotlin DSLs"),
        ContentRemoval("Kotlin", "Fundamentals", "Extension properties add computed members without a backing field"),
        ContentRemoval("Kotlin", "Fundamentals", "Reified type parameters let you inspect T at runtime"),
        ContentRemoval("Kotlin", "Generics", "Self-referential generics enable type-safe fluent builder chains"),
        ContentRemoval("Kotlin", "Generics", "Multiple bounds via a where clause combine several constraints"),
        ContentRemoval("Kotlin", "Generics", "out and in mirror producer/consumer roles, not just direction"),
        ContentRemoval("Kotlin", "Generics", "Generic classes let one implementation work across many types"),
        ContentRemoval("Kotlin", "Collections", "Sequence for large collections")
    )

    val tipPackFileNames: List<String> = listOf(
        // Android
        "Android_Activity_Lifecycle.json",
        "Android_CI_CD.json",
        "Android_Clean_Architecture.json",
        "Android_DataStore___Storage.json",
        "Android_Dependency_Injection.json",
        "Android_DexGuard___RASP.json",
        "Android_Fragments.json",
        "Android_Fundamentals.json",
        "Android_Gradle___Build_Configuration.json",
        "Android_Jetpack_Compose.json",
        "Android_Ktor_Client.json",
        "Android_Modularization.json",
        "Android_Navigation.json",
        "Android_Notifications.json",
        "Android_Paging_3.json",
        "Android_Performance___Profiling.json",
        "Android_Permissions.json",
        "Android_Room.json",
        "Android_Security.json",
        "Android_Serialization.json",
        "Android_Testing.json",
        "Android_ViewModel.json",
        "Android_WorkManager.json",
        // iOS
        "iOS_App_Lifecycle.json",
        "iOS_Core_Data___SwiftData.json",
        "iOS_Networking.json",
        "iOS_SwiftUI.json",
        "iOS_UIKit_Essentials.json",
        // Java
        "Java_Collections_Framework.json",
        "Java_Concurrency___Threads.json",
        "Java_Exception_Handling.json",
        "Java_Generics.json",
        "Java_Streams___Lambdas.json",
        // Kotlin
        "Kotlin_Channels___Actors.json",
        "Kotlin_Collections.json",
        "Kotlin_Coroutines.json",
        "Kotlin_Data___Sealed_Classes.json",
        "Kotlin_Delegation.json",
        "Kotlin_Design_Patterns.json",
        "Kotlin_DSL.json",
        "Kotlin_Extension_Functions.json",
        "Kotlin_Flow.json",
        "Kotlin_Fundamentals.json",
        "Kotlin_Generics.json",
        "Kotlin_Multiplatform.json",
        "Kotlin_Null_Safety___Types.json",
        "Kotlin_Scope_Functions.json",
        "Kotlin_Structured_Concurrency___Errors.json",
        "Kotlin_Testing_Coroutines___Flow.json",
        // Swift
        "Swift_Closures.json",
        "Swift_Enums___Pattern_Matching.json",
        "Swift_Optionals.json",
        "Swift_Protocols.json",
        "Swift_Structured_Concurrency.json"
    )
}
