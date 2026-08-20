package com.griff.subscriptions.infrastructure.id

import com.griff.subscriptions.domain.id.ObligationIdGenerator
import com.griff.subscriptions.domain.model.ObligationId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Generates random, storage independent identifiers. */
@Singleton
class UuidObligationIdGenerator @Inject constructor() : ObligationIdGenerator {
    override fun next(): ObligationId = ObligationId(UUID.randomUUID().toString())
}
