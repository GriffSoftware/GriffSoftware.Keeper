package com.griff.keeper.infrastructure.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.repository.AppCurrencyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appCurrencyPreferences: DataStore<Preferences> by preferencesDataStore(
    name = "app_currency",
)

/**
 * [AppCurrencyRepository] backed by Preferences DataStore.
 *
 * A single value is a preference, not an entity, for the same reason [ReminderSettingsDataStore] is:
 * putting it in Room would give it a table and a migration for one string. It is deliberately its own
 * file rather than a key inside `reminder_settings` - the two preferences change on entirely different
 * occasions and have no reason to share a write.
 */
@Singleton
class AppCurrencyDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AppCurrencyRepository {

    override fun observe(): Flow<Currency> = context.appCurrencyPreferences.data
        // A corrupt or unreadable preferences file must not take the app down with it; falling back
        // to the default currency is both safe and what a fresh install would already show.
        .catch { throwable -> if (throwable is IOException) emit(emptyPreferences()) else throw throwable }
        .map { preferences -> preferences.toCurrency() }

    override suspend fun current(): Currency = observe().first()

    override suspend fun set(currency: Currency) {
        context.appCurrencyPreferences.edit { preferences ->
            preferences[CurrencyKey] = currency.code
        }
    }

    /** Missing preference means "never chosen", which is exactly what a fresh install is. */
    private fun Preferences.toCurrency(): Currency =
        Currency.fromCodeOrNull(this[CurrencyKey]) ?: Currency.Default

    private companion object {
        val CurrencyKey = stringPreferencesKey("selected_currency")
    }
}
