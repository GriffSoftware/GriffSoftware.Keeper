package com.griff.subscriptions.domain.model

/** Stable identifier of a catalog entry, e.g. `spotify`. */
@JvmInline
value class ProviderId(val value: String) {
    init {
        require(value.isNotBlank()) { "ProviderId cannot be blank" }
    }

    val isOther: Boolean get() = this == OTHER

    override fun toString(): String = value

    companion object {
        /** Catch-all entry used for services that are not part of the predefined catalog. */
        val OTHER: ProviderId = ProviderId("other")
    }
}
