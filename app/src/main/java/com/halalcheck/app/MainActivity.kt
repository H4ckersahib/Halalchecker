package com.halalcheck.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.halalcheck.app.data.preferences.UserPreferencesRepository
import com.halalcheck.app.ui.navigation.HalalCheckNavGraph
import com.halalcheck.app.ui.theme.HalalCheckTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** AppCompatActivity so that AppCompatDelegate.setApplicationLocales() switches the UI language. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var preferences: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val darkModeSetting = preferences.darkMode.map { DarkModeSetting(it) }
        setContent {
            // Render nothing until the setting is read, so the theme does not flash.
            val setting by darkModeSetting.collectAsStateWithLifecycle(initialValue = null)
            val loaded = setting ?: return@setContent
            HalalCheckTheme(darkTheme = loaded.darkMode ?: isSystemInDarkTheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    HalalCheckNavGraph(navController = navController)
                }
            }
        }
    }

    private class DarkModeSetting(val darkMode: Boolean?)
}
