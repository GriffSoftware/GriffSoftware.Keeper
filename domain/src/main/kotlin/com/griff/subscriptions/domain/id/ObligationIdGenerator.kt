package com.griff.subscriptions.domain.id

import com.griff.subscriptions.domain.model.ObligationId

/** Creates identities for new obligations. */
interface ObligationIdGenerator {
    fun next(): ObligationId
}
