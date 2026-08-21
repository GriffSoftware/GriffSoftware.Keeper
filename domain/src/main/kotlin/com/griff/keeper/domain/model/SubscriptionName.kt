package com.griff.keeper.domain.model

/** Display name of a subscription, always trimmed and never blank. */
@JvmInline
value class SubscriptionName private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        const val MAX_LENGTH: Int = DisplayNames.MAX_LENGTH

        fun ofOrNull(raw: String): SubscriptionName? =
            DisplayNames.normalizeOrNull(raw)?.let(::SubscriptionName)

        fun of(raw: String): SubscriptionName =
            requireNotNull(ofOrNull(raw)) { "Invalid subscription name: '$raw'" }
    }
}
