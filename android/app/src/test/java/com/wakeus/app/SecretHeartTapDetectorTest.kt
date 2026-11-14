package com.wakeus.app

import com.wakeus.app.ui.secret.SecretHeartTapDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretHeartTapDetectorTest {

    @Test
    fun testEightConsecutiveTapsTriggersNavigation() {
        var triggeredCount = 0
        val detector = SecretHeartTapDetector(
            maxIntervalMillis = 1000L,
            requiredTaps = 8,
            onTrigger = { triggeredCount++ },
        )

        var time = 1000L
        for (i in 1..7) {
            val triggered = detector.onHeartTap(time)
            assertFalse("Tap $i should not trigger navigation", triggered)
            assertEquals("Tap count should be $i", i, detector.tapCount)
            time += 200L // 200ms interval between taps
        }

        // 8th tap
        val triggered = detector.onHeartTap(time)
        assertTrue("8th tap should trigger navigation", triggered)
        assertEquals("Callback should be invoked exactly once", 1, triggeredCount)
        assertEquals("Tap count should reset to 0 after trigger", 0, detector.tapCount)
        assertEquals("Timestamp should reset to 0 after trigger", 0L, detector.lastTapTimestamp)
    }

    @Test
    fun testSevenTapsDoesNotTrigger() {
        var triggeredCount = 0
        val detector = SecretHeartTapDetector(
            maxIntervalMillis = 1000L,
            requiredTaps = 8,
            onTrigger = { triggeredCount++ },
        )

        var time = 1000L
        for (i in 1..7) {
            detector.onHeartTap(time)
            time += 150L
        }

        assertEquals("7 taps must not trigger navigation", 0, triggeredCount)
        assertEquals(7, detector.tapCount)
    }

    @Test
    fun testTimeoutResetsCounterToOne() {
        var triggeredCount = 0
        val detector = SecretHeartTapDetector(
            maxIntervalMillis = 1000L,
            requiredTaps = 8,
            onTrigger = { triggeredCount++ },
        )

        detector.onHeartTap(1000L) // Tap 1
        detector.onHeartTap(1300L) // Tap 2 (300ms later)
        assertEquals(2, detector.tapCount)

        // Now pause for 1001ms (> 1000ms timeout)
        detector.onHeartTap(2301L) // Tap 3 after timeout
        assertEquals("Tap after timeout should reset counter to 1", 1, detector.tapCount)
        assertEquals(0, triggeredCount)
    }

    @Test
    fun testResetClearsCounterAndTimestamp() {
        var triggeredCount = 0
        val detector = SecretHeartTapDetector(
            maxIntervalMillis = 1000L,
            requiredTaps = 8,
            onTrigger = { triggeredCount++ },
        )

        // 5 qualifying taps
        var time = 5000L
        for (i in 1..5) {
            detector.onHeartTap(time)
            time += 100L
        }
        assertEquals(5, detector.tapCount)

        // Extraneous interaction or lifecycle event resets the detector
        detector.reset()
        assertEquals(0, detector.tapCount)
        assertEquals(0L, detector.lastTapTimestamp)

        // Subsequent taps start fresh from 1
        detector.onHeartTap(time + 100L)
        assertEquals(1, detector.tapCount)
        assertEquals(0, triggeredCount)
    }

    @Test
    fun testTriggerFiresOnlyOnceAndSubsequentTapStartsNewCycle() {
        var triggeredCount = 0
        val detector = SecretHeartTapDetector(
            maxIntervalMillis = 1000L,
            requiredTaps = 8,
            onTrigger = { triggeredCount++ },
        )

        var time = 1000L
        for (i in 1..8) {
            detector.onHeartTap(time)
            time += 100L
        }
        assertEquals(1, triggeredCount)
        assertEquals(0, detector.tapCount)

        // 9th tap should not trigger again, but start a new gesture cycle
        val triggered = detector.onHeartTap(time + 100L)
        assertFalse(triggered)
        assertEquals(1, detector.tapCount)
        assertEquals(1, triggeredCount)
    }
}
