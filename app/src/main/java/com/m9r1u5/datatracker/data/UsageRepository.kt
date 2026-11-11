package com.m9r1u5.datatracker.data

import android.content.Context
import com.m9r1u5.datatracker.util.Perms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class UsageRepository(private val context: Context, private val prefs: Prefs) {
    val dao: UsageDao = UsageDatabase.get(context).dao()
    private val reader = StatsReader(context)
    private val mutex = Mutex()

    /** Copies one day from Android's records into our own database (never lowers a stored value). */
    suspend fun refreshDay(day: LocalDate) {
        withContext(Dispatchers.IO) {
            if (!Perms.hasUsageAccess(context)) return@withContext
            if (day.isAfter(LocalDate.now())) return@withContext
            val cleared = prefs.clearedBefore?.let { LocalDate.parse(it) }
            if (cleared != null && day.isBefore(cleared)) return@withContext
            mutex.withLock {
                val fresh = try { reader.readDay(day) } catch (e: Exception) { emptyList() }
                if (fresh.isEmpty()) return@withLock
                val existing = dao.forDate(day.toString()).associateBy { it.appKey }
                val merged = fresh.map { f ->
                    val e = existing[f.appKey] ?: return@map f
                    f.copy(
                        mobileBytes = max(f.mobileBytes, e.mobileBytes),
                        wifiBytes = max(f.wifiBytes, e.wifiBytes),
                        mobileFgBytes = max(f.mobileFgBytes, e.mobileFgBytes),
                        mobileBgBytes = max(f.mobileBgBytes, e.mobileBgBytes),
                        wifiFgBytes = max(f.wifiFgBytes, e.wifiFgBytes),
                        wifiBgBytes = max(f.wifiBgBytes, e.wifiBgBytes),
                    )
                }
                dao.upsertAll(merged)
            }
        }
    }

    suspend fun refreshRecent() {
        mutex.withLock { reader.clearCache() }
        val today = LocalDate.now()
        refreshDay(today.minusDays(1))
        refreshDay(today)
    }

    /** Imports whatever history Android still keeps (usually a few weeks). Runs once a day. */
    suspend fun backfill(days: Int = 60, force: Boolean = false) {
        val today = LocalDate.now()
        if (!force && prefs.lastBackfill == today.toString()) return
        if (!Perms.hasUsageAccess(context)) return
        mutex.withLock { reader.clearCache() }
        for (d in 0 until days) refreshDay(today.minusDays(d.toLong()))
        prefs.lastBackfill = today.toString()
    }

    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            mutex.withLock { dao.clear() }
            prefs.clearedBefore = LocalDate.now().toString()
        }
    }

    suspend fun exportCsv(out: OutputStream) {
        withContext(Dispatchers.IO) {
            val rows = dao.all()
            out.bufferedWriter().use { w ->
                w.write("date,app,package_or_id,system,mobile_mb,wifi_mb,mobile_bytes,wifi_bytes,mobile_foreground_bytes,mobile_background_bytes,wifi_foreground_bytes,wifi_background_bytes\n")
                for (r in rows) {
                    val label = "\"" + r.label.replace("\"", "\"\"") + "\""
                    w.write(
                        listOf(
                            r.date, label, r.appKey, if (r.isSystem) "yes" else "no",
                            String.format(Locale.US, "%.2f", r.mobileBytes / 1e6),
                            String.format(Locale.US, "%.2f", r.wifiBytes / 1e6),
                            r.mobileBytes, r.wifiBytes, r.mobileFgBytes, r.mobileBgBytes,
                            r.wifiFgBytes, r.wifiBgBytes
                        ).joinToString(",")
                    )
                    w.write("\n")
                }
            }
        }
    }
}

/** Bundle cycle maths. A reset day of 31 means "last day of the month" in short months. */
object BundleMath {
    private fun dayIn(ym: YearMonth, resetDay: Int): LocalDate = ym.atDay(min(resetDay, ym.lengthOfMonth()))

    fun cycleStart(today: LocalDate, resetDay: Int): LocalDate {
        val thisMonth = dayIn(YearMonth.from(today), resetDay)
        return if (!today.isBefore(thisMonth)) thisMonth
        else dayIn(YearMonth.from(today).minusMonths(1), resetDay)
    }

    fun nextCycleStart(start: LocalDate, resetDay: Int): LocalDate =
        dayIn(YearMonth.from(start).plusMonths(1), resetDay)

    fun daysBetween(a: LocalDate, b: LocalDate): Long = ChronoUnit.DAYS.between(a, b)
}
