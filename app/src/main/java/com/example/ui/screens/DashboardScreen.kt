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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.HorizontalDivider
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
import com.example.data.model.AppGroupEntity
import com.example.data.model.TrackedAppEntity
import com.example.data.repository.AppGroupWithUsage
import com.example.data.repository.TrackedAppWithUsage
import com.example.ui.components.AppIconView
import com.example.ui.components.EmergencyOverridePinDialog
import com.example.ui.components.SetupPinDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.UsageProgressBar
import com.example.ui.theme.LockdownRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToAppPicker: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAppDetail: (String) -> Unit,
    onNavigateToAdvanced: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val trackedApps by viewModel.trackedApps.collectAsStateWithLifecycle()
    val appGroups by viewModel.appGroups.collectAsStateWithLifecycle()
    val isUsageGranted by viewModel.isUsagePermissionGranted.collectAsStateWithLifecycle()
    val isOverlayGranted by viewModel.isOverlayPermissionGranted.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val hasMasterPin by viewModel.hasMasterPin.collectAsStateWithLifecycle()

    var showSetupPinDialog by remember { mutableStateOf(false) }
    var overrideTargetPackage by remember { mutableStateOf<String?>(null) }
    var overrideTargetAppName by remember { mutableStateOf("") }

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

            // 5. Active App Groups Section (if any groups exist)
            if (appGroups.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "App Groups",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = onNavigateToAdvanced) {
                            Text("Manage in Advanced", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                items(
                    items = appGroups,
                    key = { "group_${it.group.id}" }
                ) { groupWithUsage ->
                    HomeGroupCard(
                        groupWithUsage = groupWithUsage,
                        onToggleGroup = { enabled ->
                            viewModel.toggleGroupEnabled(groupWithUsage.group, enabled)
                        },
                        onEditGroup = onNavigateToAdvanced,
                        onAppClick = onNavigateToAppDetail,
                        onEmergencyOverrideApp = { appWithUsage ->
                            if (hasMasterPin) {
                                overrideTargetPackage = appWithUsage.entity.packageName
                                overrideTargetAppName = appWithUsage.entity.appName
                            } else {
                                showSetupPinDialog = true
                            }
                        }
                    )
                }
            }

            // 6. Tracked Apps Header
            if (trackedApps.isNotEmpty()) {
                item {
                    Text(
                        text = "Monitored Apps (${trackedApps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 7. Tracked Apps List or Empty State
            if (trackedApps.isEmpty() && appGroups.isEmpty()) {
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
                        onEmergencyOverride = {
                            if (hasMasterPin) {
                                overrideTargetPackage = appWithUsage.entity.packageName
                                overrideTargetAppName = appWithUsage.entity.appName
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
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
                .testTag("fab_track_app"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Track App", fontWeight = FontWeight.Bold)
        }
    }

    if (overrideTargetPackage != null) {
        val duration = viewModel.getOverrideDuration()
        EmergencyOverridePinDialog(
            appName = overrideTargetAppName,
            durationMinutes = duration,
            onDismiss = {
                overrideTargetPackage = null
                overrideTargetAppName = ""
            },
            onVerify = { pin -> viewModel.verifyMasterPin(pin) },
            onSuccess = {
                val pkg = overrideTargetPackage
                if (pkg != null) {
                    viewModel.grantEmergencyOverride(pkg, duration)
                }
                overrideTargetPackage = null
                overrideTargetAppName = ""
            }
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
}

@Composable
fun HomeGroupCard(
    groupWithUsage: AppGroupWithUsage,
    onToggleGroup: (Boolean) -> Unit,
    onEditGroup: () -> Unit,
    onAppClick: (String) -> Unit,
    onEmergencyOverrideApp: (TrackedAppWithUsage) -> Unit,
    modifier: Modifier = Modifier
) {
    val group = groupWithUsage.group
    val memberApps = groupWithUsage.memberAppUsages
    val isTodayActive = groupWithUsage.isTodayScheduleActive

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("group_card_${group.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Group Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = group.iconEmoji.ifEmpty { "📁" },
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isTodayActive) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isTodayActive) "Active Today" else "Schedule Inactive",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isTodayActive) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = group.getActiveDaysSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Switch(
                    checked = group.isEnabled,
                    onCheckedChange = onToggleGroup,
                    modifier = Modifier.testTag("switch_group_${group.id}")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Limits description
            Text(
                text = buildString {
                    append("Individual Rules: ")
                    if (group.isFrequencyLimitEnabled) {
                        append("${group.maxOpenCount} opens / ${group.openWindowMinutes}m")
                    }
                    if (group.isFrequencyLimitEnabled && group.isScreenTimeLimitEnabled) {
                        append(" • ")
                    }
                    if (group.isScreenTimeLimitEnabled) {
                        append("${group.maxScreenTimeMinutes}m daily screen time")
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (memberApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // List of bundled apps with individual progress
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    memberApps.forEach { memberApp ->
                        val screenTimeMin = (memberApp.usage.screenTimeMillisToday / (60 * 1000)).toInt()
                        val isLocked = memberApp.entity.isLocked || memberApp.isFrequencyBreached || memberApp.isScreenTimeBreached

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .clickable { onAppClick(memberApp.entity.packageName) }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIconView(
                                    packageName = memberApp.entity.packageName,
                                    size = 32.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = memberApp.entity.appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${memberApp.usage.opensInWindow}/${group.maxOpenCount} opens • ${screenTimeMin}/${group.maxScreenTimeMinutes}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (memberApp.isUnderEmergencyOverride) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SuccessGreen.copy(alpha = 0.2f),
                                        modifier = Modifier.clickable { onEmergencyOverrideApp(memberApp) }
                                    ) {
                                        Text(
                                            text = "OVERRIDE (${maxOf(1, (memberApp.overrideRemainingSeconds + 59) / 60)}m)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else if (isLocked) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = LockdownRed.copy(alpha = 0.2f),
                                        modifier = Modifier.clickable { onEmergencyOverrideApp(memberApp) }
                                    ) {
                                        Text(
                                            text = "LOCKED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = LockdownRed,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Progress bars
                            if (group.isFrequencyLimitEnabled) {
                                Spacer(modifier = Modifier.height(6.dp))
                                UsageProgressBar(
                                    current = memberApp.usage.opensInWindow,
                                    max = group.maxOpenCount,
                                    label = "Launch Frequency",
                                    unit = "opens"
                                )
                            }
                            if (group.isScreenTimeLimitEnabled) {
                                Spacer(modifier = Modifier.height(6.dp))
                                UsageProgressBar(
                                    current = screenTimeMin,
                                    max = group.maxScreenTimeMinutes,
                                    label = "Screen Time",
                                    unit = "min"
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer action
            val isAnyMemberLocked = memberApps.any { it.entity.isLocked || it.isFrequencyBreached || it.isScreenTimeBreached }
            if (isAnyMemberLocked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Rules Locked",
                        tint = LockdownRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Rules Locked During Lockout",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = LockdownRed
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditGroup() }
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Group Rules",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceControlCard(
    isServiceRunning: Boolean,
    onToggle: (Boolean) -> Unit,
    hasPermissions: Boolean,
    onGrantPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isServiceRunning && hasPermissions)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isServiceRunning && hasPermissions)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isServiceRunning && hasPermissions) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                        contentDescription = "Protection Status",
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = if (isServiceRunning && hasPermissions) "Active Protection" else "Protection Inactive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isServiceRunning && hasPermissions)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (!hasPermissions)
                            "Setup permissions required"
                        else if (isServiceRunning)
                            "Enforcing app limits in real-time"
                        else
                            "Tap switch to enable guard",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isServiceRunning && hasPermissions)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            if (hasPermissions) {
                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("service_status_switch")
                )
            } else {
                Button(
                    onClick = onGrantPermissions,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Grant", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PermissionAlertBanner(
    isUsageGranted: Boolean,
    isOverlayGranted: Boolean,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LockdownRed.copy(alpha = 0.12f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenPermissions() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = LockdownRed,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LockdownRed
                )
                Text(
                    text = "Usage Access & Overlay permissions are required for limit enforcement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = LockdownRed
            )
        }
    }
}

@Composable
fun PinPromptCard(
    onSetupPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.12f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Set Master Security PIN",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Required for unlocking & emergency pass",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onSetupPin,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                modifier = Modifier.testTag("btn_setup_master_pin")
            ) {
                Text("Set PIN", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun DashboardMetricsRow(
    trackedCount: Int,
    lockedCount: Int,
    overrideCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard(
            title = "Tracked",
            value = "$trackedCount",
            icon = Icons.Default.Timer,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        MetricCard(
            title = "In Lockdown",
            value = "$lockedCount",
            icon = Icons.Default.Lock,
            color = if (lockedCount > 0) LockdownRed else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        MetricCard(
            title = "Override",
            value = "$overrideCount",
            icon = Icons.Default.HourglassBottom,
            color = if (overrideCount > 0) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TrackedAppCard(
    app: TrackedAppWithUsage,
    onCardClick: () -> Unit,
    onToggleLimit: (Boolean) -> Unit,
    onSimulateLock: () -> Unit,
    onEmergencyOverride: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entity = app.entity
    val usage = app.usage
    val isLocked = entity.isLocked || app.isFrequencyBreached || app.isScreenTimeBreached
    val isOverride = app.isUnderEmergencyOverride

    val screenTimeMinutes = (usage.screenTimeMillisToday / (60 * 1000)).toInt()

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isLocked)
                LockdownRed.copy(alpha = 0.05f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_app_${entity.packageName}")
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

            // 1. Launch Frequency Progress (Trigger on Limit)
            if (entity.isFrequencyLimitEnabled) {
                val subLabelText = if (usage.windowResetRemainingSeconds > 0L) {
                    val resetRemainingMin = (usage.windowResetRemainingSeconds / 60).toInt()
                    val resetRemainingSec = (usage.windowResetRemainingSeconds % 60).toInt()
                    val resetFormatted = String.format("%02d:%02d", resetRemainingMin, resetRemainingSec)
                    "Resets in $resetFormatted"
                } else {
                    "Timer starts on limit"
                }

                UsageProgressBar(
                    current = usage.opensInWindow,
                    max = entity.maxOpenCount,
                    label = "Launch Frequency (Limit: ${entity.maxOpenCount} opens)",
                    unit = "opens",
                    subLabel = subLabelText
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

            // Quick Actions Footer: Properly aligned and spaced across card width
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLocked) {
                    Button(
                        onClick = onEmergencyOverride,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("btn_override_${entity.packageName}")
                    ) {
                        Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Emergency Pass", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

                if (isLocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = LockdownRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Rules Locked",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = LockdownRed
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCardClick() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("btn_edit_rules_${entity.packageName}")
                    ) {
                        Text(
                            text = "Edit Rules",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
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
}

@Composable
fun EmptyTrackedAppsCard(
    onAddApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
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
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Apps Tracked Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Select distracting applications to set launch frequency rules and daily screen time limits.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onAddApps,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Track First App")
            }
        }
    }
}
