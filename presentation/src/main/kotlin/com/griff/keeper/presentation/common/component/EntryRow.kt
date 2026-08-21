package com.griff.keeper.presentation.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.griff.keeper.presentation.theme.MinTouchTarget
import com.griff.keeper.presentation.theme.Spacing

/**
 * The list row shape shared by subscriptions, obligations and the statistics rankings.
 *
 * Three columns: a leading glyph, the name with whatever the screen wants under it, and an amount
 * with an optional line of its own. Composition rather than a generic list component - the caller
 * supplies its own leading icon and supporting content, so nothing here needs to know which module
 * the record came from.
 *
 * Stateless on purpose: clicking is handled by the caller, so the same row serves lists, previews
 * and tests.
 */
@Composable
fun EntryRow(
    title: String,
    amount: String,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = Spacing.Medium,
    leading: (@Composable () -> Unit)? = null,
    supporting: (@Composable () -> Unit)? = null,
    amountSupporting: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinTouchTarget)
            .padding(horizontal = Spacing.Large, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Spacing.Medium))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            supporting?.invoke()
        }

        Spacer(Modifier.width(Spacing.Medium))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
            amountSupporting?.invoke()
        }
    }
}
