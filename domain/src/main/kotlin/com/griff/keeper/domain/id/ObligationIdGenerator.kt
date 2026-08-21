package com.griff.keeper.domain.id

import com.griff.keeper.domain.model.ObligationId

/** Creates identities for new obligations. */
interface ObligationIdGenerator {
    fun next(): ObligationId
}
