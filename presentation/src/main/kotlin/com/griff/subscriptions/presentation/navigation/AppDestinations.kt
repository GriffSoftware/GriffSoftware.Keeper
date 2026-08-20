package com.griff.subscriptions.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation graph.
 *
 * Only identifiers travel between destinations; every screen loads its own data through use cases.
 */
@Serializable
data object HomeRoute

@Serializable
data object StatisticsRoute

@Serializable
data class SubscriptionDetailsRoute(val subscriptionId: String)

@Serializable
data object AddSubscriptionRoute

@Serializable
data class EditSubscriptionRoute(val subscriptionId: String)

/** Argument key shared by the add and edit destinations, see `SubscriptionFormViewModel`. */
internal const val SUBSCRIPTION_ID_ARG = "subscriptionId"
