package com.griff.subscriptions.application.provider

import com.griff.subscriptions.domain.model.Provider
import com.griff.subscriptions.domain.repository.ProviderCatalog
import com.griff.subscriptions.domain.search.filterByQuery
import javax.inject.Inject

/**
 * Filters the catalog by name. The "Other" entry is always kept so the user can always fall back to
 * a custom service.
 */
class SearchProvidersUseCase @Inject constructor(
    private val catalog: ProviderCatalog,
) {
    operator fun invoke(query: String): List<Provider> {
        val matches = catalog.all().filterByQuery(query)
        val other = catalog.other()
        return if (matches.any { it.isOther }) matches else matches + other
    }
}
