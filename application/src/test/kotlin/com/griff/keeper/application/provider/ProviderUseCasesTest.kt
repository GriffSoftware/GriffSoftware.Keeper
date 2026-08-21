package com.griff.keeper.application.provider

import com.griff.keeper.domain.testing.FakeProviderCatalog
import com.griff.keeper.domain.model.ProviderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProviderUseCasesTest {

    private val catalog = FakeProviderCatalog()

    @Test
    fun `catalog keeps the other entry last`() {
        val providers = GetProvidersUseCase(catalog)()

        assertTrue(providers.last().isOther)
    }

    @Test
    fun `search keeps the other entry available`() {
        val results = SearchProvidersUseCase(catalog)("spo")

        assertEquals(listOf("Spotify", "Other"), results.map { it.displayName })
    }

    @Test
    fun `search without matches still offers the other entry`() {
        val results = SearchProvidersUseCase(catalog)("nothing matches this")

        assertEquals(listOf("Other"), results.map { it.displayName })
    }

    @Test
    fun `unknown provider ids fall back to the other entry`() {
        val provider = GetProviderUseCase(catalog)(ProviderId("unknown"))

        assertTrue(provider.isOther)
    }
}
