package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TrackedAppEntity
import com.example.data.repository.TrackedAppWithUsage
import com.example.ui.components.AppIconView
import com.example.ui.components.EmergencyOverridePinDialog
import com.example.ui.components.SetupPinDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.UsageProgressBar
import com.example.ui.theme.LockdownRed
import com.example.ui.theme.OverridePurple
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToAppPicker: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAppDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackedApps by viewModel.trackedApps.collectAsStateWithLifecycle()
    val isUsageGranted by viewModel.isUsagePermissionGranted.collectAsStateWithLifecycle()
    val isOverlayGranted by viewModel.isOverlayPermissionGranted.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val hasMasterPin by viewModel.hasMasterPin.collectAsStateWithLifecycle()

    var showSetupPinDialog by remember { mutableStateOf(false) }
    var overrideTargetApp by remember { mutableStateOf<TrackedAppWithUsage?>(null) }

    val lockedCount = trackedApps.count { it.entity.isLocked || it.isFrequencyBreached || it.isScreenTimeBreached }
    val overrideCount = trackedApps.count { it.isUnderEmergencyOverride }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Service Status Card
            item {
                ServiceControlCard(
                    isServiceRunning = isServiceRunning,
                    onToggle = { viewModel.toggleMonitoringService(it) },
                    hasPermissions = isUsageGranted && isOverlayGranted,
                    onGrantPermissions = onNavigateToPermissions
                )
            }

            // 2. Permission Alert Banner if missing
            if (!isUsageGranted || !isOverlayGranted) {
                item {
                    PermissionAlertBanner(
                        isUsageGranted = isUsageGranted,
                        isOverlayGranted = isOverlayGranted,
                        onOpenPermissions = onNavigateToPermissions
                    )
                }
            }

            // 3. PIN Setup prompt if not set
            if (!hasMasterPin) {
                item {
                    PinPromptCard(onSetupPin = { showSetupPinDialog = true })
                }
            }

            // 4. Quick Stats Metrics
            item {
                DashboardMetricsRow(
                    trackedCount = trackedApps.size,
                    lockedCount = lockedCount,
                    overrideCount = overrideCount
                )
            }

            // 5. Tracked Apps List or Empty State
            if (trackedApps.isEmpty()) {
                item {
                    EmptyTrackedAppsCard(onAddApps = onNavigateToAppPicker)
                }
            } else {
                items(
                    items = trackedApps,
                    key = { it.entity.packageName }
                ) { appWithUsage ->
                    TrackedAppCard(
                        app = appWithUsage,
                        onCardClick = { onNavigateToAppDetail(appWithUsage.entity.packageName) },
                        onToggleLimit = { enabled ->
                            viewModel.toggleAppLimit(appWithUsage.entity.packageName, enabled)
                        },
                        onSimulateLock = {
                            viewModel.simulateTriggerLock(appWithUsage.entity)
                        },
                        onUnlock = {
                            viewModel.manualUnlockApp(appWithUsage.entity.packageName)
                        },
                        onEmergencyOverride = {
                            if (hasMasterPin) {
                                overrideTargetApp = appWithUsage
                            } else {
                                showSetupPinDialog = true
                            }
                        }
                    )
                }
            }
        }

        // Floating Action Button (+ Track App)
        ExtendedFloatingActionButton(
            onClick = onNavigateToAppPicker,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Track App", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp)
                .testTag("fab_track_app")
        )
    }

    if (showSetupPinDialog) {
        SetupPinDialog(
            onDismiss = { showSetupPinDialog = false },
            onPinSet = { pin ->
                viewModel.setMasterPin(pin)
                showSetupPinDialog = false
            }
        )
    }

    overrideTargetApp?.let { target ->
        EmergencyOverridePinDialog(
            appName = target.entity.appName,
            durationMinutes = viewModel.getOverrideDuration(),
            onDismiss = { overrideTargetApp = null },
            onVerify = { pin -> viewModel.verifyMasterPin(pin) },
            onSuccess = {
                viewModel.grantEmergencyOverride(target.entity.packageName)
                overrideTargetApp = null
            }
        )
    }
}

@Composable
private fun ServiceControlCard(
    isServiceRunning: Boolean,
    onToggle: (Boolean) -> Unit,
    hasPermissions: Boolean,
    onGrantPermissions: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isServiceRunning && hasPermissions)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isServiceRunning && hasPermissions)
                                SuccessGreen.copy(alpha = 0.2f)
                            else
                                WarningOrange.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isServiceRunning && hasPermissions) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = if (isServiceRunning && hasPermissions) SuccessGreen else WarningOrange,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = if (isServiceRunning && hasPermissions) "ScrMngr Guard Active" else "Monitor Guard Paused",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isServiceRunning && hasPermissions)
                            "Fixed-window limiter & barrier active"
                        else
                            "Real-time enforcement paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = isServiceRunning && hasPermissions,
                onCheckedChange = { checked ->
                    if (!hasPermissions) {
                        onGrantPermissions()
                    } else {
                        onToggle(checked)
                    }
                },
                thumbContent = if (isServiceRunning && hasPermissions) {
                    {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                } else null,
                modifier = Modifier.testTag("service_status_switch")
            )
        }
    }
}

