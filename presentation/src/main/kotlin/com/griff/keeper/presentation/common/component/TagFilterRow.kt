package com.griff.keeper.presentation.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.TagAccent
import com.griff.keeper.presentation.theme.ThemePreviews

/** One selectable tag in a [TagFilterRow], identified by whatever key the caller filters by. */
data class TagFilterOption<T>(
    val value: T,
    val style: TagStyle,
)

/**
 * Horizontally scrolling row of single-select tag filters, with an "all" chip in front.
 *
 * Chips rather than a dropdown: the handful of tags each screen has fit on one scrollable line, and
 * the active filter stays visible instead of hiding behind a closed menu. Selection is single, so
 * `null` means "no tag filter"; the shape of the API already allows a set later.
 */
@Composable
fun <T> TagFilterRow(
    options: List<TagFilterOption<T>>,
    selected: T?,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.Large),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.filter_tag_all)) },
                colors = accentFilterChipColors(),
            )
        }
        items(items = options, key = { it.style.labelRes }) { option ->
            FilterChip(
                selected = option.value == selected,
                onClick = { onSelect(if (option.value == selected) null else option.value) },
                label = { Text(stringResource(option.style.labelRes)) },
                colors = accentFilterChipColors(),
            )
        }
    }
}

/**
 * Selection colors for filter chips.
 *
 * Material would use the neutral secondary container; an active filter is one of the few states the
 * brand accent is reserved for, matching the segmented buttons elsewhere in the app.
 */
@Composable
private fun accentFilterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@ThemePreviews
@Composable
private fun TagFilterRowPreview() {
    GriffThemePreview {
        TagFilterRow(
            options = listOf(
                TagFilterOption("oc", TagStyle(R.string.tag_vehicle_insurance, TagAccent.BLUE)),
                TagFilterOption("dom", TagStyle(R.string.tag_home_insurance, TagAccent.EMERALD)),
                TagFilterOption("podatek", TagStyle(R.string.tag_tax, TagAccent.AMBER)),
            ),
            selected = "oc",
            onSelect = {},
        )
    }
}
