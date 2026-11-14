package com.wakeus.app

import com.wakeus.app.ui.secret.BabyLoveTextTapDetector
import com.wakeus.app.ui.secret.SecretHeartTapDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BabyLoveTextTapDetectorTest {
    @Test
    fun nineFastWordmarkTapsTriggerOnceAndReset() {
        var triggers = 0
        val detector = BabyLoveTextTapDetector(onTrigger = { triggers++ })

        repeat(8) { index -> assertFalse(detector.onTextTap(1_000L + index * 150L)) }
        assertTrue(detector.onTextTap(2_200L))

        assertEquals(1, triggers)
        assertEquals(0, detector.tapCount)
        assertFalse(detector.onTextTap(2_300L))
        assertEquals(1, triggers)
    }

    @Test
    fun tapAfterTimeoutStartsANewSequence() {
        val detector = BabyLoveTextTapDetector()
        detector.onTextTap(1_000L)
        detector.onTextTap(1_500L)

        detector.onTextTap(2_501L)

        assertEquals(1, detector.tapCount)
    }

    @Test
    fun wordmarkAndHeartCountersAreIndependent() {
        var heartTriggers = 0
        var babyLoveTriggers = 0
        val heart = SecretHeartTapDetector(onTrigger = { heartTriggers++ })
        val text = BabyLoveTextTapDetector(onTrigger = { babyLoveTriggers++ })

        repeat(8) { index ->
            heart.onHeartTap(1_000L + index * 100L)
            text.onTextTap(1_000L + index * 100L)
        }

        assertEquals(1, heartTriggers)
        assertEquals(0, babyLoveTriggers)
        assertEquals(8, text.tapCount)

        assertTrue(text.onTextTap(1_800L))
        assertEquals(1, babyLoveTriggers)
        assertEquals(1, heartTriggers)
    }
}
