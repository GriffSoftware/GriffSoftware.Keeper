package com.griff.keeper.infrastructure.id

import com.griff.keeper.domain.id.SubscriptionIdGenerator
import com.griff.keeper.domain.model.SubscriptionId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Generates random, storage independent identifiers. */
@Singleton
class UuidSubscriptionIdGenerator @Inject constructor() : SubscriptionIdGenerator {
    override fun next(): SubscriptionId = SubscriptionId(UUID.randomUUID().toString())
}
