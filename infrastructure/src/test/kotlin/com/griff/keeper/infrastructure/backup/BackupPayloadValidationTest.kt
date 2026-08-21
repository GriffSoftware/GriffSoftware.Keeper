package com.griff.keeper.infrastructure.backup

import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFailureException
import com.griff.keeper.domain.backup.BackupRecordValidator
import com.griff.keeper.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * What happens when a correctly encrypted file contains records the app must not accept.
 *
 * Authentication proves nobody edited the file after it was written. It says nothing about whether the
 * thing that wrote it was healthy - a build with a bug, a hand-rolled exporter or a deliberately
 * crafted payload all produce a file with a valid tag. Every case below therefore goes through real
 * encryption and is stopped by validation rather than by the cipher.
 */
class BackupPayloadValidationTest {

    private val password = BackupTestPayloads.PASSWORD.toCharArray()

    @Test
    fun `a payload from a newer schema is refused`() {
        val image = BackupTestPayloads.hostileBackup(
            BackupTestPayloads.json(schemaVersion = 999),
        )

        val failure = assertFailsWith<BackupFailureException> {
            BackupFileCodec.decode(image, password)
        }

        assertEquals(BackupErrorType.UNSUPPORTED_VERSION, failure.errorType)
    }

    @Test
    fun `a payload from a schema below the supported range is refused`() {
        val image = BackupTestPayloads.hostileBackup(BackupTestPayloads.json(schemaVersion = 0))

        val failure = assertFailsWith<BackupFailureException> {
            BackupFileCodec.decode(image, password)
        }

        assertEquals(BackupErrorType.UNSUPPORTED_VERSION, failure.errorType)
    }

    @Test
    fun `a negative amount is refused`() {
        val image = BackupTestPayloads.hostileBackup(
            BackupTestPayloads.json(
                subscriptionFields = BackupTestPayloads.subscriptionWith(
                    priceMinorUnits = "-100",
                ),
            ),
        )

        assertValidationFailure(image)
    }

    @Test
    fun `an amount above the domain maximum is refused`() {
        val overflowing = BackupRecordValidator.MAX_AMOUNT_MINOR_UNITS + 1
        val image = BackupTestPayloads.hostileBackup(
            BackupTestPayloads.json(
                subscriptionFields = BackupTestPayloads.subscriptionWith(
                    priceMinorUnits = overflowing.toString(),
                ),
            ),
        )

        assertValidationFailure(image)
    }

    @Test
    fun `an amount that would overflow the minor unit type is refused`() {
        val image = BackupTestPayloads.hostileBackup(
            BackupTestPayloads.json(
                subscriptionFields = BackupTestPayloads.subscriptionWith(
                    priceMinorUnits = Long.MAX_VALUE.toString(),
                ),
            ),
        )

        assertValidationFailure(image)
    }

    @Test
    fun `an unknown currency is refused`() {
        val image = BackupTestPayloads.hostileBackup(
            BackupTestPayloads.json(
                subscriptionFields = BackupTestPayloads.subscriptionWith(currency = "XYZ"),
            ),
        )

        assertValidationFailure(image)
    }

    @Test
    fun `an unknown billing period is refused rather than crashing`() {
        val image = BackupTestPayloads.hostileBackup(
            BackupTestPayloads.json(
                subscriptionFields = BackupTestPayloads.subscriptionWith(
                    billingPeriod = "FORTNIGHTLY",
                ),
            ),
        )

        assertValidationFailure(image)
    }

    @Test
    fun `a blank name is refused`() {
        val image = BackupTestPayloads.hostileBackup(
            BackupTestPayloads.json(
                subscriptionFields = BackupTestPayloads.subscriptionWith(name = "   "),
            ),
        )

        assertValidationFailure(image)
    }

    @Test
    fun `an oversized name is refused`() {
        val image = BackupTestPayloads.hostileBackup(
            BackupTestPayloads.json(
                subscriptionFields = BackupTestPayloads.subscriptionWith(
                    name = "a".repeat(5_000),
                ),
            ),
        )

        assertValidationFailure(image)
    }

    @Test
    fun `an unparseable management URL is refused`() {
        val image = BackupTestPayloads.hostileBackup(
            BackupTestPayloads.json(
                subscriptionFields = BackupTestPayloads.subscriptionWith(
                    managementUrl = "javascript:alert(1)",
                ),
            ),
        )

        assertValidationFailure(image)
    }

