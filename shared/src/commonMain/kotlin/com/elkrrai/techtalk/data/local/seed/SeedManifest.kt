package com.elkrrai.techtalk.data.local.seed

/**
 * List of tip-pack JSON filenames (under `commonMain/composeResources/files/`) that
 * [DatabaseSeeder] imports on first launch.
 *
 * Battle-pack filenames are NOT listed here — they're derived per-technology
 * (`"${title.replace(" ", "_")}_battle.json"`) and missing ones are skipped quietly.
 */
object SeedManifest {
    val tipPackFileNames: List<String> = listOf(
        // Android
        "Android_Activity_Lifecycle.json",
        "Android_CI_CD.json",
        "Android_Clean_Architecture.json",
        "Android_Dependency_Injection.json",
        "Android_DexGuard___RASP.json",
        "Android_Fragments.json",
        "Android_Fundamentals.json",
        "Android_Gradle___Build_Configuration.json",
        "Android_Jetpack_Compose.json",
        "Android_Ktor_Client.json",
        "Android_Modularization.json",
        "Android_Navigation.json",
        "Android_Paging_3.json",
        "Android_Performance___Profiling.json",
        "Android_Permissions.json",
        "Android_Room.json",
        "Android_Security.json",
        "Android_Serialization.json",
        "Android_Testing.json",
        "Android_ViewModel.json",
        "Android_WorkManager.json",
        // Go
        "Go_Error_Handling.json",
        "Go_Goroutines___Channels.json",
        "Go_Interfaces.json",
        "Go_Packages___Modules.json",
        "Go_Slices___Maps.json",
        // iOS
        "iOS_App_Lifecycle.json",
        "iOS_Core_Data___SwiftData.json",
        "iOS_Networking.json",
        "iOS_SwiftUI.json",
        "iOS_UIKit_Essentials.json",
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
        "Kotlin_Null_Safety___Types.json",
        "Kotlin_Scope_Functions.json",
        // Swift
        "Swift_Closures.json",
        "Swift_Enums___Pattern_Matching.json",
        "Swift_Optionals.json",
        "Swift_Protocols.json",
        "Swift_Structured_Concurrency.json"
    )
}
