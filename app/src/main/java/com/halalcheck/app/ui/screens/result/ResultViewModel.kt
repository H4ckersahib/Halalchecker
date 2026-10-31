package com.halalcheck.app.ui.screens.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halalcheck.app.data.preferences.UserPreferencesRepository
import com.halalcheck.app.data.repository.FeedbackRepository
import com.halalcheck.app.data.repository.IngredientRepository
import com.halalcheck.app.data.repository.PhotoIngredientsRepository
import com.halalcheck.app.data.repository.ProductRepository
import com.halalcheck.app.domain.engine.HalalEngine
import com.halalcheck.app.domain.model.DataSource
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.Madhab
import com.halalcheck.app.domain.model.PackageClaim
import com.halalcheck.app.domain.model.Product
import com.halalcheck.app.domain.model.ProductFeedback
import com.halalcheck.app.domain.model.withPhotoIngredients
import com.halalcheck.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

enum class ResultError { NETWORK, RATE_LIMITED, UNEXPECTED }

data class ResultUiState(
    val isLoading: Boolean = true,
    /** Reloading while the previous result stays visible. */
    val isRefreshing: Boolean = false,
    val barcode: String = "",
    val product: Product? = null,
    /** Loading failed and there is nothing to show. */
    val error: ResultError? = null,
    /** Reloading failed; the previous result is still shown. */
    val refreshError: ResultError? = null,
    val madhab: Madhab = Madhab.HANAFI,
    /** The tester's report that this result is wrong, if any. */
    val feedback: ProductFeedback? = null
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val feedbackRepository: FeedbackRepository,
    private val ingredientRepository: IngredientRepository,
    private val photoRepository: PhotoIngredientsRepository,
    private val preferences: UserPreferencesRepository,
    private val halalEngine: HalalEngine
) : ViewModel() {

    private val barcode: String = savedStateHandle.get<String>(Routes.ARG_BARCODE) ?: ""

    /** True when opened by the scanner; opening a product from the history is not a new scan. */
    private var recordScan: Boolean = savedStateHandle.get<Boolean>(Routes.ARG_RECORD) ?: false

    private val _uiState = MutableStateFlow(ResultUiState(barcode = barcode))
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    /** The latest fetch or analysis; a newer action cancels it so an older result can't win. */
    private var job: Job? = null

    /** A cancelled re-analysis still owed the history an update. */
    private var storePending = false

    init {
        launchExclusive {
            _uiState.update { it.copy(madhab = preferences.madhab.first()) }
            if (barcode.isNotEmpty()) load(forceRefresh = false)
        }
        // A photo of the ingredient list was saved or deleted (photo screen, delete button)
        viewModelScope.launch {
            photoRepository.observe(barcode).drop(1).collect { launchExclusive { load(forceRefresh = false) } }
        }
    }

    private fun launchExclusive(block: suspend () -> Unit) {
        job?.cancel()
        job = viewModelScope.launch { block() }
    }

    private suspend fun load(forceRefresh: Boolean) {
        val previous = _uiState.value.product
        _uiState.update {
            if (previous == null) it.copy(isLoading = true, error = null) else it.copy(isRefreshing = true, refreshError = null)
        }
        try {
            val photo = photoRepository.get(barcode)
            val lookup = productRepository.lookupProduct(barcode, forceRefresh)
            // Offline, a barcode Open Food Facts doesn't know can still be checked from the user's photo
            val offProduct = lookup.getOrNull()
                ?: photo?.let { Product(barcode = barcode, name = "", source = DataSource.UNKNOWN) }
                ?: return fail(errorType(lookup.exceptionOrNull() ?: IllegalStateException()))
            val product = photo?.let {
                offProduct.withPhotoIngredients(it.text, it.savedAt)
                    .copy(photoConfirmedComplete = preferences.isPhotoComplete(barcode))
            } ?: offProduct
            val withClaims = product.copy(userClaims = preferences.packageClaims(barcode))
            val analyzed = analyze(withClaims, _uiState.value.madhab)
            store(analyzed)
            val feedback = feedbackRepository.getFeedback(barcode)
            _uiState.update { it.copy(isLoading = false, isRefreshing = false, product = analyzed, feedback = feedback) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail(ResultError.UNEXPECTED)
        }
    }

    private fun fail(type: ResultError) {
        _uiState.update {
            if (it.product == null) it.copy(isLoading = false, error = type)
            else it.copy(isRefreshing = false, refreshError = type)
        }
    }

    private fun errorType(error: Throwable): ResultError = when {
        error is HttpException && error.code() == HTTP_TOO_MANY_REQUESTS -> ResultError.RATE_LIMITED
        error is IOException || error is HttpException -> ResultError.NETWORK
        else -> ResultError.UNEXPECTED
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
        if (_uiState.value.isLoading || _uiState.value.isRefreshing) return // the running load reads the madhab
        reanalyze(store = false)
    }

    /**
     * The user says the package shows this label. Vegan and vegetarian exclude each other
     * (vegan includes vegetarian). Remembered per product until the user undoes it.
     */
    fun setPackageClaim(claim: PackageClaim, confirmed: Boolean) {
        val product = _uiState.value.product ?: return
        var claims = if (confirmed) product.userClaims + claim else product.userClaims - claim
        if (confirmed && claim == PackageClaim.VEGAN) claims = claims - PackageClaim.VEGETARIAN
        if (confirmed && claim == PackageClaim.VEGETARIAN) claims = claims - PackageClaim.VEGAN
        _uiState.update { it.copy(product = product.copy(userClaims = claims)) }
        viewModelScope.launch {
            PackageClaim.entries.forEach { preferences.setPackageClaim(barcode, it, it in claims) }
        }
        reanalyze(store = true)
    }

    private fun reanalyze(store: Boolean) {
        val product = _uiState.value.product ?: return
        storePending = storePending || store
        launchExclusive {
            val reanalyzed = analyze(product, _uiState.value.madhab)
            if (storePending) {
                store(reanalyzed)
                storePending = false
            }
            _uiState.update { it.copy(product = reanalyzed) }
        }
    }

    /** [expected] null = "don't know, but this looks wrong". Replaces an earlier report for this product. */
    fun saveFeedback(expected: HalalStatus?, comment: String) {
        val product = _uiState.value.product ?: return
        val feedback = ProductFeedback(
            barcode = barcode,
            expectedStatus = expected,
            comment = comment,
            shownStatus = product.halalStatus,
            madhab = _uiState.value.madhab
        )
        _uiState.update { it.copy(feedback = feedback) }
        viewModelScope.launch { feedbackRepository.saveFeedback(feedback) }
    }

    /** Back to the Open Food Facts ingredients; the observer re-analyzes. */
    fun deletePhotoIngredients() {
        viewModelScope.launch {
            preferences.setPhotoComplete(barcode, false)
            photoRepository.delete(barcode)
        }
    }

    /** The photo shows the whole, current list: what only Open Food Facts lists no longer counts. */
    fun confirmPhotoComplete() {
        val product = _uiState.value.product ?: return
        _uiState.update { it.copy(product = product.copy(photoConfirmedComplete = true)) }
        viewModelScope.launch { preferences.setPhotoComplete(barcode, true) }
        reanalyze(store = true)
    }

    fun deleteFeedback() {
        _uiState.update { it.copy(feedback = null) }
        viewModelScope.launch { feedbackRepository.deleteFeedback(barcode) }
    }

    fun retry() {
        launchExclusive { load(forceRefresh = false) }
    }

    /** Loads the product from Open Food Facts again, e.g. after correcting it there. */
    fun refresh() {
        launchExclusive { load(forceRefresh = true) }
    }

    private companion object {
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
