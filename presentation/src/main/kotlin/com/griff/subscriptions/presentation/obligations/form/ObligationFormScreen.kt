package com.griff.subscriptions.presentation.obligations.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.griff.subscriptions.domain.model.Currency
import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.PaymentStatus
import com.griff.subscriptions.domain.validation.ObligationField
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.Labels
import com.griff.subscriptions.presentation.common.component.CategoryOption
import com.griff.subscriptions.presentation.common.component.CategorySelector
import com.griff.subscriptions.presentation.common.component.DateField
import com.griff.subscriptions.presentation.common.component.GriffSnackbarHost
import com.griff.subscriptions.presentation.common.component.ObligationGlyph
import com.griff.subscriptions.presentation.common.component.accentSegmentedButtonColors
import com.griff.subscriptions.presentation.common.component.showMessage
import com.griff.subscriptions.presentation.common.format.symbol
import com.griff.subscriptions.presentation.common.resolve
import com.griff.subscriptions.presentation.theme.GriffThemePreview
import com.griff.subscriptions.presentation.theme.Spacing
import com.griff.subscriptions.presentation.theme.TallThemePreviews
import java.time.LocalDate

@Composable
fun ObligationFormRoute(
    onNavigateUp: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: ObligationFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ObligationFormEvent.Saved -> onSaved(event.obligationId)
            }
        }
    }

    ObligationFormScreen(
        state = state,
        onNavigateUp = onNavigateUp,
        onNameChange = viewModel::onNameChange,
        onCategoryChange = viewModel::onCategoryChange,
        onAmountChange = viewModel::onAmountChange,
        onPaymentStatusChange = viewModel::onPaymentStatusChange,
        onPaymentDateChange = viewModel::onPaymentDateChange,
        onDueDateChange = viewModel::onDueDateChange,
        onValidUntilChange = viewModel::onValidUntilChange,
        onNotesChange = viewModel::onNotesChange,
        onSave = viewModel::onSave,
        onMessageShown = viewModel::onMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ObligationFormScreen(
    state: ObligationFormUiState,
    onNavigateUp: () -> Unit,
    onNameChange: (String) -> Unit,
    onCategoryChange: (ObligationCategory) -> Unit,
    onAmountChange: (String) -> Unit,
    onPaymentStatusChange: (PaymentStatus) -> Unit,
    onPaymentDateChange: (LocalDate?) -> Unit,
    onDueDateChange: (LocalDate?) -> Unit,
    onValidUntilChange: (LocalDate?) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message?.resolve()

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showMessage(message)
            onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            when (state.mode) {
                                ObligationFormMode.ADD -> R.string.obligation_form_title_add
                                ObligationFormMode.EDIT -> R.string.obligation_form_title_edit
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { GriffSnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                ObligationFormContent(
                    state = state,
                    onNameChange = onNameChange,
                    onCategoryChange = onCategoryChange,
                    onAmountChange = onAmountChange,
                    onPaymentStatusChange = onPaymentStatusChange,
                    onPaymentDateChange = onPaymentDateChange,
                    onDueDateChange = onDueDateChange,
                    onValidUntilChange = onValidUntilChange,
                    onNotesChange = onNotesChange,
                    onSave = onSave,
                )
            }
        }
    }
}

