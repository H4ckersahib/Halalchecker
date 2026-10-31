package com.halalcheck.app.ui.screens.photo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.halalcheck.app.data.preferences.UserPreferencesRepository
import com.halalcheck.app.data.repository.PhotoIngredientsRepository
import com.halalcheck.app.domain.engine.OcrText
import com.halalcheck.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class PhotoStep { CAPTURE, RECOGNIZING, EDIT }

data class PhotoUiState(
    val step: PhotoStep = PhotoStep.CAPTURE,
    val text: String = "",
    /** The last photo had no readable text. */
    val nothingRecognized: Boolean = false,
    /** Text recognition failed, e.g. Google Play services has not downloaded the model yet or is missing. */
    val recognitionUnavailable: Boolean = false,
    /** Saved; the screen closes and the result screen re-analyzes. */
    val saved: Boolean = false
)

/**
 * Reads an ingredient list from a photo on the device (ML Kit, Latin script) and lets the user
 * correct it before it replaces the product's ingredients.
 */
@HiltViewModel
class PhotoIngredientsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PhotoIngredientsRepository,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val barcode: String = savedStateHandle.get<String>(Routes.ARG_BARCODE) ?: ""
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val _uiState = MutableStateFlow(PhotoUiState())
    val uiState: StateFlow<PhotoUiState> = _uiState.asStateFlow()

    init {
        // An earlier photo of this product opens for editing
        viewModelScope.launch {
            repository.get(barcode)?.let { photo -> _uiState.update { it.copy(step = PhotoStep.EDIT, text = photo.text) } }
        }
    }

    fun recognize(image: InputImage) {
        _uiState.update { it.copy(step = PhotoStep.RECOGNIZING, nothingRecognized = false, recognitionUnavailable = false) }
        viewModelScope.launch {
            val text = try {
                val result = recognizer.process(image).await()
                OcrText.join(result.textBlocks.map { block -> block.lines.map { it.text } })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(step = PhotoStep.CAPTURE, recognitionUnavailable = true) }
                return@launch
            }
            _uiState.update {
                if (text.isBlank()) it.copy(step = PhotoStep.CAPTURE, nothingRecognized = true)
                else it.copy(step = PhotoStep.EDIT, text = text)
            }
        }
    }

    /** The camera or the picked image could not be read. */
    fun onImageFailed() {
        _uiState.update { it.copy(step = PhotoStep.CAPTURE, nothingRecognized = true) }
    }

    fun onTextChange(text: String) {
        _uiState.update { it.copy(text = text) }
    }

    fun retake() {
        _uiState.update { it.copy(step = PhotoStep.CAPTURE, nothingRecognized = false, recognitionUnavailable = false) }
    }

    fun save() {
        val text = _uiState.value.text
        if (text.isBlank()) return
        viewModelScope.launch {
            // A new text is compared with Open Food Facts again; reset first, saving triggers the re-analysis
            preferences.setPhotoComplete(barcode, false)
            repository.save(barcode, text)
            _uiState.update { it.copy(saved = true) }
        }
    }

    override fun onCleared() {
        recognizer.close()
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
