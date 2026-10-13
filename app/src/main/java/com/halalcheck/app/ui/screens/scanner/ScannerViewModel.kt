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
    val manualBarcodeInvalid: Boolean = false
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

    fun toggleManualInput() {
        _uiState.update { it.copy(showManualInput = !it.showManualInput) }
    }

    fun onManualBarcodeChange(barcode: String) {
        _uiState.update {
            it.copy(manualBarcode = barcode.filter(Char::isDigit).take(MAX_BARCODE_LENGTH), manualBarcodeInvalid = false)
        }
    }

    fun submitManualBarcode() {
        val barcode = _uiState.value.manualBarcode
        if (barcode.length in MIN_BARCODE_LENGTH..MAX_BARCODE_LENGTH) {
            onBarcodeDetected(barcode)
        } else {
            _uiState.update { it.copy(manualBarcodeInvalid = true) }
        }
    }

    private companion object {
        // EAN-8 up to GTIN-14
        const val MIN_BARCODE_LENGTH = 8
        const val MAX_BARCODE_LENGTH = 14
    }
}
