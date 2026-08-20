package com.griff.subscriptions.infrastructure.id

import com.griff.subscriptions.domain.id.SubscriptionIdGenerator
import com.griff.subscriptions.domain.model.SubscriptionId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Generates random, storage independent identifiers. */
@Singleton
class UuidSubscriptionIdGenerator @Inject constructor() : SubscriptionIdGenerator {
    override fun next(): SubscriptionId = SubscriptionId(UUID.randomUUID().toString())
}
