package com.griff.subscriptions.domain.validation

import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.ProviderId
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubscriptionInputValidatorTest {

    @Test
    fun `accepts a complete form`() {
        val result = SubscriptionInputValidator.validate(
            input(
                name = "  Spotify ",
                price = "34,99",
                managementUrl = "spotify.com/account",
                nextBillingDate = LocalDate.of(2026, 9, 14),
            ),
        )

        val valid = result as SubscriptionInputValidation.Valid
        assertEquals("Spotify", valid.input.name.value)
        assertEquals(3499, valid.input.price.minorUnits)
        assertEquals("https://spotify.com/account", valid.input.managementUrl?.value)
        assertEquals(LocalDate.of(2026, 9, 14), valid.input.nextBillingDate)
    }

    @Test
    fun `treats management url and billing date as optional`() {
        val valid = SubscriptionInputValidator.validate(input(managementUrl = "  "))
            as SubscriptionInputValidation.Valid

        assertNull(valid.input.managementUrl)
        assertNull(valid.input.nextBillingDate)
    }

    @Test
    fun `reports every invalid field at once`() {
        val result = SubscriptionInputValidator.validate(
            SubscriptionInput(
                providerId = null,
                name = "",
                price = "34,555",
                billingPeriod = BillingPeriod.MONTHLY,
                managementUrl = "nope",
                nextBillingDate = null,
            ),
        )

        val errors = (result as SubscriptionInputValidation.Invalid).errors
        assertTrue(SubscriptionInputError.ProviderMissing in errors)
        assertTrue(SubscriptionInputError.NameMissing in errors)
        assertTrue(SubscriptionInputError.Price(PriceError.TOO_MANY_DECIMALS) in errors)
        assertTrue(SubscriptionInputError.ManagementUrlInvalid in errors)
    }

    @Test
    fun `rejects overly long names`() {
        val result = SubscriptionInputValidator.validate(input(name = "x".repeat(61)))

        val errors = (result as SubscriptionInputValidation.Invalid).errors
        assertTrue(SubscriptionInputError.NameTooLong in errors)
    }

    private fun input(
        providerId: ProviderId? = ProviderId("spotify"),
        name: String = "Spotify",
        price: String = "34,99",
        billingPeriod: BillingPeriod = BillingPeriod.MONTHLY,
        managementUrl: String = "",
        nextBillingDate: LocalDate? = null,
    ) = SubscriptionInput(providerId, name, price, billingPeriod, managementUrl, nextBillingDate)
}
