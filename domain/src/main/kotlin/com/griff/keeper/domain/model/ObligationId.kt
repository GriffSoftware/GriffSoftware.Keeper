package com.griff.keeper.domain.model

/** Opaque, storage-independent identity of an obligation. */
@JvmInline
value class ObligationId(val value: String) {
    init {
        require(value.isNotBlank()) { "ObligationId cannot be blank" }
    }

    override fun toString(): String = value
}
