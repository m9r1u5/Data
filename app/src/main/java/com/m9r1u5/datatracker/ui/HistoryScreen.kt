@file:OptIn(ExperimentalMaterial3Api::class)

package com.m9r1u5.datatracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import com.m9r1u5.datatracker.DataTrackerApp
import com.m9r1u5.datatracker.util.Fmt
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

@Composable
fun HistoryScreen(app: DataTrackerApp, onOpenApp: (String) -> Unit) {
    val today = remember { LocalDate.now() }
    var dateStr by rememberSaveable { mutableStateOf(today.toString()) }
    val date = LocalDate.parse(dateStr)
    var showPicker by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(NetFilter.BOTH) }

    val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekEnd = weekStart.plusDays(6)
    val monthStart = date.withDayOfMonth(1)
    val monthEnd = date.withDayOfMonth(date.lengthOfMonth())

    val dayRows by remember(dateStr) { app.repo.dao.observeDate(dateStr) }.collectAsState(initial = emptyList())
    val weekRows by remember(weekStart) {
        app.repo.dao.observeRange(weekStart.toString(), weekEnd.toString())
    }.collectAsState(initial = emptyList())
    val monthRows by remember(monthStart) {
        app.repo.dao.observeRange(monthStart.toString(), monthEnd.toString())
    }.collectAsState(initial = emptyList())

    // If Android still has this day, pull it in.
    LaunchedEffect(dateStr) { app.repo.refreshDay(date) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("History") })
        UsageList(
            rows = dayRows,
            filter = filter,
            showSystem = app.prefs.showSystem,
            onOpenApp = onOpenApp,
            emptyText = "No data stored for this day. Android only keeps a few weeks of history, " +
                "so older days only show up if Data Tracker was already installed."
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { dateStr = date.minusDays(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                }
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.weight(1f)) {
                    Text(date.format(Fmt.dayLong))
                }
                IconButton(
                    onClick = { dateStr = date.plusDays(1).toString() },
                    enabled = date.isBefore(today)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                }
            }
            TotalsCard("This day", dayRows.sumOf { it.mobileBytes }, dayRows.sumOf { it.wifiBytes })
            TotalsCard(
                "Week ${weekStart.format(Fmt.dayShort)} – ${weekEnd.format(Fmt.dayShort)}",
                weekRows.sumOf { it.mobileBytes },
                weekRows.sumOf { it.wifiBytes }
            )
            TotalsCard(
                monthStart.format(Fmt.month),
                monthRows.sumOf { it.mobileBytes },
                monthRows.sumOf { it.wifiBytes }
            )
            FilterToggle(filter) { filter = it }
        }
    }

    if (showPicker) {
        val todayUtc = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtc
            }
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        dateStr = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
