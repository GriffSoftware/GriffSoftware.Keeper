package com.griff.keeper.domain.backup

/**
 * A failure of a backup operation, reduced to a category the app is willing to persist and to show.
 *
 * Nothing else travels: no message, no cause, no offending value. The technical detail stops at the
 * boundary where it was produced, which is the only way to guarantee that a decrypted record or a
 * password fragment cannot end up in the history table or on the screen.
 */
class BackupFailureException(
    val errorType: BackupErrorType,
    cause: Throwable? = null,
) : Exception(errorType.name, cause)

/** Fails a [Result] with [type]. */
fun <T> backupFailure(type: BackupErrorType, cause: Throwable? = null): Result<T> =
    Result.failure(BackupFailureException(type, cause))

/**
 * The category of a failed [Result], falling back to [BackupErrorType.UNKNOWN].
 *
 * An unexpected exception is a bug, not a diagnosis to show the user, so it is not translated into
 * something more specific than "unknown".
 */
val Throwable.backupErrorType: BackupErrorType
    get() = (this as? BackupFailureException)?.errorType ?: BackupErrorType.UNKNOWN
