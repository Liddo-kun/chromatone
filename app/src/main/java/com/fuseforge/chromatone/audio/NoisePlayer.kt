package com.fuseforge.chromatone.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import kotlin.concurrent.thread

class NoisePlayer(private val bufferProvider: (frameCount: Int) -> ShortArray) {
    private var audioTrack: AudioTrack? = null
    private var playThread: Thread? = null
    @Volatile private var shouldPlay = false

    val isPlaying: Boolean
        get() = audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING

    fun start() {
        if (isPlaying) return
        shouldPlay = true
        val sampleRate = 48000
        val minStereoBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val framesPerWrite = minStereoBytes / 2

        val attributesBuilder = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        if (Build.VERSION.SDK_INT >= 32) {
            attributesBuilder.setSpatializationBehavior(
                AudioAttributes.SPATIALIZATION_BEHAVIOR_NEVER
            )
        }

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributesBuilder.build())
            .setAudioFormat(format)
            .setBufferSizeInBytes(minStereoBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_POWER_SAVING)
            .build()

        audioTrack?.play()
        playThread = thread(start = true, name = "NoisePlayer") {
            while (shouldPlay) {
                val stereo = bufferProvider(framesPerWrite)
                audioTrack?.write(stereo, 0, stereo.size)
            }
        }
    }

    fun stop() {
        shouldPlay = false
        playThread?.join(200)
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        playThread = null
    }
}
