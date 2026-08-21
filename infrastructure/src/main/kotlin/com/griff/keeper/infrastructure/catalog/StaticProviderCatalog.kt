package com.griff.keeper.infrastructure.catalog

import com.griff.keeper.domain.model.Provider
import com.griff.keeper.domain.model.ProviderId
import com.griff.keeper.domain.repository.ProviderCatalog
import javax.inject.Inject
import javax.inject.Singleton

/** [ProviderCatalog] served from the bundled, offline [ProviderCatalogSource]. */
@Singleton
class StaticProviderCatalog @Inject constructor() : ProviderCatalog {

    private val providers: List<Provider> = ProviderCatalogSource.providers

    private val byId: Map<ProviderId, Provider> = providers.associateBy { it.id }

    override fun all(): List<Provider> = providers

    override fun findById(id: ProviderId): Provider? = byId[id]

    override fun other(): Provider = byId.getValue(ProviderId.OTHER)
}
