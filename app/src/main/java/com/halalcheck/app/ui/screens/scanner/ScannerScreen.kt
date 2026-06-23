package com.halalcheck.app.ui.screens.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.halalcheck.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onNavigateBack: () -> Unit,
    onBarcodeScanned: (String) -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Camera permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onCameraPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.onCameraPermissionResult(true)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Navigate when barcode is detected
    LaunchedEffect(uiState.scannedBarcode) {
        uiState.scannedBarcode?.let { barcode ->
            onBarcodeScanned(barcode)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.hasCameraPermission && !uiState.showManualInput) {
            // Camera preview with barcode scanning
            CameraPreview(
                onBarcodeDetected = { barcode ->
                    viewModel.onBarcodeDetected(barcode)
                }
            )

            // Scan overlay
            ScanOverlay()
        } else if (!uiState.hasCameraPermission) {
            // Permission denied
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📷",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Kamera-Zugriff benötigt",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Bitte erlaube den Kamera-Zugriff um Barcodes zu scannen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                ) {
                    Text("Berechtigung erteilen")
                }
            }
        }

        // Manual input overlay
        AnimatedVisibility(
            visible = uiState.showManualInput,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🔢",
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Barcode manuell eingeben",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = uiState.manualBarcode,
                    onValueChange = { viewModel.onManualBarcodeChange(it) },
                    label = { Text("Barcode-Nummer") },
                    placeholder = { Text("z.B. 4005500068211") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { viewModel.submitManualBarcode() }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.submitManualBarcode() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald500
                    )
                ) {
                    Text(
                        text = "Produkt suchen",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Top bar overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(top = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Zurück",
                    tint = Color.White
                )
            }

            TextButton(
                onClick = { viewModel.toggleManualInput() },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (uiState.showManualInput) "Scanner" else "Manuell",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val barcodeScanner = BarcodeScanning.getClient()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(
                            ContextCompat.getMainExecutor(ctx)
                        ) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            barcode.rawValue?.let { value ->
                                                if (barcode.format in listOf(
                                                    Barcode.FORMAT_EAN_13,
                                                    Barcode.FORMAT_EAN_8,
                                                    Barcode.FORMAT_UPC_A,
                                                    Barcode.FORMAT_UPC_E,
                                                    Barcode.FORMAT_QR_CODE
                                                )) {
                                                    onBarcodeDetected(value)
                                                }
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ScanOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Semi-transparent overlay
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top darkened area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.5f))
            )
            // Middle row with scan window
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.1f)
                        .height(200.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                )
                // Scan window (clear)
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .height(200.dp)
                        .border(
                            width = 3.dp,
                            color = Emerald400,
                            shape = RoundedCornerShape(16.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .weight(0.1f)
                        .height(200.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }
            // Bottom darkened area with text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "Richte die Kamera auf den Barcode",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}
