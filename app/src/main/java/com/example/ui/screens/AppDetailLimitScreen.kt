package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppIconView
import com.example.ui.components.EmergencyOverridePinDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.UsageProgressBar
import com.example.ui.theme.OverridePurple
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailLimitScreen(
    packageName: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trackedApps by viewModel.trackedApps.collectAsStateWithLifecycle()
    val appWithUsage = trackedApps.find { it.entity.packageName == packageName }

    val entity = appWithUsage?.entity

    if (entity == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("App not found in tracking list")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onNavigateBack) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    var maxOpenCount by remember(entity) { mutableIntStateOf(entity.maxOpenCount) }
    var openWindowMinutes by remember(entity) { mutableIntStateOf(entity.openWindowMinutes) }
    var isFrequencyLimitEnabled by remember(entity) { mutableStateOf(entity.isFrequencyLimitEnabled) }

    var maxScreenTimeMinutes by remember(entity) { mutableIntStateOf(entity.maxScreenTimeMinutes) }
    var isScreenTimeLimitEnabled by remember(entity) { mutableStateOf(entity.isScreenTimeLimitEnabled) }

    var isLimitEnabled by remember(entity) { mutableStateOf(entity.isLimitEnabled) }
    var showOverrideDialog by remember { mutableStateOf(false) }

    val openCountPresets = listOf(1, 2, 3, 5, 8, 10, 15)
    val windowPresets = listOf(5, 10, 15, 30, 45, 60, 120)
    val screenTimePresets = listOf(15, 30, 45, 60, 90, 120, 180)

    val usage = appWithUsage.usage
    val screenTimeMinutes = (usage.screenTimeMillisToday / (60 * 1000)).toInt()
    val isLocked = entity.isLocked || appWithUsage.isFrequencyBreached || appWithUsage.isScreenTimeBreached

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entity.appName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.removeTrackedApp(packageName)
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("detail_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove tracking",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Header Hero
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconView(
                            packageName = entity.packageName,
                            appName = entity.appName,
                            category = entity.category,
                            size = 54.dp
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entity.appName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = entity.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            StatusBadge(
                                isLocked = isLocked,
                                isUnderOverride = appWithUsage.isUnderEmergencyOverride,
                                isLimitEnabled = isLimitEnabled,
                                overrideSecondsLeft = appWithUsage.overrideRemainingSeconds,
                                lockSecondsLeft = appWithUsage.lockRemainingSeconds
                            )
                        }
                    }
                }
            }

            // Real-time Status Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monitoring Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isLimitEnabled) "Enforced" else "Paused",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isLimitEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = isLimitEnabled,
                                    onCheckedChange = { isLimitEnabled = it },
                                    modifier = Modifier.testTag("toggle_limits_enabled_switch")
                                )
                            }
                        }

                        if (isFrequencyLimitEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val resetRemainingMin = (usage.windowResetRemainingSeconds / 60).toInt()
                            val resetRemainingSec = (usage.windowResetRemainingSeconds % 60).toInt()
                            val resetFormatted = String.format("%02d:%02d", resetRemainingMin, resetRemainingSec)

                            UsageProgressBar(
                                label = "Fixed Window Opens",
                                current = usage.opensInWindow,
                                max = maxOpenCount,
                                unit = "opens",
                                windowDescription = "${openWindowMinutes}m window",
                                subLabel = "Resets in $resetFormatted"
                            )
                        }

                        if (isScreenTimeLimitEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            UsageProgressBar(
                                label = "Daily Screen Time",
                                current = screenTimeMinutes,
                                max = maxScreenTimeMinutes,
                                unit = "min",
                                windowDescription = "today"
                            )
                        }
                    }
                }
            }

            // Section 1: Open Frequency Limit (Fixed Window)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Open Frequency Limit",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Fixed window: max $maxOpenCount opens every $openWindowMinutes min",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Switch(
                                checked = isFrequencyLimitEnabled,
                                onCheckedChange = { isFrequencyLimitEnabled = it },
                                modifier = Modifier.testTag("switch_frequency_limit_enabled")
                            )
                        }

                        if (isFrequencyLimitEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Maximum Allowed Launches: $maxOpenCount opens",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Slider(
                                value = maxOpenCount.toFloat(),
                                onValueChange = { maxOpenCount = it.roundToInt() },
                                valueRange = 1f..20f,
                                steps = 18,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("slider_max_open_count")
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(openCountPresets) { preset ->
                                    FilterChip(
                                        selected = maxOpenCount == preset,
                                        onClick = { maxOpenCount = preset },
                                        label = { Text("$preset opens") },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Fixed Reset Window: $openWindowMinutes minutes",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Slider(
                                value = openWindowMinutes.toFloat(),
                                onValueChange = { openWindowMinutes = it.roundToInt() },
                                valueRange = 5f..120f,
                                steps = 22,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.secondary,
                                    activeTrackColor = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.testTag("slider_open_window_minutes")
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(windowPresets) { preset ->
                                    FilterChip(
                                        selected = openWindowMinutes == preset,
                                        onClick = { openWindowMinutes = preset },
                                        label = { Text("${preset}m") },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Screen Time Limit
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Daily Screen Time Limit",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Max foreground time: $maxScreenTimeMinutes minutes/day",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            Switch(
                                checked = isScreenTimeLimitEnabled,
                                onCheckedChange = { isScreenTimeLimitEnabled = it },
                                modifier = Modifier.testTag("switch_screentime_limit_enabled")
                            )
                        }

                        if (isScreenTimeLimitEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Slider(
                                value = maxScreenTimeMinutes.toFloat(),
                                onValueChange = { maxScreenTimeMinutes = it.roundToInt() },
                                valueRange = 5f..300f,
                                steps = 58,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.secondary,
                                    activeTrackColor = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.testTag("slider_max_screen_time")
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(screenTimePresets) { preset ->
                                    FilterChip(
                                        selected = maxScreenTimeMinutes == preset,
                                        onClick = { maxScreenTimeMinutes = preset },
                                        label = { Text("${preset}m") },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Save & Lockdown Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.updateTrackedApp(
                                packageName = entity.packageName,
                                appName = entity.appName,
                                maxOpenCount = maxOpenCount,
                                openWindowMinutes = openWindowMinutes,
                                isFrequencyLimitEnabled = isFrequencyLimitEnabled,
                                maxScreenTimeMinutes = maxScreenTimeMinutes,
                                isScreenTimeLimitEnabled = isScreenTimeLimitEnabled,
                                isLimitEnabled = isLimitEnabled,
                                category = entity.category
                            )
                            onNavigateBack()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_limits_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Limit Settings", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.simulateTriggerLock(entity)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("detail_simulate_lock_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Lock Barrier", style = MaterialTheme.typography.labelMedium)
                        }

                        if (isLocked) {
                            Button(
                                onClick = { showOverrideDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = OverridePurple),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("detail_emergency_override_button")
                            ) {
                                Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Emergency PIN", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOverrideDialog) {
        EmergencyOverridePinDialog(
            appName = entity.appName,
            durationMinutes = viewModel.getOverrideDuration(),
            onDismiss = { showOverrideDialog = false },
            onVerify = { pin -> viewModel.verifyMasterPin(pin) },
            onSuccess = {
                viewModel.grantEmergencyOverride(entity.packageName)
                showOverrideDialog = false
            }
        )
    }
}
