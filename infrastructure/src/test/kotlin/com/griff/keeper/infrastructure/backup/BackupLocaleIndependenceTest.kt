package com.griff.keeper.infrastructure.backup

import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.infrastructure.backup.serialization.JsonBackupSerializer
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A backup means the same thing in every language.
 *
 * The interface is translated; the file is not. A `.griffbackup` written on a Polish phone has to
 * import cleanly on an English one and the other way round, which it only does if the file carries
 * stable identifiers - `MUSIC`, `VEHICLE_INSURANCE`, an ISO currency code, an epoch millisecond -
 * and never a display string. Getting this wrong would be silent and unrecoverable: nothing would
 * fail at export time, and the file would simply stop being readable once someone switched language.
 *
 * The locale is switched around the serialization itself rather than mocked, because the ways this
 * could break are the locale-sensitive defaults: `String.format` without a locale, `toLowerCase()`
 * on an enum name (Turkish `I`), a decimal separator in a number, a localized month in a date.
 */
class BackupLocaleIndependenceTest {

    private val original: Locale = Locale.getDefault()

    private val polish: Locale = Locale.forLanguageTag("pl-PL")
    private val english: Locale = Locale.forLanguageTag("en-US")

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(original)
    }

    @Test
    fun `a backup exported in polish imports in english`() {
        Locale.setDefault(polish)
        val exported = BackupTestPayloads.payload(subscriptions = 3, obligations = 2)
        val file = BackupFileCodec.encode(exported, PASSWORD)

        Locale.setDefault(english)
        val imported = BackupFileCodec.decode(file, PASSWORD)

        assertEquals(exported, imported)
    }

    @Test
    fun `a backup exported in english imports in polish`() {
        Locale.setDefault(english)
        val exported = BackupTestPayloads.payload(subscriptions = 3, obligations = 2)
        val file = BackupFileCodec.encode(exported, PASSWORD)

        Locale.setDefault(polish)
        val imported = BackupFileCodec.decode(file, PASSWORD)

        assertEquals(exported, imported)
    }

    @Test
    fun `the serialized bytes do not depend on the language`() {
        val payload = BackupTestPayloads.payload(subscriptions = 2, obligations = 2)

        Locale.setDefault(polish)
        val fromPolish = JsonBackupSerializer.serialize(payload)
        Locale.setDefault(english)
        val fromEnglish = JsonBackupSerializer.serialize(payload)

        // Byte for byte: the encryption adds a fresh salt per file, so the comparison has to happen
        // on the plaintext the codec encrypts, not on the file.
        assertContentEqualsWithMessage(fromPolish, fromEnglish)
    }

    @Test
    fun `categories are stored as stable identifiers, never as display names`() {
        val payload = BackupTestPayloads.payload(subscriptions = 0, obligations = 0).copy(
            obligations = listOf(
                testObligation(
                    id = "obl-vehicle",
                    name = "OC Ford",
                    category = ObligationCategory.VEHICLE_INSURANCE,
                ),
            ),
        )

        Locale.setDefault(polish)
        val json = JsonBackupSerializer.serialize(payload).toString(Charsets.UTF_8)

        assertTrue(json.contains("VEHICLE_INSURANCE"), json)
        // The words the UI shows for that category, in either language, must not be in the file.
        listOf("Ubezpieczenie pojazdu", "Vehicle insurance", "Vehicle").forEach { displayName ->
            assertFalse(json.contains(displayName), "Backup leaked the display name '$displayName'")
        }
        // The user's own name for the record is data, not a translation, and stays exactly as typed.
        assertTrue(json.contains("OC Ford"), json)
    }

    private fun assertContentEqualsWithMessage(expected: ByteArray, actual: ByteArray) {
        assertEquals(
            expected.toString(Charsets.UTF_8),
            actual.toString(Charsets.UTF_8),
            "Serialization changed with the default locale",
        )
    }

    private companion object {
        val PASSWORD: CharArray get() = BackupTestPayloads.PASSWORD.toCharArray()
    }
}
