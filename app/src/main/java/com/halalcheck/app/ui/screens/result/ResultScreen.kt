package com.halalcheck.app.ui.screens.result

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.halalcheck.app.R
import com.halalcheck.app.data.remote.OpenFoodFactsApi
import com.halalcheck.app.domain.model.DataSource
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.Madhab
import com.halalcheck.app.domain.model.PackageClaim
import com.halalcheck.app.domain.model.Product
import com.halalcheck.app.domain.model.ProductFeedback
import com.halalcheck.app.ui.components.HalalStatusLarge
import com.halalcheck.app.ui.components.IngredientDetailRow
import com.halalcheck.app.ui.components.description
import com.halalcheck.app.ui.components.displayName
import com.halalcheck.app.ui.components.label
import com.halalcheck.app.ui.components.localizedName
import com.halalcheck.app.ui.theme.*
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    onNavigateBack: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val product = uiState.product

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (product != null && !uiState.isLoading && !uiState.isRefreshing) {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.result_refresh))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        when {
            uiState.isLoading -> LoadingContent(contentModifier)
            uiState.error != null -> ErrorContent(uiState.error!!, onRetry = viewModel::retry, modifier = contentModifier)
            product != null && product.source == DataSource.UNKNOWN -> NotFoundContent(uiState.barcode, contentModifier)
            product != null -> ProductContent(
                product = product,
                madhab = uiState.madhab,
                isRefreshing = uiState.isRefreshing,
                refreshError = uiState.refreshError,
                onMadhabSelected = viewModel::changeMadhab,
                onClaimChanged = viewModel::setPackageClaim,
                feedback = uiState.feedback,
                onSaveFeedback = viewModel::saveFeedback,
                onDeleteFeedback = viewModel::deleteFeedback,
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = Emerald500,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(R.string.result_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(error: ResultError, onRetry: () -> Unit, modifier: Modifier) {
    CenteredMessage(
        emoji = "⚠️",
        title = null,
        text = stringResource(
            when (error) {
                ResultError.NETWORK -> R.string.result_error_network
                ResultError.RATE_LIMITED -> R.string.result_error_rate_limited
                ResultError.UNEXPECTED -> R.string.result_error_unexpected
            }
        ),
        modifier = modifier
    ) {
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text(stringResource(R.string.result_retry), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun NotFoundContent(barcode: String, modifier: Modifier) {
    val uriHandler = LocalUriHandler.current
    CenteredMessage(
        emoji = "🔍",
        title = stringResource(R.string.result_not_found),
        text = stringResource(R.string.result_not_found_hint, barcode),
        modifier = modifier
    ) {
        Button(
            onClick = { uriHandler.openUri(OpenFoodFactsApi.productPageUrl(barcode)) },
            colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
        ) {
            Text(stringResource(R.string.result_add_on_off))
        }
    }
}

@Composable
private fun CenteredMessage(
    emoji: String,
    title: String?,
    text: String,
    modifier: Modifier,
    action: @Composable () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = emoji, style = MaterialTheme.typography.displayLarge)
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            action()
        }
    }
}

@Composable
private fun ProductContent(
    product: Product,
    madhab: Madhab,
    isRefreshing: Boolean,
    refreshError: ResultError?,
    onMadhabSelected: (Madhab) -> Unit,
    onClaimChanged: (PackageClaim, Boolean) -> Unit,
    feedback: ProductFeedback?,
    onSaveFeedback: (HalalStatus?, String) -> Unit,
    onDeleteFeedback: () -> Unit,
    modifier: Modifier
) {
    val uriHandler = LocalUriHandler.current
    // The language the screen is shown in (German as the fallback for untranslated system languages)
    val uiLocale = Locale.forLanguageTag(stringResource(R.string.ui_language))
    val notice = when {
        refreshError == ResultError.RATE_LIMITED -> stringResource(R.string.result_error_rate_limited)
        refreshError != null -> stringResource(R.string.result_refresh_failed)
        product.offlineSince != null -> stringResource(R.string.result_offline, formatDateTime(product.offlineSince))
        else -> null
    }
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)
    ) {
        if (isRefreshing) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Emerald500)
            }
        }
        if (notice != null) {
            item { NoticeCard(notice) }
        }

        // Status hero
        item {
            StatusCard(product)
        }

        if (product.labelConflict) {
            item {
                LabelConflictCard()
            }
        }

        // Open Food Facts often lacks labels; the user holds the package
        val hasLabels = product.offVegetarian || product.offHalal || product.userClaims.isNotEmpty()
        if (hasLabels || product.analyzedIngredients.any { it.status == HalalStatus.MASHBOOH }) {
            item {
                PackageLabelsCard(product = product, onClaimChanged = onClaimChanged)
            }
        }

        if (product.sourceConflict) {
            item { NoticeCard(stringResource(R.string.result_source_conflict)) }
        }

        if (product.nameMentions.isNotEmpty()) {
            item {
                val names = product.nameMentions.map { it.localizedName() }.distinct().joinToString(", ")
                NoticeCard(stringResource(R.string.result_name_mentions, names))
            }
        }

        // OFF has no list in the app language ("Vann, sukker, ...")
        val listLanguage = product.ingredientsLanguage
        if (product.ingredientsText.isNotEmpty() && listLanguage != null && listLanguage != uiLocale.language) {
            item {
                val languageName = Locale.forLanguageTag(listLanguage).getDisplayLanguage(uiLocale).ifBlank { listLanguage }
                NoticeCard(stringResource(R.string.result_other_language, languageName))
            }
        }

        // Madhab selector
        item {
            MadhabSelector(
                selectedMadhab = madhab,
                onMadhabSelected = onMadhabSelected
            )
        }

        // Ingredients analysis
        if (product.analyzedIngredients.isNotEmpty()) {
            item {
                Text(
                    text = pluralStringResource(
                        R.plurals.result_ingredients_title,
                        product.analyzedIngredients.size,
                        product.analyzedIngredients.size
                    ),
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
                            text = stringResource(R.string.result_unknown_title),
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
                            text = stringResource(R.string.result_unknown_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Amber600
                        )
                    }
                }
            }
        }

        // Label text the parser could not tell from ingredients; listed so nothing disappears silently
        if (product.ignoredText.isNotEmpty()) {
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
                            text = stringResource(R.string.result_ignored_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = product.ignoredText.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.result_ignored_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            text = stringResource(R.string.result_original_ingredients),
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

        // Testers report wrong verdicts; exported from Settings
        item {
            FeedbackCard(feedback = feedback, onSave = onSaveFeedback, onDelete = onDeleteFeedback)
        }

        // Report a wrong result at the data source
        item {
            OutlinedButton(
                onClick = { uriHandler.openUri(OpenFoodFactsApi.productPageUrl(product.barcode)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(
                        if (product.analyzedIngredients.isEmpty()) R.string.result_add_ingredients_on_off else R.string.result_fix_on_off
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Disclaimer and attribution
        item {
            Text(
                text = stringResource(R.string.result_disclaimer) + "\n" + stringResource(R.string.result_attribution),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatusCard(product: Product) {
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
                text = product.name.ifBlank { stringResource(R.string.result_unnamed_product) },
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

            // Large status display; a partly recognized list says how much was checked
            val total = product.analyzedIngredients.size
            val known = total - product.unknownIngredients.size
            val partial = product.halalStatus == HalalStatus.UNKNOWN && known > 0
            HalalStatusLarge(
                status = product.halalStatus,
                description = when {
                    total == 0 -> stringResource(R.string.result_no_ingredients)
                    partial -> pluralStringResource(R.plurals.result_partial, total, known, total)
                    else -> product.halalStatus.description()
                }
            )

            // How much of the list was recognized: a count, so it is not read as "92 % halal"
            if (total > 0 && !partial) {
                val inferred = product.analyzedIngredients.count { it.inferred && it.status != HalalStatus.UNKNOWN }
                Text(
                    text = if (inferred > 0) {
                        pluralStringResource(R.plurals.result_recognized_inferred, total, known, total, inferred)
                    } else {
                        pluralStringResource(R.plurals.result_recognized, total, known, total)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PackageLabelsCard(product: Product, onClaimChanged: (PackageClaim, Boolean) -> Unit) {
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Eco, contentDescription = null, tint = Emerald500)
                Text(
                    text = stringResource(R.string.labels_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            val offLabels = listOfNotNull(
                stringResource(R.string.label_vegan).takeIf { product.offVegan },
                stringResource(R.string.label_vegetarian).takeIf { product.offVegetarian && !product.offVegan },
                stringResource(R.string.label_halal).takeIf { product.offHalal }
            )
            if (offLabels.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.labels_off, offLabels.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (product.offHalal && !product.halalConfirmed) {
                Text(
                    text = stringResource(R.string.labels_off_halal_unverified),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.labels_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    PackageClaim.VEGAN to R.string.label_vegan,
                    PackageClaim.VEGETARIAN to R.string.label_vegetarian,
                    PackageClaim.HALAL to R.string.label_halal
                ).forEach { (claim, label) ->
                    val selected = claim in product.userClaims
                    FilterChip(
                        selected = selected,
                        onClick = { onClaimChanged(claim, !selected) },
                        label = { Text(stringResource(label)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        leadingIcon = if (selected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        }
                    )
                }
            }
            if (product.veganConflicts.isNotEmpty()) {
                ConflictText(stringResource(R.string.labels_conflict_vegan, product.veganConflicts.joinToString(", ")))
            }
            if (product.vegetarianConflicts.isNotEmpty()) {
                ConflictText(stringResource(R.string.labels_conflict_vegetarian, product.vegetarianConflicts.joinToString(", ")))
            }
            if (product.userClaims.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.labels_confirmed_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FeedbackCard(
    feedback: ProductFeedback?,
    onSave: (HalalStatus?, String) -> Unit,
    onDelete: () -> Unit
) {
    var dialogOpen by remember { mutableStateOf(false) }
    if (dialogOpen) {
        FeedbackDialog(
            initial = feedback,
            onDismiss = { dialogOpen = false },
            onSave = { expected, comment ->
                onSave(expected, comment)
                dialogOpen = false
            }
        )
    }
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
                text = stringResource(R.string.feedback_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (feedback == null) {
                Text(
                    text = stringResource(R.string.feedback_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = { dialogOpen = true }, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.feedback_report), modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                val summary = feedback.expectedStatus?.let { stringResource(R.string.feedback_saved, it.label()) }
                    ?: stringResource(R.string.feedback_saved_unknown)
                Text(text = summary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (feedback.comment.isNotBlank()) {
                    Text(
                        text = "„${feedback.comment}“",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { dialogOpen = true }) { Text(stringResource(R.string.feedback_edit)) }
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
                }
            }
        }
    }
}

@Composable
private fun FeedbackDialog(
    initial: ProductFeedback?,
    onDismiss: () -> Unit,
    onSave: (HalalStatus?, String) -> Unit
) {
    // null = "don't know"
    var expected by remember { mutableStateOf(initial?.expectedStatus) }
    var comment by remember { mutableStateOf(initial?.comment.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feedback_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(HalalStatus.HALAL, HalalStatus.MASHBOOH, HalalStatus.HARAM, null).forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { expected = option },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = expected == option, onClick = { expected = option })
                        Text(option?.label() ?: stringResource(R.string.feedback_dont_know))
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(stringResource(R.string.feedback_comment_label)) },
                    placeholder = { Text(stringResource(R.string.feedback_comment_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(expected, comment) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun ConflictText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun NoticeCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Info, contentDescription = null)
            Text(text = text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LabelConflictCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = stringResource(R.string.result_label_conflict),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun formatDateTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, LocalConfiguration.current.locales[0])
        .format(Date(millis))

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
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.result_madhab_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedMadhab.displayName(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.result_madhab_change))
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
                                    Text(madhab.displayName())
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
        // Most ingredients have no school-specific ruling; say so instead of implying more detail
        Text(
            text = stringResource(R.string.result_madhab_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
}
