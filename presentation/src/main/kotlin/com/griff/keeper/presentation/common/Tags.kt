package com.griff.keeper.presentation.common

import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.model.ObligationTag
import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.component.TagStyle
import com.griff.keeper.presentation.theme.TagAccent

/**
 * The single place where a domain category becomes a visible tag.
 *
 * Colors and labels live here, in the presentation layer, while the grouping itself stays a domain
 * concept ([ObligationTag]); nothing is persisted, so a change to the palette or to the wording never
 * needs a migration. Both modules go through this object, which is why an obligation badge and a
 * subscription badge look like members of the same system.
 */
internal object Tags {

    fun of(tag: ObligationTag): TagStyle = when (tag) {
        ObligationTag.VEHICLE -> TagStyle(R.string.tag_vehicle_insurance, TagAccent.BLUE)
        ObligationTag.HOME -> TagStyle(R.string.tag_home_insurance, TagAccent.EMERALD)
        ObligationTag.LAND -> TagStyle(R.string.tag_land_insurance, TagAccent.OLIVE)
        ObligationTag.DRONE -> TagStyle(R.string.tag_drone_insurance, TagAccent.CYAN)
        ObligationTag.TAX -> TagStyle(R.string.tag_tax, TagAccent.AMBER)
        ObligationTag.OTHER -> TagStyle(R.string.tag_other, TagAccent.SLATE)
    }

    fun of(category: ObligationCategory): TagStyle = of(category.tag)

    fun of(category: ProviderCategory): TagStyle = when (category) {
        ProviderCategory.VIDEO -> TagStyle(R.string.category_video, TagAccent.RED)
        ProviderCategory.MUSIC -> TagStyle(R.string.category_music, TagAccent.EMERALD)
        ProviderCategory.AI -> TagStyle(R.string.category_ai, TagAccent.CYAN)
        ProviderCategory.CLOUD -> TagStyle(R.string.category_cloud, TagAccent.BLUE)
        ProviderCategory.SOFTWARE -> TagStyle(R.string.category_software, TagAccent.INDIGO)
        ProviderCategory.HOSTING -> TagStyle(R.string.category_hosting, TagAccent.ORANGE)
        ProviderCategory.SHOPPING -> TagStyle(R.string.category_shopping, TagAccent.PINK)
        ProviderCategory.GAMING -> TagStyle(R.string.category_gaming, TagAccent.VIOLET)
        ProviderCategory.BOOKS -> TagStyle(R.string.category_books, TagAccent.AMBER)
        ProviderCategory.OTHER -> TagStyle(R.string.category_other, TagAccent.SLATE)
    }
}
