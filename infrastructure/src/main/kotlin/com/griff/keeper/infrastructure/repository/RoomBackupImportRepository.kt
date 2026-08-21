package com.griff.keeper.infrastructure.repository

import androidx.room.withTransaction
import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFailureException
import com.griff.keeper.domain.backup.BackupImportRepository
import com.griff.keeper.domain.backup.ImportMode
import com.griff.keeper.domain.backup.ImportPlan
import com.griff.keeper.infrastructure.backup.storageErrorType
import com.griff.keeper.infrastructure.database.GriffDatabase
import com.griff.keeper.infrastructure.database.mapper.ObligationMapper
import com.griff.keeper.infrastructure.database.mapper.SubscriptionMapper
import com.griff.keeper.infrastructure.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Applies an [ImportPlan] inside a single Room transaction.
 *
 * All or nothing is the entire contract. An import that half succeeded - some records replaced, some
 * still the old ones, some deleted - is a state the user cannot reason about and cannot undo, and it
 * would be *worse* than the import simply failing. Every write of a plan therefore happens in one
 * transaction, and anything thrown inside it rolls the whole thing back.
 *
 * What the transaction deliberately does not touch:
 *
 * - `backup_operations`, the local import/export log. A REPLACE wipes the user's portable data, not
 *   the device's record of what was done to it, so the very import that emptied the tables is still
 *   visible in the history straight afterwards.
 * - `reminder_events`, the delivery ledger. It describes what *this* device has already shown the
 *   user; clearing or importing it would either repeat notifications or silence ones that were never
 *   sent here.
 */
@Singleton
class RoomBackupImportRepository @Inject constructor(
    private val database: GriffDatabase,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BackupImportRepository {

    override suspend fun apply(plan: ImportPlan) = withContext(dispatcher) {
        try {
            database.withTransaction {
                val subscriptions = database.subscriptionDao()
                val obligations = database.obligationDao()

                if (plan.mode == ImportMode.REPLACE) {
                    subscriptions.deleteAll()
                    obligations.deleteAll()
                }

                // Inserts and updates go through the same upsert: the plan has already decided which
                // rows to write, and re-deciding it here in SQL would be a second implementation of
                // the merge rules waiting to disagree with the first.
                val subscriptionRows = (plan.subscriptions.toInsert + plan.subscriptions.toUpdate)
                    .map(SubscriptionMapper::toEntity)
                val obligationRows = (plan.obligations.toInsert + plan.obligations.toUpdate)
                    .map(ObligationMapper::toEntity)

                if (subscriptionRows.isNotEmpty()) subscriptions.upsertAll(subscriptionRows)
                if (obligationRows.isNotEmpty()) obligations.upsertAll(obligationRows)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: BackupFailureException) {
            throw error
        } catch (error: Throwable) {
            // The transaction has already rolled back by the time this runs; what is left is to say
            // why, in a category the UI can explain.
            throw BackupFailureException(error.storageErrorType(), error)
        }
    }
}
