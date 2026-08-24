package com.griff.keeper.presentation.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.statistics.ExpenseSource
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.component.EntryRow
import com.griff.keeper.presentation.common.component.ProviderLogo
import com.griff.keeper.presentation.common.format.DateFormatter
import com.griff.keeper.presentation.common.format.MoneyFormatter
import com.griff.keeper.presentation.statistics.RankedExpenseItem
import com.griff.keeper.presentation.statistics.RankedSubscription
import com.griff.keeper.presentation.statistics.UpcomingCharge
import com.griff.keeper.presentation.theme.GriffGradients
import com.griff.keeper.presentation.theme.GriffShapes
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews
import java.time.LocalDate

/**
 * Row of the subscription "largest costs" ranking, always compared per month.
 *
 * The bar underneath is the row's share of the ranking's largest entry, so the list reads at a
 * glance instead of requiring the numbers to be compared by eye.
 */
@Composable
internal fun RankedSubscriptionRow(
    subscription: RankedSubscription,
    shareOfMax: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.ExtraSmall),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        EntryRow(
            title = subscription.name,
            amount = stringResource(
                R.string.amount_per_month,
                MoneyFormatter.format(subscription.monthlyEquivalent),
            ),
            verticalPadding = Spacing.ExtraSmall,
            leading = {
                ProviderLogo(
                    logoKey = subscription.logoKey,
                    name = subscription.name,
                    size = RowLogoSize,
                )
            },
        )
        ShareBar(share = shareOfMax, brush = GriffGradients.accentHorizontal())
    }
}

/**
 * Row of the combined ranking.
 *
 * The source is spelled out under the name, because a subscription's monthly cost and an
 * obligation's paid amount are not the same kind of number and the list must not imply they are.
 * The leading tile carries the same distinction visually: navy for a normalized estimate, green for
 * a settled payment, matching the chart above.
 */
@Composable
internal fun RankedExpenseRow(
    expense: RankedExpenseItem,
    shareOfMax: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.ExtraSmall),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        EntryRow(
            title = expense.name,
            amount = when (expense.source) {
                ExpenseSource.SUBSCRIPTION -> stringResource(
                    R.string.amount_per_month,
                    MoneyFormatter.format(expense.amount),
                )

                ExpenseSource.OBLIGATION -> MoneyFormatter.format(expense.amount)
            },
            verticalPadding = Spacing.ExtraSmall,
            leading = { SourceIconTile(source = expense.source) },
            supporting = {
                Text(
                    text = stringResource(expense.sourceLabelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
        ShareBar(
            share = shareOfMax,
            brush = when (expense.source) {
                ExpenseSource.SUBSCRIPTION -> GriffGradients.accentHorizontal()
                ExpenseSource.OBLIGATION -> GriffGradients.obligationBarHorizontal()
            },
        )
    }
}

/** The generic subscription/obligation glyph for a row that has no provider logo of its own. */
@Composable
private fun SourceIconTile(source: ExpenseSource) {
    val isSubscription = source == ExpenseSource.SUBSCRIPTION
    Box(
        modifier = Modifier
            .size(RowLogoSize)
            .clip(RoundedCornerShape(SourceTileCorner))
            .background(
                if (isSubscription) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    GriffTheme.colors.obligationSeries.copy(alpha = ObligationTileAlpha)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isSubscription) {
                Icons.AutoMirrored.Filled.ReceiptLong
            } else {
                Icons.Default.VerifiedUser
            },
            contentDescription = null,
            tint = if (isSubscription) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                GriffTheme.colors.obligationSeries
            },
            modifier = Modifier.size(SourceIconSize),
        )
    }
}

/** Thin proportion bar shared by both ranking rows, its fill share-of-max rather than share-of-total. */
@Composable
private fun ShareBar(share: Float, brush: Brush) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ShareBarHeight)
            .clip(GriffShapes.Pill)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(share.coerceIn(ShareBarMinFraction, 1f))
                .height(ShareBarHeight)
                .clip(GriffShapes.Pill)
                .background(brush),
        )
    }
}

/**
 * Row of an expected charge, showing the date it is due.
 *
 * A charge that is about to happen is marked with a warning-tinted badge. The badge is labelled, so
 * the warning survives being read out loud or seen without color perception.
 */
@Composable
internal fun UpcomingChargeRow(
    charge: UpcomingCharge,
    modifier: Modifier = Modifier,
) {
    EntryRow(
        title = charge.name,
        amount = MoneyFormatter.format(charge.amount),
        modifier = modifier,
        verticalPadding = Spacing.Small,
        leading = {
            ProviderLogo(logoKey = charge.logoKey, name = charge.name, size = RowLogoSize)
        },
        supporting = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Text(
                    text = DateFormatter.formatFullDate(charge.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (charge.isDueSoon) DueSoonBadge()
            }
        },
    )
}

@Composable
private fun DueSoonBadge() {
    Text(
        text = stringResource(R.string.statistics_upcoming_soon),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .background(
                color = GriffTheme.colors.warning.copy(alpha = BadgeBackgroundAlpha),
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall / 2),
    )
}

private val RowLogoSize = 36.dp
private val SourceTileCorner = 7.dp
private val SourceIconSize = 18.dp
private val ShareBarHeight = 5.dp
private const val ShareBarMinFraction = 0.04f
private const val ObligationTileAlpha = 0.16f
private const val BadgeBackgroundAlpha = 0.22f

@ThemePreviews
@Composable
private fun StatisticsRowsPreview() {
    GriffThemePreview {
        Column(modifier = Modifier.padding(vertical = Spacing.Large)) {
            UpcomingChargeRow(
                charge = UpcomingCharge(
                    subscriptionId = "1",
                    name = "Spotify",
                    logoKey = "spotify",
                    date = LocalDate.of(2026, 8, 25),
                    amount = Money.ofUnits(34, 99),
                    isDueSoon = true,
                ),
            )
            RankedSubscriptionRow(
                subscription = RankedSubscription(
                    id = "3",
                    name = "Google Workspace",
                    logoKey = "google_workspace",
                    billingPeriod = BillingPeriod.YEARLY,
                    monthlyEquivalent = Money.ofUnits(86),
                ),
                shareOfMax = 0.4f,
            )
            RankedExpenseRow(
                shareOfMax = 1f,
                expense = RankedExpenseItem(
                    id = "4",
                    name = "OC Ford",
                    amount = Money.ofUnits(1_240),
                    source = ExpenseSource.OBLIGATION,
                    sourceLabelRes = R.string.statistics_source_obligation,
                ),
            )
        }
    }
}
