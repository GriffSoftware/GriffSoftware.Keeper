package com.griff.keeper.domain.currency

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ExchangeRate

/**
 * Rewrites every stored [com.griff.keeper.domain.model.Money] value from [from] to [to] using [rate],
 * as one all-or-nothing step.
 *
 * The contract is the point of the port, exactly as for
 * [com.griff.keeper.domain.backup.BackupImportRepository]: a currency switch that converted some
 * subscriptions but not others - or subscriptions but not obligations - is a state the user cannot
 * reason about and cannot undo, and it would be *worse* than the conversion simply failing. An
 * implementation is required to be transactional.
 */
interface CurrencyConversionRepository {

    suspend fun convertAll(from: Currency, to: Currency, rate: ExchangeRate)
}

/**
 * Why a currency conversion failed, in categories the UI can explain.
 *
 * Mirrors [com.griff.keeper.domain.backup.BackupErrorType]: coarse on purpose, so nothing about the
 * user's data or a stack trace can end up on screen.
 */
enum class CurrencyConversionErrorType {
    /** The Room transaction failed and was rolled back; every record is still in its old currency. */
    STORAGE_ERROR,

    /**
     * The records were converted, but recording the new active currency itself did not succeed after
     * retrying. The data is safe - every record already carries its new [Currency] in
     * `currency_code` - only the app-wide preference needs to be corrected, which a retry on next
     * launch does automatically.
     */
    PREFERENCE_NOT_PERSISTED,

    UNKNOWN,
}

/** A failure of a currency conversion, reduced to a category the app is willing to show. */
class CurrencyConversionException(
    val errorType: CurrencyConversionErrorType,
    cause: Throwable? = null,
) : Exception(errorType.name, cause)
