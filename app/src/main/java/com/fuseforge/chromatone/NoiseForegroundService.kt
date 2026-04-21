package com.fuseforge.chromatone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fuseforge.chromatone.audio.Biquad
import com.fuseforge.chromatone.audio.NoiseGenerator
import com.fuseforge.chromatone.audio.NoisePlayer
import com.fuseforge.chromatone.audio.PcmDecoder
import kotlinx.coroutines.*

class NoiseForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "chromatone_playback"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "com.fuseforge.chromatone.PLAY"
        const val ACTION_PAUSE = "com.fuseforge.chromatone.PAUSE"
        const val ACTION_STOP = "com.fuseforge.chromatone.STOP"
        const val ACTION_SYNC = "com.fuseforge.chromatone.SYNC"
        const val MASTER_GAIN = 0.8

        @Volatile
        var activeNoises: Map<SoundSource, Float> = emptyMap()
    }

    private var noisePlayer: NoisePlayer? = null
    private var mixBuffer: DoubleArray? = null
    private var resultBuffer: ShortArray? = null
    private val highPassL = Biquad.highPass(40.0, 48000.0)
    private val highPassR = Biquad.highPass(40.0, 48000.0)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var pcmBuffers: Map<String, ShortArray> = emptyMap()
    private val pcmPositions = mutableMapOf<String, Int>()
    private val decoding = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private val isPlaying: Boolean
        get() = noisePlayer?.isPlaying ?: false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> handlePlay()
            ACTION_PAUSE -> handlePause()
            ACTION_STOP -> handleStop()
            ACTION_SYNC -> syncAmbientBuffers()
            else -> if (!isPlaying) handlePlay()
        }
        startForeground(NOTIFICATION_ID, buildNotification(isPlaying))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handlePlay() {
        noisePlayer?.stop()
        NoiseGenerator.reset()
        highPassL.reset()
        highPassR.reset()
        noisePlayer = NoisePlayer { bufferSize ->
            mixNoiseBuffers(bufferSize)
        }
        noisePlayer?.start()
        syncAmbientBuffers()
        updateNotification()
    }

    private fun handlePause() {
        if (!isPlaying) return
        noisePlayer?.stop()
        noisePlayer = null
        updateNotification()
    }

    private fun handleStop() {
        noisePlayer?.stop()
        noisePlayer = null
        pcmBuffers = emptyMap()
        pcmPositions.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun syncAmbientBuffers() {
        val noises = activeNoises
        val activeKeys = noises.keys.filterIsInstance<AmbientSound>().map { it.key }.toSet()

        pcmPositions.keys.removeAll { it !in activeKeys }

        for ((source, volume) in noises) {
            if (source !is AmbientSound || volume <= 0f) continue
            val key = source.key
            if (key in pcmBuffers || key in decoding) continue
            decoding.add(key)
            val resId = source.rawResId
            serviceScope.launch {
                val decoded = PcmDecoder.decode(this@NoiseForegroundService, key, resId)
                pcmBuffers = pcmBuffers + (key to decoded)
                decoding.remove(key)
            }
        }
    }

    private fun mixNoiseBuffers(frameCount: Int): ShortArray {
        val stereoSize = frameCount * 2
        val noises = activeNoises
        if (noises.isEmpty()) return ShortArray(stereoSize)

        var mixed = mixBuffer
        if (mixed == null || mixed.size != stereoSize) {
            mixed = DoubleArray(stereoSize)
            mixBuffer = mixed
        } else {
            mixed.fill(0.0)
        }

        var hasActive = false

        // Mix generated noise (stereo interleaved)
        for ((source, volume) in noises) {
            if (source !is GeneratedNoise || volume <= 0f) continue
            hasActive = true
            val scaledVolume = volume.toDouble() * volume.toDouble()
            val buffer = NoiseGenerator.getNoiseBuffer(source.noiseType, frameCount)
            for (i in 0 until stereoSize) {
                mixed[i] += buffer[i].toDouble() * scaledVolume
            }
        }

        // Mix ambient PCM loops (stereo interleaved, frame-level position tracking)
        val buffers = pcmBuffers
        for ((source, volume) in noises) {
            if (source !is AmbientSound || volume <= 0f) continue
            val pcm = buffers[source.key] ?: continue
            if (pcm.size < 2) continue
            hasActive = true
            val scaledVolume = volume.toDouble() * volume.toDouble()
            val pcmFrames = pcm.size / 2
            var pos = pcmPositions.getOrPut(source.key) { 0 }
            for (i in 0 until frameCount) {
                mixed[i * 2]     += pcm[pos * 2].toDouble()     * scaledVolume
                mixed[i * 2 + 1] += pcm[pos * 2 + 1].toDouble() * scaledVolume
                pos++
                if (pos >= pcmFrames) pos = 0
            }
            pcmPositions[source.key] = pos
        }

        if (!hasActive) return ShortArray(stereoSize)

        highPassL.processInterleaved(mixed, 0, 2)
        highPassR.processInterleaved(mixed, 1, 2)

        var result = resultBuffer
        if (result == null || result.size != stereoSize) {
            result = ShortArray(stereoSize)
            resultBuffer = result
        }
        for (i in 0 until stereoSize) {
            result[i] = (mixed[i] * MASTER_GAIN)
                .coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble())
                .toInt().toShort()
        }
        return result
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(isPlaying))
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val activeNames = activeNoises.filter { it.value > 0f }.keys
            .joinToString(", ") { it.displayName }
        val contentText = if (isPlaying && activeNames.isNotEmpty())
            "Playing $activeNames"
        else if (isPlaying)
            "Playing"
        else
            "Paused"

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "Pause",
                getPendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "Play",
                getPendingIntent(ACTION_PLAY)
            )
        }
        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            getPendingIntent(ACTION_STOP)
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SleepyTone")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .addAction(playPauseAction)
            .addAction(stopAction)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, NoiseForegroundService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Playback",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        noisePlayer?.stop()
        noisePlayer = null
        pcmBuffers = emptyMap()
        pcmPositions.clear()
        super.onDestroy()
    }
}
