package com.griff.keeper.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation graph.
 *
 * Only identifiers travel between destinations; every screen loads its own data through use cases.
 */
@Serializable
data object SubscriptionRoute

@Serializable
data object ObligationsRoute

@Serializable
data object StatisticsRoute

@Serializable
data object RemindersRoute

@Serializable
data class SubscriptionDetailsRoute(val subscriptionId: String)

@Serializable
data object AddSubscriptionRoute

@Serializable
data class EditSubscriptionRoute(val subscriptionId: String)

@Serializable
data class ObligationDetailsRoute(val obligationId: String)

@Serializable
data object AddObligationRoute

@Serializable
data class EditObligationRoute(val obligationId: String)

/** Argument key shared by the add and edit destinations, see `SubscriptionFormViewModel`. */
internal const val SUBSCRIPTION_ID_ARG = "subscriptionId"

/** Argument key shared by the add and edit destinations, see `ObligationFormViewModel`. */
internal const val OBLIGATION_ID_ARG = "obligationId"
