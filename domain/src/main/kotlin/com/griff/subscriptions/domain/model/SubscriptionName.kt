package com.griff.subscriptions.domain.model

/** Display name of a subscription, always trimmed and never blank. */
@JvmInline
value class SubscriptionName private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        const val MAX_LENGTH: Int = 60

        fun ofOrNull(raw: String): SubscriptionName? {
            val trimmed = raw.trim()
            return when {
                trimmed.isEmpty() -> null
                trimmed.length > MAX_LENGTH -> null
                else -> SubscriptionName(trimmed)
            }
        }

        fun of(raw: String): SubscriptionName =
            requireNotNull(ofOrNull(raw)) { "Invalid subscription name: '$raw'" }
    }
}
