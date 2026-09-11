package com.elkrrai.techtalk.presentation.technologylist.mapper

import com.elkrrai.techtalk.domain.model.tech.TechnologyInfo
import com.elkrrai.techtalk.presentation.technologylist.state.TechnologyUiItem
import org.jetbrains.compose.resources.DrawableResource

/**
 * Resolves the technology's icon drawable, if one is bundled. No drawable resources
 * are bundled in this build at all (`commonMain/composeResources/drawable/` is empty,
 * out of the agreed content scope), and Compose Resources doesn't generate a
 * name -> DrawableResource lookup map when the drawable set is empty — so this always
 * resolves to null for now. That's the expected end state, not a bug.
 *
 * Once real drawables are added (named `android_icon`, `kotlin_icon`, `swift_icon`,
 * `ios_icon`, `go_lang_icon`, matching [TechnologyInfo.iconResourceName] or those
 * fallback names), wire this up to reference the generated `Res.drawable.*` accessors
 * directly — Compose Resources resolves drawables by direct reference, not by string
 * lookup, so this needs a concrete `when` over the known names once they exist.
 */
fun TechnologyInfo.toTechnologyUiItem(isSubscribed: Boolean): TechnologyUiItem {
    val icon: DrawableResource? = null
    return TechnologyUiItem(
        id = id,
        name = name,
        iconResourceName = iconResourceName,
        iconResource = icon,
        description = description,
        tagColor = tagColor,
        isSubscribed = isSubscribed
    )
}