@Composable
private fun PermissionAlertBanner(
    isUsageGranted: Boolean,
    isOverlayGranted: Boolean,
    onOpenPermissions: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = WarningOrange,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "System Setup Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        !isUsageGranted && !isOverlayGranted -> "Usage Stats and Overlay permissions are missing."
                        !isUsageGranted -> "Usage Access permission is required for launch tracking."
                        else -> "Overlay permission is required for lock barriers."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = onOpenPermissions,
                modifier = Modifier.testTag("banner_grant_permissions_button")
            ) {
                Text("Setup", fontWeight = FontWeight.Bold, color = WarningOrange)
            }
        }
    }
}

@Composable
private fun PinPromptCard(onSetupPin: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = OverridePurple.copy(alpha = 0.12f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OverridePurple.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = OverridePurple,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Master Security PIN",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Set a 4-digit PIN for emergency temporary access overrides.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onSetupPin,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("dashboard_setup_pin_button")
            ) {
                Text("Set PIN", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DashboardMetricsRow(
    trackedCount: Int,
    lockedCount: Int,
    overrideCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricTile(
            title = "Tracked",
            value = "$trackedCount",
            icon = Icons.Default.Timer,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        MetricTile(
            title = "Lockdown",
            value = "$lockedCount",
            icon = Icons.Default.Lock,
            tint = if (lockedCount > 0) LockdownRed else SuccessGreen,
            modifier = Modifier.weight(1f)
        )

        MetricTile(
            title = "Overrides",
            value = "$overrideCount",
            icon = Icons.Default.HourglassBottom,
            tint = OverridePurple,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TrackedAppCard(
    app: TrackedAppWithUsage,
    onCardClick: () -> Unit,
    onToggleLimit: (Boolean) -> Unit,
    onSimulateLock: () -> Unit,
    onUnlock: () -> Unit,
    onEmergencyOverride: () -> Unit
) {
    val entity = app.entity
    val usage = app.usage
    val isLocked = entity.isLocked || app.isFrequencyBreached || app.isScreenTimeBreached
    val isOverride = app.isUnderEmergencyOverride

    val screenTimeMinutes = (usage.screenTimeMillisToday / (60 * 1000)).toInt()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOverride -> OverridePurple.copy(alpha = 0.08f)
                isLocked -> LockdownRed.copy(alpha = 0.08f)
                !entity.isLimitEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("tracked_app_card_${entity.packageName}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Icon, Name, Category, Status Badge & Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconView(packageName = entity.packageName, size = 46.dp)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entity.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = entity.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        StatusBadge(
                            isLocked = isLocked,
                            isUnderOverride = isOverride,
                            isEnabled = entity.isLimitEnabled,
                            overrideSecondsLeft = app.overrideRemainingSeconds,
                            lockSecondsLeft = app.lockRemainingSeconds
                        )
                    }
                }

                Switch(
                    checked = entity.isLimitEnabled,
                    onCheckedChange = onToggleLimit,
                    modifier = Modifier.testTag("switch_app_limit_${entity.packageName}")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Launch Frequency Progress (Fixed Window)
            if (entity.isFrequencyLimitEnabled) {
                val resetRemainingMin = (usage.windowResetRemainingSeconds / 60).toInt()
                val resetRemainingSec = (usage.windowResetRemainingSeconds % 60).toInt()
                val resetFormatted = String.format("%02d:%02d", resetRemainingMin, resetRemainingSec)

                UsageProgressBar(
                    current = usage.opensInWindow,
                    max = entity.maxOpenCount,
                    label = "Launch Frequency (Fixed ${entity.openWindowMinutes}m window)",
                    unit = "opens",
                    subLabel = "Resets in $resetFormatted"
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 2. Screen Time Progress
            if (entity.isScreenTimeLimitEnabled) {
                UsageProgressBar(
                    current = screenTimeMinutes,
                    max = entity.maxScreenTimeMinutes,
                    label = "Daily Screen Time Limit",
                    unit = "min"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Quick Actions Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLocked) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onEmergencyOverride,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OverridePurple),
                            modifier = Modifier.testTag("btn_override_${entity.packageName}")
                        ) {
                            Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Emergency Pass", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onUnlock,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unlock", fontSize = 12.sp)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onSimulateLock,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Barrier", fontSize = 11.sp)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCardClick() }
                ) {
                    Text(
                        text = "Edit Rules",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTrackedAppsCard(onAddApps: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Monitored Apps Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tap the '+ Track App' button below to choose apps from your device and set custom open frequency & screen time limits.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAddApps,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("empty_state_add_app_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select Apps to Track", fontWeight = FontWeight.Bold)
            }
        }
    }
}
