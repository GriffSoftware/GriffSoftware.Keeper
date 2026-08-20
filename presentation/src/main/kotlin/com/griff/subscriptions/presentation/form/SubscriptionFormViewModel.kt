package com.griff.subscriptions.presentation.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griff.subscriptions.application.provider.GetProviderUseCase
import com.griff.subscriptions.application.provider.GetProvidersUseCase
import com.griff.subscriptions.application.provider.SearchProvidersUseCase
import com.griff.subscriptions.application.subscription.AddSubscriptionUseCase
import com.griff.subscriptions.application.subscription.GetSubscriptionUseCase
import com.griff.subscriptions.application.subscription.UpdateSubscriptionUseCase
import com.griff.subscriptions.application.subscription.ValidateSubscriptionInputUseCase
import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.Provider
import com.griff.subscriptions.domain.model.ProviderId
import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.model.SubscriptionId
import com.griff.subscriptions.domain.validation.SubscriptionField
import com.griff.subscriptions.domain.validation.SubscriptionInput
import com.griff.subscriptions.domain.validation.SubscriptionInputValidation
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.Labels
import com.griff.subscriptions.presentation.common.UiMessage
import com.griff.subscriptions.presentation.common.format.PriceInput
import com.griff.subscriptions.presentation.navigation.SUBSCRIPTION_ID_ARG
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

/** One-off signals of the add/edit form. */
sealed interface SubscriptionFormEvent {
    data class Saved(val subscriptionId: String) : SubscriptionFormEvent
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
) : ViewModel() {

    private val editedId: SubscriptionId? =
        savedStateHandle.get<String>(SUBSCRIPTION_ID_ARG)?.let(::SubscriptionId)

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

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onSave() {
        saveAttempted = true
        val validation = validateInput(currentInput())
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
                .onSuccess { savedId -> _events.tryEmit(SubscriptionFormEvent.Saved(savedId)) }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update {
                        it.copy(isSaving = false, message = UiMessage(R.string.error_save_failed))
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
                            it.copy(isLoading = false, message = UiMessage(R.string.error_load_failed))
                        }
                    } else {
                        _uiState.update { it.prefilledWith(subscription) }
                        revalidate()
                    }
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update {
                        it.copy(isLoading = false, message = UiMessage(R.string.error_load_failed))
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
            price = PriceInput.format(subscription.price),
            billingPeriod = subscription.billingPeriod,
            nextBillingDate = subscription.nextBillingDate,
            managementUrl = subscription.managementUrl?.value ?: "",
        )
    }

    private fun currentInput(): SubscriptionInput = with(_uiState.value) {
        SubscriptionInput(
            providerId = selectedProvider?.id?.let(::ProviderId),
            name = name,
            price = price,
            billingPeriod = billingPeriod,
            managementUrl = managementUrl,
            nextBillingDate = nextBillingDate,
        )
    }

    private fun revalidate() {
        val state = _uiState.value
        val validation = validateInput(currentInput())
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
}

private fun Provider.toOption() = ProviderOption(
    id = id.value,
    displayName = displayName,
    logoKey = logoKey,
    isOther = isOther,
)
