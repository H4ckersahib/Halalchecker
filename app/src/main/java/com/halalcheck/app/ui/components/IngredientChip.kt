package com.halalcheck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.halalcheck.app.R
import com.halalcheck.app.domain.model.AnalyzedIngredient
import com.halalcheck.app.domain.model.Ingredient
import com.halalcheck.app.domain.model.Reason

/**
 * A detailed ingredient row for the result screen.
 */
@Composable
fun IngredientDetailRow(
    analyzedIngredient: AnalyzedIngredient,
    modifier: Modifier = Modifier
) {
    val reason = analyzedIngredient.reason?.text()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(analyzedIngredient.status.backgroundColor().copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = analyzedIngredient.status.icon(),
            contentDescription = null,
            tint = analyzedIngredient.status.contentColor(),
            modifier = Modifier.size(20.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = analyzedIngredient.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = analyzedIngredient.status.contentColor()
            )
            if (reason != null) {
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = analyzedIngredient.status.contentColor().copy(alpha = 0.8f)
                )
            }
            val references = analyzedIngredient.ingredient?.references.orEmpty()
            if (references.isNotEmpty()) {
                val uriHandler = LocalUriHandler.current
                val german = LocalConfiguration.current.locales[0].language == "de"
                Text(
                    text = stringResource(R.string.reason_sources),
                    style = MaterialTheme.typography.labelSmall,
                    color = analyzedIngredient.status.contentColor().copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                references.forEach { reference ->
                    Text(
                        text = (if (german) reference.titleDe else null) ?: reference.title,
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                        color = analyzedIngredient.status.contentColor(),
                        modifier = Modifier.clickable { uriHandler.openUri(reference.url) }
                    )
                }
            }
        }

        HalalStatusBadge(
            status = analyzedIngredient.status,
            showLabel = false,
            size = BadgeSize.SMALL
        )
    }
}

@Composable
private fun Reason.text(): String? = when (this) {
    is Reason.Database -> ingredient.localizedReason()
    is Reason.Inferred -> withReason(stringResource(R.string.reason_inferred, ingredient.localizedName()), ingredient)
    is Reason.NotSpecified -> ingredient.localizedReason() ?: stringResource(R.string.reason_not_specified)
    is Reason.OffAdditive -> withReason(stringResource(R.string.reason_off_additive, ingredient.localizedName()), ingredient)
    is Reason.HaramQualifier -> stringResource(R.string.reason_haram_qualifier, qualifier)
    is Reason.AnimalQualifier -> stringResource(R.string.reason_animal_qualifier, qualifier)
    is Reason.HalalSourceQualifier -> stringResource(R.string.reason_halal_source_qualifier, qualifier)
    Reason.VeganLabel -> stringResource(R.string.reason_vegan_label)
    Reason.HalalLabel -> stringResource(R.string.reason_halal_label)
}

@Composable
private fun withReason(prefix: String, ingredient: Ingredient): String =
    ingredient.localizedReason()?.let { "$prefix – $it" } ?: prefix

@Composable
private fun Ingredient.localizedReason(): String? {
    val german = LocalConfiguration.current.locales[0].language == "de"
    val text = if (german) reasonDe ?: reason else reason ?: reasonDe
    return text ?: when (source) {
        "off_vegan" -> stringResource(R.string.source_off_vegan)
        "off_vegetarian" -> stringResource(R.string.source_off_vegetarian)
        "off_maybe" -> stringResource(R.string.source_off_maybe)
        "off_animal" -> stringResource(R.string.source_off_animal)
        else -> null
    }
}

@Composable
private fun Ingredient.localizedName(): String = when (LocalConfiguration.current.locales[0].language) {
    "de" -> nameDe
    "tr" -> nameTr ?: nameEn
    "ar" -> nameAr ?: nameEn
    "fr" -> nameFr ?: nameEn
    else -> nameEn
}
