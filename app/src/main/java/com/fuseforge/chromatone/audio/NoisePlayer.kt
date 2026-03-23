package com.fuseforge.chromatone.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread

class NoisePlayer(private val bufferProvider: (Int) -> ShortArray) {
    private var audioTrack: AudioTrack? = null
    private var playThread: Thread? = null
    @Volatile private var shouldPlay = false

    val isPlaying: Boolean
        get() = audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING

    fun start() {
        if (isPlaying) return
        shouldPlay = true
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )
        audioTrack?.play()
        playThread = thread(start = true) {
            while (shouldPlay) {
                val noise = bufferProvider(bufferSize)
                audioTrack?.write(noise, 0, noise.size)
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
