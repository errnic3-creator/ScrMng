package com.example.ui.lockdown

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ScreenTimeApplication
import com.example.service.LockdownOverlayService
import com.example.ui.components.AppIconView
import com.example.ui.components.EmergencyOverridePinDialog
import com.example.ui.components.SetupPinDialog
import com.example.ui.theme.LockdownRed
import com.example.ui.theme.ScreenTimeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LockdownActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Restricted App"
        val reason = intent.getStringExtra(EXTRA_REASON) ?: "Limit Exceeded"
        val lockUntil = intent.getLongExtra(EXTRA_LOCK_UNTIL, 0L)

        val settings = (applicationContext as ScreenTimeApplication).settings
        val currentTheme = settings.selectedTheme

        setContent {
            ScreenTimeTheme(themeKey = currentTheme, darkTheme = true) {
                LockdownScreen(
                    packageName = packageName,
                    appName = appName,
                    reason = reason,
                    initialLockUntil = lockUntil,
                    onEmergencyOverrideGranted = {
                        LockdownOverlayService.dismiss(this)
                        finishAndRemoveTask()
                    },
                    onExitToHome = {
                        LockdownOverlayService.dismiss(this)
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Automatically remove from recents stack when navigation transitions away
        finishAndRemoveTask()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && !isFinishing) {
            finishAndRemoveTask()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finishAndRemoveTask()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_REASON = "extra_reason"
        const val EXTRA_LOCK_UNTIL = "extra_lock_until"
    }
}

@Composable
fun LockdownScreen(
    packageName: String,
    appName: String,
    reason: String,
    initialLockUntil: Long,
    onEmergencyOverrideGranted: () -> Unit,
    onExitToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as ScreenTimeApplication).repository }
    val settings = remember { (context.applicationContext as ScreenTimeApplication).settings }
    val scope = rememberCoroutineScope()

    var showPinDialog by remember { mutableStateOf(false) }
    var showSetupDialog by remember { mutableStateOf(false) }

    var targetLockUntil by remember(initialLockUntil) {
        mutableLongStateOf(
            if (initialLockUntil > System.currentTimeMillis()) initialLockUntil
            else (System.currentTimeMillis() + 30 * 60 * 1000L)
        )
    }

    // Refresh lock status from DB if available
    LaunchedEffect(packageName) {
        if (packageName.isNotEmpty()) {
            val app = repository.getTrackedApp(packageName)
            if (app != null && app.isLocked && app.lockUntilTimestamp > System.currentTimeMillis()) {
                targetLockUntil = app.lockUntilTimestamp
            }
        }
    }

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentTime = System.currentTimeMillis()
            delay(500L)
        }
    }

    val remainingSeconds = maxOf(0L, (targetLockUntil - currentTime) / 1000L)
    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor,
                        surfaceColor,
                        primaryColor.copy(alpha = 0.25f)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lock Shield Hero
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(LockdownRed.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lockdown Shield",
                    tint = LockdownRed,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "APP IN LOCKDOWN",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                color = LockdownRed
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Screen Time Limit Breached",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // App details card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIconView(
                            packageName = packageName,
                            appName = appName,
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LockdownRed.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFCA5A5),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Countdown Timer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LOCKDOWN TIMER",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (hours > 0) {
                        "%02d:%02d:%02d".format(hours, minutes, seconds)
                    } else {
                        "%02d:%02d".format(minutes, seconds)
                    },
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "App will unlock automatically when timer expires",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Button(
                onClick = {
                    if (settings.hasPinConfigured) {
                        showPinDialog = true
                    } else {
                        showSetupDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("lockdown_emergency_override_button")
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassBottom,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Emergency Override (${settings.emergencyOverrideDurationMinutes} min)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onExitToHome,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("lockdown_exit_home_button")
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exit to Home Screen", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showPinDialog) {
        EmergencyOverridePinDialog(
            appName = appName,
            durationMinutes = settings.emergencyOverrideDurationMinutes,
            onDismiss = { showPinDialog = false },
            onVerify = { pin -> repository.verifyMasterPin(pin) },
            onSuccess = {
                scope.launch {
                    repository.grantEmergencyOverride(packageName, settings.emergencyOverrideDurationMinutes)
                    showPinDialog = false
                    onEmergencyOverrideGranted()
                }
            }
        )
    }

    if (showSetupDialog) {
        SetupPinDialog(
            onDismiss = { showSetupDialog = false },
            onPinSet = { pin ->
                repository.setMasterPin(pin)
                showSetupDialog = false
                showPinDialog = true
            }
        )
    }
}
