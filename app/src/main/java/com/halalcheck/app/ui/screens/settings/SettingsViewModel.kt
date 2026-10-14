package com.halalcheck.app.ui.screens.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halalcheck.app.data.export.ExportRepository
import com.halalcheck.app.data.preferences.UserPreferencesRepository
import com.halalcheck.app.data.repository.FeedbackRepository
import com.halalcheck.app.data.repository.ProductRepository
import com.halalcheck.app.domain.model.Madhab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val selectedMadhab: Madhab = Madhab.HANAFI,
    /** null = follow the system. */
    val darkMode: Boolean? = null,
    /** Language tag ("de", "en"), null = follow the system. */
    val language: String? = null,
    /** Package labels the user confirmed (vegan, vegetarian, halal), over all products. */
    val packageClaimCount: Int = 0
)

/** The test data export, see ExportRepository. */
data class ExportUiState(
    val testerName: String = "",
    val productCount: Int = 0,
    val feedbackCount: Int = 0,
    val isExporting: Boolean = false,
    val failed: Boolean = false,
    /** A finished export waiting to be shared; cleared by [SettingsViewModel.onExportShared]. */
    val file: File? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val exportRepository: ExportRepository,
    productRepository: ProductRepository,
    feedbackRepository: FeedbackRepository
) : ViewModel() {

    private val language = MutableStateFlow(currentLanguage())

    val uiState: StateFlow<SettingsUiState> =
        combine(preferences.madhab, preferences.darkMode, language, preferences.packageClaimCount) { madhab, darkMode, language, claims ->
            SettingsUiState(selectedMadhab = madhab, darkMode = darkMode, language = language, packageClaimCount = claims)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState(language = language.value))

    private val exportProgress = MutableStateFlow(ExportUiState())

    val exportState: StateFlow<ExportUiState> =
        combine(
            exportProgress,
            preferences.testerName,
            productRepository.scanCountFlow,
            feedbackRepository.feedbackCount
        ) { progress, name, products, feedback ->
            progress.copy(testerName = name, productCount = products, feedbackCount = feedback)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExportUiState())

    fun setTesterName(name: String) {
        viewModelScope.launch { preferences.setTesterName(name) }
    }

    fun export() {
        if (exportProgress.value.isExporting) return
        exportProgress.update { it.copy(isExporting = true, failed = false) }
        viewModelScope.launch {
            val file = runCatching { exportRepository.export() }.getOrNull()
            exportProgress.update { it.copy(isExporting = false, failed = file == null, file = file) }
        }
    }

    fun onExportShared() {
        exportProgress.update { it.copy(file = null) }
    }

    fun setMadhab(madhab: Madhab) {
        viewModelScope.launch { preferences.setMadhab(madhab) }
    }

    fun setDarkMode(enabled: Boolean?) {
        viewModelScope.launch { preferences.setDarkMode(enabled) }
    }

    fun clearPackageClaims() {
        viewModelScope.launch { preferences.clearPackageClaims() }
    }

    /** AppCompat stores the choice and recreates the activity. */
    fun setLanguage(language: String?) {
        this.language.value = language
        AppCompatDelegate.setApplicationLocales(
            if (language == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(language)
        )
    }

    private fun currentLanguage(): String? =
        AppCompatDelegate.getApplicationLocales().takeUnless { it.isEmpty }?.get(0)?.language
}
