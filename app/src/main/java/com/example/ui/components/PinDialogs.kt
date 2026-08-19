package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LockdownRed
import com.example.ui.theme.OverridePurple
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PinDots(
    pinLength: Int,
    currentLength: Int,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until pinLength) {
            val isFilled = i < currentLength
            val dotColor = when {
                isError -> LockdownRed
                isFilled -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in digits) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (item in row) {
                    when (item) {
                        "" -> {
                            Box(modifier = Modifier.size(68.dp))
                        }
                        "DEL" -> {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier
                                    .size(68.dp)
                                    .testTag("pin_delete_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        else -> {
                            Surface(
                                onClick = { onDigitClick(item) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(68.dp)
                                    .testTag("pin_digit_$item")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(68.dp)
                                ) {
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupPinDialog(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Enter, 2: Confirm
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val currentPin = if (step == 1) firstPin else confirmPin

    fun triggerShake() {
        scope.launch {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(20f, animationSpec = tween(50))
            shakeOffset.animateTo(-20f, animationSpec = tween(50))
            shakeOffset.animateTo(15f, animationSpec = tween(50))
            shakeOffset.animateTo(-15f, animationSpec = tween(50))
            shakeOffset.animateTo(0f, animationSpec = tween(50))
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (step == 1) "Create Master PIN" else "Confirm Master PIN",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (step == 1)
                        "Set a 4-digit Master Security PIN for Emergency Overrides and limit protection."
                    else
                        "Re-enter your 4-digit PIN to confirm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                PinDots(
                    pinLength = 4,
                    currentLength = currentPin.length,
                    isError = errorMessage != null
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = LockdownRed,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                PinKeypad(
                    onDigitClick = { digit ->
                        if (currentPin.length < 4) {
                            errorMessage = null
                            val updated = currentPin + digit
                            if (step == 1) {
                                firstPin = updated
                                if (updated.length == 4) {
                                    step = 2
                                }
                            } else {
                                confirmPin = updated
                                if (updated.length == 4) {
                                    if (updated == firstPin) {
                                        onPinSet(updated)
                                    } else {
                                        errorMessage = "PINs do not match. Try again."
                                        triggerShake()
                                        confirmPin = ""
                                    }
                                }
                            }
                        }
                    },
                    onDeleteClick = {
                        errorMessage = null
                        if (step == 1 && firstPin.isNotEmpty()) {
                            firstPin = firstPin.dropLast(1)
                        } else if (step == 2) {
                            if (confirmPin.isNotEmpty()) {
                                confirmPin = confirmPin.dropLast(1)
                            } else {
                                step = 1
                                firstPin = ""
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("pin_cancel_button")
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun EmergencyOverridePinDialog(
    appName: String,
    durationMinutes: Int,
    onDismiss: () -> Unit,
    onVerify: (String) -> Boolean,
    onSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun triggerShake() {
        scope.launch {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(20f, animationSpec = tween(50))
            shakeOffset.animateTo(-20f, animationSpec = tween(50))
            shakeOffset.animateTo(15f, animationSpec = tween(50))
            shakeOffset.animateTo(-15f, animationSpec = tween(50))
            shakeOffset.animateTo(0f, animationSpec = tween(50))
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(OverridePurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = OverridePurple,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Emergency Override",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enter Master PIN to grant $durationMinutes minutes of emergency access to $appName.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                PinDots(
                    pinLength = 4,
                    currentLength = enteredPin.length,
                    isError = errorMessage != null
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = LockdownRed,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                PinKeypad(
                    onDigitClick = { digit ->
                        if (enteredPin.length < 4) {
                            errorMessage = null
                            val updated = enteredPin + digit
                            enteredPin = updated
                            if (updated.length == 4) {
                                val isCorrect = onVerify(updated)
                                if (isCorrect) {
                                    onSuccess()
                                } else {
                                    errorMessage = "Incorrect Master PIN. Try again."
                                    triggerShake()
                                    enteredPin = ""
                                }
                            }
                        }
                    },
                    onDeleteClick = {
                        errorMessage = null
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("override_cancel_button")
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
