package com.halalcheck.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halalcheck.app.data.repository.IngredientRepository
import com.halalcheck.app.data.repository.ProductRepository
import com.halalcheck.app.domain.model.ScanHistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val todayScanCount: Int = 0,
    val totalScanCount: Int = 0,
    val ingredientCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val ingredientRepository: IngredientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val recentScans: StateFlow<List<ScanHistoryItem>> =
        productRepository.getRecentScansFlow(limit = 10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    todayScanCount = productRepository.getTodayScanCount(),
                    totalScanCount = productRepository.getTotalScanCount()
                )
            }
            // Waits for the ingredient seeding, which takes a few seconds after an app update
            _uiState.update { it.copy(ingredientCount = ingredientRepository.getIngredientCount()) }
        }
    }
}
