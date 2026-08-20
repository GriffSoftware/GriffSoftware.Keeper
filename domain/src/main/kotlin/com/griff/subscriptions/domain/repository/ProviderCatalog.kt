package com.griff.subscriptions.domain.repository

import com.griff.subscriptions.domain.model.Provider
import com.griff.subscriptions.domain.model.ProviderId

/**
 * Read-only catalog of known services.
 *
 * The catalog is a static, offline data set; it is a port so it can later be backed by a remote
 * configuration without touching the layers above.
 */
interface ProviderCatalog {

    /** All providers in display order, with the "Other" entry last. */
    fun all(): List<Provider>

    fun findById(id: ProviderId): Provider?

    /** The catch-all entry, guaranteed to exist. */
    fun other(): Provider
}
