package com.griff.subscriptions.domain.model

/** Opaque, storage-independent identity of a subscription. */
@JvmInline
value class SubscriptionId(val value: String) {
    init {
        require(value.isNotBlank()) { "SubscriptionId cannot be blank" }
    }

    override fun toString(): String = value
}
