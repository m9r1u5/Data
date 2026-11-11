package com.m9r1u5.datatracker.util

import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    /** MB for small values, GB from 1 GB up (1 GB = 1000 MB, like your network provider). */
    fun bytes(b: Long): String {
        val mb = b / 1_000_000.0
        return when {
            b >= 1_000_000_000L -> String.format(Locale.US, "%.2f GB", b / 1e9)
            mb >= 100 -> String.format(Locale.US, "%.0f MB", mb)
            mb >= 10 -> String.format(Locale.US, "%.1f MB", mb)
            else -> String.format(Locale.US, "%.2f MB", mb)
        }
    }

    fun rand(v: Double): String = String.format(Locale.US, "R %.2f", v)

    fun number(v: Float): String =
        if (v == v.toLong().toFloat()) v.toLong().toString() else v.toString()

    val dayLong: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy")
    val dayShort: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
    val month: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
}
