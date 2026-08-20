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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
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
import java.util.Calendar
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

    // Selected day for weekly schedule view: Calendar values 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat, 1=Sun (0=All)
    var selectedScheduleDay by remember {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        mutableIntStateOf(today)
    }

    val daysOfWeek = listOf(
        2 to "Mon",
        3 to "Tue",
        4 to "Wed",
        5 to "Thu",
        6 to "Fri",
        7 to "Sat",
        1 to "Sun"
    )

    // Filter groups by selected schedule day if not viewing all
    val displayedGroups = remember(appGroups, selectedScheduleDay) {
        if (selectedScheduleDay == 0) appGroups
        else appGroups.filter { it.group.getDaysList().contains(selectedScheduleDay) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Master Group Rules Toggle
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
                                    text = "App Group Enforcement",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isGroupLimitsEnabled) "Individual rules applied to grouped apps" else "Group enforcement paused",
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

            // 2. Weekly Schedule Day Selector Profile
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Weekly Schedule Profiles",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Filter or inspect active rules for specific days of the week:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 7-day selector buttons
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedScheduleDay == 0,
                                    onClick = { selectedScheduleDay = 0 },
                                    label = { Text("All Days") },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                            items(daysOfWeek) { (dayVal, dayLabel) ->
                                val isSelected = selectedScheduleDay == dayVal
                                val calendar = Calendar.getInstance()
                                val isToday = calendar.get(Calendar.DAY_OF_WEEK) == dayVal

                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedScheduleDay = dayVal },
                                    label = {
                                        Text(if (isToday) "$dayLabel (Today)" else dayLabel)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 3. Header: Configured App Groups
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedScheduleDay == 0)
                            "All App Groups (${appGroups.size})"
                        else {
                            val dayLabel = daysOfWeek.find { it.first == selectedScheduleDay }?.second ?: ""
                            "Active on $dayLabel (${displayedGroups.size})"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 4. Group List or Empty State
            if (displayedGroups.isEmpty()) {
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
                                text = if (appGroups.isEmpty()) "No App Groups Configured" else "No Groups Active on Selected Day",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Create groups like 'Doomscroll' or 'Games' to enforce individual limits across multiple apps with schedule profiles.",
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
                                Text("Create App Group")
                            }
                        }
                    }
                }
            } else {
                items(displayedGroups, key = { it.group.id }) { groupWithUsage ->
                    AdvancedGroupCard(
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
private fun AdvancedGroupCard(
    groupWithUsage: AppGroupWithUsage,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val group = groupWithUsage.group
    val memberApps = groupWithUsage.memberAppUsages
    val isTodayActive = groupWithUsage.isTodayScheduleActive
    val packages = group.getPackageList()
    val activeDays = group.getDaysList()

    val dayAbbrs = listOf(
        2 to "M",
        3 to "T",
        4 to "W",
        5 to "T",
        6 to "F",
        7 to "S",
        1 to "S"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!group.isEnabled)
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
                        .size(44.dp)
                        .clip(CircleShape)
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
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${packages.size} apps • ${group.getActiveDaysSummary()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Switch(
                    checked = group.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("toggle_group_${group.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day indicators (M T W T F S S)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dayAbbrs.forEach { (dayVal, label) ->
                    val isActive = activeDays.contains(dayVal)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Individual Rule Details
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "INDIVIDUAL APP RULES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (group.isFrequencyLimitEnabled) {
                        Text(
                            text = "• Launch Frequency: Max ${group.maxOpenCount} opens in ${group.openWindowMinutes}m window",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (group.isScreenTimeLimitEnabled) {
                        Text(
                            text = "• Daily Screen Time: Max ${group.maxScreenTimeMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Bundled Apps with Individual Live Tracking
            if (memberApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Bundled Apps (${memberApps.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    memberApps.forEach { memberApp ->
                        val screenTimeMin = (memberApp.usage.screenTimeMillisToday / (60 * 1000)).toInt()
                        val isLocked = memberApp.entity.isLocked || memberApp.isFrequencyBreached || memberApp.isScreenTimeBreached

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIconView(
                                        packageName = memberApp.entity.packageName,
                                        size = 28.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = memberApp.entity.appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isLocked) {
                                        Text(
                                            text = "LOCKED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = LockdownRed
                                        )
                                    } else {
                                        Text(
                                            text = "${memberApp.usage.opensInWindow}/${group.maxOpenCount} opens • ${screenTimeMin}/${group.maxScreenTimeMinutes}m",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (group.isFrequencyLimitEnabled) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    UsageProgressBar(
                                        current = memberApp.usage.opensInWindow,
                                        max = group.maxOpenCount,
                                        label = "Launch Frequency",
                                        unit = "opens"
                                    )
                                }

                                if (group.isScreenTimeLimitEnabled) {
                                    Spacer(modifier = Modifier.height(4.dp))
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions footer
            val isGroupLocked = memberApps.any { it.entity.isLocked || it.isFrequencyBreached || it.isScreenTimeBreached }
            if (isGroupLocked) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LockdownRed.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = LockdownRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rules Locked During Active Lockout",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = LockdownRed
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onEdit, modifier = Modifier.testTag("btn_edit_group_${group.id}")) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Rules")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("btn_delete_group_${group.id}")) {
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
    var iconEmoji by remember { mutableStateOf(group?.iconEmoji ?: "📱") }

    // Independent limit switches
    var isFrequencyLimitEnabled by remember { mutableStateOf(group?.isFrequencyLimitEnabled ?: true) }
    var maxOpenCount by remember { mutableIntStateOf(group?.maxOpenCount ?: 5) }
    var openWindowMinutes by remember { mutableIntStateOf(group?.openWindowMinutes ?: 30) }

    var isScreenTimeLimitEnabled by remember { mutableStateOf(group?.isScreenTimeLimitEnabled ?: true) }
    var maxScreenTimeMinutes by remember { mutableIntStateOf(group?.maxScreenTimeMinutes ?: 45) }

    // Weekly Schedule Days: 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat
    var selectedDays by remember {
        mutableStateOf(group?.getDaysList()?.toSet() ?: setOf(1, 2, 3, 4, 5, 6, 7))
    }

    // Search bar query for filtering target apps
    var appSearchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(installedApps, appSearchQuery) {
        if (appSearchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.appName.contains(appSearchQuery, ignoreCase = true) ||
            it.packageName.contains(appSearchQuery, ignoreCase = true)
        }
    }

    val daysOfWeek = listOf(
        2 to "Mon",
        3 to "Tue",
        4 to "Wed",
        5 to "Thu",
        6 to "Fri",
        7 to "Sat",
        1 to "Sun"
    )

    val emojis = listOf("📱", "🎮", "💬", "📺", "🔥", "🛑", "⏰", "🎯")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (group == null) "Create App Group" else "Edit App Group Rules",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Group Name & Emoji
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Group Name") },
                            placeholder = { Text("e.g. Doomscroll, Games") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_group_name")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(emojis) { emoji ->
                            FilterChip(
                                selected = iconEmoji == emoji,
                                onClick = { iconEmoji = emoji },
                                label = { Text(emoji, fontSize = 16.sp) },
                                shape = CircleShape
                            )
                        }
                    }
                }

                // Weekly Schedule Days Selector
                item {
                    HorizontalDivider()
                    Text(
                        text = "Weekly Schedule Days",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select days when this group's rules apply:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick buttons: All, Weekdays, Weekends
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedDays.size == 7,
                            onClick = { selectedDays = setOf(1, 2, 3, 4, 5, 6, 7) },
                            label = { Text("All Week", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = selectedDays == setOf(2, 3, 4, 5, 6),
                            onClick = { selectedDays = setOf(2, 3, 4, 5, 6) },
                            label = { Text("Weekdays", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = selectedDays == setOf(1, 7),
                            onClick = { selectedDays = setOf(1, 7) },
                            label = { Text("Weekends", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 7-day checkboxes/chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(daysOfWeek) { (dayVal, label) ->
                            val isSelected = selectedDays.contains(dayVal)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedDays = if (isSelected) {
                                        if (selectedDays.size > 1) selectedDays - dayVal else selectedDays
                                    } else {
                                        selectedDays + dayVal
                                    }
                                },
                                label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Independent Limit 1: Launch Frequency Limit
                item {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Launch Frequency Limit",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enforce max opens per time window",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isFrequencyLimitEnabled,
                            onCheckedChange = { isFrequencyLimitEnabled = it },
                            modifier = Modifier.testTag("switch_frequency_limit")
                        )
                    }

                    if (isFrequencyLimitEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Max Opens per App: $maxOpenCount opens in $openWindowMinutes min",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = maxOpenCount.toFloat(),
                            onValueChange = { maxOpenCount = it.roundToInt() },
                            valueRange = 1f..20f,
                            steps = 18
                        )

                        Text(
                            text = "Window Reset Duration: $openWindowMinutes minutes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf(15, 30, 45, 60, 120)) { win ->
                                FilterChip(
                                    selected = openWindowMinutes == win,
                                    onClick = { openWindowMinutes = win },
                                    label = { Text("${win}m") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Independent Limit 2: Daily Screen Time Limit
                item {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Screen Time Limit",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enforce daily screen time quota per app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isScreenTimeLimitEnabled,
                            onCheckedChange = { isScreenTimeLimitEnabled = it },
                            modifier = Modifier.testTag("switch_screentime_limit")
                        )
                    }

                    if (isScreenTimeLimitEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Max Screen Time per App: $maxScreenTimeMinutes min / day",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = maxScreenTimeMinutes.toFloat(),
                            onValueChange = { maxScreenTimeMinutes = it.roundToInt() },
                            valueRange = 10f..240f,
                            steps = 22
                        )
                    }
                }

                // App Picker Checklist with dedicated search bar
                item {
                    HorizontalDivider()
                    Text(
                        text = "Select Target Apps (${selectedPackages.size} selected):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dedicated search bar
                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        placeholder = { Text("Search installed apps...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (appSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { appSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_group_app_search")
                    )
                }

                items(filteredApps, key = { it.packageName }) { appInfo ->
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appInfo.appName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = appInfo.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                            iconEmoji = iconEmoji,
                            packageNamesCsv = selectedPackages.joinToString(","),
                            isEnabled = group?.isEnabled ?: true,
                            maxOpenCount = maxOpenCount,
                            openWindowMinutes = openWindowMinutes,
                            isFrequencyLimitEnabled = isFrequencyLimitEnabled,
                            maxScreenTimeMinutes = maxScreenTimeMinutes,
                            isScreenTimeLimitEnabled = isScreenTimeLimitEnabled,
                            isScheduleEnabled = true,
                            daysOfWeekCsv = selectedDays.sorted().joinToString(",")
                        )
                        onSave(groupEntity)
                    }
                },
                enabled = name.isNotBlank() && selectedPackages.isNotEmpty(),
                modifier = Modifier.testTag("btn_save_group")
            ) {
                Text("Save Group Rules")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
