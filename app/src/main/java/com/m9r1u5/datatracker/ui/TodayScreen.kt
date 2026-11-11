@file:OptIn(ExperimentalMaterial3Api::class)

package com.m9r1u5.datatracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.m9r1u5.datatracker.DataTrackerApp
import com.m9r1u5.datatracker.util.Fmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun TodayScreen(app: DataTrackerApp, onOpenApp: (String) -> Unit) {
    val today = remember { LocalDate.now() }
    var filter by rememberSaveable { mutableStateOf(NetFilter.BOTH) }
    val rows by remember(today) { app.repo.dao.observeDate(today.toString()) }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Keep today's numbers fresh while the screen is open.
    LaunchedEffect(today) {
        while (true) {
            app.repo.refreshDay(today)
            delay(60_000)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Today")
                    Text(today.format(Fmt.dayLong), style = MaterialTheme.typography.bodySmall)
                }
            },
            actions = {
                IconButton(onClick = { scope.launch { app.repo.refreshRecent() } }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
        )
        UsageList(
            rows = rows,
            filter = filter,
            showSystem = app.prefs.showSystem,
            onOpenApp = onOpenApp,
        ) {
            TotalsCard("Today", rows.sumOf { it.mobileBytes }, rows.sumOf { it.wifiBytes })
            FilterToggle(filter) { filter = it }
        }
    }
}
