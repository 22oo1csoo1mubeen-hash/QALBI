package com.wakeus.app.ui.secret

/**
 * State machine managing the Easter egg eight-consecutive-tap gesture.
 * Enforces a strict 1,000ms maximum interval between qualifying taps.
 */
class SecretHeartTapDetector(
    val maxIntervalMillis: Long = 1000L,
    val requiredTaps: Int = 8,
    private val onTrigger: () -> Unit = {},
) {
    var tapCount: Int = 0
        private set
    var lastTapTimestamp: Long = 0L
        private set

    /**
     * Process a tap on the qualified branding heart.
     * Returns true if this tap completed the gesture and triggered navigation.
     */
    fun onHeartTap(currentTimeMillis: Long): Boolean {
        if (currentTimeMillis - lastTapTimestamp <= maxIntervalMillis) {
            tapCount++
        } else {
            tapCount = 1
        }
        lastTapTimestamp = currentTimeMillis

        if (tapCount >= requiredTaps) {
            reset()
            onTrigger()
            return true
        }
        return false
    }

    /**
     * Resets the gesture counter to 0 immediately.
     * Called on timeout expiration, extraneous tap, navigation away, or lifecycle pause/resume.
     */
    fun reset() {
        tapCount = 0
        lastTapTimestamp = 0L
    }
}
