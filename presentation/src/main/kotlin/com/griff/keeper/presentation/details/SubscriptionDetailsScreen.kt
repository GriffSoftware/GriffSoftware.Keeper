package com.griff.keeper.presentation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.Labels
import com.griff.keeper.presentation.common.Tags
import com.griff.keeper.presentation.common.MessageSeverity
import com.griff.keeper.application.reminder.ItemReminderState
import com.griff.keeper.presentation.common.UiMessage
import com.griff.keeper.presentation.common.component.GriffSnackbarHost
import com.griff.keeper.presentation.common.component.ProviderLogo
import com.griff.keeper.presentation.common.component.ProviderLogoDefaults
import com.griff.keeper.presentation.common.component.showMessage
import com.griff.keeper.presentation.common.format.DateFormatter
import com.griff.keeper.presentation.common.format.MoneyFormatter
import com.griff.keeper.presentation.common.rememberUrlOpener
import com.griff.keeper.presentation.common.resolve
import com.griff.keeper.presentation.common.component.DeleteConfirmationDialog
import com.griff.keeper.presentation.common.component.DetailsInfoRow
import com.griff.keeper.presentation.common.component.TagChip
import com.griff.keeper.presentation.reminders.components.ItemReminderSection
import com.griff.keeper.presentation.reminders.rememberSystemNotificationsEnabled
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews
import java.time.LocalDate

