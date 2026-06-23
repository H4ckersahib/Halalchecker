package com.halalcheck.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halalcheck.app.data.repository.ProductRepository
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.ScanHistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val selectedFilter: HalalStatus? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    val scans: StateFlow<List<ScanHistoryItem>> =
        _uiState.flatMapLatest { state ->
            when {
                state.searchQuery.isNotBlank() -> productRepository.searchScansFlow(state.searchQuery)
                state.selectedFilter != null -> productRepository.getScansByStatusFlow(state.selectedFilter)
                else -> productRepository.getAllScansFlow()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(status: HalalStatus?) {
        _uiState.value = _uiState.value.copy(selectedFilter = status)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch {
            productRepository.deleteScan(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            productRepository.clearHistory()
        }
    }
}
