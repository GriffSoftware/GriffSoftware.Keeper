package com.griff.keeper.presentation.common.currency

import androidx.annotation.StringRes
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.presentation.R

/**
 * Display name of [Currency] in the active UI language, e.g. "Polski złoty" or "Polish zloty".
 *
 * A presentation resource rather than a domain property, exactly like
 * [com.griff.keeper.presentation.common.locale.AppLanguage.displayNameRes]: nothing about a
 * subscription changes with the name a currency is called by, and the picker should read the same
 * words the rest of the UI does.
 */
@get:StringRes
val Currency.displayNameRes: Int
    get() = when (this) {
        Currency.PLN -> R.string.currency_pln_name
        Currency.EUR -> R.string.currency_eur_name
    }
