package com.halalcheck.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.halalcheck.app.ui.screens.history.HistoryScreen
import com.halalcheck.app.ui.screens.home.HomeScreen
import com.halalcheck.app.ui.screens.photo.PhotoIngredientsScreen
import com.halalcheck.app.ui.screens.result.ResultScreen
import com.halalcheck.app.ui.screens.scanner.ScannerScreen
import com.halalcheck.app.ui.screens.settings.SettingsScreen

/**
 * Navigation routes for the HalalCheck app.
 */
object Routes {
    const val HOME = "home"
    const val SCANNER = "scanner"
    const val ARG_BARCODE = "barcode"
    const val ARG_RECORD = "record"
    const val RESULT = "result/{$ARG_BARCODE}?$ARG_RECORD={$ARG_RECORD}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PHOTO = "photo/{$ARG_BARCODE}"

    /** [record]: a new scan that goes into the history; false when reopening a product from the history. */
    fun result(barcode: String, record: Boolean = false) = "result/${Uri.encode(barcode)}?$ARG_RECORD=$record"

    /** Photograph the ingredient list of [barcode]. */
    fun photo(barcode: String) = "photo/${Uri.encode(barcode)}"
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
        composable(Routes.HOME) { entry ->
            HomeScreen(
                onNavigateToScanner = {
                    entry.ifResumed { navController.navigate(Routes.SCANNER) }
                },
                onNavigateToHistory = {
                    entry.ifResumed { navController.navigate(Routes.HISTORY) }
                },
                onNavigateToSettings = {
                    entry.ifResumed { navController.navigate(Routes.SETTINGS) }
                },
                onNavigateToResult = { barcode ->
                    entry.ifResumed { navController.navigate(Routes.result(barcode)) }
                }
            )
        }

        composable(Routes.SCANNER) { entry ->
            ScannerScreen(
                onNavigateBack = {
                    entry.ifResumed { navController.popBackStack() }
                },
                // ScannerViewModel reports only the first barcode, so no tap guard here
                onBarcodeScanned = { barcode ->
                    navController.navigate(Routes.result(barcode, record = true)) {
                        popUpTo(Routes.SCANNER) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(
                navArgument(Routes.ARG_BARCODE) { type = NavType.StringType },
                navArgument(Routes.ARG_RECORD) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { entry ->
            ResultScreen(
                onNavigateBack = {
                    entry.ifResumed { navController.popBackStack() }
                },
                onTakePhoto = { barcode ->
                    entry.ifResumed { navController.navigate(Routes.photo(barcode)) }
                }
            )
        }

        // Back to the result, which re-analyzes when the photo text is saved
        composable(
            route = Routes.PHOTO,
            arguments = listOf(navArgument(Routes.ARG_BARCODE) { type = NavType.StringType })
        ) { entry ->
            PhotoIngredientsScreen(
                onNavigateBack = {
                    entry.ifResumed { navController.popBackStack() }
                },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Routes.HISTORY) { entry ->
            HistoryScreen(
                onNavigateBack = {
                    entry.ifResumed { navController.popBackStack() }
                },
                onNavigateToResult = { barcode ->
                    entry.ifResumed { navController.navigate(Routes.result(barcode)) }
                }
            )
        }

        composable(Routes.SETTINGS) { entry ->
            SettingsScreen(
                onNavigateBack = {
                    entry.ifResumed { navController.popBackStack() }
                }
            )
        }
    }
}

/**
 * Ignores taps while a screen transition is running, so tapping a list item twice
 * opens the product once.
 */
private inline fun NavBackStackEntry.ifResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}
