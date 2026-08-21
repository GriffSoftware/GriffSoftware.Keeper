package com.griff.keeper.presentation.obligations.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ScheduleSend
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.model.PaymentStatus
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.Labels
import com.griff.keeper.application.reminder.ItemReminderState
import com.griff.keeper.presentation.common.MessageSeverity
import com.griff.keeper.presentation.common.Tags
import com.griff.keeper.presentation.common.UiMessage
import com.griff.keeper.presentation.common.component.DeleteConfirmationDialog
import com.griff.keeper.presentation.common.component.DetailsInfoRow
import com.griff.keeper.presentation.common.component.GriffSnackbarHost
import com.griff.keeper.presentation.common.component.ObligationIcon
import com.griff.keeper.presentation.common.component.ObligationIconDefaults
import com.griff.keeper.presentation.common.component.TagChip
import com.griff.keeper.presentation.reminders.components.ItemReminderSection
import com.griff.keeper.presentation.reminders.rememberSystemNotificationsEnabled
import com.griff.keeper.presentation.common.component.showMessage
import com.griff.keeper.presentation.common.format.DateFormatter
import com.griff.keeper.presentation.common.format.MoneyFormatter
import com.griff.keeper.presentation.common.resolve
import com.griff.keeper.presentation.obligations.DeadlineStatus
import com.griff.keeper.presentation.obligations.DeadlineUrgency
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews
import java.time.LocalDate

@Composable
fun ObligationDetailsRoute(
    onNavigateUp: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: (UiMessage) -> Unit,
    pendingMessage: UiMessage? = null,
    onPendingMessageShown: () -> Unit = {},
    viewModel: ObligationDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ObligationDetailsEvent.Deleted -> {
                    onDeleted(
                        UiMessage(
                            textRes = R.string.obligation_delete_success,
                            formatArgs = listOf(event.name),
                            severity = MessageSeverity.SUCCESS,
                        ),
                    )
                    onNavigateUp()
                }
            }
        }
    }

    ObligationDetailsScreen(
        state = state,
        onNavigateUp = onNavigateUp,
        onEdit = onEdit,
        onDeleteRequest = viewModel::onDeleteRequest,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onDeleteDismiss = viewModel::onDeleteDismiss,
        onRemindersEnabledChange = viewModel::onRemindersEnabledChange,
        onMessageShown = viewModel::onMessageShown,
        pendingMessage = pendingMessage,
        onPendingMessageShown = onPendingMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ObligationDetailsScreen(
    state: ObligationDetailsUiState,
    onNavigateUp: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleteRequest: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onMessageShown: () -> Unit,
    pendingMessage: UiMessage? = null,
    onPendingMessageShown: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message?.resolve()
    // Feedback from a screen that has already closed - typically the edit form, which pops back to
    // here - so this is where it gets shown.
    val externalMessage = pendingMessage?.resolve()

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showMessage(message)
            onMessageShown()
        }
    }

    LaunchedEffect(externalMessage) {
        if (externalMessage != null) {
            snackbarHostState.showMessage(externalMessage)
            onPendingMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.obligation_details_title)) },
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
                                contentDescription = stringResource(
                                    R.string.obligation_details_edit,
                                ),
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
                    text = stringResource(R.string.obligation_details_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(Spacing.ExtraLarge),
                    textAlign = TextAlign.Center,
                )

                state.details != null -> ObligationDetailsContent(
                    details = state.details,
                    reminders = state.reminders,
                    isDeleting = state.isDeleting,
                    onDeleteRequest = onDeleteRequest,
                    onRemindersEnabledChange = onRemindersEnabledChange,
                )
            }
        }
    }

    val details = state.details
    if (state.isDeleteDialogVisible && details != null) {
        DeleteConfirmationDialog(
            title = stringResource(R.string.obligation_delete_dialog_title),
            message = stringResource(
                R.string.obligation_delete_dialog_message,
                details.name,
            ),
            onConfirm = onDeleteConfirm,
            onDismiss = onDeleteDismiss,
        )
    }
}

/**
 * A details page, not a read-only form: the identity of the record leads (icon, name, tag, amount),
 * and the facts about it follow in one grouped card.
 */
