package com.elkrrai.techtalk.domain.model.user

/**
 * Lives in **domain**, not presentation, because both other layers need it: `data`'s
 * `UserProfileEntity.avatarKey` defaults to [AvatarCatalog.defaultAvatarKey], and
 * presentation renders [AvatarCatalog.emojiFor]. `key` values are persisted in
 * `user_profile.avatarKey` — never rename an existing one.
 */
object AvatarCatalog {
    val defaultAvatarKey: String
        get() = "\uD83D\uDC80"
    val options: List<AvatarOption> = AvatarOption.entries

    fun emojiFor(key: String): String =
        options.firstOrNull { it.key == key }?.emoji ?: "\uD83D\uDC80"
}

enum class AvatarOption(val key: String, val emoji: String, val label: String) {
    Fox("avatar_fox", "🦊", "Fox"),
    Panda("avatar_panda", "🐼", "Panda"),
    Bear("avatar_bear", "🐻", "Bear"),
    Bull("avatar_bull", "🐂", "Bull"),
    Rabbit("avatar_rabbit", "🐰", "Rabbit"),
    Koala("avatar_koala", "🐨", "Koala"),
    Cow("avatar_cow", "🐄", "Cow"),
    Lion("avatar_lion", "🦁", "Lion"),
    Tiger("avatar_tiger", "🐯", "Tiger"),
    Penguin("avatar_penguin", "🐧", "Penguin"),
    Cat("avatar_cat", "🐱", "Cat"),
    Dove("avatar_dove", "🕊", "Dove"),
    Mule("avatar_mule", "🫏", "Mule"),
    Dog("avatar_dog", "🐶", "Dog"),
    Rat("avatar_rat", "🐀", "Rat"),
    Snake("avatar_snake", "🐍", "Snake"),
    Eagle("avatar_eagle", "🦅", "Eagle"),
    Monkey("avatar_monkey", "🐵", "Monkey");
}
