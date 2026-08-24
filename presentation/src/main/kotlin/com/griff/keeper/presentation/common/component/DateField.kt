package com.griff.keeper.presentation.common.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.format.DateFormatter
import com.griff.keeper.presentation.theme.GriffShapes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * A date picked in a Material 3 dialog.
 *
 * The text field only displays the value, so what is stored is always a valid [LocalDate] and never
 * free text. Shared by every form field that asks for a date; the label, the hint and whether the
 * value can be cleared are the only things that differ between them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    date: LocalDate?,
    label: String,
    enabled: Boolean,
    onDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    var isDialogVisible by remember { mutableStateOf(false) }
    val pickDateLabel = stringResource(R.string.form_date_pick)

    Box(modifier = modifier.fillMaxWidth()) {
        TextField(
            value = date?.let { DateFormatter.formatFullDate(it) } ?: "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = enabled,
            isError = isError,
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingText = supportingText?.let { { Text(it) } },
            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            trailingIcon = {
                if (date != null) {
                    IconButton(onClick = { onDateChange(null) }, enabled = enabled) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.form_date_clear),
                        )
                    }
                }
            },
            shape = GriffShapes.Interactive,
            colors = griffFilledTextFieldColors(),
        )

        // A read-only text field does not react to taps, so a transparent overlay opens the dialog.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { isDialogVisible = true }
                .semantics { contentDescription = pickDateLabel },
        )
    }

    if (isDialogVisible) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date?.toEpochMillis())
        DatePickerDialog(
            onDismissRequest = { isDialogVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateChange(pickerState.selectedDateMillis?.toLocalDate())
                        isDialogVisible = false
                    },
                ) {
                    Text(stringResource(R.string.form_date_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDialogVisible = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
