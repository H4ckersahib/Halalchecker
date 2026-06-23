package com.halalcheck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halalcheck.app.domain.model.AnalyzedIngredient
import com.halalcheck.app.domain.model.HalalStatus

/**
 * A chip displaying a single ingredient with its halal status color.
 */
@Composable
fun IngredientChip(
    analyzedIngredient: AnalyzedIngredient,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(analyzedIngredient.status.backgroundColor())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = analyzedIngredient.status.icon(),
            contentDescription = null,
            tint = analyzedIngredient.status.contentColor(),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = analyzedIngredient.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = analyzedIngredient.status.contentColor()
        )
    }
}

/**
 * A detailed ingredient row for the result screen.
 */
@Composable
fun IngredientDetailRow(
    analyzedIngredient: AnalyzedIngredient,
    modifier: Modifier = Modifier
) {
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

        androidx.compose.foundation.layout.Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = analyzedIngredient.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = analyzedIngredient.status.contentColor()
            )
            if (analyzedIngredient.reason != null) {
                Text(
                    text = analyzedIngredient.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = analyzedIngredient.status.contentColor().copy(alpha = 0.8f)
                )
            }
        }

        HalalStatusBadge(
            status = analyzedIngredient.status,
            showLabel = false,
            size = BadgeSize.SMALL
        )
    }
}
