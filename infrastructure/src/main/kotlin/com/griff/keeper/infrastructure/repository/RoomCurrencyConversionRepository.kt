package com.griff.keeper.infrastructure.repository

import androidx.room.withTransaction
import com.griff.keeper.domain.calculation.MoneyConverter
import com.griff.keeper.domain.currency.CurrencyConversionErrorType
import com.griff.keeper.domain.currency.CurrencyConversionException
import com.griff.keeper.domain.currency.CurrencyConversionRepository
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ExchangeRate
import com.griff.keeper.domain.model.Money
import com.griff.keeper.infrastructure.database.GriffDatabase
import com.griff.keeper.infrastructure.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Rewrites every subscription and obligation from [Currency] `from` to `to` inside a single Room
 * transaction.
 *
 * All or nothing, exactly like [RoomBackupImportRepository]: a conversion that touched some records
 * and not others would break the invariant that every stored [Money] carries the app's active
 * currency, in a way the user cannot see and has no way to undo. Reading and writing both happen
 * inside the same transaction so nothing else can slip a new record between the read and the write.
 */
@Singleton
class RoomCurrencyConversionRepository @Inject constructor(
    private val database: GriffDatabase,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : CurrencyConversionRepository {

    override suspend fun convertAll(from: Currency, to: Currency, rate: ExchangeRate) {
        withContext(dispatcher) {
            try {
                database.withTransaction {
                    val subscriptionDao = database.subscriptionDao()
                    val obligationDao = database.obligationDao()

                    val convertedSubscriptions = subscriptionDao.getAll().map { entity ->
                        val converted = MoneyConverter.convert(
                            amount = Money.ofMinorUnits(entity.priceMinorUnits),
                            from = from,
                            to = to,
                            rate = rate,
                        )
                        entity.copy(priceMinorUnits = converted.minorUnits, currencyCode = to.code)
                    }
                    val convertedObligations = obligationDao.getAll().map { entity ->
                        val converted = MoneyConverter.convert(
                            amount = Money.ofMinorUnits(entity.amountMinorUnits),
                            from = from,
                            to = to,
                            rate = rate,
                        )
                        entity.copy(amountMinorUnits = converted.minorUnits, currencyCode = to.code)
                    }

                    if (convertedSubscriptions.isNotEmpty()) subscriptionDao.upsertAll(convertedSubscriptions)
                    if (convertedObligations.isNotEmpty()) obligationDao.upsertAll(convertedObligations)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: CurrencyConversionException) {
                throw error
            } catch (error: Throwable) {
                // The transaction has already rolled back by the time this runs: every record is
                // still in its original currency.
                throw CurrencyConversionException(CurrencyConversionErrorType.STORAGE_ERROR, error)
            }
        }
    }
}
