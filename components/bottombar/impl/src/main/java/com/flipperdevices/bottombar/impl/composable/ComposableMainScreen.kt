package com.flipperdevices.bottombar.impl.composable

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.flipperdevices.bottombar.impl.main.compose.ComposeBottomBar
import com.flipperdevices.bottombar.impl.main.viewmodel.BottomNavigationViewModel
import com.flipperdevices.connection.api.ConnectionApi
import com.flipperdevices.core.ui.navigation.AggregateFeatureEntry
import com.flipperdevices.core.ui.navigation.ComposableFeatureEntry
import kotlinx.collections.immutable.ImmutableSet
import tangle.viewmodel.compose.tangleViewModel

@Composable
fun ComposableMainScreen(
    connectionApi: ConnectionApi,
    featureEntries: ImmutableSet<AggregateFeatureEntry>,
    composableEntries: ImmutableSet<ComposableFeatureEntry>,
    modifier: Modifier = Modifier,
    navigationViewModel: BottomNavigationViewModel = tangleViewModel()
) {
    val selectedTab by navigationViewModel.selectedTab.collectAsState()
    val startDestination = remember { navigationViewModel.getStartDestination().startRoute.name }
    val navController = rememberNavController()
    val currentDestination by navController.currentBackStackEntryAsState()
    key(currentDestination?.destination) {
        navigationViewModel.invalidateSelectedTab(currentDestination?.destination)
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            ComposeBottomBar(
                connectionApi = connectionApi,
                selectedItem = selectedTab,
                onBottomBarClick = {
                    val topLevelNavOptions = navOptions {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                    navController.navigate(it.startRoute.name, topLevelNavOptions)
                }
            )
        }
    ) {
        NavHost(
            modifier = Modifier.padding(it),
            navController = navController,
            startDestination = startDestination
        ) {
            featureEntries.forEach {
                with(it) {
                    navigation(navController)
                }
            }
            composableEntries.forEach {
                with(it) {
                    composable(navController)
                }
            }
        }
    }
}