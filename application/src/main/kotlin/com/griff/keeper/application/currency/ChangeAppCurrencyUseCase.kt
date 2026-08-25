package com.griff.keeper.application.currency

import com.griff.keeper.domain.currency.CurrencyConversionErrorType
import com.griff.keeper.domain.currency.CurrencyConversionException
import com.griff.keeper.domain.currency.CurrencyConversionRepository
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ExchangeRate
import com.griff.keeper.domain.repository.AppCurrencyRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Commits an already previewed and confirmed currency conversion.
 *
 * The order matters and is deliberately not symmetrical (see [AppCurrencyRepository]): the Room
 * transaction runs first, because it is the step that can fail on its own terms (storage, a
 * constraint) and rolls back cleanly with nothing else touched. Only once every record safely carries
 * [to] is the active-currency preference written. That write is a single small file edit that is
 * realistically never going to fail, but it is retried a few times regardless, because the one failure
 * mode left at that point - records converted, preference not yet updated - has a cheap, honest fix
 * (retry) instead of an inconsistent app state.
 */
class ChangeAppCurrencyUseCase @Inject constructor(
    private val conversionRepository: CurrencyConversionRepository,
    private val appCurrency: AppCurrencyRepository,
) {
    suspend operator fun invoke(from: Currency, to: Currency, rate: ExchangeRate): Result<Unit> =
        runCatching {
            conversionRepository.convertAll(from, to, rate)
            persistSelectedCurrency(to)
        }

    private suspend fun persistSelectedCurrency(currency: Currency) {
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) {
            try {
                appCurrency.set(currency)
                return
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastError = error
            }
        }
        throw CurrencyConversionException(CurrencyConversionErrorType.PREFERENCE_NOT_PERSISTED, lastError)
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
