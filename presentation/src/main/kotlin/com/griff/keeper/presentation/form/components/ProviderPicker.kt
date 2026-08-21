package com.griff.keeper.presentation.form.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.component.ProviderLogo
import com.griff.keeper.presentation.form.ProviderOption
import com.griff.keeper.presentation.theme.MinTouchTarget
import com.griff.keeper.presentation.theme.Spacing

/**
 * Searchable provider selector.
 *
 * Instead of a long dropdown the user types a fragment and picks from the filtered list; the
 * catch-all "Other" entry is always available.
 */
@Composable
internal fun ProviderPicker(
    query: String,
    options: List<ProviderOption>,
    selected: ProviderOption?,
    enabled: Boolean,
    errorMessage: String?,
    autoFocus: Boolean,
    onQueryChange: (String) -> Unit,
    onSelect: (ProviderOption) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.form_provider_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.Small),
        )

        if (selected != null) {
            SelectedProviderCard(
                option = selected,
                enabled = enabled,
                onClear = onClear,
            )
        } else {
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(autoFocus) {
                if (autoFocus) focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                enabled = enabled,
                singleLine = true,
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it) } },
                placeholder = { Text(stringResource(R.string.form_provider_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )

            if (options.isEmpty()) {
                Text(
                    text = stringResource(R.string.form_provider_no_results, query),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.Small),
                )
            } else {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.Small),
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = SuggestionsMaxHeight)) {
                        items(items = options, key = { it.id }) { option ->
                            ProviderRow(
                                option = option,
                                onClick = { onSelect(option) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedProviderCard(
    option: ProviderOption,
    enabled: Boolean,
    onClear: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProviderLogo(logoKey = option.logoKey, name = option.displayLabel())
            Spacer(Modifier.width(Spacing.Large))
            Text(
                text = option.displayLabel(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClear, enabled = enabled) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.form_provider_change),
                )
            }
        }
    }
}

@Composable
private fun ProviderRow(
    option: ProviderOption,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = MinTouchTarget)
            .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        ProviderLogo(
            logoKey = option.logoKey,
            name = option.displayLabel(),
            size = ProviderRowLogoSize,
        )
        Spacer(Modifier.width(Spacing.Medium))
        Text(
            text = option.displayLabel(),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The catch-all entry is the only catalog name that is translated. */
@Composable
internal fun ProviderOption.displayLabel(): String =
    if (isOther) stringResource(R.string.provider_other) else displayName

private val SuggestionsMaxHeight = 264.dp
private val ProviderRowLogoSize = 36.dp
