package com.griff.subscriptions.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.Labels
import com.griff.subscriptions.presentation.common.Tags
import com.griff.subscriptions.presentation.common.component.EntryRow
import com.griff.subscriptions.presentation.common.component.ProviderLogo
import com.griff.subscriptions.presentation.common.component.TagChip
import com.griff.subscriptions.presentation.common.format.MoneyFormatter
import com.griff.subscriptions.presentation.home.SubscriptionListItem
import com.griff.subscriptions.presentation.theme.GriffThemePreview
import com.griff.subscriptions.presentation.theme.Spacing
import com.griff.subscriptions.presentation.theme.ThemePreviews
import com.griff.subscriptions.domain.model.ProviderCategory

/**
 * A single subscription row: logo, name, category tag, billing period and price.
 *
 * The tag and the billing period share one supporting line separated by a middle dot, so the row
 * gains the category without gaining a third line of height.
 */
@Composable
internal fun SubscriptionListItemRow(
    item: SubscriptionListItem,
    modifier: Modifier = Modifier,
) {
    val priceText = MoneyFormatter.format(item.price)
    val periodText = stringResource(Labels.billingPeriodShort(item.billingPeriod))
    val tag = Tags.of(item.category)
    val description = stringResource(
        R.string.home_item_description,
        item.name,
        stringResource(tag.labelRes),
        periodText,
        priceText,
    )

    EntryRow(
        title = item.name,
        amount = priceText,
        modifier = modifier.semantics { contentDescription = description },
        leading = { ProviderLogo(logoKey = item.logoKey, name = item.name) },
        supporting = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                TagChip(style = tag)
                Text(
                    text = periodText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@ThemePreviews
@Composable
private fun SubscriptionListItemRowPreview() {
    GriffThemePreview {
        SubscriptionListItemRow(
            item = SubscriptionListItem(
                id = "1",
                name = "Spotify",
                logoKey = "spotify",
                category = ProviderCategory.MUSIC,
                billingPeriod = BillingPeriod.MONTHLY,
                price = Money.ofUnits(34, 99),
            ),
        )
    }
}
