package com.griff.keeper.infrastructure.repository

import com.griff.keeper.domain.model.Subscription
import com.griff.keeper.domain.model.SubscriptionId
import com.griff.keeper.domain.repository.SubscriptionRepository
import com.griff.keeper.infrastructure.database.dao.SubscriptionDao
import com.griff.keeper.infrastructure.database.mapper.SubscriptionMapper
import com.griff.keeper.infrastructure.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Room backed implementation of [SubscriptionRepository]. */
@Singleton
class RoomSubscriptionRepository @Inject constructor(
    private val dao: SubscriptionDao,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : SubscriptionRepository {

    override fun observeAll(): Flow<List<Subscription>> =
        dao.observeAll()
            .map { entities -> entities.map(SubscriptionMapper::toDomain) }
            .flowOn(dispatcher)

    override fun observeById(id: SubscriptionId): Flow<Subscription?> =
        dao.observeById(id.value)
            .map { entity -> entity?.let(SubscriptionMapper::toDomain) }
            .flowOn(dispatcher)

    override suspend fun findById(id: SubscriptionId): Subscription? = withContext(dispatcher) {
        dao.findById(id.value)?.let(SubscriptionMapper::toDomain)
    }

    override suspend fun add(subscription: Subscription) = withContext(dispatcher) {
        dao.insert(SubscriptionMapper.toEntity(subscription))
    }

    override suspend fun update(subscription: Subscription) = withContext(dispatcher) {
        dao.update(SubscriptionMapper.toEntity(subscription))
    }

    override suspend fun delete(id: SubscriptionId) = withContext(dispatcher) {
        dao.deleteById(id.value)
    }
}