    @Test
    fun `a duplicated record id inside one file is refused`() {
        val duplicated = BackupTestPayloads.subscriptionWith() + "," +
            BackupTestPayloads.subscriptionWith()
        val image = BackupTestPayloads.hostileBackup(
            BackupTestPayloads.json(subscriptionFields = duplicated),
        )

        assertValidationFailure(image)
    }

    @Test
    fun `a paid obligation without a payment date is refused`() {
        val obligation = """
            {
              "id": "obl-1",
              "name": "OC Ford",
              "category": "VEHICLE_INSURANCE",
              "amountMinorUnits": 124000,
              "currency": "PLN",
              "paymentStatus": "PAID",
              "remindersEnabled": true,
              "createdAtEpochMillis": 1767225600000,
              "updatedAtEpochMillis": 1767225600000
            }
        """.trimIndent()
        val image = BackupTestPayloads.hostileBackup(
            BackupTestPayloads.json(obligationFields = obligation),
        )

        assertValidationFailure(image)
    }

    @Test
    fun `an oversized note is refused`() {
        val obligation = """
            {
              "id": "obl-1",
              "name": "OC Ford",
              "category": "VEHICLE_INSURANCE",
              "amountMinorUnits": 124000,
              "currency": "PLN",
              "paymentStatus": "UNPAID",
              "notes": "${"n".repeat(50_000)}",
              "remindersEnabled": true,
              "createdAtEpochMillis": 1767225600000,
              "updatedAtEpochMillis": 1767225600000
            }
        """.trimIndent()
        val image = BackupTestPayloads.hostileBackup(
            BackupTestPayloads.json(obligationFields = obligation),
        )

        assertValidationFailure(image)
    }

    @Test
    fun `a date far outside the representable range is refused`() {
        val obligation = """
            {
              "id": "obl-1",
              "name": "OC Ford",
              "category": "VEHICLE_INSURANCE",
              "amountMinorUnits": 124000,
              "currency": "PLN",
              "paymentStatus": "UNPAID",
              "dueDateEpochDay": ${Long.MAX_VALUE},
              "remindersEnabled": true,
              "createdAtEpochMillis": 1767225600000,
              "updatedAtEpochMillis": 1767225600000
            }
        """.trimIndent()
        val image = BackupTestPayloads.hostileBackup(
            BackupTestPayloads.json(obligationFields = obligation),
        )

        assertValidationFailure(image)
    }

    @Test
    fun `a historical expiry date is accepted`() {
        val obligation = """
            {
              "id": "obl-1",
              "name": "OC Ford",
              "category": "VEHICLE_INSURANCE",
              "amountMinorUnits": 124000,
              "currency": "PLN",
              "paymentStatus": "PAID",
              "paymentDateEpochDay": 16000,
              "validUntilEpochDay": 16365,
              "remindersEnabled": true,
              "createdAtEpochMillis": 1767225600000,
              "updatedAtEpochMillis": 1767225600000
            }
        """.trimIndent()
        val image = BackupTestPayloads.hostileBackup(
            BackupTestPayloads.json(obligationFields = obligation),
        )

        // A policy that expired years ago is perfectly good history and must not be rejected.
        val payload = BackupFileCodec.decode(image, password)

        assertEquals(1, payload.obligations.size)
        assertEquals(Money.ofUnits(1_240), payload.obligations.single().amount)
    }

    @Test
    fun `unknown fields written by a newer build are ignored`() {
        val subscription = """
            {
              "id": "sub-1",
              "providerId": "spotify",
              "name": "Spotify",
              "priceMinorUnits": 3499,
              "currency": "PLN",
              "billingPeriod": "MONTHLY",
              "remindersEnabled": true,
              "createdAtEpochMillis": 1767225600000,
              "updatedAtEpochMillis": 1767225600000,
              "somethingFromTheFuture": {"nested": [1, 2, 3]}
            }
        """.trimIndent()
        val image = BackupTestPayloads.hostileBackup(
            BackupTestPayloads.json(subscriptionFields = subscription),
        )

        val payload = BackupFileCodec.decode(image, password)

        assertEquals("Spotify", payload.subscriptions.single().name.value)
        assertNull(payload.subscriptions.single().managementUrl)
    }

    @Test
    fun `plaintext that is not JSON at all is refused`() {
        val image = BackupTestPayloads.hostileBackup("not json, just words")

        assertValidationFailure(image)
    }

    private fun assertValidationFailure(image: ByteArray) {
        val failure = assertFailsWith<BackupFailureException> {
            BackupFileCodec.decode(image, password)
        }

        assertEquals(BackupErrorType.VALIDATION_ERROR, failure.errorType)
    }
}
