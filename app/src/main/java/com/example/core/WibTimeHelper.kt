package com.example.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class WibOperationalState(
    val isoDate: String,
    val formattedDate: String,
    val formattedTime: String,
    val isOpen: Boolean
)

object WibTimeHelper {
    private val wibTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Jakarta")
    private val indonesianLocale: Locale = Locale("id", "ID")

    const val OPEN_HOUR = 8
    const val CLOSE_HOUR = 16

    fun nowCalendarWib(): Calendar = Calendar.getInstance(wibTimeZone)

    fun todayIsoDateWib(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = wibTimeZone
        }
        return formatter.format(Date())
    }

    fun currentIsoUtcTimestamp(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date())
    }

    fun formatWibTime(date: Date = Date()): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
            timeZone = wibTimeZone
        }
        return "${formatter.format(date)} WIB"
    }

    fun formatWibDate(date: Date = Date()): String {
        val formatter = SimpleDateFormat("EEEE, dd MMM yyyy", indonesianLocale).apply {
            timeZone = wibTimeZone
        }
        return formatter.format(date)
    }

    fun isWithinOperationalHoursWib(calendar: Calendar = nowCalendarWib()): Boolean {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekday = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return isWeekday && hour in OPEN_HOUR until CLOSE_HOUR
    }

    fun snapshot(): WibOperationalState {
        val now = Date()
        val cal = Calendar.getInstance(wibTimeZone).apply { time = now }
        return WibOperationalState(
            isoDate = todayIsoDateWib(),
            formattedDate = formatWibDate(now),
            formattedTime = formatWibTime(now),
            isOpen = isWithinOperationalHoursWib(cal)
        )
    }
}
