package com.halalcheck.app.ui.screens.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halalcheck.app.data.preferences.UserPreferencesRepository
import com.halalcheck.app.data.repository.IngredientRepository
import com.halalcheck.app.data.repository.ProductRepository
import com.halalcheck.app.domain.engine.HalalEngine
import com.halalcheck.app.domain.model.DataSource
import com.halalcheck.app.domain.model.Madhab
import com.halalcheck.app.domain.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

enum class ResultError { NETWORK, UNEXPECTED }

data class ResultUiState(
    val isLoading: Boolean = true,
    val barcode: String = "",
    val product: Product? = null,
    val error: ResultError? = null,
    val madhab: Madhab = Madhab.HANAFI
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val ingredientRepository: IngredientRepository,
    private val preferences: UserPreferencesRepository,
    private val halalEngine: HalalEngine
) : ViewModel() {

    private val barcode: String = savedStateHandle.get<String>("barcode") ?: ""

    private val _uiState = MutableStateFlow(ResultUiState(barcode = barcode))
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(madhab = preferences.madhab.first()) }
            if (barcode.isNotEmpty()) analyzeProduct()
        }
    }

    private suspend fun analyzeProduct() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            productRepository.lookupProduct(barcode).fold(
                onSuccess = { product ->
                    val analyzed = analyze(product, _uiState.value.madhab)
                    if (analyzed.source != DataSource.UNKNOWN) {
                        productRepository.saveAnalysis(analyzed)
                        productRepository.saveScanToHistory(analyzed)
                    }
                    _uiState.update { it.copy(isLoading = false, product = analyzed) }
                },
                onFailure = { error ->
                    val type = if (error is IOException || error is HttpException) ResultError.NETWORK else ResultError.UNEXPECTED
                    _uiState.update { it.copy(isLoading = false, error = type) }
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = ResultError.UNEXPECTED) }
        }
    }

    private suspend fun analyze(product: Product, madhab: Madhab): Product {
        val index = ingredientRepository.getIndex()
        return withContext(Dispatchers.Default) { halalEngine.analyzeProduct(product, index, madhab) }
    }

    /** Re-analyze with another madhab for this result only; the default stays in Settings. */
    fun changeMadhab(madhab: Madhab) {
        _uiState.update { it.copy(madhab = madhab) }
        val product = _uiState.value.product ?: return
        viewModelScope.launch {
            val reanalyzed = analyze(product, madhab)
            _uiState.update { it.copy(product = reanalyzed) }
        }
    }

    fun retry() {
        viewModelScope.launch { analyzeProduct() }
    }
}
