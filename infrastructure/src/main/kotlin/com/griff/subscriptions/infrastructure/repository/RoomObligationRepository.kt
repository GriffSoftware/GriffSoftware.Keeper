package com.griff.subscriptions.infrastructure.repository

import com.griff.subscriptions.domain.model.Obligation
import com.griff.subscriptions.domain.model.ObligationId
import com.griff.subscriptions.domain.repository.ObligationRepository
import com.griff.subscriptions.infrastructure.database.dao.ObligationDao
import com.griff.subscriptions.infrastructure.database.mapper.ObligationMapper
import com.griff.subscriptions.infrastructure.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Room backed implementation of [ObligationRepository]. */
@Singleton
class RoomObligationRepository @Inject constructor(
    private val dao: ObligationDao,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ObligationRepository {

    override fun observeAll(): Flow<List<Obligation>> =
        dao.observeAll()
            .map { entities -> entities.map(ObligationMapper::toDomain) }
            .flowOn(dispatcher)

    override fun observeById(id: ObligationId): Flow<Obligation?> =
        dao.observeById(id.value)
            .map { entity -> entity?.let(ObligationMapper::toDomain) }
            .flowOn(dispatcher)

    override suspend fun findById(id: ObligationId): Obligation? = withContext(dispatcher) {
        dao.findById(id.value)?.let(ObligationMapper::toDomain)
    }

    override suspend fun add(obligation: Obligation) = withContext(dispatcher) {
        dao.insert(ObligationMapper.toEntity(obligation))
    }

    override suspend fun update(obligation: Obligation) = withContext(dispatcher) {
        dao.update(ObligationMapper.toEntity(obligation))
    }

    override suspend fun delete(id: ObligationId) = withContext(dispatcher) {
        dao.deleteById(id.value)
    }
}
