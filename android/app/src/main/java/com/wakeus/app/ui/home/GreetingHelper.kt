package com.wakeus.app.ui.home

import java.time.LocalTime

/**
 * Reusable, pure utility function for determining the greeting based on local device time.
 *
 * Supported ranges:
 * - Morning:   05:00 .. 11:59
 * - Afternoon: 12:00 .. 16:59
 * - Evening:   17:00 .. 20:59
 * - Night:     21:00 .. 04:59
 */
object GreetingHelper {

    fun getGreeting(time: LocalTime = LocalTime.now()): String {
        val totalMinutes = time.hour * 60 + time.minute
        return when {
            // 05:00 (300 mins) to 11:59 (719 mins)
            totalMinutes in 300..719 -> "Good morning"
            // 12:00 (720 mins) to 16:59 (1019 mins)
            totalMinutes in 720..1019 -> "Good afternoon"
            // 17:00 (1020 mins) to 20:59 (1259 mins)
            totalMinutes in 1020..1259 -> "Good evening"
            // 21:00 (1260 mins) to 04:59 (299 mins)
            else -> "Good night"
        }
    }
}
