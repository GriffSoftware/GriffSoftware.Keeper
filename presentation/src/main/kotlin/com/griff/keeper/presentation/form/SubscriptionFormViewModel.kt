package com.griff.keeper.presentation.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griff.keeper.application.provider.GetProviderUseCase
import com.griff.keeper.application.provider.GetProvidersUseCase
import com.griff.keeper.application.provider.SearchProvidersUseCase
import com.griff.keeper.application.currency.ObserveAppCurrencyUseCase
import com.griff.keeper.application.subscription.AddSubscriptionUseCase
import com.griff.keeper.application.subscription.GetSubscriptionUseCase
import com.griff.keeper.application.subscription.UpdateSubscriptionUseCase
import com.griff.keeper.application.subscription.ValidateSubscriptionInputUseCase
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.Provider
import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.model.ProviderId
import com.griff.keeper.domain.model.Subscription
import com.griff.keeper.domain.model.SubscriptionId
import com.griff.keeper.domain.validation.SubscriptionField
import com.griff.keeper.domain.validation.SubscriptionInput
import com.griff.keeper.domain.validation.SubscriptionInputValidation
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.Labels
import com.griff.keeper.presentation.common.MessageSeverity
import com.griff.keeper.presentation.common.UiMessage
import com.griff.keeper.presentation.common.format.PriceInput
import com.griff.keeper.presentation.navigation.SUBSCRIPTION_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One-off signals of the add/edit form. */
sealed interface SubscriptionFormEvent {
    /**
     * The record was written and the form is done.
     *
     * Carries the confirmation because the form closes itself: by the time it could show a snackbar
     * it no longer exists, so the message travels to whichever screen the user lands on.
     */
    data class Saved(
        val subscriptionId: String,
        val message: UiMessage,
    ) : SubscriptionFormEvent
}

/**
 * Drives both the add and the edit form.
 *
 * The presence of the [SUBSCRIPTION_ID_ARG] navigation argument decides the mode, so the two
 * destinations can share one screen without duplicating state handling.
 */
