package com.fuseforge.chromatone.audio

import com.fuseforge.chromatone.NoiseType
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

object NoiseGenerator {
    // Pink noise state
    private var pinkRows = DoubleArray(16)
    private var pinkRunningSum = 0.0

    // Brown noise state
    private var brownLast = 0.0

    // Green noise state
    private var greenPhase = 0.0

    // Blue noise state
    private var blueLast = 0.0

    // Violet noise state
    private var violetLast = 0.0
    private var violetLastBlue = 0.0

    fun reset() {
        pinkRows = DoubleArray(16)
        pinkRunningSum = 0.0
        brownLast = 0.0
        greenPhase = 0.0
        blueLast = 0.0
        violetLast = 0.0
        violetLastBlue = 0.0
    }

    fun generateWhiteNoise(numSamples: Int): ShortArray {
        val buffer = ShortArray(numSamples)
        for (i in buffer.indices) {
            buffer[i] = (Random.nextDouble(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    fun generatePinkNoise(numSamples: Int): ShortArray {
        val buffer = ShortArray(numSamples)
        val rows = pinkRows.size
        val random = Random.Default
        for (i in buffer.indices) {
            val k = random.nextInt(rows)
            val newValue = random.nextDouble(-1.0, 1.0)
            pinkRunningSum -= pinkRows[k]
            pinkRunningSum += newValue
            pinkRows[k] = newValue
            buffer[i] = ((pinkRunningSum / rows) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    fun generateBrownNoise(numSamples: Int): ShortArray {
        val buffer = ShortArray(numSamples)
        for (i in buffer.indices) {
            val white = Random.nextDouble(-1.0, 1.0)
            brownLast = brownLast * 0.996 + 0.004 * white
            buffer[i] = (tanh(brownLast * 4.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    fun generateGreenNoise(numSamples: Int, sampleRate: Int = 44100): ShortArray {
        val buffer = ShortArray(numSamples)
        val freq = 500.0
        val phaseIncrement = 2 * PI * freq / sampleRate
        for (i in buffer.indices) {
            val white = Random.nextDouble(-1.0, 1.0)
            val mod = sin(greenPhase)
            greenPhase += phaseIncrement
            if (greenPhase > 2 * PI) greenPhase -= 2 * PI
            buffer[i] = ((white * mod) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    fun generateBlueNoise(numSamples: Int): ShortArray {
        val buffer = ShortArray(numSamples)
        for (i in buffer.indices) {
            val white = Random.nextDouble(-1.0, 1.0)
            val blue = white - blueLast
            blueLast = white
            buffer[i] = (blue * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    fun generateVioletNoise(numSamples: Int): ShortArray {
        val buffer = ShortArray(numSamples)
        for (i in buffer.indices) {
            val white = Random.nextDouble(-1.0, 1.0)
            val blue = white - violetLast
            val violet = blue - violetLastBlue
            violetLast = white
            violetLastBlue = blue
            buffer[i] = (violet * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    fun getNoiseBuffer(type: NoiseType, numSamples: Int): ShortArray = when (type) {
        NoiseType.White -> generateWhiteNoise(numSamples)
        NoiseType.Pink -> generatePinkNoise(numSamples)
        NoiseType.Brown -> generateBrownNoise(numSamples)
        NoiseType.Green -> generateGreenNoise(numSamples)
        NoiseType.Blue -> generateBlueNoise(numSamples)
        NoiseType.Violet -> generateVioletNoise(numSamples)
    }
}
