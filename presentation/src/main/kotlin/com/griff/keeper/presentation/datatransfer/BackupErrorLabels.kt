package com.griff.keeper.presentation.datatransfer

import androidx.annotation.StringRes
import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.presentation.R

/**
 * Turns a failure category into something a person can act on.
 *
 * This is the only place a [BackupErrorType] becomes words, and the mapping is deliberately
 * conservative: the user is told what happened and what to try, never how it failed. No exception
 * name, no crypto vocabulary and no hint about which check refused the file - the categories are
 * already coarse enough not to help anyone probe the format, and keeping it that way is a choice
 * rather than an accident.
 */
internal object BackupErrorLabels {

    @StringRes
    fun messageRes(errorType: BackupErrorType): Int = when (errorType) {
        // "Not one of ours" and "too big to be one of ours" are the same answer to the user: the
        // file they picked is not a Griff backup.
        BackupErrorType.INVALID_FILE,
        BackupErrorType.FILE_TOO_LARGE,
        -> R.string.data_transfer_error_invalid_file

        BackupErrorType.WRONG_PASSWORD_OR_CORRUPTED ->
            R.string.data_transfer_import_wrong_password

        BackupErrorType.UNSUPPORTED_VERSION -> R.string.data_transfer_error_newer_version

        BackupErrorType.VALIDATION_ERROR -> R.string.data_transfer_error_corrupted

        BackupErrorType.INSUFFICIENT_STORAGE -> R.string.data_transfer_error_no_space_import

        BackupErrorType.CURRENCY_MISMATCH -> R.string.data_transfer_error_currency_mismatch

        BackupErrorType.IO_ERROR,
        BackupErrorType.UNKNOWN,
        -> R.string.data_transfer_error_import_failed
    }

    /** The short line shown under a failed entry in the history list. */
    @StringRes
    fun historyRes(errorType: BackupErrorType): Int = when (errorType) {
        BackupErrorType.INVALID_FILE,
        BackupErrorType.FILE_TOO_LARGE,
        -> R.string.data_transfer_history_error_invalid_file

        BackupErrorType.WRONG_PASSWORD_OR_CORRUPTED ->
            R.string.data_transfer_history_error_unreadable

        BackupErrorType.UNSUPPORTED_VERSION ->
            R.string.data_transfer_history_error_unsupported_version

        BackupErrorType.VALIDATION_ERROR -> R.string.data_transfer_history_error_validation

        BackupErrorType.INSUFFICIENT_STORAGE -> R.string.data_transfer_history_error_no_space

        BackupErrorType.CURRENCY_MISMATCH -> R.string.data_transfer_history_error_currency_mismatch

        BackupErrorType.IO_ERROR -> R.string.data_transfer_history_error_io

        BackupErrorType.UNKNOWN -> R.string.data_transfer_history_error_unknown
    }
}
