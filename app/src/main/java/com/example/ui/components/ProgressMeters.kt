package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LockdownRed
import com.example.ui.theme.OverridePurple
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange

@Composable
fun UsageProgressBar(
    current: Int = 0,
    max: Int = 0,
    currentValue: Int = current,
    maxValue: Int = max,
    label: String,
    unit: String,
    windowDescription: String = "",
    subLabel: String = "",
    modifier: Modifier = Modifier
) {
    val actualCurrent = if (currentValue != 0) currentValue else current
    val actualMax = if (maxValue != 0) maxValue else max
    val progress = if (actualMax > 0) (actualCurrent.toFloat() / actualMax.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    val progressColor = when {
        progress >= 1.0f -> LockdownRed
        progress >= 0.75f -> WarningOrange
        else -> MaterialTheme.colorScheme.primary
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (windowDescription.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "($windowDescription)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (subLabel.isNotEmpty()) {
                    Text(
                        text = subLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Text(
                text = "$actualCurrent / $actualMax $unit",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedProgress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(progressColor)
            )
        }
    }
}

@Composable
fun StatusBadge(
    isLocked: Boolean,
    isUnderOverride: Boolean,
    isEnabled: Boolean = true,
    isLimitEnabled: Boolean = isEnabled,
    overrideSecondsLeft: Long = 0L,
    lockSecondsLeft: Long = 0L,
    modifier: Modifier = Modifier
) {
    val effectiveLimitEnabled = if (isLimitEnabled) isEnabled else isLimitEnabled
    val (bgColor, contentColor, text, icon) = when {
        !effectiveLimitEnabled -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Paused",
            Icons.Default.LockOpen
        )
        isUnderOverride -> {
            val mins = (overrideSecondsLeft / 60).toInt()
            val textDisplay = if (mins > 0) "Override ($mins m)" else "Override Active"
            Quadruple(
                OverridePurple.copy(alpha = 0.15f),
                OverridePurple,
                textDisplay,
                Icons.Default.HourglassBottom
            )
        }
        isLocked -> {
            val mins = (lockSecondsLeft / 60).toInt()
            val textDisplay = if (mins > 0) "Locked ($mins m)" else "Locked Down"
            Quadruple(
                LockdownRed.copy(alpha = 0.15f),
                LockdownRed,
                textDisplay,
                Icons.Default.Lock
            )
        }
        else -> Quadruple(
            SuccessGreen.copy(alpha = 0.15f),
            SuccessGreen,
            "Protected",
            Icons.Default.Lock
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
