@file:OptIn(ExperimentalMaterial3Api::class)

package com.m9r1u5.datatracker.ui

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.m9r1u5.datatracker.BuildConfig
import com.m9r1u5.datatracker.DataTrackerApp
import com.m9r1u5.datatracker.util.Perms
import com.m9r1u5.datatracker.util.Updater
import com.m9r1u5.datatracker.work.Scheduler
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

@Composable
fun SettingsScreen(app: DataTrackerApp) {
    val context = LocalContext.current
    val prefs = app.prefs
    val scope = rememberCoroutineScope()

    var dailyLimit by rememberSaveable { mutableStateOf(prefs.dailyMobileLimitMb.takeIf { it > 0 }?.toString() ?: "") }
    var appLimit by rememberSaveable { mutableStateOf(prefs.perAppLimitMb.takeIf { it > 0 }?.toString() ?: "") }
    var bundleAlerts by remember { mutableStateOf(prefs.bundleAlerts) }
    var summaryOn by remember { mutableStateOf(prefs.summaryEnabled) }
    var sumHour by remember { mutableIntStateOf(prefs.summaryHour) }
    var sumMinute by remember { mutableIntStateOf(prefs.summaryMinute) }
    val themeMode by app.themeMode.collectAsState()
    var showSystem by remember { mutableStateOf(prefs.showSystem) }
    var confirmClear by remember { mutableStateOf(false) }
    var dataMessage by remember { mutableStateOf<String?>(null) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var updating by remember { mutableStateOf(false) }
    var notifOk by remember { mutableStateOf(Perms.hasNotifications(context)) }

    LifecycleResumeEffect(Unit) {
        notifOk = Perms.hasNotifications(context)
        onPauseOrDispose { }
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifOk = Perms.hasNotifications(context)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch {
                dataMessage = try {
                    val out = context.contentResolver.openOutputStream(uri)
                    if (out == null) "Couldn't open that file." else {
                        out.use { app.repo.exportCsv(it) }
                        "Exported to CSV."
                    }
                } catch (e: Exception) {
                    "Export failed: ${e.message}"
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings") })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---- Alerts ----
            Section("Alerts") {
                OutlinedTextField(
                    value = dailyLimit,
                    onValueChange = { v ->
                        dailyLimit = v.filter { it.isDigit() }.take(6)
                        prefs.dailyMobileLimitMb = dailyLimit.toIntOrNull() ?: 0
                    },
                    label = { Text("Daily mobile data limit (MB)") },
                    supportingText = { Text("Leave empty to switch off") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = appLimit,
                    onValueChange = { v ->
                        appLimit = v.filter { it.isDigit() }.take(6)
                        prefs.perAppLimitMb = appLimit.toIntOrNull() ?: 0
                    },
                    label = { Text("Alert when one app uses more than (MB)") },
                    supportingText = { Text("Mobile data, per app, per day. Leave empty to switch off") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                SwitchRow("Bundle alerts", "Warn at 80% and 100% of your bundle", bundleAlerts) {
                    bundleAlerts = it
                    prefs.bundleAlerts = it
                }
                Text(
                    "Limits are checked every hour and whenever the app is open.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ---- Daily summary ----
            Section("Daily summary") {
                SwitchRow("Daily summary notification", "A short summary of today's data use", summaryOn) {
                    summaryOn = it
                    prefs.summaryEnabled = it
                    Scheduler.scheduleSummary(context, prefs)
                }
                if (summaryOn) {
                    OutlinedButton(onClick = {
                        TimePickerDialog(context, { _, h, m ->
                            sumHour = h
                            sumMinute = m
                            prefs.summaryHour = h
                            prefs.summaryMinute = m
                            Scheduler.scheduleSummary(context, prefs)
                        }, sumHour, sumMinute, true).show()
                    }) {
                        Text("Time: " + String.format(Locale.US, "%02d:%02d", sumHour, sumMinute))
                    }
                }
            }

            // ---- Appearance ----
            Section("Appearance") {
                val options = listOf("Phone", "Light", "Dark")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = themeMode == i,
                            onClick = {
                                prefs.themeMode = i
                                app.themeMode.value = i
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size)
                        ) { Text(label) }
                    }
                }
                SwitchRow("Show system apps", "Android system, background services and removed apps", showSystem) {
                    showSystem = it
                    prefs.showSystem = it
                }
            }

            // ---- Permissions ----
            Section("Permissions") {
                Text("Usage access: " + if (Perms.hasUsageAccess(context)) "On" else "Off")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notifications: " + if (notifOk) "On" else "Off", Modifier.weight(1f))
                        if (!notifOk) {
                            TextButton(onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                Text("Allow")
                            }
                        }
                    }
                }
            }

            // ---- Data ----
            Section("Your data") {
                Row {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("data-usage-${LocalDate.now()}.csv") },
                        modifier = Modifier.weight(1f)
                    ) { Text("Export CSV") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.weight(1f)) {
                        Text("Clear history")
                    }
                }
                dataMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }

            // ---- About / update ----
            Section("About") {
                Text("Data Tracker version ${BuildConfig.VERSION_NAME}")
                Button(
                    enabled = !updating,
                    onClick = {
                        scope.launch {
                            updating = true
                            updateStatus = "Checking GitHub…"
                            updateStatus = try {
                                val rel = Updater.latest()
                                when {
                                    rel == null -> "No release found on GitHub yet."
                                    rel.versionCode <= BuildConfig.VERSION_CODE ->
                                        "You have the latest version (${BuildConfig.VERSION_NAME})."
                                    else -> {
                                        updateStatus = "Downloading ${rel.tag}…"
                                        val file = Updater.download(context, rel.apkUrl) { pct ->
                                            updateStatus = "Downloading ${rel.tag}… $pct%"
                                        }
                                        if (Updater.install(context, file)) "Opening the installer for ${rel.tag}…"
                                        else "Allow \"Install unknown apps\" for Data Tracker, then tap Check for update again."
                                    }
                                }
                            } catch (e: Exception) {
                                "Couldn't check for updates: ${e.message ?: "no internet?"}"
                            }
                            updating = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Check for update") }
                updateStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text(
                    "The update check is the only time Data Tracker uses the internet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("This deletes all stored daily usage. Today's numbers will start counting again straight away. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch {
                        app.repo.clearHistory()
                        app.repo.refreshDay(LocalDate.now())
                        dataMessage = "History cleared."
                    }
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
