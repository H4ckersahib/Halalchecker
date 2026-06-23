package com.halalcheck.app.ui.screens.scanner

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class ScannerUiState(
    val isScanning: Boolean = true,
    val scannedBarcode: String? = null,
    val hasCameraPermission: Boolean = false,
    val showManualInput: Boolean = false,
    val manualBarcode: String = ""
)

@HiltViewModel
class ScannerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onBarcodeDetected(barcode: String) {
        if (_uiState.value.scannedBarcode == null) {
            _uiState.value = _uiState.value.copy(
                scannedBarcode = barcode,
                isScanning = false
            )
        }
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasCameraPermission = granted)
    }

    fun toggleManualInput() {
        _uiState.value = _uiState.value.copy(
            showManualInput = !_uiState.value.showManualInput
        )
    }

    fun onManualBarcodeChange(barcode: String) {
        _uiState.value = _uiState.value.copy(manualBarcode = barcode)
    }

    fun submitManualBarcode() {
        val barcode = _uiState.value.manualBarcode.trim()
        if (barcode.isNotEmpty()) {
            onBarcodeDetected(barcode)
        }
    }

    fun resetScanner() {
        _uiState.value = ScannerUiState(hasCameraPermission = _uiState.value.hasCameraPermission)
    }
}
