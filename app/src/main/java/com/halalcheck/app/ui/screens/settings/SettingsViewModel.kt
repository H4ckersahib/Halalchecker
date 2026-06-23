package com.halalcheck.app.ui.screens.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halalcheck.app.domain.model.Madhab
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class SettingsUiState(
    val selectedMadhab: Madhab = Madhab.HANAFI,
    val darkMode: Boolean = false,
    val language: String = "de"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    companion object {
        val KEY_MADHAB = stringPreferencesKey("madhab")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        val KEY_LANGUAGE = stringPreferencesKey("language")
    }

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            val madhab = prefs[KEY_MADHAB]?.let { Madhab.fromString(it) } ?: Madhab.HANAFI
            val darkMode = prefs[KEY_DARK_MODE] ?: false
            val language = prefs[KEY_LANGUAGE] ?: "de"
            _uiState.value = SettingsUiState(
                selectedMadhab = madhab,
                darkMode = darkMode,
                language = language
            )
        }
    }

    fun setMadhab(madhab: Madhab) {
        _uiState.value = _uiState.value.copy(selectedMadhab = madhab)
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_MADHAB] = madhab.name }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(darkMode = enabled)
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_DARK_MODE] = enabled }
        }
    }

    fun setLanguage(language: String) {
        _uiState.value = _uiState.value.copy(language = language)
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_LANGUAGE] = language }
        }
    }
}
