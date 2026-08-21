package com.griff.subscriptions.presentation.obligations.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griff.subscriptions.application.obligation.AddObligationUseCase
import com.griff.subscriptions.application.obligation.GetObligationUseCase
import com.griff.subscriptions.application.obligation.UpdateObligationUseCase
import com.griff.subscriptions.application.obligation.ValidateObligationInputUseCase
import com.griff.subscriptions.domain.model.Obligation
import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.ObligationId
import com.griff.subscriptions.domain.model.PaymentStatus
import com.griff.subscriptions.domain.time.ClockProvider
import com.griff.subscriptions.domain.validation.ObligationField
import com.griff.subscriptions.domain.validation.ObligationInput
import com.griff.subscriptions.domain.validation.ObligationInputValidation
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.Labels
import com.griff.subscriptions.presentation.common.MessageSeverity
import com.griff.subscriptions.presentation.common.UiMessage
import com.griff.subscriptions.presentation.common.format.PriceInput
import com.griff.subscriptions.presentation.navigation.OBLIGATION_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One-off signals of the obligation add/edit form. */
sealed interface ObligationFormEvent {
    data class Saved(val obligationId: String) : ObligationFormEvent
}

/**
 * Drives both the add and the edit form.
 *
 * The presence of the [OBLIGATION_ID_ARG] navigation argument decides the mode, so the two
 * destinations share one screen - the same arrangement the subscription form uses.
 */
@HiltViewModel
class ObligationFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getObligation: GetObligationUseCase,
    private val addObligation: AddObligationUseCase,
    private val updateObligation: UpdateObligationUseCase,
    private val validateInput: ValidateObligationInputUseCase,
    clock: ClockProvider,
) : ViewModel() {

    private val editedId: ObligationId? =
        savedStateHandle.get<String>(OBLIGATION_ID_ARG)?.let(::ObligationId)

    private val _uiState = MutableStateFlow(
        ObligationFormUiState(
            mode = if (editedId == null) ObligationFormMode.ADD else ObligationFormMode.EDIT,
            isLoading = editedId != null,
            // A new record is almost always one the user has just paid, so today is the useful
            // default; it stays editable and only applies while the status is "paid".
            paymentDate = if (editedId == null) clock.today() else null,
        ),
    )
    val uiState: StateFlow<ObligationFormUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ObligationFormEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: Flow<ObligationFormEvent> = _events.asSharedFlow()

    /** Errors are hidden until the user tries to save, except for clearly wrong values. */
    private var saveAttempted = false

    init {
        editedId?.let(::loadObligation)
    }

    fun onNameChange(value: String) = update { it.copy(name = value) }

    fun onCategoryChange(value: ObligationCategory) = update { it.copy(category = value) }

    fun onAmountChange(value: String) = update { it.copy(amount = PriceInput.sanitize(value)) }

    fun onPaymentStatusChange(value: PaymentStatus) = update { it.copy(paymentStatus = value) }

    fun onPaymentDateChange(value: LocalDate?) = update { it.copy(paymentDate = value) }

    fun onDueDateChange(value: LocalDate?) = update { it.copy(dueDate = value) }

    fun onValidUntilChange(value: LocalDate?) = update { it.copy(validUntil = value) }

    fun onNotesChange(value: String) = update { it.copy(notes = value) }

    fun onRemindersEnabledChange(value: Boolean) = update { it.copy(remindersEnabled = value) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onSave() {
        saveAttempted = true
        val validation = validateInput(currentInput())
        if (validation !is ObligationInputValidation.Valid) {
            revalidate()
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                when (val id = editedId) {
                    null -> addObligation(validation.input).value
                    else -> {
                        updateObligation(id, validation.input).getOrThrow()
                        id.value
                    }
                }
            }
                .onSuccess { savedId -> _events.tryEmit(ObligationFormEvent.Saved(savedId)) }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            message = UiMessage(
                                R.string.error_save_failed,
                                severity = MessageSeverity.ERROR,
                            ),
                        )
                    }
                }
        }
    }

    private fun loadObligation(id: ObligationId) {
        viewModelScope.launch {
            runCatching { getObligation(id) }
                .onSuccess { obligation ->
                    if (obligation == null) {
                        _uiState.update { it.copy(isLoading = false, message = loadFailure()) }
                    } else {
                        _uiState.update { it.prefilledWith(obligation) }
                        revalidate()
                    }
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update { it.copy(isLoading = false, message = loadFailure()) }
                }
        }
    }

    private fun loadFailure() =
        UiMessage(R.string.error_load_failed, severity = MessageSeverity.ERROR)

    private fun ObligationFormUiState.prefilledWith(obligation: Obligation) = copy(
        isLoading = false,
        name = obligation.name.value,
        category = obligation.category,
        amount = PriceInput.format(obligation.amount),
        paymentStatus = obligation.payment.status,
        paymentDate = obligation.paymentDate,
        dueDate = obligation.dueDate,
        validUntil = obligation.validUntil,
        notes = obligation.notes ?: "",
        remindersEnabled = obligation.remindersEnabled,
    )

    private fun currentInput(): ObligationInput = with(_uiState.value) {
        ObligationInput(
            name = name,
            category = category,
            amount = amount,
            paymentStatus = paymentStatus,
            // An open record must not carry a settlement date, even if one was typed before the
            // status was switched back.
            paymentDate = paymentDate.takeIf { paymentStatus == PaymentStatus.PAID },
            dueDate = dueDate,
            validUntil = validUntil,
            notes = notes,
            remindersEnabled = remindersEnabled,
        )
    }

    /** Applies a change and re-runs validation, which every field edit needs. */
    private fun update(transform: (ObligationFormUiState) -> ObligationFormUiState) {
        _uiState.update(transform)
        revalidate()
    }

    private fun revalidate() {
        val state = _uiState.value
        val validation = validateInput(currentInput())
        val errors = when (validation) {
            is ObligationInputValidation.Valid -> emptyMap()
            is ObligationInputValidation.Invalid -> validation.errors
                .filter { error -> saveAttempted || state.isEagerlyReported(error.field) }
                .associate { error -> error.field to Labels.obligationInputError(error) }
        }

        _uiState.update {
            it.copy(
                fieldErrors = errors,
                isSaveEnabled = validation is ObligationInputValidation.Valid,
            )
        }
    }

    /** Amount and note problems are shown while typing; missing values only after a save attempt. */
    private fun ObligationFormUiState.isEagerlyReported(field: ObligationField): Boolean =
        when (field) {
            ObligationField.AMOUNT -> amount.isNotBlank()
            ObligationField.NOTES -> notes.isNotBlank()
            ObligationField.NAME, ObligationField.CATEGORY, ObligationField.PAYMENT_DATE -> false
        }
}
