package com.griff.keeper.infrastructure.backup

import com.griff.keeper.domain.backup.BackupFormat
import com.griff.keeper.domain.backup.BackupPayload
import com.griff.keeper.domain.backup.PortableSettings
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.domain.testing.testSubscription
import com.griff.keeper.infrastructure.backup.crypto.BackupCipher
import com.griff.keeper.infrastructure.backup.crypto.BackupEnvelope
import java.time.Instant

/**
 * Builders shared by the backup format tests.
 *
 * [hostileBackup] is the important one: several requirements can only be tested with a file the app
 * itself would refuse to produce - a negative amount, an unknown enum, a schema from the future - so
 * the tests write the plaintext by hand and encrypt it through the same primitives the real codec
 * uses. Going through [BackupFileCodec.encode] instead would be testing the exporter, not the
 * importer, because the domain constructors would reject the payload before it was ever written.
 */
internal object BackupTestPayloads {

    const val PASSWORD: String = "MyBackupPassword"

    fun payload(
        subscriptions: Int = 2,
        obligations: Int = 1,
        globalRemindersEnabled: Boolean = true,
    ): BackupPayload = BackupPayload(
        schemaVersion = BackupFormat.SCHEMA_VERSION,
        exportedAt = Instant.parse("2026-08-21T00:43:00Z"),
        appVersion = "1.3.0",
        subscriptions = List(subscriptions) { index ->
            testSubscription(id = "sub-$index", name = "Service $index")
        },
        obligations = List(obligations) { index ->
            testObligation(id = "obl-$index", name = "Policy $index")
        },
        settings = PortableSettings.Default.copy(
            globalRemindersEnabled = globalRemindersEnabled,
        ),
    )

    /** Encrypts arbitrary plaintext as a structurally valid backup file. */
    fun encryptedFile(
        plaintext: ByteArray,
        password: String = PASSWORD,
        compress: Boolean = true,
    ): ByteArray {
        val body = if (compress) BackupCompression.compress(plaintext) else plaintext
        val compressionId = if (compress) {
            BackupEnvelope.COMPRESSION_GZIP
        } else {
            BackupEnvelope.COMPRESSION_NONE
        }
        return BackupCipher.seal(body, password.toCharArray(), compressionId).toByteArray()
    }

    /** A well-formed, correctly encrypted file whose *contents* are not acceptable records. */
    fun hostileBackup(json: String, password: String = PASSWORD): ByteArray =
        encryptedFile(json.toByteArray(Charsets.UTF_8), password)

    /**
     * JSON in the current wire shape, with individual fields overridable.
     *
     * Written as a template rather than built from the DTOs so a test can inject a value the DTOs
     * could not hold - which is exactly the case being defended against.
     */
    fun json(
        schemaVersion: Int = BackupFormat.SCHEMA_VERSION,
        subscriptionFields: String = DEFAULT_SUBSCRIPTION,
        obligationFields: String? = null,
    ): String {
        val obligations = obligationFields?.let { "[$it]" } ?: "[]"
        return """
        {
          "schemaVersion": $schemaVersion,
          "exportedAtEpochMillis": 1787272980000,
          "appVersion": "1.3.0",
          "subscriptions": [$subscriptionFields],
          "obligations": $obligations,
          "settings": {
            "globalRemindersEnabled": true,
            "reminderDefaults": {
              "insuranceDaysBefore": [30, 7, 1],
              "paymentDaysBefore": [7, 1],
              "subscriptionDaysBefore": [7, 1]
            }
          }
        }
        """.trimIndent()
    }

    const val DEFAULT_SUBSCRIPTION: String = """
        {
          "id": "sub-1",
          "providerId": "spotify",
          "name": "Spotify",
          "priceMinorUnits": 3499,
          "currency": "PLN",
          "billingPeriod": "MONTHLY",
          "remindersEnabled": true,
          "createdAtEpochMillis": 1767225600000,
          "updatedAtEpochMillis": 1767225600000
        }
    """

    fun subscriptionWith(
        priceMinorUnits: String = "3499",
        currency: String = "PLN",
        billingPeriod: String = "MONTHLY",
        name: String = "Spotify",
        managementUrl: String? = null,
    ): String {
        val url = managementUrl?.let { """"managementUrl": "$it",""" } ?: ""
        return """
        {
          "id": "sub-1",
          "providerId": "spotify",
          "name": "$name",
          $url
          "priceMinorUnits": $priceMinorUnits,
          "currency": "$currency",
          "billingPeriod": "$billingPeriod",
          "remindersEnabled": true,
          "createdAtEpochMillis": 1767225600000,
          "updatedAtEpochMillis": 1767225600000
        }
        """.trimIndent()
    }
}
