package com.griff.subscriptions.domain.model

/**
 * A service the user can subscribe to.
 *
 * [logoKey] is an abstract asset key resolved by the presentation layer, so the domain never
 * depends on Android resources.
 */
data class Provider(
    val id: ProviderId,
    val displayName: String,
    val category: ProviderCategory,
    val logoKey: String,
    val defaultManagementUrl: ManagementUrl?,
) {
    val isOther: Boolean get() = id.isOther
}
