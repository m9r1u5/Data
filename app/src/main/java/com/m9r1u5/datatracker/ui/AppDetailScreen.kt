@file:OptIn(ExperimentalMaterial3Api::class)

package com.m9r1u5.datatracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.m9r1u5.datatracker.DataTrackerApp
import com.m9r1u5.datatracker.util.Fmt
import java.time.LocalDate

@Composable
fun AppDetailScreen(app: DataTrackerApp, appKey: String, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    val from = remember { today.minusDays(29) }
    val rows by remember(appKey) {
        app.repo.dao.observeApp(appKey, from.toString(), today.toString())
    }.collectAsState(initial = emptyList())
    var filter by rememberSaveable { mutableStateOf(NetFilter.BOTH) }

    LaunchedEffect(Unit) { app.repo.refreshDay(today) }

    val byDate = rows.associateBy { it.date }
    val dates = (0..29).map { from.plusDays(it.toLong()) }
    val dayRows = dates.map { byDate[it.toString()] }
    val total = dayRows.map { it?.bytes(filter) ?: 0L }
    val fg = dayRows.map { it?.fg(filter) ?: 0L }
    val bg = dayRows.map { it?.bg(filter) ?: 0L }
    val label = rows.lastOrNull()?.label ?: appKey

    val mobile30 = rows.sumOf { it.mobileBytes }
    val wifi30 = rows.sumOf { it.wifiBytes }
    val todayRow = byDate[today.toString()]
    val daysUsed = rows.count { it.bytes(filter) > 0 }
    val splitKnown = rows.any { it.mobileFgBytes + it.wifiFgBytes > 0 }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(appKey, label, 48.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    Text(appKey, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FilterToggle(filter) { filter = it }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Last 30 days", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    DailyBarChart(dates, fg, bg, total)
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatLine("Today — mobile", Fmt.bytes(todayRow?.mobileBytes ?: 0L))
                    StatLine("Today — Wi-Fi", Fmt.bytes(todayRow?.wifiBytes ?: 0L))
                    HorizontalDivider()
                    StatLine("30 days — mobile", Fmt.bytes(mobile30))
                    StatLine("30 days — Wi-Fi", Fmt.bytes(wifi30))
                    StatLine(
                        "Average per day (${filter.label})",
                        Fmt.bytes(if (daysUsed > 0) total.sum() / 30 else 0L)
                    )
                    HorizontalDivider()
                    Text("Foreground vs background (30 days)", style = MaterialTheme.typography.titleSmall)
                    if (splitKnown) {
                        StatLine("Mobile — while open", Fmt.bytes(rows.sumOf { it.mobileFgBytes }))
                        StatLine("Mobile — in background", Fmt.bytes(rows.sumOf { it.mobileBgBytes }))
                        StatLine("Wi-Fi — while open", Fmt.bytes(rows.sumOf { it.wifiFgBytes }))
                        StatLine("Wi-Fi — in background", Fmt.bytes(rows.sumOf { it.wifiBgBytes }))
                    } else {
                        Text(
                            "Your phone doesn't report a foreground/background split for this app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatLine(name: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(name, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
