package com.griff.subscriptions.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ManagementUrlTest {

    @Test
    fun `accepts http and https addresses`() {
        assertEquals(
            "https://www.spotify.com/account",
            ManagementUrl.ofOrNull("https://www.spotify.com/account")?.value,
        )
        assertEquals("http://home.pl", ManagementUrl.ofOrNull("http://home.pl")?.value)
    }

    @Test
    fun `adds https to a bare host`() {
        assertEquals("https://netflix.com/account", ManagementUrl.ofOrNull("netflix.com/account")?.value)
    }

    @Test
    fun `trims input`() {
        assertEquals("https://tidal.com", ManagementUrl.ofOrNull("  https://tidal.com  ")?.value)
    }

    @Test
    fun `rejects unsupported schemes and malformed values`() {
        assertNull(ManagementUrl.ofOrNull("javascript:alert(1)"))
        assertNull(ManagementUrl.ofOrNull("ftp://files.example.com"))
        assertNull(ManagementUrl.ofOrNull("not a url"))
        assertNull(ManagementUrl.ofOrNull("localhost"))
        assertNull(ManagementUrl.ofOrNull(""))
    }
}
