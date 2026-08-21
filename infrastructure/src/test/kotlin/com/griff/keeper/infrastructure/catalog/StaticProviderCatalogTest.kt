package com.griff.keeper.infrastructure.catalog

import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.model.ProviderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StaticProviderCatalogTest {

    private val catalog = StaticProviderCatalog()

    @Test
    fun `contains the popular services of the first release`() {
        val names = catalog.all().map { it.displayName }

        assertTrue(names.containsAll(listOf("Netflix", "Spotify", "ChatGPT", "SeoHost.pl", "Xbox Game Pass")))
        assertTrue(catalog.all().size > 50, "catalog is unexpectedly small: ${catalog.all().size}")
    }

    @Test
    fun `other is the last entry and always resolvable`() {
        assertTrue(catalog.all().last().isOther)
        assertEquals(ProviderId.OTHER, catalog.other().id)
        assertEquals(ProviderCategory.OTHER, catalog.other().category)
        assertNull(catalog.other().defaultManagementUrl)
    }

    @Test
    fun `ids and logo keys are unique`() {
        val ids = catalog.all().map { it.id.value }

        assertEquals(ids.size, ids.distinct().size)
        assertEquals(ids.size, catalog.all().map { it.logoKey }.distinct().size)
    }

    @Test
    fun `every catalog entry except other has a valid management url`() {
        catalog.all().filterNot { it.isOther }.forEach { provider ->
            val url = provider.defaultManagementUrl
            assertNotNull(url, "missing url for ${provider.displayName}")
            assertTrue(url.value.startsWith("https://"), "insecure url for ${provider.displayName}")
        }
    }

    @Test
    fun `lookup by id works for known and unknown ids`() {
        assertEquals("Spotify", catalog.findById(ProviderId("spotify"))?.displayName)
        assertNull(catalog.findById(ProviderId("does_not_exist")))
    }

    @Test
    fun `every category is represented`() {
        val categories = catalog.all().map { it.category }.toSet()

        assertEquals(ProviderCategory.entries.toSet(), categories)
    }
}
