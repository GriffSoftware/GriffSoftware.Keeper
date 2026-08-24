package com.griff.keeper.presentation.form.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.Labels
import com.griff.keeper.presentation.common.component.GriffSegmentedControl
import com.griff.keeper.presentation.common.component.SegmentOption
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/** Two option selector; a segmented control reads better than radio buttons for a binary choice. */
@Composable
internal fun BillingPeriodSelector(
    selected: BillingPeriod,
    enabled: Boolean,
    onSelect: (BillingPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.form_billing_period_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.Small),
        )
        GriffSegmentedControl(
            options = BillingPeriod.entries.map {
                SegmentOption(value = it, label = stringResource(Labels.billingPeriodOption(it)))
            },
            selected = selected,
            onSelect = onSelect,
            enabled = enabled,
        )
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
