package com.griff.subscriptions.infrastructure.repository

import com.griff.subscriptions.domain.reminder.ReminderEventStore
import com.griff.subscriptions.infrastructure.database.dao.ReminderEventDao
import com.griff.subscriptions.infrastructure.database.entity.ReminderEventEntity
import com.griff.subscriptions.infrastructure.di.IoDispatcher
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Room backed [ReminderEventStore]. */
@Singleton
class RoomReminderEventStore @Inject constructor(
    private val dao: ReminderEventDao,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ReminderEventStore {

    override fun observeDeliveredKeys(): Flow<Set<String>> =
        dao.observeKeys().map { it.toSet() }.flowOn(dispatcher)

    override suspend fun deliveredKeys(): Set<String> =
        withContext(dispatcher) { dao.keys().toSet() }

    override suspend fun markDelivered(key: String, sentAt: Instant) = withContext(dispatcher) {
        dao.insert(ReminderEventEntity(reminderKey = key, sentAtEpochMillis = sentAt.toEpochMilli()))
    }

    override suspend fun deleteSentBefore(threshold: Instant) = withContext(dispatcher) {
        dao.deleteSentBefore(threshold.toEpochMilli())
    }
}
