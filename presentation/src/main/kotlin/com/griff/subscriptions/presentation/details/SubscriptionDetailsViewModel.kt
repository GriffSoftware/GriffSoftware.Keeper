package com.griff.subscriptions.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griff.subscriptions.application.provider.GetProviderUseCase
import com.griff.subscriptions.application.subscription.DeleteSubscriptionUseCase
import com.griff.subscriptions.application.subscription.GetSubscriptionCategoryUseCase
import com.griff.subscriptions.application.subscription.ObserveSubscriptionUseCase
import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.model.SubscriptionId
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.MessageSeverity
import com.griff.subscriptions.presentation.common.UiMessage
import com.griff.subscriptions.presentation.navigation.SUBSCRIPTION_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One-off signals the details screen reacts to. */
sealed interface SubscriptionDetailsEvent {
    data class Deleted(val name: String) : SubscriptionDetailsEvent
}

@HiltViewModel
class SubscriptionDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeSubscription: ObserveSubscriptionUseCase,
    private val deleteSubscription: DeleteSubscriptionUseCase,
    private val getProvider: GetProviderUseCase,
    private val getCategory: GetSubscriptionCategoryUseCase,
) : ViewModel() {

    private val subscriptionId = SubscriptionId(
        requireNotNull(savedStateHandle.get<String>(SUBSCRIPTION_ID_ARG)) {
            "Missing $SUBSCRIPTION_ID_ARG navigation argument"
        },
    )

    private val _uiState = MutableStateFlow(SubscriptionDetailsUiState())
    val uiState: StateFlow<SubscriptionDetailsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SubscriptionDetailsEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: Flow<SubscriptionDetailsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeSubscription(subscriptionId)
                .catch { throwable ->
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
                .collect { subscription -> onSubscriptionLoaded(subscription) }
        }
    }

    fun onDeleteRequest() = _uiState.update { it.copy(isDeleteDialogVisible = true) }

    fun onDeleteDismiss() = _uiState.update { it.copy(isDeleteDialogVisible = false) }

    fun onDeleteConfirm() {
        val name = _uiState.value.details?.name ?: return
        _uiState.update { it.copy(isDeleting = true, isDeleteDialogVisible = false) }
        viewModelScope.launch {
            runCatching { deleteSubscription(subscriptionId) }
                .onSuccess { _events.tryEmit(SubscriptionDetailsEvent.Deleted(name)) }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            message = UiMessage(
                                R.string.error_delete_failed,
                                severity = MessageSeverity.ERROR,
                            ),
                        )
                    }
                }
        }
    }

    fun onManagementUrlOpenFailed() = _uiState.update {
        it.copy(
            message = UiMessage(R.string.error_open_url_failed, severity = MessageSeverity.ERROR),
        )
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private fun onSubscriptionLoaded(subscription: Subscription?) {
        _uiState.update { current ->
            current.copy(
                isLoading = false,
                details = subscription?.toDetails(),
            )
        }
    }

    private fun Subscription.toDetails(): SubscriptionDetails {
        val provider = getProvider(providerId)
        return SubscriptionDetails(
            id = id.value,
            name = name.value,
            logoKey = if (provider.isOther) name.value else provider.logoKey,
            category = getCategory(this),
            price = price,
            billingPeriod = billingPeriod,
            monthlyEquivalent = monthlyEquivalent,
            yearlyEquivalent = yearlyEquivalent,
            nextBillingDate = nextBillingDate,
            managementUrl = managementUrl?.value,
        )
    }
}
