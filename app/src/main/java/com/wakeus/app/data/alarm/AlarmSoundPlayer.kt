package com.wakeus.app.data.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import com.wakeus.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Lifecycle-aware, robust alarm sound playback engine.
 * Bundles a native acoustic chime asset (R.raw.qalbi_alarm) with fallback to system alarm ringtone.
 * Handles audio focus, looping, and resource safety without ever crashing.
 */
object AlarmSoundPlayer {

    private const val TAG = "AlarmSoundPlayer"

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isPlayingAlarm = false
    private var isPlayingPreview = false
    private var volumeRampJob: kotlinx.coroutines.Job? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus lost temporarily or permanently. Pausing playback.")
                try {
                    mediaPlayer?.pause()
                } catch (_: Exception) {}
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus regained. Resuming playback.")
                try {
                    mediaPlayer?.start()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Starts continuous looping alarm sound playback with USAGE_ALARM.
     */
    @Synchronized
    fun start(context: Context): Boolean {
        if (isPlayingAlarm) {
            Log.d(TAG, "Alarm sound is already playing. Skipping duplicate start.")
            return true
        }

        stop() // Release any preview or prior instance cleanly

        val appContext = context.applicationContext
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        requestAlarmAudioFocus()

        return try {
            val player = createConfiguredPlayer(appContext, isLooping = true)
            val gentleRamp = AlarmPreferences.isGentleVolumeRamp(appContext)
            if (gentleRamp) {
                player.setVolume(0.15f, 0.15f)
            }
            player.start()
            mediaPlayer = player
            isPlayingAlarm = true

            if (gentleRamp) {
                volumeRampJob?.cancel()
                volumeRampJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                    for (step in 1..10) {
                        kotlinx.coroutines.delay(1500)
                        val vol = (0.15f + (step / 10f) * 0.85f).coerceAtMost(1.0f)
                        try {
                            mediaPlayer?.setVolume(vol, vol)
                        } catch (_: Exception) {
                            break
                        }
                    }
                }
            }

            Log.d(TAG, "Alarm sound playback successfully started. Gentle ramp: $gentleRamp")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm sound playback", e)
            abandonAlarmAudioFocus()
            false
        }
    }

    /**
     * Previews the alarm chime for 5 seconds or until manually stopped (used in Settings).
     */
    @Synchronized
    fun preview(context: Context, onComplete: () -> Unit = {}): Boolean {
        if (isPlayingAlarm) {
            Log.d(TAG, "Real alarm is currently active. Ignoring preview request.")
            return false
        }

        stop()

        val appContext = context.applicationContext
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        requestAlarmAudioFocus()

        return try {
            val player = createConfiguredPlayer(appContext, isLooping = false)
            player.setOnCompletionListener {
                stop()
                onComplete()
            }
            player.start()
            mediaPlayer = player
            isPlayingPreview = true
            Log.d(TAG, "Alarm sound preview started.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preview alarm sound", e)
            abandonAlarmAudioFocus()
            false
        }
    }

    /**
     * Stops and releases the player and abandons audio focus.
     */
    @Synchronized
    fun stop() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping media player", e)
        } finally {
            volumeRampJob?.cancel()
            volumeRampJob = null
            mediaPlayer = null
            isPlayingAlarm = false
            isPlayingPreview = false
            abandonAlarmAudioFocus()
        }
    }

    val isPlaying: Boolean
        @Synchronized get() = isPlayingAlarm || isPlayingPreview

    private fun createConfiguredPlayer(context: Context, isLooping: Boolean): MediaPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        // 1. Try bundled raw asset (qalbi_alarm.wav)
        var player: MediaPlayer? = try {
            val assetUri = Uri.parse("android.resource://${context.packageName}/${R.raw.qalbi_alarm}")
            MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(context, assetUri)
                this.isLooping = isLooping
                prepare()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Bundled raw chime failed to load. Falling back to default system alarm.", e)
            null
        }

        // 2. Fallback to system default alarm or notification ringtone
        if (player == null) {
            val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(context, fallbackUri)
                this.isLooping = isLooping
                prepare()
            }
        }

        return player
    }

    private fun requestAlarmAudioFocus() {
        val am = audioManager ?: return
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }
    }

    private fun abandonAlarmAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (_: Exception) {
        } finally {
            audioFocusRequest = null
            audioManager = null
        }
    }
}
