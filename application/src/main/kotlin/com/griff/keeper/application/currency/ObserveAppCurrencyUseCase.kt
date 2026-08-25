package com.griff.keeper.application.currency

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.repository.AppCurrencyRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams the app's single, global active currency. */
class ObserveAppCurrencyUseCase @Inject constructor(
    private val repository: AppCurrencyRepository,
) {
    operator fun invoke(): Flow<Currency> = repository.observe()
}
