package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.navigation.Screen
import com.example.ui.screens.ActivityLogsScreen
import com.example.ui.screens.AdvancedScreen
import com.example.ui.screens.AppDetailLimitScreen
import com.example.ui.screens.AppPickerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ScreenTimeTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
            ScreenTimeTheme(themeKey = currentTheme) {
                MainApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
        viewModel.refreshUsage()
        val app = application as? ScreenTimeApplication
        if (app?.settings?.isMonitorServiceEnabled == true && com.example.data.util.UsageStatsHelper.hasUsageStatsPermission(this)) {
            com.example.service.AppMonitorService.start(this)
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val isUsagePermissionGranted by viewModel.isUsagePermissionGranted.collectAsStateWithLifecycle()
    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val allPermissionsGranted = isUsagePermissionGranted && isOverlayPermissionGranted

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    // 4 Core Bottom Navigation Tabs: Home, Advanced, History, Settings
    val bottomNavItems = Screen.bottomNavItems

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (allPermissionsGranted) Screen.Home.route else Screen.Permissions.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAppPicker = { navController.navigate(Screen.AppPicker.route) },
                    onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                    onNavigateToAppDetail = { packageName ->
                        navController.navigate(Screen.AppDetail.createRoute(packageName))
                    }
                )
            }

            composable(Screen.Advanced.route) {
                AdvancedScreen(viewModel = viewModel)
            }

            composable(Screen.AppPicker.route) {
                AppPickerScreen(
                    viewModel = viewModel,
                    onNavigateToAppDetail = { packageName ->
                        navController.navigate(Screen.AppDetail.createRoute(packageName))
                    }
                )
            }

            composable(Screen.Permissions.route) {
                PermissionsScreen(
                    viewModel = viewModel,
                    onContinue = { navController.navigate(Screen.Home.route) }
                )
            }

            composable(Screen.History.route) {
                ActivityLogsScreen(viewModel = viewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) }
                )
            }

            composable(
                route = Screen.AppDetail.route,
                arguments = listOf(navArgument("packageName") { type = NavType.StringType })
            ) { backStackEntry ->
                val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                AppDetailLimitScreen(
                    packageName = packageName,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
