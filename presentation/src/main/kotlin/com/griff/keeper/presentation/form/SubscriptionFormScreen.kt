package com.griff.keeper.presentation.form

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import java.time.LocalDate
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.validation.SubscriptionField
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.Labels
import com.griff.keeper.presentation.common.UiMessage
import com.griff.keeper.presentation.common.component.CategoryOption
import com.griff.keeper.presentation.common.component.CategorySelector
import com.griff.keeper.presentation.common.component.DateField
import com.griff.keeper.presentation.common.component.GriffSnackbarHost
import com.griff.keeper.presentation.common.component.RemindersToggleField
import com.griff.keeper.presentation.common.component.showMessage
import com.griff.keeper.presentation.common.format.currentLocale
import com.griff.keeper.presentation.common.format.symbol
import com.griff.keeper.presentation.common.resolve
import com.griff.keeper.presentation.form.components.BillingPeriodSelector
import com.griff.keeper.presentation.form.components.ProviderPicker
import com.griff.keeper.presentation.theme.GriffShapes
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

@Composable
fun SubscriptionFormRoute(
    onNavigateUp: () -> Unit,
    onSaved: (String, UiMessage) -> Unit,
    viewModel: SubscriptionFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SubscriptionFormEvent.Saved -> onSaved(event.subscriptionId, event.message)
            }
        }
    }

    SubscriptionFormScreen(
        state = state,
        onNavigateUp = onNavigateUp,
        onProviderQueryChange = viewModel::onProviderQueryChange,
        onProviderSelected = viewModel::onProviderSelected,
        onProviderCleared = viewModel::onProviderCleared,
        onNameChange = viewModel::onNameChange,
        onCategoryChange = viewModel::onCategoryChange,
        onPriceChange = viewModel::onPriceChange,
        onBillingPeriodChange = viewModel::onBillingPeriodChange,
        onNextBillingDateChange = viewModel::onNextBillingDateChange,
        onManagementUrlChange = viewModel::onManagementUrlChange,
        onRemindersEnabledChange = viewModel::onRemindersEnabledChange,
        onSave = viewModel::onSave,
        onMessageShown = viewModel::onMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubscriptionFormScreen(
    state: SubscriptionFormUiState,
    onNavigateUp: () -> Unit,
    onProviderQueryChange: (String) -> Unit,
    onProviderSelected: (ProviderOption) -> Unit,
    onProviderCleared: () -> Unit,
    onNameChange: (String) -> Unit,
    onCategoryChange: (ProviderCategory) -> Unit,
    onPriceChange: (String) -> Unit,
    onBillingPeriodChange: (BillingPeriod) -> Unit,
    onNextBillingDateChange: (LocalDate?) -> Unit,
    onManagementUrlChange: (String) -> Unit,
    onRemindersEnabledChange: (Boolean) -> Unit,
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
                                SubscriptionFormMode.ADD -> R.string.form_title_add
                                SubscriptionFormMode.EDIT -> R.string.form_title_edit
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
                SubscriptionFormContent(
                    state = state,
                    onProviderQueryChange = onProviderQueryChange,
                    onProviderSelected = onProviderSelected,
                    onProviderCleared = onProviderCleared,
                    onNameChange = onNameChange,
                    onCategoryChange = onCategoryChange,
                    onPriceChange = onPriceChange,
                    onBillingPeriodChange = onBillingPeriodChange,
                    onNextBillingDateChange = onNextBillingDateChange,
                    onManagementUrlChange = onManagementUrlChange,
                    onRemindersEnabledChange = onRemindersEnabledChange,
                    onSave = onSave,
                )
            }
        }
    }
}

