package com.wakeus.app.ui.secret

/** Tracks consecutive taps on the QALBI wordmark independently of the heart Easter egg. */
class BabyLoveTextTapDetector(
    private val maxIntervalMillis: Long = 1_000L,
    private val requiredTaps: Int = 9,
    private val onTrigger: () -> Unit = {},
) {
    var tapCount: Int = 0
        private set

    private var lastTapTimestamp: Long = 0L

    fun onTextTap(currentTimeMillis: Long): Boolean {
        tapCount = if (lastTapTimestamp != 0L && currentTimeMillis - lastTapTimestamp <= maxIntervalMillis) {
            tapCount + 1
        } else {
            1
        }
        lastTapTimestamp = currentTimeMillis

        if (tapCount < requiredTaps) return false

        reset()
        onTrigger()
        return true
    }

    fun reset() {
        tapCount = 0
        lastTapTimestamp = 0L
    }
}
