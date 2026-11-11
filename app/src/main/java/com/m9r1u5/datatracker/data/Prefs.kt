package com.m9r1u5.datatracker.data

import android.content.Context

/** Simple settings store. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var bundleGb: Float
        get() = sp.getFloat("bundleGb", 0f)
        set(v) { sp.edit().putFloat("bundleGb", v).apply() }

    /** Day of the month the bundle resets (1–31). */
    var resetDay: Int
        get() = sp.getInt("resetDay", 1)
        set(v) { sp.edit().putInt("resetDay", v.coerceIn(1, 31)).apply() }

    var pricePerGb: Float
        get() = sp.getFloat("pricePerGb", 0f)
        set(v) { sp.edit().putFloat("pricePerGb", v).apply() }

    /** 0 = off */
    var dailyMobileLimitMb: Int
        get() = sp.getInt("dailyMobileLimitMb", 0)
        set(v) { sp.edit().putInt("dailyMobileLimitMb", v).apply() }

    /** 0 = off */
    var perAppLimitMb: Int
        get() = sp.getInt("perAppLimitMb", 0)
        set(v) { sp.edit().putInt("perAppLimitMb", v).apply() }

    var bundleAlerts: Boolean
        get() = sp.getBoolean("bundleAlerts", true)
        set(v) { sp.edit().putBoolean("bundleAlerts", v).apply() }

    var summaryEnabled: Boolean
        get() = sp.getBoolean("summaryEnabled", false)
        set(v) { sp.edit().putBoolean("summaryEnabled", v).apply() }

    var summaryHour: Int
        get() = sp.getInt("summaryHour", 21)
        set(v) { sp.edit().putInt("summaryHour", v).apply() }

    var summaryMinute: Int
        get() = sp.getInt("summaryMinute", 0)
        set(v) { sp.edit().putInt("summaryMinute", v).apply() }

    /** 0 = follow phone, 1 = light, 2 = dark */
    var themeMode: Int
        get() = sp.getInt("themeMode", 0)
        set(v) { sp.edit().putInt("themeMode", v).apply() }

    var showSystem: Boolean
        get() = sp.getBoolean("showSystem", false)
        set(v) { sp.edit().putBoolean("showSystem", v).apply() }

    /** Days before this date are not re-imported after "Clear history". */
    var clearedBefore: String?
        get() = sp.getString("clearedBefore", null)
        set(v) { sp.edit().putString("clearedBefore", v).apply() }

    var lastBackfill: String?
        get() = sp.getString("lastBackfill", null)
        set(v) { sp.edit().putString("lastBackfill", v).apply() }

    var askedNotifications: Boolean
        get() = sp.getBoolean("askedNotifications", false)
        set(v) { sp.edit().putBoolean("askedNotifications", v).apply() }

    fun alertSent(key: String): Boolean = sp.getBoolean("alert_$key", false)
    fun markAlert(key: String) { sp.edit().putBoolean("alert_$key", true).apply() }
}
