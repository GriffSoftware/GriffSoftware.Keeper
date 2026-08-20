package com.griff.subscriptions.presentation.common.component

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Guards the brand glyph catalog: it must only ever cover providers the catalog actually knows,
 * and every entry must resolve to a real drawable.
 */
class ProviderLogoAssetsTest {

    @Test
    fun `resolves a glyph for every bundled provider`() {
        val knownGlyphProviders = listOf(
            "netflix", "hbo_max", "spotify", "youtube_music", "apple_music", "tidal",
            "apple_tv_plus", "claude", "google_gemini", "perplexity_pro", "github_copilot",
            "google_workspace", "icloud_plus", "dropbox", "jetbrains", "ovhcloud",
            "allegro_smart", "glovo_prime", "playstation_plus", "ea_play",
        )

        knownGlyphProviders.forEach { key ->
            assertNotNull(ProviderLogoAssets.of(key), "expected a glyph for '$key'")
        }
    }

    @Test
    fun `falls back to null for services without a bundled glyph`() {
        assertNull(ProviderLogoAssets.of("other"))
        assertNull(ProviderLogoAssets.of("seohost"))
        assertNull(ProviderLogoAssets.of("disney_plus"))
        assertNull(ProviderLogoAssets.of("a-custom-name-typed-by-the-user"))
    }
}
