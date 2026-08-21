package com.griff.keeper.domain.model

import java.net.URI

/**
 * Web address where the user manages the subscription.
 *
 * Only `http` and `https` are accepted so that the presentation layer can safely hand the value
 * over to an external browser.
 */
@JvmInline
value class ManagementUrl private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        private val ALLOWED_SCHEMES = setOf("http", "https")

        fun ofOrNull(raw: String): ManagementUrl? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            val normalized = if (trimmed.contains("://")) trimmed else "https://$trimmed"
            val uri = runCatching { URI(normalized) }.getOrNull() ?: return null
            val scheme = uri.scheme?.lowercase()
            if (scheme !in ALLOWED_SCHEMES) return null
            if (uri.host.isNullOrBlank()) return null
            if (!uri.host.contains('.')) return null
            return ManagementUrl(normalized)
        }
    }
}
