package com.griff.keeper.application.subscription

import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.model.ProviderCategoryResolver
import com.griff.keeper.domain.model.Subscription
import com.griff.keeper.domain.model.categoryWith
import com.griff.keeper.domain.repository.ProviderCatalog
import javax.inject.Inject

/**
 * The category a subscription is tagged and grouped under.
 *
 * The single place that knows the rule: a custom entry keeps the category the user picked, a catalog
 * entry always follows the catalog. Everything that shows a tag or groups by category goes through
 * here, so there is never a second answer to the same question.
 */
class GetSubscriptionCategoryUseCase @Inject constructor(
    private val catalog: ProviderCatalog,
) {
    /** Reusable resolver for the domain calculators, which only know a provider id. */
    val resolver = ProviderCategoryResolver { providerId ->
        catalog.findById(providerId)?.category ?: ProviderCategory.OTHER
    }

    operator fun invoke(subscription: Subscription): ProviderCategory =
        subscription.categoryWith(resolver)
}
