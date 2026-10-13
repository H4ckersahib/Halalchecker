package com.halalcheck.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.halalcheck.app.ui.screens.history.HistoryScreen
import com.halalcheck.app.ui.screens.home.HomeScreen
import com.halalcheck.app.ui.screens.result.ResultScreen
import com.halalcheck.app.ui.screens.scanner.ScannerScreen
import com.halalcheck.app.ui.screens.settings.SettingsScreen

/**
 * Navigation routes for the HalalCheck app.
 */
object Routes {
    const val HOME = "home"
    const val SCANNER = "scanner"
    const val RESULT = "result/{barcode}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    fun result(barcode: String) = "result/${Uri.encode(barcode)}"
}

@Composable
fun HalalCheckNavGraph(
    navController: NavHostController,
    startDestination: String = Routes.HOME
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToScanner = {
                    navController.navigate(Routes.SCANNER)
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.HISTORY)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToResult = { barcode ->
                    navController.navigate(Routes.result(barcode))
                }
            )
        }

        composable(Routes.SCANNER) {
            ScannerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onBarcodeScanned = { barcode ->
                    navController.navigate(Routes.result(barcode)) {
                        popUpTo(Routes.SCANNER) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(
                navArgument("barcode") { type = NavType.StringType }
            )
        ) {
            ResultScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToResult = { barcode ->
                    navController.navigate(Routes.result(barcode))
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