@Composable
private fun SubscriptionFormContent(
    state: SubscriptionFormUiState,
    onProviderQueryChange: (String) -> Unit,
    onProviderSelected: (ProviderOption) -> Unit,
    onProviderCleared: () -> Unit,
    onNameChange: (String) -> Unit,
    onCategoryChange: (ProviderCategory) -> Unit,
    onPriceChange: (String) -> Unit,
    onBillingPeriodChange: (BillingPeriod) -> Unit,
    onNextBillingDateChange: (LocalDate?) -> Unit,
    onManagementUrlChange: (String) -> Unit,
    onRemindersEnabledChange: (Boolean) -> Unit,
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
        ProviderPicker(
            query = state.providerQuery,
            options = state.providerOptions,
            selected = state.selectedProvider,
            enabled = state.isEditable,
            errorMessage = state.errorFor(SubscriptionField.PROVIDER),
            autoFocus = state.mode == SubscriptionFormMode.ADD,
            onQueryChange = onProviderQueryChange,
            onSelect = onProviderSelected,
            onClear = onProviderCleared,
        )

        if (state.isNameFieldVisible) {
            val nameError = state.errorFor(SubscriptionField.NAME)
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isEditable,
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                label = { Text(stringResource(R.string.form_name_label)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
        }

        if (state.isCategoryFieldVisible) {
            CategorySelector(
                label = stringResource(R.string.form_category_label),
                options = ProviderCategory.entries.map { CategoryOption(it, Labels.category(it)) },
                selected = state.category,
                enabled = state.isEditable,
                onSelect = onCategoryChange,
            )
        }

        val priceError = state.errorFor(SubscriptionField.PRICE)
        OutlinedTextField(
            value = state.price,
            onValueChange = onPriceChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isEditable,
            singleLine = true,
            isError = priceError != null,
            supportingText = priceError?.let { { Text(it) } },
            label = { Text(stringResource(R.string.form_price_label)) },
            suffix = { Text(Currency.Default.symbol(currentLocale())) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
        )

        BillingPeriodSelector(
            selected = state.billingPeriod,
            enabled = state.isEditable,
            onSelect = onBillingPeriodChange,
        )

        DateField(
            date = state.nextBillingDate,
            label = stringResource(R.string.form_next_billing_label),
            supportingText = stringResource(R.string.form_next_billing_hint),
            enabled = state.isEditable,
            onDateChange = onNextBillingDateChange,
        )

        val urlError = state.errorFor(SubscriptionField.MANAGEMENT_URL)
        OutlinedTextField(
            value = state.managementUrl,
            onValueChange = onManagementUrlChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isEditable,
            singleLine = true,
            isError = urlError != null,
            supportingText = { Text(urlError ?: stringResource(R.string.form_management_url_hint)) },
            label = { Text(stringResource(R.string.form_management_url_label)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
        )

        RemindersToggleField(
            enabled = state.remindersEnabled,
            hint = stringResource(R.string.form_reminders_hint_subscription),
            isEditable = state.isEditable,
            onEnabledChange = onRemindersEnabledChange,
        )

        Button(
            onClick = onSave,
            enabled = state.isSaveEnabled && !state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.Small),
            shape = GriffShapes.Interactive,
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(SaveIndicatorSize),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(R.string.form_save))
            }
        }
    }
}

@Composable
private fun SubscriptionFormUiState.errorFor(field: SubscriptionField): String? =
    fieldErrors[field]?.let { stringResource(it) }

private val SaveIndicatorSize = 18.dp

@ThemePreviews
@Composable
private fun SubscriptionFormScreenPreview() {
    GriffThemePreview {
        SubscriptionFormScreen(
            state = SubscriptionFormUiState(
                providerOptions = listOf(
                    ProviderOption("spotify", "Spotify", "spotify", isOther = false),
                    ProviderOption("netflix", "Netflix", "netflix", isOther = false),
                    ProviderOption("other", "Other", "other", isOther = true),
                ),
                price = "34,99",
            ),
            onNavigateUp = {},
            onProviderQueryChange = {},
            onProviderSelected = {},
            onProviderCleared = {},
            onNameChange = {},
            onCategoryChange = {},
            onPriceChange = {},
            onBillingPeriodChange = {},
            onNextBillingDateChange = {},
            onManagementUrlChange = {},
            onRemindersEnabledChange = {},
            onSave = {},
            onMessageShown = {},
        )
    }
}
