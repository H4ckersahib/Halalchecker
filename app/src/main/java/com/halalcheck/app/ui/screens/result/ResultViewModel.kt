package com.halalcheck.app.ui.screens.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halalcheck.app.data.repository.IngredientRepository
import com.halalcheck.app.data.repository.ProductRepository
import com.halalcheck.app.domain.engine.HalalEngine
import com.halalcheck.app.domain.model.Madhab
import com.halalcheck.app.domain.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val error: String? = null,
    val madhab: Madhab = Madhab.HANAFI
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val ingredientRepository: IngredientRepository,
    private val halalEngine: HalalEngine
) : ViewModel() {

    private val barcode: String = savedStateHandle.get<String>("barcode") ?: ""

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        if (barcode.isNotEmpty()) {
            analyzeProduct()
        }
    }

    private fun analyzeProduct() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Step 1: Look up the product
                val result = productRepository.lookupProduct(barcode)

                result.fold(
                    onSuccess = { product ->
                        // Step 2: Get all known ingredients
                        val knownIngredients = ingredientRepository.getAllIngredients()

                        // Step 3: Analyze with HalalEngine
                        val analyzedProduct = halalEngine.analyzeProduct(
                            product = product,
                            knownIngredients = knownIngredients,
                            madhab = _uiState.value.madhab
                        )

                        // Step 4: Cache the result and save to history
                        productRepository.cacheProduct(analyzedProduct)
                        productRepository.saveScanToHistory(analyzedProduct)

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            product = analyzedProduct
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Netzwerkfehler: ${error.localizedMessage ?: "Produkt konnte nicht geladen werden"}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Fehler: ${e.localizedMessage ?: "Unbekannter Fehler"}"
                )
            }
        }
    }

    fun changeMadhab(madhab: Madhab) {
        _uiState.value = _uiState.value.copy(madhab = madhab)
        // Re-analyze with new madhab
        _uiState.value.product?.let { product ->
            viewModelScope.launch {
                val knownIngredients = ingredientRepository.getAllIngredients()
                val reanalyzed = halalEngine.analyzeProduct(
                    product = product.copy(ingredientsText = product.ingredientsText),
                    knownIngredients = knownIngredients,
                    madhab = madhab
                )
                _uiState.value = _uiState.value.copy(product = reanalyzed)
            }
        }
    }

    fun retry() {
        analyzeProduct()
    }
}
