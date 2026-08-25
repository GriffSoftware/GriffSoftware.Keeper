package com.griff.keeper.presentation.common.currency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.griff.keeper.domain.currency.CurrencyConversionPreview
import com.griff.keeper.domain.currency.CurrencyConversionSample
import com.griff.keeper.domain.model.ExchangeRate
import com.griff.keeper.domain.validation.ExchangeRateError
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.Labels
import com.griff.keeper.presentation.common.component.griffFilledTextFieldColors
import com.griff.keeper.presentation.common.format.MoneyFormatter
import com.griff.keeper.presentation.common.format.currentLocale
import com.griff.keeper.presentation.theme.GriffShapes
import com.griff.keeper.presentation.theme.Spacing
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Step one of a currency change that actually touches stored data: asks for the "1 EUR = X PLN"
 * exchange rate the conversion will use.
 *
 * The rate is always asked for and shown in this one fixed direction (see [ExchangeRate]),
 * independent of whether the switch is PLN -> EUR or EUR -> PLN, so the field never needs to explain
 * which currency it multiplies.
 */
@Composable
internal fun ExchangeRateDialog(
    fromName: String,
    toName: String,
    rateInput: String,
    error: ExchangeRateError?,
    isBusy: Boolean,
    onRateChange: (String) -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CurrencyExchange, contentDescription = null) },
        title = { Text(stringResource(R.string.currency_rate_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Text(stringResource(R.string.currency_rate_dialog_message, fromName, toName))
                TextField(
                    value = rateInput,
                    onValueChange = { onRateChange(ExchangeRateInput.sanitize(it)) },
                    enabled = !isBusy,
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(stringResource(Labels.exchangeRateError(it))) } },
                    prefix = { Text(stringResource(R.string.currency_rate_input_prefix)) },
                    suffix = { Text(stringResource(R.string.currency_rate_input_suffix)) },
                    shape = GriffShapes.Interactive,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = griffFilledTextFieldColors(),
                )
                Text(
                    text = stringResource(R.string.currency_rate_dialog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue, enabled = !isBusy && rateInput.isNotBlank()) {
                Text(stringResource(R.string.action_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/**
 * Step two: a handful of real before/after amounts computed with the rate the user just typed, so a
 * mistyped rate is visible before anything is written.
 */
@Composable
internal fun CurrencyConversionPreviewDialog(
    preview: CurrencyConversionPreview,
    isBusy: Boolean,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = currentLocale()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CurrencyExchange, contentDescription = null) },
        title = { Text(stringResource(R.string.currency_preview_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Text(
                    text = stringResource(
                        R.string.currency_preview_direction,
                        preview.from.code,
                        preview.to.code,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                    Text(
                        text = stringResource(R.string.currency_preview_rate_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.currency_preview_rate_value,
                            formatRate(preview.rate, locale),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                if (preview.samples.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                        Text(
                            text = stringResource(R.string.currency_preview_sample_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        preview.samples.forEach { sample ->
                            SampleRow(sample = sample, preview = preview, locale = locale)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                    Text(
                        text = stringResource(R.string.currency_preview_affected_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.data_transfer_count_subscriptions,
                            preview.subscriptionCount,
                            preview.subscriptionCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.data_transfer_count_obligations,
                            preview.obligationCount,
                            preview.obligationCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue, enabled = !isBusy) {
                Text(stringResource(R.string.action_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun SampleRow(sample: CurrencyConversionSample, preview: CurrencyConversionPreview, locale: Locale) {
    Text(
        text = stringResource(
            R.string.currency_preview_sample_row,
            sample.name,
            MoneyFormatter.format(sample.before, preview.from, locale),
            MoneyFormatter.format(sample.after, preview.to, locale),
        ),
        style = MaterialTheme.typography.bodyMedium,
    )
}

/**
 * Step three: the last, explicit gate. Converting affects every stored amount at once, so it is
 * confirmed like the backup replace flow is - clearly, but without the error color, since nothing is
 * deleted.
 */
@Composable
internal fun CurrencyConversionConfirmDialog(
    preview: CurrencyConversionPreview,
    isBusy: Boolean,
    onConfirm: () -> Unit,
    onCreateBackup: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val locale = currentLocale()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CurrencyExchange, contentDescription = null) },
        title = { Text(stringResource(R.string.currency_confirm_title)) },
        text = {
            Text(
                stringResource(
                    R.string.currency_confirm_message,
                    preview.from.code,
                    preview.to.code,
                    formatRate(preview.rate, locale),
                ),
            )
        },
        confirmButton = {
            Column {
                TextButton(onClick = onConfirm, enabled = !isBusy) {
                    Text(stringResource(R.string.currency_confirm_action))
                }
                if (onCreateBackup != null) {
                    TextButton(onClick = onCreateBackup, enabled = !isBusy) {
                        Text(stringResource(R.string.currency_confirm_backup_action))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/** Shown while the Room transaction runs; not dismissible, so a double conversion cannot start. */
@Composable
internal fun CurrencyConversionProgressDialog() {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(ProgressIndicatorSize))
                Text(
                    text = stringResource(R.string.currency_progress_message),
                    modifier = Modifier.padding(start = Spacing.Medium),
                )
            }
        },
    )
}

/**
 * Renders [rate] with exactly the decimals the user typed, e.g. `4,2500` - never rounded, never
 * padded beyond what they entered.
 */
private fun formatRate(rate: ExchangeRate, locale: Locale): String {
    val scale = rate.eurToPln.scale().coerceAtLeast(0)
    val pattern = if (scale == 0) "#,##0" else "#,##0." + "0".repeat(scale)
    return DecimalFormat(pattern, DecimalFormatSymbols(locale)).format(rate.eurToPln)
}

private val ProgressIndicatorSize = 24.dp