@Composable
private fun ObligationDetailsContent(
    details: ObligationDetails,
    reminders: ItemReminderState?,
    isDeleting: Boolean,
    onDeleteRequest: () -> Unit,
    onRemindersEnabledChange: (Boolean) -> Unit,
) {
    val systemNotificationsEnabled = rememberSystemNotificationsEnabled()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.Large, vertical = Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        ObligationIcon(
            category = details.category,
            size = ObligationIconDefaults.LargeSize,
        )

        Text(
            text = details.name,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        TagChip(style = Tags.of(details.category))

        Text(
            text = MoneyFormatter.format(details.amount),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )

        val deadline = details.deadline
        if (deadline != null && deadline.urgency != DeadlineUrgency.NORMAL) {
            DeadlineBanner(deadline)
        }

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.Small),
        ) {
            Column(modifier = Modifier.padding(Spacing.Large)) {
                DetailsInfoRow(
                    label = stringResource(R.string.obligation_details_status_label),
                    value = stringResource(Labels.paymentStatus(details.paymentStatus)),
                )
                HorizontalDivider()
                DetailsInfoRow(
                    label = stringResource(R.string.obligation_details_category_label),
                    value = stringResource(Labels.obligationCategory(details.category)),
                )

                // Only the dates the record actually has: an empty row would suggest the value is
                // missing rather than irrelevant for this kind of cost.
                if (details.paymentDate != null) {
                    HorizontalDivider()
                    DetailsInfoRow(
                        label = stringResource(R.string.obligation_details_paid_label),
                        value = DateFormatter.formatFullDate(details.paymentDate),
                    )
                }
                if (details.dueDate != null) {
                    HorizontalDivider()
                    DetailsInfoRow(
                        label = stringResource(R.string.obligation_details_due_label),
                        value = DateFormatter.formatFullDate(details.dueDate),
                    )
                }
                if (details.validUntil != null) {
                    HorizontalDivider()
                    DetailsInfoRow(
                        label = stringResource(R.string.obligation_details_valid_until_label),
                        value = DateFormatter.formatFullDate(details.validUntil),
                    )
                }
                if (!details.notes.isNullOrBlank()) {
                    HorizontalDivider()
                    DetailsInfoRow(
                        label = stringResource(R.string.obligation_details_notes_label),
                        value = details.notes,
                    )
                }
            }
        }

        if (reminders != null) {
            ItemReminderSection(
                state = reminders,
                systemNotificationsEnabled = systemNotificationsEnabled,
                disabledText = stringResource(R.string.reminder_section_off_obligation),
                // A settled charge has no deadline left to warn about; an insurance still expires,
                // so the same record can move between these two states as its payment status
                // changes.
                noDateText = if (details.paymentStatus == PaymentStatus.PAID) {
                    stringResource(R.string.reminder_section_paid)
                } else {
                    stringResource(R.string.reminder_section_no_date_obligation)
                },
                noDateHint = stringResource(R.string.reminder_section_no_date_obligation_hint),
                onEnabledChange = onRemindersEnabledChange,
                isEditable = !isDeleting,
                modifier = Modifier.padding(top = Spacing.Small),
            )
        }

        OutlinedButton(
            onClick = onDeleteRequest,
            enabled = !isDeleting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.Small),
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
                text = stringResource(R.string.obligation_details_delete),
                modifier = Modifier.padding(start = Spacing.Small),
            )
        }
    }
}

/** Deadline notice: icon plus words, so the state never rests on the color alone. */
@Composable
private fun DeadlineBanner(deadline: DeadlineStatus) {
    val color = when (deadline.urgency) {
        DeadlineUrgency.OVERDUE -> MaterialTheme.colorScheme.error
        else -> GriffTheme.colors.warning
    }
    val text = when {
        deadline.daysPluralRes != null ->
            pluralStringResource(deadline.daysPluralRes, deadline.days, deadline.days)

        deadline.textRes != null -> stringResource(deadline.textRes)
        else -> return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Icon(
            imageVector = when (deadline.urgency) {
                DeadlineUrgency.OVERDUE -> Icons.Default.ErrorOutline
                else -> Icons.AutoMirrored.Filled.ScheduleSend
            },
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(BannerIconSize),
        )
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

private val ButtonIconSize = 18.dp
private val BannerIconSize = 18.dp

@ThemePreviews
@Composable
private fun ObligationDetailsScreenPreview() {
    GriffThemePreview {
        ObligationDetailsScreen(
            state = ObligationDetailsUiState(
                isLoading = false,
                details = ObligationDetails(
                    id = "1",
                    name = "OC Ford",
                    category = ObligationCategory.VEHICLE_INSURANCE,
                    amount = Money.ofUnits(1_240),
                    paymentStatus = PaymentStatus.PAID,
                    paymentDate = LocalDate.of(2026, 3, 12),
                    dueDate = null,
                    validUntil = LocalDate.of(2027, 3, 11),
                    notes = "Polisa PZU nr ABC123",
                    deadline = DeadlineStatus(DeadlineUrgency.NORMAL),
                ),
            ),
            onNavigateUp = {},
            onEdit = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteDismiss = {},
            onRemindersEnabledChange = {},
            onMessageShown = {},
        )
    }
}
