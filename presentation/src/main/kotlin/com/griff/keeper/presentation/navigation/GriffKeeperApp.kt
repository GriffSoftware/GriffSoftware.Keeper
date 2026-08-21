package com.griff.keeper.presentation.navigation

import android.content.Intent
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
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
import com.griff.keeper.presentation.about.AboutRoute
import com.griff.keeper.presentation.common.TransientMessages
import com.griff.keeper.presentation.common.locale.AppLanguages
import com.griff.keeper.presentation.datatransfer.DataTransferRoute
import com.griff.keeper.presentation.details.SubscriptionDetailsRoute
import com.griff.keeper.presentation.drawer.AppDrawerContent
import com.griff.keeper.presentation.drawer.AppDrawerViewModel
import com.griff.keeper.presentation.drawer.DrawerDestination
import com.griff.keeper.presentation.form.SubscriptionFormRoute
import com.griff.keeper.presentation.subscription.SubscriptionRoute
import com.griff.keeper.presentation.obligations.ObligationsRoute
import com.griff.keeper.presentation.obligations.details.ObligationDetailsRoute
import com.griff.keeper.presentation.obligations.form.ObligationFormRoute
import com.griff.keeper.presentation.reminders.RemindersRoute
import com.griff.keeper.presentation.statistics.StatisticsRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

/**
 * Root composable of the app: navigation drawer, navigation graph and the little bit of
 * cross-screen state (a message shown on a list after a record was deleted).
 */
@Composable
fun GriffKeeperApp(
    navController: NavHostController = rememberNavController(),
    drawerViewModel: AppDrawerViewModel = hiltViewModel(),
    deepLinkIntents: Flow<Intent> = emptyFlow(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Confirmations for records live longer than the screens that cause them: a form closes itself
    // and a details screen disappears when its record is deleted, so the message follows the user to
    // wherever they land instead of being lost with the screen that produced it.
    val messages = remember { TransientMessages() }

    // The intent the activity started with is consumed by NavHost itself; these are the ones that
    // arrive later, when a reminder is tapped while the app is already open.
    LaunchedEffect(navController, deepLinkIntents) {
        deepLinkIntents.collect { intent -> navController.handleDeepLink(intent) }
    }

    // Keyed on the configuration: the language the resources are resolving against is exactly what
    // the drawer has to show, and it changes with the configuration rather than on its own.
    val configuration = LocalConfiguration.current
    val language = remember(configuration) { AppLanguages.current() }

    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentDestination.isDrawerDestination(),
        drawerContent = {
            AppDrawerContent(
                selected = currentDestination.toDrawerDestination(),
                appVersion = drawerViewModel.appVersion,
                language = language,
                onSelect = { destination ->
                    closeDrawer()
                    navController.navigateToDrawerDestination(destination)
                },
                // The navigation state is left exactly as it is: Android recreates the activity, the
                // back stack is restored with it, and the user stays on the destination they were
                // reading - About stays About.
                onLanguageSelected = AppLanguages::apply,
            )
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = SubscriptionRoute,
        ) {
            composable<SubscriptionRoute> {
                SubscriptionRoute(
                    onOpenDrawer = openDrawer,
                    onSubscriptionClick = { id ->
                        navController.navigate(SubscriptionDetailsRoute(id))
                    },
                    onAddSubscription = { navController.navigate(AddSubscriptionRoute) },
                    pendingMessage = messages.pending,
                    onPendingMessageShown = messages::consume,
                )
            }

            composable<ObligationsRoute> {
                ObligationsRoute(
                    onOpenDrawer = openDrawer,
                    onObligationClick = { id ->
                        navController.navigate(ObligationDetailsRoute(id))
                    },
                    onAddObligation = { navController.navigate(AddObligationRoute) },
                    pendingMessage = messages.pending,
                    onPendingMessageShown = messages::consume,
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

            composable<DataTransferRoute> {
                DataTransferRoute(onOpenDrawer = openDrawer)
            }

            composable<AboutRoute> {
                // The version is already in this composition, so the screen is handed the same
                // values the drawer shows rather than reading them again.
                AboutRoute(
                    appVersion = drawerViewModel.appVersion,
                    onOpenDrawer = openDrawer,
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
                    onDeleted = messages::show,
                    pendingMessage = messages.pending,
                    onPendingMessageShown = messages::consume,
                )
            }

            composable<AddSubscriptionRoute> {
                SubscriptionFormRoute(
                    onNavigateUp = { navController.popBackStack() },
                    onSaved = { _, message ->
                        messages.show(message)
                        navController.popBackStack()
                    },
                )
            }

            composable<EditSubscriptionRoute> {
                SubscriptionFormRoute(
                    onNavigateUp = { navController.popBackStack() },
                    onSaved = { _, message ->
                        messages.show(message)
                        navController.popBackStack()
                    },
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
                    onDeleted = messages::show,
                    pendingMessage = messages.pending,
                    onPendingMessageShown = messages::consume,
                )
            }

            composable<AddObligationRoute> {
                ObligationFormRoute(
                    onNavigateUp = { navController.popBackStack() },
                    onSaved = { _, message ->
                        messages.show(message)
                        navController.popBackStack()
                    },
                )
            }

            composable<EditObligationRoute> {
                ObligationFormRoute(
                    onNavigateUp = { navController.popBackStack() },
                    onSaved = { _, message ->
                        messages.show(message)
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

private fun NavHostController.navigateToDrawerDestination(destination: DrawerDestination) {
    val route: Any = when (destination) {
        DrawerDestination.SUBSCRIPTIONS -> SubscriptionRoute
        DrawerDestination.OBLIGATIONS -> ObligationsRoute
        DrawerDestination.STATISTICS -> StatisticsRoute
        DrawerDestination.REMINDERS -> RemindersRoute
        DrawerDestination.DATA_TRANSFER -> DataTransferRoute
        DrawerDestination.ABOUT -> AboutRoute
    }
    navigate(route) {
        popUpTo(SubscriptionRoute) { inclusive = destination == DrawerDestination.SUBSCRIPTIONS }
        launchSingleTop = true
    }
}

private fun NavDestination?.toDrawerDestination(): DrawerDestination = when {
    this?.hasRoute<ObligationsRoute>() == true -> DrawerDestination.OBLIGATIONS
    this?.hasRoute<StatisticsRoute>() == true -> DrawerDestination.STATISTICS
    this?.hasRoute<RemindersRoute>() == true -> DrawerDestination.REMINDERS
    this?.hasRoute<DataTransferRoute>() == true -> DrawerDestination.DATA_TRANSFER
    this?.hasRoute<AboutRoute>() == true -> DrawerDestination.ABOUT
    else -> DrawerDestination.SUBSCRIPTIONS
}

private fun NavDestination?.isDrawerDestination(): Boolean =
    this?.hasRoute<SubscriptionRoute>() == true ||
        this?.hasRoute<ObligationsRoute>() == true ||
        this?.hasRoute<StatisticsRoute>() == true ||
        this?.hasRoute<RemindersRoute>() == true ||
        this?.hasRoute<DataTransferRoute>() == true ||
        this?.hasRoute<AboutRoute>() == true
