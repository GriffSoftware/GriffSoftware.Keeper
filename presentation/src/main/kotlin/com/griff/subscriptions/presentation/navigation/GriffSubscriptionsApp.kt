package com.griff.subscriptions.presentation.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import androidx.navigation.toRoute
import com.griff.subscriptions.presentation.common.UiMessage
import com.griff.subscriptions.presentation.details.SubscriptionDetailsRoute
import com.griff.subscriptions.presentation.drawer.AppDrawerContent
import com.griff.subscriptions.presentation.drawer.AppDrawerViewModel
import com.griff.subscriptions.presentation.drawer.DrawerDestination
import com.griff.subscriptions.presentation.form.SubscriptionFormRoute
import com.griff.subscriptions.presentation.home.HomeRoute
import com.griff.subscriptions.presentation.statistics.StatisticsRoute
import kotlinx.coroutines.launch

/**
 * Root composable of the app: navigation drawer, navigation graph and the little bit of
 * cross-screen state (a message shown on home after a subscription was deleted).
 */
@Composable
fun GriffSubscriptionsApp(
    navController: NavHostController = rememberNavController(),
    drawerViewModel: AppDrawerViewModel = hiltViewModel(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    var pendingHomeMessage by remember { mutableStateOf<UiMessage?>(null) }

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

            composable<StatisticsRoute> {
                StatisticsRoute(onOpenDrawer = openDrawer)
            }

            composable<SubscriptionDetailsRoute> { entry ->
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
        }
    }
}

private fun NavHostController.navigateToDrawerDestination(destination: DrawerDestination) {
    val route: Any = when (destination) {
        DrawerDestination.HOME -> HomeRoute
        DrawerDestination.STATISTICS -> StatisticsRoute
    }
    navigate(route) {
        popUpTo(HomeRoute) { inclusive = destination == DrawerDestination.HOME }
        launchSingleTop = true
    }
}

private fun NavDestination?.toDrawerDestination(): DrawerDestination = when {
    this?.hasRoute<StatisticsRoute>() == true -> DrawerDestination.STATISTICS
    else -> DrawerDestination.HOME
}

private fun NavDestination?.isDrawerDestination(): Boolean =
    this?.hasRoute<HomeRoute>() == true || this?.hasRoute<StatisticsRoute>() == true
