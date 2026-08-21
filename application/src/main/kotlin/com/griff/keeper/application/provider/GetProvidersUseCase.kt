package com.griff.keeper.application.provider

import com.griff.keeper.domain.model.Provider
import com.griff.keeper.domain.repository.ProviderCatalog
import javax.inject.Inject

/** Returns the full provider catalog, "Other" last. */
class GetProvidersUseCase @Inject constructor(
    private val catalog: ProviderCatalog,
) {
    operator fun invoke(): List<Provider> = catalog.all()
}
