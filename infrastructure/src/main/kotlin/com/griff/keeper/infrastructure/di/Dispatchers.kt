package com.griff.keeper.infrastructure.di

import javax.inject.Qualifier

/** Marks the dispatcher used for disk backed work. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
