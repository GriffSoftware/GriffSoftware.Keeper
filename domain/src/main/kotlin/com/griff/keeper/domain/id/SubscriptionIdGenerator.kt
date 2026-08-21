package com.griff.keeper.domain.id

import com.griff.keeper.domain.model.SubscriptionId

/** Creates identities for new subscriptions. */
interface SubscriptionIdGenerator {
    fun next(): SubscriptionId
}
