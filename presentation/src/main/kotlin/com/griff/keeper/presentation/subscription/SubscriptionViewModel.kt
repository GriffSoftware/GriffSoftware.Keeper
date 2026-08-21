package com.griff.keeper.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griff.keeper.application.provider.GetProviderUseCase
import com.griff.keeper.application.subscription.CalculateSubscriptionTotalsUseCase
import com.griff.keeper.application.subscription.GetSubscriptionCategoryUseCase
import com.griff.keeper.application.subscription.SearchSubscriptionsUseCase
import com.griff.keeper.application.subscription.SubscriptionSearchResult
import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.model.Subscription
import com.griff.keeper.domain.search.SubscriptionFilter
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.MessageSeverity
import com.griff.keeper.presentation.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    searchSubscriptions: SearchSubscriptionsUseCase,
    private val calculateTotals: CalculateSubscriptionTotalsUseCase,
    private val getProvider: GetProviderUseCase,
    private val getCategory: GetSubscriptionCategoryUseCase,
) : ViewModel() {

    /** Search text and tag live in one value, so a change to either re-runs the same filter. */
    private val filter = MutableStateFlow(SubscriptionFilter())

    val uiState: StateFlow<SubscriptionUiState> = searchSubscriptions(filter.asStateFlow())
        .map { result -> result.toUiState() }
        .catch { throwable ->
            if (throwable is kotlinx.coroutines.CancellationException) throw throwable
            emit(
                SubscriptionUiState(
                    isLoading = false,
                    message = UiMessage(R.string.error_load_failed, severity = MessageSeverity.ERROR),
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SubscriptionUiState(),
        )

    fun onQueryChange(value: String) = filter.update { it.copy(query = value) }

    fun onCategoryChange(value: ProviderCategory?) = filter.update { it.copy(category = value) }

    private fun SubscriptionSearchResult.toUiState() = SubscriptionUiState(
        isLoading = false,
        query = filter.query,
        selectedCategory = filter.category,
        availableCategories = availableCategories,
        items = matching.map { it.toListItem() },
        totals = calculateTotals(matching),
        totalSubscriptionCount = totalCount,
    )

    private fun Subscription.toListItem(): SubscriptionListItem {
        val provider = getProvider(providerId)
        return SubscriptionListItem(
            id = id.value,
            name = name.value,
            // Custom entries share a single provider id, so their monogram is seeded by the name.
            logoKey = if (provider.isOther) name.value else provider.logoKey,
            category = getCategory(this),
            billingPeriod = billingPeriod,
            price = price,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
