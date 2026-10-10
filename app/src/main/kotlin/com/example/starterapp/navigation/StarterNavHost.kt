package com.example.starterapp.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.starterapp.StarterApplication
import com.example.starterapp.ui.home.HomeScreen
import com.example.starterapp.ui.home.HomeViewModel
import com.example.starterapp.ui.serverconfig.ServerConfigScreen
import com.example.starterapp.ui.serverconfig.ServerConfigViewModel

private object Routes {
    const val SERVER_CONFIG = "server_config"
    const val HOME = "home"
}

/**
 * Navigation graph. ViewModels are built with small hand-written factories that take their
 * dependencies from the composition root ([StarterApplication]).
 */
@Composable
fun StarterNavHost(app: StarterApplication) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SERVER_CONFIG) {
        composable(Routes.SERVER_CONFIG) {
            val viewModel: ServerConfigViewModel =
                viewModel(factory = ServerConfigViewModel.Factory(app.serverConfigStore))
            ServerConfigScreen(
                viewModel = viewModel,
                onContinue = { navController.navigate(Routes.HOME) }
            )
        }
        composable(Routes.HOME) {
            val viewModel: HomeViewModel =
                viewModel(
                    factory = HomeViewModel.Factory(
                        app.actionRepository,
                        app.connectivityObserver
                    )
                )
            HomeScreen(
                viewModel = viewModel,
                onOpenSettings = { navController.navigate(Routes.SERVER_CONFIG) }
            )
        }
    }
}
