@file:OptIn(ExperimentalMaterial3Api::class)

package com.m9r1u5.datatracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.m9r1u5.datatracker.data.DailyUsage
import com.m9r1u5.datatracker.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class NetFilter(val label: String) { BOTH("Both"), MOBILE("Mobile"), WIFI("Wi-Fi") }

fun DailyUsage.bytes(f: NetFilter): Long = when (f) {
    NetFilter.MOBILE -> mobileBytes
    NetFilter.WIFI -> wifiBytes
    NetFilter.BOTH -> mobileBytes + wifiBytes
}

fun DailyUsage.fg(f: NetFilter): Long = when (f) {
    NetFilter.MOBILE -> mobileFgBytes
    NetFilter.WIFI -> wifiFgBytes
    NetFilter.BOTH -> mobileFgBytes + wifiFgBytes
}

fun DailyUsage.bg(f: NetFilter): Long = when (f) {
    NetFilter.MOBILE -> mobileBgBytes
    NetFilter.WIFI -> wifiBgBytes
    NetFilter.BOTH -> mobileBgBytes + wifiBgBytes
}

@Composable
fun FilterToggle(selected: NetFilter, onSelect: (NetFilter) -> Unit) {
    val all = NetFilter.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        all.forEachIndexed { i, f ->
            SegmentedButton(
                selected = f == selected,
                onClick = { onSelect(f) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = all.size),
            ) { Text(f.label) }
        }
    }
}

/** A simple horizontal bar, 0..1 */
@Composable
fun Bar(fraction: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color)
        )
    }
}

private val iconCache = HashMap<String, ImageBitmap?>()

@Composable
fun AppIcon(appKey: String, label: String, size: Dp = 40.dp) {
    val context = LocalContext.current
    val icon by produceState(initialValue = iconCache[appKey], appKey) {
        if (!iconCache.containsKey(appKey)) {
            val bmp = withContext(Dispatchers.IO) {
                if (!appKey.contains('.')) null
                else try {
                    context.packageManager.getApplicationIcon(appKey).toBitmap(96, 96).asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
            iconCache[appKey] = bmp
            value = bmp
        }
    }
    val bmp = icon
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label.take(1).uppercase(),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun UsageRow(row: DailyUsage, filter: NetFilter, maxBytes: Long, extra: String?, onClick: () -> Unit) {
    val value = row.bytes(filter)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(row.appKey, row.label)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(Fmt.bytes(value), fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Mobile ${Fmt.bytes(row.mobileBytes)}  ·  Wi-Fi ${Fmt.bytes(row.wifiBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (extra != null) {
                Text(extra, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
            Spacer(Modifier.height(4.dp))
            Bar(if (maxBytes > 0) value.toFloat() / maxBytes else 0f)
        }
    }
}

/** Header (cards, toggles) followed by the per-app list, biggest user first. */
@Composable
fun UsageList(
    rows: List<DailyUsage>,
    filter: NetFilter,
    showSystem: Boolean,
    onOpenApp: (String) -> Unit,
    extra: ((DailyUsage) -> String?)? = null,
    emptyText: String = "No data used yet.",
    header: @Composable () -> Unit,
) {
    val visible = remember(rows, filter, showSystem) {
        rows.filter { (showSystem || !it.isSystem) && it.bytes(filter) > 0 }
            .sortedByDescending { it.bytes(filter) }
    }
    val hiddenCount = remember(rows, filter, showSystem) {
        if (showSystem) 0 else rows.count { it.isSystem && it.bytes(filter) > 0 }
    }
    val max = visible.firstOrNull()?.bytes(filter) ?: 0L
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                header()
            }
        }
        if (visible.isEmpty()) {
            item { Text(emptyText, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(visible, key = { it.appKey }) { r ->
            UsageRow(r, filter, max, extra?.invoke(r)) { onOpenApp(r.appKey) }
        }
        if (hiddenCount > 0) {
            item {
                Text(
                    "$hiddenCount system item(s) hidden — turn on \"Show system apps\" in Settings to see them.",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TotalsCard(title: String, mobile: Long, wifi: Long) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Mobile", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Fmt.bytes(mobile), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("All SIMs combined", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f)) {
                    Text("Wi-Fi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Fmt.bytes(wifi), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Bar per day, stacked: foreground (dark) on top of background (light). Tap a bar for its numbers. */
@Composable
fun DailyBarChart(dates: List<LocalDate>, fg: List<Long>, bg: List<Long>, total: List<Long>) {
    var selected by remember(dates) { mutableStateOf<Int?>(null) }
    val fgColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val highlight = MaterialTheme.colorScheme.tertiary
    val max = (total.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Column {
        val sel = selected
        Text(
            if (sel != null) "${dates[sel].format(Fmt.dayShort)}: ${Fmt.bytes(total[sel])}  (foreground ${Fmt.bytes(fg[sel])}, background ${Fmt.bytes(bg[sel])})"
            else "Tap a bar for details · busiest day ${Fmt.bytes(total.maxOrNull() ?: 0L)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(dates) {
                    detectTapGestures { o ->
                        if (dates.isNotEmpty()) {
                            val slot = size.width.toFloat() / dates.size
                            selected = (o.x / slot).toInt().coerceIn(0, dates.size - 1)
                        }
                    }
                }
        ) {
            val n = dates.size
            if (n == 0) return@Canvas
            val slot = size.width / n
            val barW = slot * 0.7f
            for (i in 0 until n) {
                val t = total[i]
                if (t <= 0L) continue
                val x = i * slot + (slot - barW) / 2f
                val h = size.height * (t.toFloat() / max.toFloat())
                val split = fg[i] + bg[i]
                val fgH = if (split > 0L) h * (fg[i].toFloat() / split.toFloat()) else 0f
                val top = size.height - h
                if (i == sel) {
                    drawRect(highlight, topLeft = Offset(x, top), size = Size(barW, h))
                } else {
                    drawRect(bgColor, topLeft = Offset(x, top), size = Size(barW, h))
                    if (fgH > 0f) drawRect(fgColor, topLeft = Offset(x, top), size = Size(barW, fgH))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (dates.isNotEmpty()) {
                Text(dates.first().format(Fmt.dayShort), style = MaterialTheme.typography.labelSmall)
                Text(dates.last().format(Fmt.dayShort), style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(fgColor))
            Text("  Foreground   ", style = MaterialTheme.typography.labelSmall)
            Box(Modifier.size(10.dp).background(bgColor))
            Text("  Background", style = MaterialTheme.typography.labelSmall)
        }
    }
}
