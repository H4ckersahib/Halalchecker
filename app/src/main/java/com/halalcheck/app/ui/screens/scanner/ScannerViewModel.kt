package com.halalcheck.app.ui.screens.scanner

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ScannerUiState(
    val scannedBarcode: String? = null,
    val hasCameraPermission: Boolean = false,
    val showManualInput: Boolean = false,
    val manualBarcode: String = "",
    val manualBarcodeProblem: Gtin.Problem? = null
)

@HiltViewModel
class ScannerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    /** Called from the camera analyzer; only the first barcode counts. */
    fun onBarcodeDetected(barcode: String) {
        _uiState.update { state ->
            if (state.scannedBarcode == null) state.copy(scannedBarcode = barcode) else state
        }
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun showManualInput() {
        _uiState.update { it.copy(showManualInput = true) }
    }

    fun toggleManualInput() {
        _uiState.update { it.copy(showManualInput = !it.showManualInput) }
    }

    fun onManualBarcodeChange(barcode: String) {
        _uiState.update {
            it.copy(manualBarcode = barcode.filter(Char::isDigit).take(MAX_BARCODE_LENGTH), manualBarcodeProblem = null)
        }
    }

    fun submitManualBarcode() {
        val barcode = _uiState.value.manualBarcode
        val problem = Gtin.problem(barcode)
        if (problem == null) {
            onBarcodeDetected(barcode)
        } else {
            _uiState.update { it.copy(manualBarcodeProblem = problem) }
        }
    }

    private companion object {
        const val MAX_BARCODE_LENGTH = 14 // GTIN-14
    }
}
