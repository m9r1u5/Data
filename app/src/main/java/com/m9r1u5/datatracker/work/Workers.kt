package com.m9r1u5.datatracker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.m9r1u5.datatracker.DataTrackerApp
import com.m9r1u5.datatracker.data.BundleMath
import com.m9r1u5.datatracker.data.Prefs
import com.m9r1u5.datatracker.util.Fmt
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Runs every hour: saves a snapshot and checks the alerts. */
class SnapshotWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as DataTrackerApp
        app.repo.refreshRecent()
        app.repo.backfill()
        Alerts.check(app, app.repo, app.prefs)
        return Result.success()
    }
}

/** Runs just after midnight: finalises yesterday's numbers. */
class MidnightWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as DataTrackerApp
        app.repo.refreshRecent()
        Scheduler.scheduleMidnight(app)
        return Result.success()
    }
}

/** Sends the daily summary notification at the time chosen in Settings. */
class SummaryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as DataTrackerApp
        val prefs = app.prefs
        if (!prefs.summaryEnabled) return Result.success()
        app.repo.refreshRecent()
        val today = LocalDate.now()
        val rows = app.repo.dao.forDate(today.toString())
        val mobile = rows.sumOf { it.mobileBytes }
        val wifi = rows.sumOf { it.wifiBytes }
        val top = rows.filter { !it.isSystem }.maxByOrNull { it.mobileBytes + it.wifiBytes }
        val sb = StringBuilder("Mobile ${Fmt.bytes(mobile)} · Wi-Fi ${Fmt.bytes(wifi)}.")
        if (top != null) sb.append(" Top app: ${top.label} (${Fmt.bytes(top.mobileBytes + top.wifiBytes)}).")
        if (prefs.bundleGb > 0f) {
            val start = BundleMath.cycleStart(today, prefs.resetDay)
            val used = app.repo.dao.range(start.toString(), today.toString()).sumOf { it.mobileBytes }
            val left = (prefs.bundleGb * 1e9).toLong() - used
            sb.append(if (left >= 0) " Bundle left: ${Fmt.bytes(left)}." else " Bundle over by ${Fmt.bytes(-left)}.")
        }
        Notifs.show(app, 1001, Notifs.SUMMARY, "Today's data use", sb.toString())
        Scheduler.scheduleSummary(app, prefs)
        return Result.success()
    }
}

object Scheduler {
    fun scheduleAll(context: Context, prefs: Prefs) {
        schedulePeriodic(context)
        scheduleMidnight(context)
        scheduleSummary(context, prefs)
    }

    private fun schedulePeriodic(context: Context) {
        val req = PeriodicWorkRequestBuilder<SnapshotWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork("snapshot", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun scheduleMidnight(context: Context) {
        val now = LocalDateTime.now()
        val next = now.toLocalDate().plusDays(1).atTime(0, 5)
        val req = OneTimeWorkRequestBuilder<MidnightWorker>()
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("midnight", ExistingWorkPolicy.REPLACE, req)
    }

    fun scheduleSummary(context: Context, prefs: Prefs) {
        val wm = WorkManager.getInstance(context)
        if (!prefs.summaryEnabled) {
            wm.cancelUniqueWork("summary")
            return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(prefs.summaryHour, prefs.summaryMinute)
        if (!next.isAfter(now.plusMinutes(1))) next = next.plusDays(1)
        val req = OneTimeWorkRequestBuilder<SummaryWorker>()
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniqueWork("summary", ExistingWorkPolicy.REPLACE, req)
    }
}
