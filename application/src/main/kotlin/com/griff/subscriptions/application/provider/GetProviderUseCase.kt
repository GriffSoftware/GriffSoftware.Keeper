package com.griff.subscriptions.application.provider

import com.griff.subscriptions.domain.model.Provider
import com.griff.subscriptions.domain.model.ProviderId
import com.griff.subscriptions.domain.repository.ProviderCatalog
import javax.inject.Inject

/** Resolves a single catalog entry, falling back to the "Other" entry for unknown ids. */
class GetProviderUseCase @Inject constructor(
    private val catalog: ProviderCatalog,
) {
    operator fun invoke(id: ProviderId): Provider = catalog.findById(id) ?: catalog.other()
}
