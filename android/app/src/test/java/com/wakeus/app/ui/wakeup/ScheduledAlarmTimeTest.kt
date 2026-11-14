package com.wakeus.app.ui.wakeup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScheduledAlarmTimeTest {

    @Test
    fun `default scheduled time is in the future and aligned to a minute`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 14, 20, 48, 37)
            set(Calendar.MILLISECOND, 456)
        }.timeInMillis

        val result = defaultScheduledAlarmTime(now)

        assertTrue(result.timeInMillis > now)
        assertEquals(0, result.get(Calendar.SECOND))
        assertEquals(0, result.get(Calendar.MILLISECOND))
    }

    @Test
    fun `combined selected date and time retains the selected local hour and minute`() {
        val date = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 0, 0, 0)
        }
        val time = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 14, 7, 42, 0)
        }

        val result = Calendar.getInstance().apply {
            timeInMillis = scheduledAlarmTimeMillis(date, time)
        }

        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, result.get(Calendar.MONTH))
        assertEquals(15, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(7, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(42, result.get(Calendar.MINUTE))
        assertEquals(0, result.get(Calendar.SECOND))
    }
}
