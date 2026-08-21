package com.griff.keeper.domain.model

/** Resolves the category of a catalog entry. Implemented on top of the provider catalog. */
fun interface ProviderCategoryResolver {
    fun categoryOf(providerId: ProviderId): ProviderCategory
}

/**
 * The category a subscription is shown and grouped under.
 *
 * A custom entry keeps its own choice; a catalog entry always follows the catalog, so renaming or
 * recategorizing a known service does not leave stale copies behind in the database.
 */
fun Subscription.categoryWith(resolver: ProviderCategoryResolver): ProviderCategory =
    categoryOverride ?: resolver.categoryOf(providerId)
