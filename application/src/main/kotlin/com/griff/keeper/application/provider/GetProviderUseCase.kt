package com.griff.keeper.application.provider

import com.griff.keeper.domain.model.Provider
import com.griff.keeper.domain.model.ProviderId
import com.griff.keeper.domain.repository.ProviderCatalog
import javax.inject.Inject

/** Resolves a single catalog entry, falling back to the "Other" entry for unknown ids. */
class GetProviderUseCase @Inject constructor(
    private val catalog: ProviderCatalog,
) {
    operator fun invoke(id: ProviderId): Provider = catalog.findById(id) ?: catalog.other()
}
