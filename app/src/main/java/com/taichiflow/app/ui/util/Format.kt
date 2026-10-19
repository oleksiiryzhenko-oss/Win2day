package com.taichiflow.app.ui.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    private val dayFormat = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    private val fullFormat = DateTimeFormatter.ofPattern("EEE, MMM d yyyy · HH:mm", Locale.US)
    private val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

    fun clock(totalSec: Int): String {
        val s = totalSec.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%02d:%02d", m, sec)
    }

    fun clockMs(ms: Long): String = clock(((ms + 999) / 1000).toInt())

    fun number(value: Long): String = NumberFormat.getIntegerInstance(Locale.US).format(value)
    fun number(value: Int): String = number(value.toLong())

    fun day(timestamp: Long): String = local(timestamp).format(dayFormat)
    fun date(timestamp: Long): String = local(timestamp).format(dateFormat)
    fun full(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(fullFormat)

    fun minutes(totalSec: Int): String {
        val m = totalSec / 60
        return if (m >= 60) "${m / 60}h ${m % 60}m" else "$m min"
    }

    fun relativeDay(timestamp: Long, today: LocalDate = LocalDate.now()): String {
        val d = local(timestamp)
        return when (d) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> d.format(dayFormat)
        }
    }

    private fun local(timestamp: Long): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
}
