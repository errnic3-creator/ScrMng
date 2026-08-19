package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppGroupEntity
import com.example.data.repository.AppGroupWithUsage
import com.example.data.util.InstalledAppInfo
import com.example.ui.components.AppIconView
import com.example.ui.components.UsageProgressBar
import com.example.ui.theme.LockdownRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

@Composable
fun AdvancedScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val appGroups by viewModel.appGroups.collectAsStateWithLifecycle()
    val isGroupLimitsEnabled by viewModel.isGroupLimitsEnabled.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()

    var showGroupDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<AppGroupEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master group toggle card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isGroupLimitsEnabled)
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
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isGroupLimitsEnabled) SuccessGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = if (isGroupLimitsEnabled) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "App Group Pooling",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isGroupLimitsEnabled) "Multi-app quotas & schedules active" else "Group pooling paused",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isGroupLimitsEnabled,
                            onCheckedChange = { viewModel.setGroupLimitsEnabled(it) },
                            modifier = Modifier.testTag("switch_group_limits_enabled")
                        )
                    }
                }
            }

            // Explanatory Banner
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Bundle multiple apps into unified groups to share a combined screen time pool or enforce active hours schedules (e.g. Work Hours, Bedtime).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Header
            item {
                Text(
                    text = "Configured Groups (${appGroups.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Group List or Empty State
            if (appGroups.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
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
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No App Groups Configured",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Create groups like 'Social Media' or 'Gaming' to share a single combined screen time limit.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    editingGroup = null
                                    showGroupDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("create_first_group_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create First Group")
                            }
                        }
                    }
                }
            } else {
                items(appGroups, key = { it.group.id }) { groupWithUsage ->
                    AppGroupCard(
                        groupWithUsage = groupWithUsage,
                        onToggle = { enabled ->
                            viewModel.toggleGroupEnabled(groupWithUsage.group, enabled)
                        },
                        onEdit = {
                            editingGroup = groupWithUsage.group
                            showGroupDialog = true
                        },
                        onDelete = {
                            viewModel.deleteAppGroup(groupWithUsage.group)
                        }
                    )
                }
            }
        }

        // FAB to add new group
        ExtendedFloatingActionButton(
            onClick = {
                editingGroup = null
                showGroupDialog = true
            },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("New Group", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp)
                .testTag("fab_add_group")
        )
    }

    if (showGroupDialog) {
        AppGroupEditorDialog(
            group = editingGroup,
            installedApps = installedApps,
            onDismiss = {
                showGroupDialog = false
                editingGroup = null
            },
            onSave = { savedGroup ->
                if (editingGroup != null) {
                    viewModel.updateAppGroup(savedGroup)
                } else {
                    viewModel.addAppGroup(savedGroup)
                }
                showGroupDialog = false
                editingGroup = null
            }
        )
    }
}

