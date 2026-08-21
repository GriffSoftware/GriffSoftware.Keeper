package com.griff.subscriptions.presentation.navigation

import android.content.Intent
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.griff.subscriptions.presentation.common.UiMessage
import com.griff.subscriptions.presentation.details.SubscriptionDetailsRoute
import com.griff.subscriptions.presentation.drawer.AppDrawerContent
import com.griff.subscriptions.presentation.drawer.AppDrawerViewModel
import com.griff.subscriptions.presentation.drawer.DrawerDestination
import com.griff.subscriptions.presentation.form.SubscriptionFormRoute
import com.griff.subscriptions.presentation.home.HomeRoute
import com.griff.subscriptions.presentation.obligations.ObligationsRoute
import com.griff.subscriptions.presentation.obligations.details.ObligationDetailsRoute
import com.griff.subscriptions.presentation.obligations.form.ObligationFormRoute
import com.griff.subscriptions.presentation.reminders.RemindersRoute
import com.griff.subscriptions.presentation.statistics.StatisticsRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

/**
 * Root composable of the app: navigation drawer, navigation graph and the little bit of
 * cross-screen state (a message shown on a list after a record was deleted).
 */
@Composable
fun GriffSubscriptionsApp(
    navController: NavHostController = rememberNavController(),
    drawerViewModel: AppDrawerViewModel = hiltViewModel(),
    deepLinkIntents: Flow<Intent> = emptyFlow(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    var pendingHomeMessage by remember { mutableStateOf<UiMessage?>(null) }
    var pendingObligationsMessage by remember { mutableStateOf<UiMessage?>(null) }

    // The intent the activity started with is consumed by NavHost itself; these are the ones that
    // arrive later, when a reminder is tapped while the app is already open.
    LaunchedEffect(navController, deepLinkIntents) {
        deepLinkIntents.collect { intent -> navController.handleDeepLink(intent) }
    }

    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentDestination.isDrawerDestination(),
        drawerContent = {
            AppDrawerContent(
                selected = currentDestination.toDrawerDestination(),
                appVersion = drawerViewModel.appVersion,
                onSelect = { destination ->
                    closeDrawer()
                    navController.navigateToDrawerDestination(destination)
                },
            )
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
        ) {
            composable<HomeRoute> {
                HomeRoute(
                    onOpenDrawer = openDrawer,
                    onSubscriptionClick = { id ->
                        navController.navigate(SubscriptionDetailsRoute(id))
                    },
                    onAddSubscription = { navController.navigate(AddSubscriptionRoute) },
                    pendingMessage = pendingHomeMessage,
                    onPendingMessageShown = { pendingHomeMessage = null },
                )
            }

            composable<ObligationsRoute> {
                ObligationsRoute(
                    onOpenDrawer = openDrawer,
                    onObligationClick = { id ->
                        navController.navigate(ObligationDetailsRoute(id))
                    },
                    onAddObligation = { navController.navigate(AddObligationRoute) },
                    pendingMessage = pendingObligationsMessage,
                    onPendingMessageShown = { pendingObligationsMessage = null },
                )
            }

            composable<StatisticsRoute> {
                StatisticsRoute(onOpenDrawer = openDrawer)
            }

            composable<RemindersRoute> {
                RemindersRoute(
                    onOpenDrawer = openDrawer,
                    onSubscriptionClick = { id ->
                        navController.navigate(SubscriptionDetailsRoute(id))
                    },
                    onObligationClick = { id ->
                        navController.navigate(ObligationDetailsRoute(id))
                    },
                )
            }

            composable<SubscriptionDetailsRoute>(
                // Tapping a reminder opens the record itself; navigation rebuilds the stack up to
                // the start destination, so Back lands on the subscriptions list rather than
                // dropping the user out of the app.
                deepLinks = listOf(
                    navDeepLink<SubscriptionDetailsRoute>(basePath = "griff://subscription"),
                ),
            ) { entry ->
                val route = entry.toRoute<SubscriptionDetailsRoute>()
                SubscriptionDetailsRoute(
                    onNavigateUp = { navController.popBackStack() },
                    onEdit = { navController.navigate(EditSubscriptionRoute(route.subscriptionId)) },
                    onDeleted = { message -> pendingHomeMessage = message },
                )
            }

            composable<AddSubscriptionRoute> {
                SubscriptionFormRoute(
                    onNavigateUp = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable<EditSubscriptionRoute> {
                SubscriptionFormRoute(
                    onNavigateUp = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable<ObligationDetailsRoute>(
                deepLinks = listOf(
                    navDeepLink<ObligationDetailsRoute>(basePath = "griff://obligation"),
                ),
            ) { entry ->
                val route = entry.toRoute<ObligationDetailsRoute>()
                ObligationDetailsRoute(
                    onNavigateUp = { navController.popBackStack() },
                    onEdit = { navController.navigate(EditObligationRoute(route.obligationId)) },
                    onDeleted = { message -> pendingObligationsMessage = message },
                )
            }

            composable<AddObligationRoute> {
                ObligationFormRoute(
                    onNavigateUp = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable<EditObligationRoute> {
                ObligationFormRoute(
                    onNavigateUp = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.navigateToDrawerDestination(destination: DrawerDestination) {
    val route: Any = when (destination) {
        DrawerDestination.SUBSCRIPTIONS -> HomeRoute
        DrawerDestination.OBLIGATIONS -> ObligationsRoute
        DrawerDestination.STATISTICS -> StatisticsRoute
        DrawerDestination.REMINDERS -> RemindersRoute
    }
    navigate(route) {
        popUpTo(HomeRoute) { inclusive = destination == DrawerDestination.SUBSCRIPTIONS }
        launchSingleTop = true
    }
}

private fun NavDestination?.toDrawerDestination(): DrawerDestination = when {
    this?.hasRoute<ObligationsRoute>() == true -> DrawerDestination.OBLIGATIONS
    this?.hasRoute<StatisticsRoute>() == true -> DrawerDestination.STATISTICS
    this?.hasRoute<RemindersRoute>() == true -> DrawerDestination.REMINDERS
    else -> DrawerDestination.SUBSCRIPTIONS
}

private fun NavDestination?.isDrawerDestination(): Boolean =
    this?.hasRoute<HomeRoute>() == true ||
        this?.hasRoute<ObligationsRoute>() == true ||
        this?.hasRoute<StatisticsRoute>() == true ||
        this?.hasRoute<RemindersRoute>() == true
