package com.m9r1u5.datatracker

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.m9r1u5.datatracker.ui.AppDetailScreen
import com.m9r1u5.datatracker.ui.BundleScreen
import com.m9r1u5.datatracker.ui.DataTrackerTheme
import com.m9r1u5.datatracker.ui.HistoryScreen
import com.m9r1u5.datatracker.ui.SettingsScreen
import com.m9r1u5.datatracker.ui.SetupScreen
import com.m9r1u5.datatracker.ui.TodayScreen
import com.m9r1u5.datatracker.util.Perms

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DataTrackerApp
        setContent {
            val mode by app.themeMode.collectAsState()
            DataTrackerTheme(mode) {
                Root(app)
            }
        }
    }
}

@Composable
private fun Root(app: DataTrackerApp) {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(Perms.hasUsageAccess(context)) }

    // Re-check every time the user comes back to the app (e.g. from Settings).
    LifecycleResumeEffect(Unit) {
        hasAccess = Perms.hasUsageAccess(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(hasAccess) {
        if (hasAccess) app.repo.backfill()
    }

    if (hasAccess) MainNav(app) else SetupScreen()
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun MainNav(app: DataTrackerApp) {
    val context = LocalContext.current
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !app.prefs.askedNotifications && !Perms.hasNotifications(context)
        ) {
            app.prefs.askedNotifications = true
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val tabs = listOf(
        Tab("today", "Today", Icons.Filled.Home),
        Tab("history", "History", Icons.Filled.DateRange),
        Tab("bundle", "Bundle", Icons.Filled.ShoppingCart),
        Tab("settings", "Settings", Icons.Filled.Settings),
    )
    val openApp: (String) -> Unit = { key -> nav.navigate("app/" + Uri.encode(key)) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "today", modifier = Modifier.padding(padding)) {
            composable("today") { TodayScreen(app, openApp) }
            composable("history") { HistoryScreen(app, openApp) }
            composable("bundle") { BundleScreen(app, openApp) }
            composable("settings") { SettingsScreen(app) }
            composable(
                "app/{key}",
                arguments = listOf(navArgument("key") { type = NavType.StringType })
            ) { entry ->
                AppDetailScreen(
                    app = app,
                    appKey = entry.arguments?.getString("key") ?: "",
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
