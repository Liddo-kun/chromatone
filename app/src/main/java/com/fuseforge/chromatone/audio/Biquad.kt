package com.fuseforge.chromatone.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Biquad private constructor(
    private val b0: Double,
    private val b1: Double,
    private val b2: Double,
    private val a1: Double,
    private val a2: Double,
) {
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun processInPlace(buffer: DoubleArray) {
        for (i in buffer.indices) {
            val x0 = buffer[i]
            val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x0
            y2 = y1; y1 = y0
            buffer[i] = y0
        }
    }

    fun processInterleaved(buffer: DoubleArray, channel: Int, channelCount: Int) {
        var i = channel
        while (i < buffer.size) {
            val x0 = buffer[i]
            val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x0
            y2 = y1; y1 = y0
            buffer[i] = y0
            i += channelCount
        }
    }

    fun reset() {
        x1 = 0.0; x2 = 0.0; y1 = 0.0; y2 = 0.0
    }

    companion object {
        fun highPass(cutoffHz: Double, sampleRate: Double, q: Double = 1.0 / sqrt(2.0)): Biquad {
            val w0 = 2.0 * PI * cutoffHz / sampleRate
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0 * q)
            val a0 = 1.0 + alpha
            val b0 = (1.0 + cosW0) / 2.0 / a0
            val b1 = -(1.0 + cosW0) / a0
            val b2 = (1.0 + cosW0) / 2.0 / a0
            val a1 = -2.0 * cosW0 / a0
            val a2 = (1.0 - alpha) / a0
            return Biquad(b0, b1, b2, a1, a2)
        }
    }
}
