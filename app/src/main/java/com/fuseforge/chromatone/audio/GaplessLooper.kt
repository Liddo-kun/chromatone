package com.fuseforge.chromatone.audio

import android.content.Context
import android.media.MediaPlayer

class GaplessLooper(private val context: Context, private val rawResId: Int) {
    private var current: MediaPlayer? = null
    private var next: MediaPlayer? = null
    private var volume: Float = 1f

    fun start() {
        current = createPlayer()
        next = createPlayer()
        current?.setNextMediaPlayer(next)
        current?.setOnCompletionListener { onPlayerComplete(it) }
        current?.start()
    }

    fun pause() {
        current?.takeIf { it.isPlaying }?.pause()
    }

    fun setVolume(v: Float) {
        volume = v
        current?.setVolume(v, v)
        next?.setVolume(v, v)
    }

    fun release() {
        current?.release()
        next?.release()
        current = null
        next = null
    }

    val isPlaying: Boolean get() = current?.isPlaying == true

    private fun onPlayerComplete(finished: MediaPlayer) {
        // next is now playing, becomes current
        val old = current
        current = next
        current?.setOnCompletionListener { onPlayerComplete(it) }
        // Recycle finished player as the new next
        old?.release()
        next = createPlayer()
        current?.setNextMediaPlayer(next)
    }

    private fun createPlayer(): MediaPlayer {
        return MediaPlayer.create(context, rawResId).apply {
            setVolume(volume, volume)
        }
    }
}
