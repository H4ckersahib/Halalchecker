package com.halalcheck.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.halalcheck.app.R
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.ui.theme.*

/**
 * Badge showing the halal status with icon, color, and optional text.
 */
@Composable
fun HalalStatusBadge(
    status: HalalStatus,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    size: BadgeSize = BadgeSize.MEDIUM
) {
    val backgroundColor by animateColorAsState(
        targetValue = status.backgroundColor(),
        animationSpec = tween(300),
        label = "badge_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = status.contentColor(),
        animationSpec = tween(300),
        label = "badge_content"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(size.cornerRadius))
            .background(backgroundColor)
            .padding(horizontal = size.horizontalPadding, vertical = size.verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(size.spacing)
    ) {
        Icon(
            imageVector = status.icon(),
            contentDescription = status.label(),
            tint = contentColor,
            modifier = Modifier.size(size.iconSize)
        )
        if (showLabel) {
            Text(
                text = status.label(),
                color = contentColor,
                style = if (size == BadgeSize.LARGE) MaterialTheme.typography.titleMedium
                       else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Large status display for the result screen.
 */
@Composable
fun HalalStatusLarge(
    status: HalalStatus,
    modifier: Modifier = Modifier,
    description: String = status.description()
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(status.backgroundColor()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = status.icon(),
                contentDescription = status.label(),
                tint = status.contentColor(),
                modifier = Modifier.size(48.dp)
            )
        }
        Text(
            text = status.label(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = status.textColor()
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

enum class BadgeSize(
    val iconSize: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val cornerRadius: Dp,
    val spacing: Dp
) {
    SMALL(14.dp, 8.dp, 4.dp, 8.dp, 4.dp),
    MEDIUM(18.dp, 12.dp, 6.dp, 12.dp, 6.dp),
    LARGE(24.dp, 16.dp, 10.dp, 16.dp, 8.dp)
}

// Extension functions for HalalStatus UI properties

fun HalalStatus.icon(): ImageVector = when (this) {
    HalalStatus.HALAL -> Icons.Default.Check
    HalalStatus.HARAM -> Icons.Default.Close
    HalalStatus.MASHBOOH -> Icons.Default.Warning
    HalalStatus.UNKNOWN -> Icons.Default.QuestionMark
}

fun HalalStatus.backgroundColor(): Color = when (this) {
    HalalStatus.HALAL -> Emerald100
    HalalStatus.HARAM -> Coral100
    HalalStatus.MASHBOOH -> Amber100
    HalalStatus.UNKNOWN -> Slate200
}

fun HalalStatus.contentColor(): Color = when (this) {
    HalalStatus.HALAL -> Emerald700
    HalalStatus.HARAM -> Coral700
    HalalStatus.MASHBOOH -> Amber700
    HalalStatus.UNKNOWN -> Slate600
}

fun HalalStatus.textColor(): Color = when (this) {
    HalalStatus.HALAL -> Emerald600
    HalalStatus.HARAM -> Coral600
    HalalStatus.MASHBOOH -> Amber600
    HalalStatus.UNKNOWN -> Slate500
}

@Composable
fun HalalStatus.label(): String = stringResource(
    when (this) {
        HalalStatus.HALAL -> R.string.status_halal
        HalalStatus.HARAM -> R.string.status_haram
        HalalStatus.MASHBOOH -> R.string.status_mashbooh
        HalalStatus.UNKNOWN -> R.string.status_unknown
    }
)

@Composable
fun HalalStatus.description(): String = stringResource(
    when (this) {
        HalalStatus.HALAL -> R.string.status_halal_desc
        HalalStatus.HARAM -> R.string.status_haram_desc
        HalalStatus.MASHBOOH -> R.string.status_mashbooh_desc
        HalalStatus.UNKNOWN -> R.string.status_unknown_desc
    }
)

fun HalalStatus.emoji(): String = when (this) {
    HalalStatus.HALAL -> "✅"
    HalalStatus.HARAM -> "❌"
    HalalStatus.MASHBOOH -> "⚠️"
    HalalStatus.UNKNOWN -> "❓"
}
