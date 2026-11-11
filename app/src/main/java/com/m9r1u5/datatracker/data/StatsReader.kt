package com.m9r1u5.datatracker.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reads Android's own per-app network records (NetworkStatsManager).
 * Mobile data covers every SIM in the phone, added together.
 */
class StatsReader(private val context: Context) {
    private val nsm = context.getSystemService(NetworkStatsManager::class.java)
    private val pm = context.packageManager

    private class Acc {
        var total = 0L
        var fg = 0L
        var bg = 0L
    }

    private data class UidInfo(val key: String, val label: String, val system: Boolean)

    private val cache = HashMap<Int, UidInfo>()

    fun clearCache() = cache.clear()

    fun readDay(day: LocalDate): List<DailyUsage> {
        val zone = ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val mobile = HashMap<Int, Acc>()
        val wifi = HashMap<Int, Acc>()
        for (sub in mobileSubscriberIds()) {
            @Suppress("DEPRECATION")
            query(ConnectivityManager.TYPE_MOBILE, sub, start, end, mobile)
        }
        @Suppress("DEPRECATION")
        query(ConnectivityManager.TYPE_WIFI, "", start, end, wifi)

        val out = LinkedHashMap<String, DailyUsage>()
        val date = day.toString()
        for (uid in mobile.keys + wifi.keys) {
            val info = describe(uid)
            val m = mobile[uid]
            val w = wifi[uid]
            val prev = out[info.key]
            out[info.key] = DailyUsage(
                date = date,
                appKey = info.key,
                label = info.label,
                isSystem = info.system,
                mobileBytes = (m?.total ?: 0L) + (prev?.mobileBytes ?: 0L),
                wifiBytes = (w?.total ?: 0L) + (prev?.wifiBytes ?: 0L),
                mobileFgBytes = (m?.fg ?: 0L) + (prev?.mobileFgBytes ?: 0L),
                mobileBgBytes = (m?.bg ?: 0L) + (prev?.mobileBgBytes ?: 0L),
                wifiFgBytes = (w?.fg ?: 0L) + (prev?.wifiFgBytes ?: 0L),
                wifiBgBytes = (w?.bg ?: 0L) + (prev?.wifiBgBytes ?: 0L),
            )
        }
        return out.values.filter { it.mobileBytes + it.wifiBytes > 0 }
    }

    private fun query(type: Int, subscriberId: String?, start: Long, end: Long, into: HashMap<Int, Acc>) {
        val stats: NetworkStats? = try {
            nsm.querySummary(type, subscriberId, start, end)
        } catch (e: Exception) {
            null
        }
        if (stats == null) return
        try {
            val b = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(b)
                val bytes = b.rxBytes + b.txBytes
                if (bytes <= 0) continue
                val acc = into.getOrPut(b.uid) { Acc() }
                acc.total += bytes
                if (b.state == NetworkStats.Bucket.STATE_FOREGROUND) acc.fg += bytes else acc.bg += bytes
            }
        } finally {
            stats.close()
        }
    }

    /**
     * Android 10+ : null means "all mobile networks", which already combines both SIMs.
     * Android 8–9 : each SIM has to be asked for separately (needs phone permission).
     */
    @SuppressLint("MissingPermission", "HardwareIds")
    private fun mobileSubscriberIds(): List<String?> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return listOf(null)
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return listOf(null)
        }
        return try {
            val sm = context.getSystemService(SubscriptionManager::class.java)
            val tm = context.getSystemService(TelephonyManager::class.java)
            val subs = sm.activeSubscriptionInfoList ?: emptyList()
            @Suppress("DEPRECATION")
            val ids = subs.mapNotNull { tm.createForSubscriptionId(it.subscriptionId).subscriberId }.distinct()
            if (ids.isEmpty()) listOf(null) else ids
        } catch (e: Exception) {
            listOf(null)
        }
    }

    private fun describe(uid: Int): UidInfo = cache.getOrPut(uid) {
        when (uid) {
            NetworkStats.Bucket.UID_REMOVED -> UidInfo("removed", "Removed apps", true)
            NetworkStats.Bucket.UID_TETHERING -> UidInfo("tethering", "Hotspot & tethering", false)
            0 -> UidInfo("uid:0", "Android OS (root)", true)
            Process.SYSTEM_UID -> UidInfo("android", "Android system", true)
            else -> {
                val pkgs = try { pm.getPackagesForUid(uid)?.sorted() } catch (e: Exception) { null }
                if (pkgs.isNullOrEmpty()) {
                    UidInfo("uid:$uid", "System process $uid", true)
                } else {
                    val pkg = pkgs.first()
                    val ai: ApplicationInfo? = try {
                        @Suppress("DEPRECATION")
                        pm.getApplicationInfo(pkg, 0)
                    } catch (e: Exception) {
                        null
                    }
                    var label = ai?.loadLabel(pm)?.toString() ?: pkg
                    if (pkgs.size > 1) label += " (+${pkgs.size - 1} shared)"
                    val systemFlag = ai != null && (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val hasLauncher = pm.getLaunchIntentForPackage(pkg) != null
                    UidInfo(pkg, label, uid < Process.FIRST_APPLICATION_UID || (systemFlag && !hasLauncher))
                }
            }
        }
    }
}
