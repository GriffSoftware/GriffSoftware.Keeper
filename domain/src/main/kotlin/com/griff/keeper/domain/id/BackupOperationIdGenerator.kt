package com.griff.keeper.domain.id

/** Creates identities for entries of the local import/export log. */
interface BackupOperationIdGenerator {
    fun next(): String
}
