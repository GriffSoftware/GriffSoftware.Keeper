package com.griff.subscriptions.domain.id

import com.griff.subscriptions.domain.model.SubscriptionId

/** Creates identities for new subscriptions. */
interface SubscriptionIdGenerator {
    fun next(): SubscriptionId
}
