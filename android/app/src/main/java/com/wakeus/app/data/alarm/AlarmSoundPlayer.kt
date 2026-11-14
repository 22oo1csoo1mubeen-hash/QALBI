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
    private var ringtone: android.media.Ringtone? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isPlayingAlarm = false
    private var isPlayingPreview = false
    private var volumeRampJob: kotlinx.coroutines.Job? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Permanent focus loss: attempting immediate reclaim for active alarm.")
                requestAlarmAudioFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Alarms must NEVER pause or silence themselves for transient notifications
                Log.d(TAG, "Transient focus loss ignored. Alarm continues audible playback.")
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus regained.")
                try {
                    mediaPlayer?.start()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Starts continuous looping alarm sound playback with USAGE_ALARM.
     * Guaranteed loud volume and dual playback redundancy.
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

        // Ensure alarm stream volume is at MAXIMUM
        try {
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
                Log.d(TAG, "Set alarm stream volume to MAX ($maxVol)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not adjust alarm stream volume", e)
        }

        requestAlarmAudioFocus()

        var startedAny = false

        // 1. Primary: Custom Acoustic Chime through MediaPlayer (USAGE_ALARM, full volume, looped)
        try {
            val player = createConfiguredPlayer(appContext, isLooping = true)
            player.setVolume(1.0f, 1.0f) // Full volume immediately
            player.start()
            mediaPlayer = player
            startedAny = true
            Log.d(TAG, "MediaPlayer alarm playback successfully started at full volume.")
        } catch (e: Exception) {
            Log.w(TAG, "MediaPlayer start failed, falling back to native Ringtone", e)
        }

        // 2. Fallback: If MediaPlayer failed, use native System Alarm Ringtone
        if (!startedAny) {
            try {
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                if (ringtoneUri != null) {
                    val r = RingtoneManager.getRingtone(appContext, ringtoneUri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        r.audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        r.isLooping = true
                    }
                    r.play()
                    ringtone = r
                    startedAny = true
                    Log.d(TAG, "Native System Alarm Ringtone successfully started as fallback.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Native ringtone start failed", e)
            }
        }

        isPlayingAlarm = startedAny
        if (!startedAny) {
            abandonAlarmAudioFocus()
        }
        return startedAny
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
            player.setVolume(1.0f, 1.0f)
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
     * Stops and releases the player and ringtone, abandoning audio focus.
     */
    @Synchronized
    fun stop() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping ringtone", e)
        } finally {
            ringtone = null
        }

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

        // 1. Try bundled raw asset (qalbi_alarm.wav) directly via openRawResourceFd
        var player: MediaPlayer? = try {
            val afd = context.resources.openRawResourceFd(R.raw.qalbi_alarm)
            if (afd != null) {
                MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    this.isLooping = isLooping
                    prepare()
                }
            } else null
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
