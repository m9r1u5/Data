@file:OptIn(ExperimentalMaterial3Api::class)

package com.m9r1u5.datatracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.m9r1u5.datatracker.DataTrackerApp
import com.m9r1u5.datatracker.data.BundleMath
import com.m9r1u5.datatracker.data.sumByApp
import com.m9r1u5.datatracker.util.Fmt
import java.time.LocalDate

@Composable
fun BundleScreen(app: DataTrackerApp, onOpenApp: (String) -> Unit) {
    val prefs = app.prefs
    val today = remember { LocalDate.now() }

    var bundleText by rememberSaveable { mutableStateOf(if (prefs.bundleGb > 0f) Fmt.number(prefs.bundleGb) else "") }
    var resetText by rememberSaveable { mutableStateOf(prefs.resetDay.toString()) }
    var priceText by rememberSaveable { mutableStateOf(if (prefs.pricePerGb > 0f) Fmt.number(prefs.pricePerGb) else "") }

    val bundleGb = bundleText.replace(',', '.').toFloatOrNull() ?: 0f
    val resetDay = resetText.toIntOrNull()?.coerceIn(1, 31) ?: 1
    val price = priceText.replace(',', '.').toFloatOrNull() ?: 0f

    LaunchedEffect(bundleGb, resetDay, price) {
        prefs.bundleGb = bundleGb
        prefs.resetDay = resetDay
        prefs.pricePerGb = price
    }

    val start = BundleMath.cycleStart(today, resetDay)
    val next = BundleMath.nextCycleStart(start, resetDay)
    val rows by remember(start) {
        app.repo.dao.observeRange(start.toString(), today.toString())
    }.collectAsState(initial = emptyList())

    // Make sure every day of this cycle is stored.
    LaunchedEffect(start) {
        var d = start
        while (!d.isAfter(today)) {
            app.repo.refreshDay(d)
            d = d.plusDays(1)
        }
    }

    val perApp = remember(rows) { rows.sumByApp() }
    val used = rows.sumOf { it.mobileBytes }
    val bundleBytes = (bundleGb * 1e9).toLong()
    val elapsed = BundleMath.daysBetween(start, today) + 1
    val length = BundleMath.daysBetween(start, next)
    val daysLeft = BundleMath.daysBetween(today, next)
    val perDay = used / elapsed
    val projected = perDay * length
    val remaining = bundleBytes - used

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Bundle") })
        UsageList(
            rows = perApp,
            filter = NetFilter.MOBILE,
            showSystem = prefs.showSystem,
            onOpenApp = onOpenApp,
            extra = { r -> if (price > 0f) "≈ ${Fmt.rand(r.mobileBytes / 1e9 * price)} this cycle" else null },
            emptyText = "No mobile data used this cycle yet."
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your bundle", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = bundleText,
                        onValueChange = { bundleText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(7) },
                        label = { Text("Monthly bundle (GB)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row {
                        OutlinedTextField(
                            value = resetText,
                            onValueChange = { resetText = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Resets on day") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(7) },
                            label = { Text("Price per GB (R)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Cycle ${start.format(Fmt.dayShort)} – ${next.minusDays(1).format(Fmt.dayShort)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (bundleBytes <= 0L) {
                        Text("Mobile data used so far: ${Fmt.bytes(used)} (all SIMs combined)")
                        Text(
                            "Enter your bundle size above to see what's left.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val frac = used.toFloat() / bundleBytes.toFloat()
                        Text(
                            "${Fmt.bytes(used)} of ${Fmt.number(bundleGb)} GB used",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Bar(
                            frac,
                            color = if (frac >= 1f) MaterialTheme.colorScheme.error
                            else if (frac >= 0.8f) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary
                        )
                        StatLine(
                            if (remaining >= 0) "Remaining" else "Over by",
                            Fmt.bytes(if (remaining >= 0) remaining else -remaining)
                        )
                        StatLine("Days left (incl. today)", daysLeft.toString())
                        StatLine("Average per day", Fmt.bytes(perDay))
                        StatLine("Projected for the cycle", Fmt.bytes(projected))
                        if (remaining > 0 && daysLeft > 0) {
                            StatLine("You can use per day", Fmt.bytes(remaining / daysLeft))
                        }
                        if (projected > bundleBytes && perDay > 0 && remaining > 0) {
                            val runOut = today.plusDays(remaining / perDay)
                            Text(
                                "At this rate your bundle runs out around ${runOut.format(Fmt.dayShort)}.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (price > 0f) {
                        StatLine("Cost of data used", Fmt.rand(used / 1e9 * price))
                    }
                    Text(
                        "Mobile data, all SIMs combined. Your network may count slightly differently.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text("Mobile data per app this cycle", style = MaterialTheme.typography.titleMedium)
        }
    }
}
