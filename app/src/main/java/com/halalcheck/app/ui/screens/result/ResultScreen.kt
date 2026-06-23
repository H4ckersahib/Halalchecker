package com.halalcheck.app.ui.screens.result

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.Madhab
import com.halalcheck.app.ui.components.HalalStatusBadge
import com.halalcheck.app.ui.components.HalalStatusLarge
import com.halalcheck.app.ui.components.IngredientDetailRow
import com.halalcheck.app.ui.components.BadgeSize
import com.halalcheck.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(
    onNavigateBack: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ergebnis") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Emerald500,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Produkt wird analysiert...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "⚠️",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = uiState.error ?: "Unbekannter Fehler",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.retry() },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text("Erneut versuchen", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            uiState.product != null -> {
                val product = uiState.product!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 32.dp
                    )
                ) {
                    // Status hero
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Product image
                                if (product.imageUrl != null) {
                                    AsyncImage(
                                        model = product.imageUrl,
                                        contentDescription = product.name,
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(16.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                // Product name
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                if (product.brand != null) {
                                    Text(
                                        text = product.brand,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Large status display
                                HalalStatusLarge(status = product.halalStatus)

                                // Confidence indicator
                                if (product.confidence > 0) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { product.confidence },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = Emerald500,
                                            trackColor = Slate200
                                        )
                                        Text(
                                            text = "Sicherheit: ${(product.confidence * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Madhab selector
                    item {
                        MadhabSelector(
                            selectedMadhab = uiState.madhab,
                            onMadhabSelected = { viewModel.changeMadhab(it) }
                        )
                    }

                    // Ingredients analysis
                    if (product.analyzedIngredients.isNotEmpty()) {
                        item {
                            Text(
                                text = "Zutatenanalyse (${product.analyzedIngredients.size} Zutaten)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Problematic ingredients first
                        val sorted = product.analyzedIngredients.sortedBy {
                            when (it.status) {
                                HalalStatus.HARAM -> 0
                                HalalStatus.MASHBOOH -> 1
                                HalalStatus.UNKNOWN -> 2
                                HalalStatus.HALAL -> 3
                            }
                        }

                        items(sorted) { ingredient ->
                            IngredientDetailRow(analyzedIngredient = ingredient)
                        }
                    }

                    // Unknown ingredients warning
                    if (product.unknownIngredients.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Amber50
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "⚠️ Unbekannte Zutaten",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Amber700
                                    )
                                    Text(
                                        text = product.unknownIngredients.joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Amber600
                                    )
                                    Text(
                                        text = "Diese Zutaten wurden nicht in unserer Datenbank gefunden. Bei Unsicherheit empfehlen wir, einen Gelehrten zu konsultieren.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Amber600
                                    )
                                }
                            }
                        }
                    }

                    // Raw ingredients text
                    if (product.ingredientsText.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "📋 Originale Zutatenliste",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = product.ingredientsText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Disclaimer
                    item {
                        Text(
                            text = "⚠️ Diese Analyse ersetzt keine religiöse Beratung. Bei Zweifeln konsultiere bitte einen Gelehrten.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MadhabSelector(
    selectedMadhab: Madhab,
    onMadhabSelected: (Madhab) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Rechtsschule (Madhab)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedMadhab.displayNameDe,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ändern")
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    Madhab.entries.forEach { madhab ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(madhab.displayNameDe)
                                    if (madhab == selectedMadhab) {
                                        Text("✓", color = Emerald500)
                                    }
                                }
                            },
                            onClick = {
                                onMadhabSelected(madhab)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
