package com.griff.keeper.presentation.datatransfer

/**
 * What counts as an acceptable backup password.
 *
 * A minimum length and nothing more. The password is the only thing standing between a stolen file
 * and the user's records, so it cannot be trivially short - but a composition rule ("one digit, one
 * capital") would push people towards a short password with a decoration on the end, which is worse
 * than a long one they can remember. There is also no recovery: the app is offline and has no copy of
 * the password, so a rejected password here is far less costly than a forgotten one later.
 */
internal object BackupPasswordRules {

    const val MIN_LENGTH: Int = 8

    fun validate(password: String, confirmation: String): BackupPasswordProblem? = when {
        password.isEmpty() -> BackupPasswordProblem.EMPTY
        password.length < MIN_LENGTH -> BackupPasswordProblem.TOO_SHORT
        confirmation.isEmpty() -> BackupPasswordProblem.CONFIRMATION_EMPTY
        password != confirmation -> BackupPasswordProblem.MISMATCH
        else -> null
    }
}

internal enum class BackupPasswordProblem {
    EMPTY,
    TOO_SHORT,
    CONFIRMATION_EMPTY,
    MISMATCH,
}

/** Trimmed, or `null` when it is blank or not a plausible address. */
internal fun String.asOptionalEmailRecipient(): String? {
    val trimmed = trim()
    if (trimmed.isEmpty()) return null
    return trimmed.takeIf(::isPlausibleEmail)
}

/**
 * A deliberately loose check.
 *
 * The address is a hint passed to a mail client, which will do its own validation and let the user
 * fix it before sending. Rejecting an unusual but valid address here would be the app being wrong
 * about something it does not decide.
 */
internal fun isPlausibleEmail(value: String): Boolean {
    if (value.length > MAX_EMAIL_LENGTH || value.any(Char::isWhitespace)) return false
    val at = value.indexOf('@')
    if (at <= 0 || at != value.lastIndexOf('@')) return false
    val domain = value.substring(at + 1)
    return domain.length >= MIN_DOMAIN_LENGTH &&
        domain.contains('.') &&
        !domain.startsWith('.') &&
        !domain.endsWith('.')
}

private const val MAX_EMAIL_LENGTH = 254
private const val MIN_DOMAIN_LENGTH = 3
