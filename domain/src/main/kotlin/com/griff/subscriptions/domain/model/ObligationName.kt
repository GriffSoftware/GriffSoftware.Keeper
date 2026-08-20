package com.griff.subscriptions.domain.model

/** Display name of an obligation, always trimmed and never blank. */
@JvmInline
value class ObligationName private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        const val MAX_LENGTH: Int = DisplayNames.MAX_LENGTH

        fun ofOrNull(raw: String): ObligationName? =
            DisplayNames.normalizeOrNull(raw)?.let(::ObligationName)

        fun of(raw: String): ObligationName =
            requireNotNull(ofOrNull(raw)) { "Invalid obligation name: '$raw'" }
    }
}
