package com.griff.keeper.presentation.common.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.TagAccent
import com.griff.keeper.presentation.theme.ThemePreviews

/**
 * What a tag looks like: its label and the accent it is drawn in.
 *
 * The label stays a resource reference so view models can build a tag without touching localized
 * text, exactly like [com.griff.keeper.presentation.common.UiMessage] does for snackbars.
 */
data class TagStyle(
    @param:StringRes val labelRes: Int,
    val accent: TagAccent,
)

/**
 * Small badge identifying the kind of a record.
 *
 * Deliberately not a Material `AssistChip`: those are interactive, come with a 32dp touch target and
 * would make every list row taller. This is a passive label - a tinted container, its own text color
 * from the same pair, and nothing else - so it can sit inside a row without changing its height.
 * [FilterTagChip] is the interactive counterpart used in filter rows.
 */
@Composable
fun TagChip(
    style: TagStyle,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = GriffTheme.colors.tagColors.getValue(style.accent)

    Row(
        modifier = modifier
            .background(color = colors.container, shape = RoundedCornerShape(ChipCornerRadius))
            .padding(horizontal = Spacing.Small, vertical = ChipVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(ChipIconSize),
            )
        }
        Text(
            text = stringResource(style.labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = colors.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val ChipCornerRadius = 6.dp
private val ChipVerticalPadding = 3.dp
private val ChipIconSize = 12.dp

@ThemePreviews
@Composable
private fun TagChipPreview() {
    GriffThemePreview {
        Row(
            modifier = Modifier.padding(Spacing.Large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            TagChip(TagStyle(R.string.tag_vehicle_insurance, TagAccent.BLUE))
            TagChip(TagStyle(R.string.tag_tax, TagAccent.AMBER))
            TagChip(TagStyle(R.string.category_music, TagAccent.EMERALD))
            TagChip(TagStyle(R.string.tag_other, TagAccent.SLATE))
        }
    }
}