@Composable
private fun AppGroupCard(
    groupWithUsage: AppGroupWithUsage,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val group = groupWithUsage.group
    val totalScreenTimeMinutes = (groupWithUsage.usage.combinedScreenTimeMillisToday / (60 * 1000)).toInt()
    val isLocked = groupWithUsage.isFrequencyBreached || groupWithUsage.isScreenTimeBreached || groupWithUsage.isScheduleActive
    val packages = group.getPackageList()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked)
                LockdownRed.copy(alpha = 0.08f)
            else if (!group.isEnabled)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("group_card_${group.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Icon, Name, Apps count & Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = group.iconEmoji.ifEmpty { "📱" },
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${packages.size} apps bundled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = group.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("toggle_group_${group.id}")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // App Icons row in this group
            if (packages.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(packages) { pkg ->
                        AppIconView(packageName = pkg, size = 32.dp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Schedule tag if active
            if (group.isScheduleEnabled) {
                val startFormatted = String.format("%02d:00", group.startHour)
                val endFormatted = String.format("%02d:00", group.endHour)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Schedule: $startFormatted – $endFormatted",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Shared Screen Time
            UsageProgressBar(
                label = "Shared Screen Time",
                current = totalScreenTimeMinutes,
                max = group.maxScreenTimeMinutes,
                unit = "min",
                windowDescription = "pooled"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Shared Launch Frequency
            UsageProgressBar(
                label = "Shared Launch Frequency",
                current = groupWithUsage.usage.combinedOpensInWindow,
                max = group.maxOpenCount,
                unit = "opens",
                windowDescription = "${group.openWindowMinutes}m window"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Actions footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Rules")
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete group",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppGroupEditorDialog(
    group: AppGroupEntity?,
    installedApps: List<InstalledAppInfo>,
    onDismiss: () -> Unit,
    onSave: (AppGroupEntity) -> Unit
) {
    var name by remember { mutableStateOf(group?.name ?: "") }
    var selectedPackages by remember { mutableStateOf(group?.getPackageList()?.toSet() ?: emptySet()) }
    var maxScreenTimeMinutes by remember { mutableIntStateOf(group?.maxScreenTimeMinutes ?: 60) }
    var maxOpenCount by remember { mutableIntStateOf(group?.maxOpenCount ?: 10) }
    var openWindowMinutes by remember { mutableIntStateOf(group?.openWindowMinutes ?: 60) }

    var isScheduleEnabled by remember { mutableStateOf(group?.isScheduleEnabled ?: false) }
    var startHour by remember { mutableIntStateOf(group?.startHour ?: 9) }
    var endHour by remember { mutableIntStateOf(group?.endHour ?: 17) }

    val presetTemplates = listOf(
        "Social Media" to listOf("com.instagram.android", "com.zhiliaoapp.musically", "com.twitter.android", "com.reddit.frontpage"),
        "Entertainment" to listOf("com.google.android.youtube", "com.netflix.mediaclient", "com.spotify.music"),
        "Focus Group" to emptyList()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (group == null) "Create App Group" else "Edit App Group",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Group Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Group Name") },
                        placeholder = { Text("e.g. Social Media, Games") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Presets chips
                if (group == null) {
                    item {
                        Text("Quick Presets:", style = MaterialTheme.typography.labelSmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(presetTemplates) { (presetName, defaultPkgs) ->
                                FilterChip(
                                    selected = name == presetName,
                                    onClick = {
                                        name = presetName
                                        if (defaultPkgs.isNotEmpty()) {
                                            val available = installedApps.map { it.packageName }.toSet()
                                            val matched = defaultPkgs.filter { available.contains(it) }.toSet()
                                            if (matched.isNotEmpty()) {
                                                selectedPackages = matched
                                            }
                                        }
                                    },
                                    label = { Text(presetName) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Shared Screen Time Slider
                item {
                    Text(
                        text = "Shared Daily Screen Time: $maxScreenTimeMinutes min",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = maxScreenTimeMinutes.toFloat(),
                        onValueChange = { maxScreenTimeMinutes = it.roundToInt() },
                        valueRange = 15f..240f,
                        steps = 14
                    )
                }

                // Shared Launch Count Slider
                item {
                    Text(
                        text = "Shared Open Frequency: $maxOpenCount opens in $openWindowMinutes min",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = maxOpenCount.toFloat(),
                        onValueChange = { maxOpenCount = it.roundToInt() },
                        valueRange = 2f..30f,
                        steps = 13
                    )
                }

                // Schedule Enforcing Toggle & Presets
                item {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Time-of-Day Schedule",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = isScheduleEnabled,
                            onCheckedChange = { isScheduleEnabled = it }
                        )
                    }

                    if (isScheduleEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = startHour == 9 && endHour == 17,
                                onClick = {
                                    startHour = 9
                                    endHour = 17
                                },
                                label = { Text("Work (9-17)") },
                                shape = RoundedCornerShape(8.dp)
                            )
                            FilterChip(
                                selected = startHour == 22 && endHour == 7,
                                onClick = {
                                    startHour = 22
                                    endHour = 7
                                },
                                label = { Text("Bedtime (22-7)") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // App Picker Selection Checklist
                item {
                    HorizontalDivider()
                    Text(
                        text = "Select Apps in Group (${selectedPackages.size} selected):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(installedApps, key = { it.packageName }) { appInfo ->
                    val isChecked = selectedPackages.contains(appInfo.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedPackages = if (isChecked) {
                                    selectedPackages - appInfo.packageName
                                } else {
                                    selectedPackages + appInfo.packageName
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconView(packageName = appInfo.packageName, appName = appInfo.appName, size = 32.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = appInfo.appName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selectedPackages = if (checked) {
                                    selectedPackages + appInfo.packageName
                                } else {
                                    selectedPackages - appInfo.packageName
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val groupEntity = AppGroupEntity(
                            id = group?.id ?: 0,
                            name = name.trim(),
                            iconEmoji = group?.iconEmoji ?: "📱",
                            packageNamesCsv = selectedPackages.joinToString(","),
                            isEnabled = group?.isEnabled ?: true,
                            maxOpenCount = maxOpenCount,
                            openWindowMinutes = openWindowMinutes,
                            isFrequencyLimitEnabled = true,
                            maxScreenTimeMinutes = maxScreenTimeMinutes,
                            isScreenTimeLimitEnabled = true,
                            isScheduleEnabled = isScheduleEnabled,
                            startHour = startHour,
                            startMinute = 0,
                            endHour = endHour,
                            endMinute = 0
                        )
                        onSave(groupEntity)
                    }
                },
                enabled = name.isNotBlank() && selectedPackages.isNotEmpty()
            ) {
                Text("Save Group")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
