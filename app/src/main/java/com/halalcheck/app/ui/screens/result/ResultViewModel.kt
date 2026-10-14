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
import com.halalcheck.app.ui.navigation.Routes
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

    private val barcode: String = savedStateHandle.get<String>(Routes.ARG_BARCODE) ?: ""

    /** True when opened by the scanner; opening a product from the history is not a new scan. */
    private var recordScan: Boolean = savedStateHandle.get<Boolean>(Routes.ARG_RECORD) ?: false

    private val _uiState = MutableStateFlow(ResultUiState(barcode = barcode))
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(madhab = preferences.madhab.first()) }
            if (barcode.isNotEmpty()) analyzeProduct(forceRefresh = false)
        }
    }

    private suspend fun analyzeProduct(forceRefresh: Boolean) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            productRepository.lookupProduct(barcode, forceRefresh).fold(
                onSuccess = { product ->
                    val withUserLabels = product.copy(userConfirmedVegan = preferences.isVeganConfirmed(barcode))
                    val analyzed = analyze(withUserLabels, _uiState.value.madhab)
                    store(analyzed)
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

    /** Saves the result; only the first analysis after a scan adds (or moves up) the history entry. */
    private suspend fun store(product: Product) {
        if (product.source == DataSource.UNKNOWN) return
        productRepository.saveAnalysis(product)
        if (recordScan) {
            productRepository.saveScanToHistory(product)
            recordScan = false
        } else {
            productRepository.updateHistoryEntry(product)
        }
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

    /** The package says vegan/vegetarian although Open Food Facts lacks the label. Remembered per product. */
    fun setVeganConfirmed(confirmed: Boolean) {
        val product = _uiState.value.product ?: return
        viewModelScope.launch {
            preferences.setVeganConfirmed(barcode, confirmed)
            val reanalyzed = analyze(product.copy(userConfirmedVegan = confirmed), _uiState.value.madhab)
            store(reanalyzed)
            _uiState.update { it.copy(product = reanalyzed) }
        }
    }

    fun retry() {
        viewModelScope.launch { analyzeProduct(forceRefresh = false) }
    }

    /** Loads the product from Open Food Facts again, e.g. after correcting it there. */
    fun refresh() {
        viewModelScope.launch { analyzeProduct(forceRefresh = true) }
    }
}