@HiltViewModel
class SubscriptionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProviders: GetProvidersUseCase,
    private val searchProviders: SearchProvidersUseCase,
    private val getProvider: GetProviderUseCase,
    private val getSubscription: GetSubscriptionUseCase,
    private val addSubscription: AddSubscriptionUseCase,
    private val updateSubscription: UpdateSubscriptionUseCase,
    private val validateInput: ValidateSubscriptionInputUseCase,
    observeAppCurrency: ObserveAppCurrencyUseCase,
) : ViewModel() {

    private val editedId: SubscriptionId? =
        savedStateHandle.get<String>(SUBSCRIPTION_ID_ARG)?.let(::SubscriptionId)

    /**
     * The currency a *new* record is saved in. Read reactively rather than fetched once at save
     * time: [onSave] and [revalidate] are both synchronous, so the value has to already be here when
     * they run, the same way every other piece of form state is.
     */
    private val appCurrency: StateFlow<Currency> = observeAppCurrency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Currency.Default)

    private val _uiState = MutableStateFlow(
        SubscriptionFormUiState(
            mode = if (editedId == null) SubscriptionFormMode.ADD else SubscriptionFormMode.EDIT,
            isLoading = editedId != null,
            providerOptions = emptyList(),
        ),
    )
    val uiState: StateFlow<SubscriptionFormUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SubscriptionFormEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: Flow<SubscriptionFormEvent> = _events.asSharedFlow()

    /** Errors are hidden until the user tries to save, except for clearly wrong values. */
    private var saveAttempted = false

    init {
        _uiState.update { it.copy(providerOptions = getProviders().map(Provider::toOption)) }
        editedId?.let(::loadSubscription)
    }

    fun onProviderQueryChange(query: String) {
        _uiState.update {
            it.copy(
                providerQuery = query,
                providerOptions = searchProviders(query).map(Provider::toOption),
            )
        }
        revalidate()
    }

    fun onProviderSelected(option: ProviderOption) {
        val provider = getProvider(ProviderId(option.id))
        _uiState.update { current ->
            current.copy(
                selectedProvider = option,
                providerQuery = "",
                providerOptions = getProviders().map(Provider::toOption),
                name = if (provider.isOther) current.name else provider.displayName,
                // A catalog entry brings its own category; a custom one keeps whatever the user
                // has already picked, defaulting to "other".
                category = if (provider.isOther) current.category else provider.category,
                managementUrl = provider.defaultManagementUrl?.value ?: "",
            )
        }
        revalidate()
    }

    fun onProviderCleared() {
        _uiState.update {
            it.copy(
                selectedProvider = null,
                providerQuery = "",
                providerOptions = getProviders().map(Provider::toOption),
            )
        }
        revalidate()
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value) }
        revalidate()
    }

    fun onCategoryChange(value: ProviderCategory) {
        _uiState.update { it.copy(category = value) }
        revalidate()
    }

    fun onPriceChange(value: String) {
        _uiState.update { it.copy(price = PriceInput.sanitize(value)) }
        revalidate()
    }

    fun onBillingPeriodChange(period: BillingPeriod) {
        _uiState.update { it.copy(billingPeriod = period) }
        revalidate()
    }

    fun onNextBillingDateChange(date: LocalDate?) {
        _uiState.update { it.copy(nextBillingDate = date) }
        revalidate()
    }

    fun onManagementUrlChange(value: String) {
        _uiState.update { it.copy(managementUrl = value) }
        revalidate()
    }

    fun onRemindersEnabledChange(value: Boolean) {
        _uiState.update { it.copy(remindersEnabled = value) }
        revalidate()
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onSave() {
        saveAttempted = true
        val validation = validateInput(currentInput(), appCurrency.value)
        if (validation !is SubscriptionInputValidation.Valid) {
            revalidate()
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                when (val id = editedId) {
                    null -> addSubscription(validation.input).value
                    else -> {
                        updateSubscription(id, validation.input).getOrThrow()
                        id.value
                    }
                }
            }
                .onSuccess { savedId ->
                    _events.tryEmit(
                        SubscriptionFormEvent.Saved(
                            subscriptionId = savedId,
                            message = UiMessage(
                                textRes = if (editedId == null) {
                                    R.string.subscription_added
                                } else {
                                    R.string.subscription_updated
                                },
                                severity = MessageSeverity.SUCCESS,
                            ),
                        ),
                    )
                }
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

    private fun loadSubscription(id: SubscriptionId) {
        viewModelScope.launch {
            runCatching { getSubscription(id) }
                .onSuccess { subscription ->
                    if (subscription == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                message = UiMessage(
                                    R.string.error_load_failed,
                                    severity = MessageSeverity.ERROR,
                                ),
                            )
                        }
                    } else {
                        _uiState.update { it.prefilledWith(subscription) }
                        revalidate()
                    }
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = UiMessage(
                                R.string.error_load_failed,
                                severity = MessageSeverity.ERROR,
                            ),
                        )
                    }
                }
        }
    }

    private fun SubscriptionFormUiState.prefilledWith(
        subscription: Subscription,
    ): SubscriptionFormUiState {
        val provider = getProvider(subscription.providerId)
        return copy(
            isLoading = false,
            selectedProvider = provider.toOption(),
            name = subscription.name.value,
            category = subscription.categoryOverride ?: provider.category,
            price = PriceInput.format(subscription.price),
            billingPeriod = subscription.billingPeriod,
            nextBillingDate = subscription.nextBillingDate,
            managementUrl = subscription.managementUrl?.value ?: "",
            remindersEnabled = subscription.remindersEnabled,
        )
    }

    private fun currentInput(): SubscriptionInput = with(_uiState.value) {
        SubscriptionInput(
            providerId = selectedProvider?.id?.let(::ProviderId),
            name = name,
            // Ignored by the validator for catalog entries, which follow the catalog instead.
            category = category,
            price = price,
            billingPeriod = billingPeriod,
            managementUrl = managementUrl,
            nextBillingDate = nextBillingDate,
            remindersEnabled = remindersEnabled,
        )
    }

    private fun revalidate() {
        val state = _uiState.value
        val validation = validateInput(currentInput(), appCurrency.value)
        val errors = when (validation) {
            is SubscriptionInputValidation.Valid -> emptyMap()
            is SubscriptionInputValidation.Invalid -> validation.errors
                .filter { error -> saveAttempted || state.isEagerlyReported(error.field) }
                .associate { error -> error.field to Labels.inputError(error) }
        }

        _uiState.update {
            it.copy(
                fieldErrors = errors,
                isSaveEnabled = validation is SubscriptionInputValidation.Valid,
            )
        }
    }

    /** Price and URL problems are shown while typing; missing values only after a save attempt. */
    private fun SubscriptionFormUiState.isEagerlyReported(field: SubscriptionField): Boolean =
        when (field) {
            SubscriptionField.PRICE -> price.isNotBlank()
            SubscriptionField.MANAGEMENT_URL -> managementUrl.isNotBlank()
            SubscriptionField.PROVIDER, SubscriptionField.NAME -> false
        }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun Provider.toOption() = ProviderOption(
    id = id.value,
    displayName = displayName,
    logoKey = logoKey,
    isOther = isOther,
)
