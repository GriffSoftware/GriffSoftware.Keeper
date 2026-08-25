package com.griff.keeper.domain.repository

import com.griff.keeper.domain.model.Currency
import kotlinx.coroutines.flow.Flow

/**
 * Persistence port for the single, global currency the app is currently using.
 *
 * Separate from [ReminderSettingsRepository] and from the record repositories for the same reason
 * they are separate from each other: this is one small preference, not an entity, and it has its own
 * lifecycle - it changes through a deliberate, user confirmed currency conversion, never through an
 * ordinary record edit.
 */
interface AppCurrencyRepository {

    fun observe(): Flow<Currency>

    suspend fun current(): Currency

    /**
     * Records [currency] as the app's active currency.
     *
     * Does not touch a single stored [com.griff.keeper.domain.model.Money] value: callers are
     * responsible for converting existing records *before* calling this, or for confirming there are
     * none to convert. See `ChangeAppCurrencyUseCase` in the application layer for the orchestration.
     */
    suspend fun set(currency: Currency)
}
