package com.halalcheck.app.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.halalcheck.app.BuildConfig
import com.halalcheck.app.R
import com.halalcheck.app.domain.model.Madhab
import com.halalcheck.app.ui.components.displayName
import com.halalcheck.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Madhab Selection
            SettingsSection(title = stringResource(R.string.settings_madhab), icon = Icons.AutoMirrored.Filled.MenuBook) {
                Madhab.entries.forEach { madhab ->
                    MadhabOption(
                        madhab = madhab,
                        isSelected = uiState.selectedMadhab == madhab,
                        onClick = { viewModel.setMadhab(madhab) }
                    )
                }
            }

            // Appearance
            SettingsSection(title = stringResource(R.string.settings_appearance), icon = Icons.Default.DarkMode) {
                listOf(
                    null to R.string.settings_theme_system,
                    false to R.string.settings_theme_light,
                    true to R.string.settings_theme_dark
                ).forEach { (darkMode, label) ->
                    SelectableRow(
                        text = stringResource(label),
                        isSelected = uiState.darkMode == darkMode,
                        onClick = { viewModel.setDarkMode(darkMode) }
                    )
                }
            }

            // Language (TR and AR follow once their translations exist)
            SettingsSection(title = stringResource(R.string.settings_language), icon = Icons.Default.Language) {
                listOf(
                    null to stringResource(R.string.settings_language_system),
                    "de" to "🇩🇪 Deutsch",
                    "en" to "🇬🇧 English"
                ).forEach { (code, name) ->
                    SelectableRow(
                        text = name,
                        isSelected = uiState.language == code,
                        onClick = { viewModel.setLanguage(code) }
                    )
                }
            }

            // Package labels confirmed on results (stored on this device only)
            SettingsSection(title = stringResource(R.string.settings_claims_title), icon = Icons.Default.Eco) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_claims_text, uiState.packageClaimCount),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = viewModel::clearPackageClaims,
                        enabled = uiState.packageClaimCount > 0
                    ) {
                        Text(stringResource(R.string.settings_claims_clear))
                    }
                }
            }

            // Test phase: scans and feedback go to the developer as a JSON file
            TestingSection(viewModel)

            // About
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_about),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.settings_about_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.settings_data_sources),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.settings_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = Amber600
                    )
                }
            }
        }
    }
}

@Composable
private fun TestingSection(viewModel: SettingsViewModel) {
    val state by viewModel.exportState.collectAsState()
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.export_share_title)
    LaunchedEffect(state.file) {
        val file = state.file ?: return@LaunchedEffect
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, shareTitle))
        viewModel.onExportShared()
    }
    // Local text while typing; DataStore only provides the initial value
    var typedName by remember { mutableStateOf<String?>(null) }

    SettingsSection(title = stringResource(R.string.settings_testing_title), icon = Icons.Default.Science) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = typedName ?: state.testerName,
                onValueChange = {
                    typedName = it
                    viewModel.setTesterName(it)
                },
                label = { Text(stringResource(R.string.settings_tester_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.settings_testing_text, state.productCount, state.feedbackCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = viewModel::export,
                enabled = !state.isExporting && state.productCount + state.feedbackCount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.settings_export), modifier = Modifier.padding(start = 8.dp))
            }
            if (state.failed) {
                Text(
                    text = stringResource(R.string.settings_export_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Emerald500
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun MadhabOption(
    madhab: Madhab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = madhab.displayName(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = madhab.displayNameAr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.option_selected),
                tint = Emerald500
            )
        }
    }
}

@Composable
private fun SelectableRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.option_selected),
                tint = Emerald500
            )
        }
    }
}
