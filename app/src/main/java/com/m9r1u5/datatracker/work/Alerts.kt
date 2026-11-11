package com.m9r1u5.datatracker.work

import android.content.Context
import com.m9r1u5.datatracker.data.BundleMath
import com.m9r1u5.datatracker.data.Prefs
import com.m9r1u5.datatracker.data.UsageRepository
import com.m9r1u5.datatracker.util.Fmt
import java.time.LocalDate

object Alerts {
    /** Checks all limits and sends each alert at most once (per day, or per bundle cycle). */
    suspend fun check(context: Context, repo: UsageRepository, prefs: Prefs) {
        val today = LocalDate.now()
        val date = today.toString()
        val rows = repo.dao.forDate(date)

        val dailyLimit = prefs.dailyMobileLimitMb
        val mobileToday = rows.sumOf { it.mobileBytes }
        if (dailyLimit > 0 && mobileToday > dailyLimit * 1_000_000L) {
            val key = "daily_$date"
            if (!prefs.alertSent(key)) {
                prefs.markAlert(key)
                Notifs.show(
                    context, key.hashCode(), Notifs.ALERTS,
                    "Daily mobile data limit passed",
                    "You've used ${Fmt.bytes(mobileToday)} of mobile data today (your limit is $dailyLimit MB)."
                )
            }
        }

        val appLimit = prefs.perAppLimitMb
        if (appLimit > 0) {
            rows.filter { it.mobileBytes > appLimit * 1_000_000L }.forEach { r ->
                val key = "app_${date}_${r.appKey}"
                if (!prefs.alertSent(key)) {
                    prefs.markAlert(key)
                    Notifs.show(
                        context, key.hashCode(), Notifs.ALERTS,
                        "${r.label} is using a lot of data",
                        "${r.label} has used ${Fmt.bytes(r.mobileBytes)} of mobile data today (your per-app limit is $appLimit MB)."
                    )
                }
            }
        }

        if (prefs.bundleAlerts && prefs.bundleGb > 0f) {
            val start = BundleMath.cycleStart(today, prefs.resetDay)
            val used = repo.dao.range(start.toString(), date).sumOf { it.mobileBytes }
            val bundle = (prefs.bundleGb * 1e9).toLong()
            val pct = if (bundle > 0) used * 100 / bundle else 0L
            if (pct >= 100) {
                val key = "b100_$start"
                if (!prefs.alertSent(key)) {
                    prefs.markAlert(key)
                    prefs.markAlert("b80_$start")
                    Notifs.show(
                        context, key.hashCode(), Notifs.ALERTS,
                        "Data bundle used up",
                        "You've used ${Fmt.bytes(used)} — your whole ${Fmt.number(prefs.bundleGb)} GB bundle for this cycle."
                    )
                }
            } else if (pct >= 80) {
                val key = "b80_$start"
                if (!prefs.alertSent(key)) {
                    prefs.markAlert(key)
                    Notifs.show(
                        context, key.hashCode(), Notifs.ALERTS,
                        "80% of your data bundle used",
                        "You've used ${Fmt.bytes(used)} of your ${Fmt.number(prefs.bundleGb)} GB bundle."
                    )
                }
            }
        }
    }
}