@Composable
fun SubscriptionDetailsRoute(
    onNavigateUp: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: (UiMessage) -> Unit,
    viewModel: SubscriptionDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SubscriptionDetailsEvent.Deleted -> {
                    onDeleted(
                        UiMessage(
                            textRes = R.string.delete_success,
                            formatArgs = listOf(event.name),
                            severity = MessageSeverity.SUCCESS,
                        ),
                    )
                    onNavigateUp()
                }
            }
        }
    }

    SubscriptionDetailsScreen(
        state = state,
        onNavigateUp = onNavigateUp,
        onEdit = onEdit,
        onDeleteRequest = viewModel::onDeleteRequest,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onDeleteDismiss = viewModel::onDeleteDismiss,
        onManagementUrlOpenFailed = viewModel::onManagementUrlOpenFailed,
        onRemindersEnabledChange = viewModel::onRemindersEnabledChange,
        onMessageShown = viewModel::onMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubscriptionDetailsScreen(
    state: SubscriptionDetailsUiState,
    onNavigateUp: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleteRequest: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onManagementUrlOpenFailed: () -> Unit,
    onRemindersEnabledChange: (Boolean) -> Unit,
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
                title = { Text(stringResource(R.string.details_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    val details = state.details
                    if (details != null) {
                        IconButton(onClick = { onEdit(details.id) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.details_edit),
                            )
                        }
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
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.isMissing -> Text(
                    text = stringResource(R.string.details_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(Spacing.ExtraLarge),
                    textAlign = TextAlign.Center,
                )

                state.details != null -> SubscriptionDetailsContent(
                    details = state.details,
                    reminders = state.reminders,
                    isDeleting = state.isDeleting,
                    onDeleteRequest = onDeleteRequest,
                    onManagementUrlOpenFailed = onManagementUrlOpenFailed,
                    onRemindersEnabledChange = onRemindersEnabledChange,
                )
            }
        }
    }

    val details = state.details
    if (state.isDeleteDialogVisible && details != null) {
        DeleteConfirmationDialog(
            title = stringResource(R.string.delete_dialog_title),
            message = stringResource(R.string.delete_dialog_message, details.name),
            onConfirm = onDeleteConfirm,
            onDismiss = onDeleteDismiss,
        )
    }
}

@Composable
private fun SubscriptionDetailsContent(
    details: SubscriptionDetails,
    reminders: ItemReminderState?,
    isDeleting: Boolean,
    onDeleteRequest: () -> Unit,
    onManagementUrlOpenFailed: () -> Unit,
    onRemindersEnabledChange: (Boolean) -> Unit,
) {
    val openUrl = rememberUrlOpener()
    val systemNotificationsEnabled = rememberSystemNotificationsEnabled()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.Large, vertical = Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        ProviderLogo(
            logoKey = details.logoKey,
            name = details.name,
            size = ProviderLogoDefaults.LargeSize,
        )

        Text(
            text = details.name,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        // The tag is a visual accent for the category, not a second headline: it sits between the
        // name and the amount, where it reads as a label on the record rather than as data.
        TagChip(style = Tags.of(details.category))

        Text(
            text = MoneyFormatter.format(details.price),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.Small),
        ) {
            Column(modifier = Modifier.padding(Spacing.Large)) {
                DetailsInfoRow(
                    label = stringResource(R.string.details_billing_label),
                    value = stringResource(Labels.billingPeriodRecurrence(details.billingPeriod)),
                )
                HorizontalDivider()
                DetailsInfoRow(
                    label = stringResource(R.string.details_next_payment_label),
                    value = details.nextBillingDate
                        ?.let { DateFormatter.formatFullDate(it) }
                        ?: stringResource(R.string.details_next_payment_unknown),
                )
                HorizontalDivider()
                DetailsInfoRow(
                    label = stringResource(R.string.details_monthly_equivalent_label),
                    value = stringResource(
                        R.string.amount_per_month,
                        MoneyFormatter.format(details.monthlyEquivalent),
                    ),
                )
                HorizontalDivider()
                DetailsInfoRow(
                    label = stringResource(R.string.details_yearly_equivalent_label),
                    value = stringResource(
                        R.string.amount_per_year,
                        MoneyFormatter.format(details.yearlyEquivalent),
                    ),
                )
            }
        }

        if (reminders != null) {
            ItemReminderSection(
                state = reminders,
                systemNotificationsEnabled = systemNotificationsEnabled,
                disabledText = stringResource(R.string.reminder_section_off_subscription),
                noDateText = stringResource(R.string.reminder_section_no_billing_date),
                noDateHint = stringResource(R.string.reminder_section_no_billing_date_hint),
                onEnabledChange = onRemindersEnabledChange,
                isEditable = !isDeleting,
                modifier = Modifier.padding(top = Spacing.Small),
            )
        }

        val managementUrl = details.managementUrl
        Button(
            onClick = {
                if (managementUrl != null && !openUrl(managementUrl)) onManagementUrlOpenFailed()
            },
            enabled = managementUrl != null && !isDeleting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.Small),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(ButtonIconSize),
            )
            Text(
                text = stringResource(R.string.details_manage),
                modifier = Modifier.padding(start = Spacing.Small),
            )
        }

        if (managementUrl == null) {
            Text(
                text = stringResource(R.string.details_manage_missing_url),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        OutlinedButton(
            onClick = onDeleteRequest,
            enabled = !isDeleting,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            if (isDeleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ButtonIconSize),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonIconSize),
                )
            }
            Text(
                text = stringResource(R.string.details_delete),
                modifier = Modifier.padding(start = Spacing.Small),
            )
        }
    }
}

private val ButtonIconSize = 18.dp

@ThemePreviews
@Composable
private fun SubscriptionDetailsScreenPreview() {
    GriffThemePreview {
        SubscriptionDetailsScreen(
            state = SubscriptionDetailsUiState(
                isLoading = false,
                details = SubscriptionDetails(
                    id = "1",
                    name = "Spotify",
                    logoKey = "spotify",
                    category = ProviderCategory.MUSIC,
                    price = Money.ofUnits(34, 99),
                    billingPeriod = BillingPeriod.MONTHLY,
                    monthlyEquivalent = Money.ofUnits(34, 99),
                    yearlyEquivalent = Money.ofUnits(419, 88),
                    nextBillingDate = LocalDate.of(2026, 9, 14),
                    managementUrl = "https://www.spotify.com/account/subscription",
                ),
            ),
            onNavigateUp = {},
            onEdit = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteDismiss = {},
            onManagementUrlOpenFailed = {},
            onRemindersEnabledChange = {},
            onMessageShown = {},
        )
    }
}
