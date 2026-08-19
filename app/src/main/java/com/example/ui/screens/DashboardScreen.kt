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

            // 2. Permission / PIN Alert Banners if missing
            if (!isUsageGranted || !isOverlayGranted) {
                item {
                    PermissionAlertBanner(
                        isUsageGranted = isUsageGranted,
                        isOverlayGranted = isOverlayGranted,
                        onFix = onNavigateToPermissions
                    )
                }
            }

            if (!hasMasterPin) {
                item {
                    PinAlertBanner(onSetupPin = { showSetupPinDialog = true })
                }
            }

            // 3. Overview Metric Chips
            item {
                MetricSummaryRow(
                    monitoredCount = trackedApps.size,
                    lockedCount = lockedCount,
                    overrideCount = overrideCount
                )
            }

            // 4. Header for Tracked Apps
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Monitored Applications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (trackedApps.isEmpty()) "No apps tracked yet" else "${trackedApps.size} active limits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (trackedApps.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onNavigateToAppPicker,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_more_apps_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add App", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // 5. Tracked Apps List or Empty State
            if (trackedApps.isEmpty()) {
                item {
                    EmptyTrackedAppsCard(onAddApps = onNavigateToAppPicker)
                }
            } else {
                items(trackedApps, key = { it.entity.packageName }) { appWithUsage ->
                    TrackedAppCard(
                        appWithUsage = appWithUsage,
                        onCardClick = { onNavigateToAppDetail(appWithUsage.entity.packageName) },
                        onEmergencyOverrideClick = {
                            if (!hasMasterPin) {
                                showSetupPinDialog = true
                            } else {
                                overrideTargetApp = appWithUsage
                            }
                        },
                        onManualUnlock = { viewModel.manualUnlockApp(appWithUsage.entity.packageName) },
                        onSimulateLock = { viewModel.simulateTriggerLock(appWithUsage.entity) }
                    )
                }
            }
        }

        // FAB to add apps
        ExtendedFloatingActionButton(
            onClick = onNavigateToAppPicker,
            icon = { Icon(Icons.Default.Add, contentDescription = "Add Apps") },
            text = { Text("Track App") },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("dashboard_fab_add_app")
        )
    }

    // PIN Setup Dialog
    if (showSetupPinDialog) {
        SetupPinDialog(
            onDismiss = { showSetupPinDialog = false },
            onPinSet = { pin ->
                viewModel.setMasterPin(pin)
                showSetupPinDialog = false
            }
        )
    }

    // Emergency Override PIN Dialog
    overrideTargetApp?.let { app ->
        EmergencyOverridePinDialog(
            appName = app.entity.appName,
            durationMinutes = viewModel.getOverrideDuration(),
            onDismiss = { overrideTargetApp = null },
            onVerify = { pin -> viewModel.verifyMasterPin(pin) },
            onSuccess = {
                viewModel.grantEmergencyOverride(app.entity.packageName)
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
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isServiceRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isServiceRunning) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = if (isServiceRunning) "Limit Guard Active" else "Limit Guard Paused",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isServiceRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isServiceRunning) "Enforcing launch frequency & screen time" else "Turn on to protect your focus limits",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isServiceRunning) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = isServiceRunning,
                onCheckedChange = { enabled ->
                    if (enabled && !hasPermissions) {
                        onGrantPermissions()
                    } else {
                        onToggle(enabled)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("service_toggle_switch")
            )
        }
    }
}

