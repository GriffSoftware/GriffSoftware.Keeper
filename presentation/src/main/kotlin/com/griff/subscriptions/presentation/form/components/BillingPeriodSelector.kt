package com.griff.subscriptions.presentation.form.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.Labels
import com.griff.subscriptions.presentation.common.component.accentSegmentedButtonColors
import com.griff.subscriptions.presentation.theme.GriffThemePreview
import com.griff.subscriptions.presentation.theme.Spacing
import com.griff.subscriptions.presentation.theme.ThemePreviews

/** Two option selector; a segmented button reads better than radio buttons for a binary choice. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BillingPeriodSelector(
    selected: BillingPeriod,
    enabled: Boolean,
    onSelect: (BillingPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val periods = BillingPeriod.entries

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.form_billing_period_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.Small),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            periods.forEachIndexed { index, period ->
                SegmentedButton(
                    selected = period == selected,
                    onClick = { onSelect(period) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size),
                    colors = accentSegmentedButtonColors(),
                ) {
                    Text(stringResource(Labels.billingPeriodOption(period)))
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun BillingPeriodSelectorPreview() {
    GriffThemePreview {
        BillingPeriodSelector(
            selected = BillingPeriod.MONTHLY,
            enabled = true,
            onSelect = {},
            modifier = Modifier.padding(Spacing.Large),
        )
    }
}
