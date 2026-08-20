package com.griff.subscriptions.presentation.home.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.Labels
import com.griff.subscriptions.presentation.common.component.ProviderLogo
import com.griff.subscriptions.presentation.common.format.MoneyFormatter
import com.griff.subscriptions.presentation.home.SubscriptionListItem
import com.griff.subscriptions.presentation.theme.GriffSubscriptionsTheme
import com.griff.subscriptions.presentation.theme.MinTouchTarget
import com.griff.subscriptions.presentation.theme.Spacing

/**
 * A single subscription row: logo, name, billing period and price.
 *
 * The row is stateless; clicking is handled by the caller so the same component can be reused in
 * previews and tests.
 */
@Composable
internal fun SubscriptionListItemRow(
    item: SubscriptionListItem,
    modifier: Modifier = Modifier,
) {
    val priceText = MoneyFormatter.format(item.price)
    val periodText = stringResource(Labels.billingPeriodShort(item.billingPeriod))
    val description = stringResource(
        R.string.home_item_description,
        item.name,
        periodText,
        priceText,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinTouchTarget)
            .padding(horizontal = Spacing.Large, vertical = Spacing.Medium)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProviderLogo(logoKey = item.logoKey, name = item.name)

        Spacer(Modifier.width(Spacing.Large))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall / 2),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = periodText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.width(Spacing.Medium))

        Text(
            text = priceText,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SubscriptionListItemRowPreview() {
    GriffSubscriptionsTheme(dynamicColor = false) {
        SubscriptionListItemRow(
            item = SubscriptionListItem(
                id = "1",
                name = "Spotify",
                logoKey = "spotify",
                billingPeriod = BillingPeriod.MONTHLY,
                price = Money.ofUnits(34, 99),
            ),
        )
    }
}