@Composable
private fun MetricSummaryRow(
    monitoredCount: Int,
    lockedCount: Int,
    overrideCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricItem(
            title = "Monitored",
            value = "$monitoredCount",
            subtitle = "Apps configured",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        MetricItem(
            title = "Lockdowns",
            value = "$lockedCount",
            subtitle = if (lockedCount > 0) "Limits breached" else "No breaches",
            containerColor = if (lockedCount > 0) LockdownRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (lockedCount > 0) LockdownRed else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        MetricItem(
            title = "Overrides",
            value = "$overrideCount",
            subtitle = "Emergency passes",
            containerColor = if (overrideCount > 0) OverridePurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (overrideCount > 0) OverridePurple else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricItem(
    title: String,
    value: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TrackedAppCard(
    appWithUsage: TrackedAppWithUsage,
    onCardClick: () -> Unit,
    onEmergencyOverrideClick: () -> Unit,
    onManualUnlock: () -> Unit,
    onSimulateLock: () -> Unit
) {
    val entity = appWithUsage.entity
    val usage = appWithUsage.usage
    val screenTimeMinutes = (usage.screenTimeMillisToday / (60 * 1000)).toInt()

    val isLocked = entity.isLocked || appWithUsage.isFrequencyBreached || appWithUsage.isScreenTimeBreached

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                appWithUsage.isUnderEmergencyOverride -> OverridePurple.copy(alpha = 0.08f)
                isLocked -> LockdownRed.copy(alpha = 0.08f)
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
            // Header Row: Icon, Name, Category, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconView(
                    packageName = entity.packageName,
                    appName = entity.appName,
                    category = entity.category,
                    size = 46.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entity.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entity.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(
                    isLocked = isLocked,
                    isUnderOverride = appWithUsage.isUnderEmergencyOverride,
                    isLimitEnabled = entity.isLimitEnabled
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress 1: Open Frequency Limit
            UsageProgressBar(
                label = "Launch Frequency",
                currentValue = usage.opensInWindow,
                maxValue = entity.maxOpenCount,
                unit = "opens",
                windowDescription = "${entity.openWindowMinutes}m window"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress 2: Screen Time Limit
            UsageProgressBar(
                label = "Daily Screen Time",
                currentValue = screenTimeMinutes,
                maxValue = entity.maxScreenTimeMinutes,
                unit = "min",
                windowDescription = "today"
            )

            // Lockdown or Override Alert State Box
            if (isLocked && !appWithUsage.isUnderEmergencyOverride) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LockdownRed.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = LockdownRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (appWithUsage.lockRemainingSeconds > 0) {
                                    "Lockdown: ${appWithUsage.lockRemainingSeconds / 60}m remaining"
                                } else "Lockdown Active",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = LockdownRed
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = onEmergencyOverrideClick,
                                colors = ButtonDefaults.buttonColors(containerColor = OverridePurple),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("card_override_button_${entity.packageName}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Emergency PIN", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            } else if (appWithUsage.isUnderEmergencyOverride) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OverridePurple.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HourglassBottom,
                                contentDescription = null,
                                tint = OverridePurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val minutes = appWithUsage.overrideRemainingSeconds / 60
                            val seconds = appWithUsage.overrideRemainingSeconds % 60
                            Text(
                                text = "Emergency Override: %02d:%02d".format(minutes, seconds),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = OverridePurple
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onSimulateLock,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("test_lock_button_${entity.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simulate Lock Test", style = MaterialTheme.typography.labelSmall)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCardClick() }
                ) {
                    Text(
                        text = "Edit Limits",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
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
                text = "No Apps Monitored",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select apps from your device to enforce open frequency and screen time limits.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAddApps,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("empty_state_add_apps_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Apps to Track")
            }
        }
    }
}

@Composable
private fun PermissionAlertBanner(
    isUsageGranted: Boolean,
    isOverlayGranted: Boolean,
    onFix: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFix() }
            .testTag("permission_alert_banner")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = WarningOrange,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "System Permissions Needed",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val missingList = mutableListOf<String>()
                if (!isUsageGranted) missingList.add("Usage Access")
                if (!isOverlayGranted) missingList.add("Display Over Other Apps")
                Text(
                    text = "Grant ${missingList.joinToString(" & ")} to track limits & show lock screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = WarningOrange
            )
        }
    }
}

@Composable
private fun PinAlertBanner(onSetupPin: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSetupPin() }
            .testTag("pin_setup_banner")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Set Master Security PIN",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Configure a 4-digit PIN for Emergency Overrides during lockdowns.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Button(
                onClick = onSetupPin,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("banner_setup_pin_button")
            ) {
                Text("Setup", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