/**
 * One form for every category.
 *
 * The category only changes the *order* of the two optional dates and whether the payment date is
 * asked for at all - an insurance leads with its expiry, a tax with its deadline. Nothing is hidden
 * permanently, so an unusual record can still be entered, and there is no separate screen per
 * category to keep in sync.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObligationFormContent(
    state: ObligationFormUiState,
    onNameChange: (String) -> Unit,
    onCategoryChange: (ObligationCategory) -> Unit,
    onAmountChange: (String) -> Unit,
    onPaymentStatusChange: (PaymentStatus) -> Unit,
    onPaymentDateChange: (LocalDate?) -> Unit,
    onDueDateChange: (LocalDate?) -> Unit,
    onValidUntilChange: (LocalDate?) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = Spacing.Large, vertical = Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.Large),
    ) {
        val nameError = state.errorFor(ObligationField.NAME)
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isEditable,
            singleLine = true,
            isError = nameError != null,
            supportingText = {
                Text(nameError ?: stringResource(R.string.obligation_form_name_hint))
            },
            label = { Text(stringResource(R.string.obligation_form_name_label)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        CategorySelector(
            label = stringResource(R.string.obligation_form_category_label),
            options = ObligationCategory.entries.map {
                CategoryOption(it, Labels.obligationCategory(it))
            },
            selected = state.category,
            enabled = state.isEditable,
            onSelect = onCategoryChange,
            errorMessage = state.errorFor(ObligationField.CATEGORY),
            leadingIcon = { category ->
                ObligationGlyph(
                    category = category,
                    modifier = Modifier.size(ChipIconSize),
                )
            },
        )

        val amountError = state.errorFor(ObligationField.AMOUNT)
        OutlinedTextField(
            value = state.amount,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isEditable,
            singleLine = true,
            isError = amountError != null,
            supportingText = amountError?.let { { Text(it) } },
            label = { Text(stringResource(R.string.obligation_form_amount_label)) },
            suffix = { Text(Currency.Default.symbol()) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
        )

        PaymentStatusSelector(
            selected = state.paymentStatus,
            enabled = state.isEditable,
            onSelect = onPaymentStatusChange,
        )

        if (state.isPaymentDateVisible) {
            DateField(
                date = state.paymentDate,
                label = stringResource(R.string.obligation_form_payment_date_label),
                supportingText = state.errorFor(ObligationField.PAYMENT_DATE)
                    ?: stringResource(R.string.obligation_form_payment_date_hint),
                isError = state.fieldErrors.containsKey(ObligationField.PAYMENT_DATE),
                enabled = state.isEditable,
                onDateChange = onPaymentDateChange,
            )
        }

        // The category decides which optional date leads; both stay available either way.
        val dateFields = listOf<@Composable () -> Unit>(
            {
                DateField(
                    date = state.validUntil,
                    label = stringResource(R.string.obligation_form_valid_until_label),
                    supportingText = stringResource(R.string.obligation_form_valid_until_hint),
                    enabled = state.isEditable,
                    onDateChange = onValidUntilChange,
                )
            },
            {
                DateField(
                    date = state.dueDate,
                    label = stringResource(R.string.obligation_form_due_date_label),
                    supportingText = stringResource(R.string.obligation_form_due_date_hint),
                    enabled = state.isEditable,
                    onDateChange = onDueDateChange,
                )
            },
        )
        val ordered = if (state.isExpiryLed) dateFields else dateFields.reversed()
        ordered.forEach { field -> field() }

        val notesError = state.errorFor(ObligationField.NOTES)
        OutlinedTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isEditable,
            minLines = NotesMinLines,
            isError = notesError != null,
            supportingText = {
                Text(notesError ?: stringResource(R.string.obligation_form_notes_hint))
            },
            label = { Text(stringResource(R.string.obligation_form_notes_label)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
        )

        Button(
            onClick = onSave,
            enabled = state.isSaveEnabled && !state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.Small),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(SaveIndicatorSize),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(R.string.obligation_form_save))
            }
        }
    }
}

/** Two option selector; a segmented button reads better than radio buttons for a binary choice. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentStatusSelector(
    selected: PaymentStatus,
    enabled: Boolean,
    onSelect: (PaymentStatus) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.obligation_form_status_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.Small),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PaymentStatus.entries.forEachIndexed { index, status ->
                SegmentedButton(
                    selected = status == selected,
                    onClick = { onSelect(status) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = PaymentStatus.entries.size,
                    ),
                    colors = accentSegmentedButtonColors(),
                ) {
                    Text(stringResource(Labels.paymentStatus(status)))
                }
            }
        }
    }
}

@Composable
private fun ObligationFormUiState.errorFor(field: ObligationField): String? =
    fieldErrors[field]?.let { stringResource(it) }

private val SaveIndicatorSize = 18.dp
private val ChipIconSize = 18.dp
private const val NotesMinLines = 3

@TallThemePreviews
@Composable
private fun ObligationFormScreenPreview() {
    GriffThemePreview {
        ObligationFormScreen(
            state = ObligationFormUiState(
                name = "OC Ford",
                category = ObligationCategory.VEHICLE_INSURANCE,
                amount = "1240,00",
                paymentStatus = PaymentStatus.PAID,
                paymentDate = LocalDate.of(2026, 3, 12),
                validUntil = LocalDate.of(2027, 3, 11),
                isSaveEnabled = true,
            ),
            onNavigateUp = {},
            onNameChange = {},
            onCategoryChange = {},
            onAmountChange = {},
            onPaymentStatusChange = {},
            onPaymentDateChange = {},
            onDueDateChange = {},
            onValidUntilChange = {},
            onNotesChange = {},
            onSave = {},
            onMessageShown = {},
        )
    }
}
