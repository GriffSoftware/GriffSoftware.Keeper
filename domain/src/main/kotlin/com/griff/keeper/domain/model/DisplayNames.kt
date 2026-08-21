package com.griff.keeper.domain.model

/**
 * Rules shared by every user supplied display name in the app.
 *
 * Both [SubscriptionName] and [ObligationName] are the same kind of value - a trimmed, non blank,
 * length limited label - so the normalization lives in one place while the two types stay distinct
 * and cannot be mixed up by accident.
 */
internal object DisplayNames {

    const val MAX_LENGTH: Int = 60

    fun normalizeOrNull(raw: String): String? {
        val trimmed = raw.trim()
        return when {
            trimmed.isEmpty() -> null
            trimmed.length > MAX_LENGTH -> null
            else -> trimmed
        }
    }
}
