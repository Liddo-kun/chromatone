package com.fuseforge.chromatone.audio

import com.fuseforge.chromatone.NoiseType
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

object NoiseGenerator {
    // Pink noise state (decorrelated L/R)
    private var pinkRowsL = DoubleArray(16)
    private var pinkRowsR = DoubleArray(16)
    private var pinkSumL = 0.0
    private var pinkSumR = 0.0

    // Brown noise state (mono — duplicated L=R as tonal anchor)
    private var brownLast = 0.0

    // Green noise state (mono — duplicated L=R as tonal anchor)
    private var greenPhase = 0.0

    // Blue noise state (decorrelated L/R)
    private var blueLastL = 0.0
    private var blueLastR = 0.0

    // Violet noise state (decorrelated L/R)
    private var violetLastL = 0.0
    private var violetLastR = 0.0
    private var violetLastBlueL = 0.0
    private var violetLastBlueR = 0.0

    fun reset() {
        pinkRowsL = DoubleArray(16)
        pinkRowsR = DoubleArray(16)
        pinkSumL = 0.0
        pinkSumR = 0.0
        brownLast = 0.0
        greenPhase = 0.0
        blueLastL = 0.0
        blueLastR = 0.0
        violetLastL = 0.0
        violetLastR = 0.0
        violetLastBlueL = 0.0
        violetLastBlueR = 0.0
    }

    fun generateWhiteNoise(numFrames: Int): ShortArray {
        val out = ShortArray(numFrames * 2)
        for (i in 0 until numFrames) {
            out[i * 2]     = (Random.nextDouble(-1.0, 1.0) * Short.MAX_VALUE * 0.5).toInt().toShort()
            out[i * 2 + 1] = (Random.nextDouble(-1.0, 1.0) * Short.MAX_VALUE * 0.5).toInt().toShort()
        }
        return out
    }

    fun generatePinkNoise(numFrames: Int): ShortArray {
        val out = ShortArray(numFrames * 2)
        val random = Random.Default
        val rows = pinkRowsL.size
        for (i in 0 until numFrames) {
            val kL = random.nextInt(rows)
            val nvL = random.nextDouble(-1.0, 1.0)
            pinkSumL -= pinkRowsL[kL]
            pinkSumL += nvL
            pinkRowsL[kL] = nvL
            out[i * 2] = ((pinkSumL / rows) * Short.MAX_VALUE).toInt().toShort()

            val kR = random.nextInt(rows)
            val nvR = random.nextDouble(-1.0, 1.0)
            pinkSumR -= pinkRowsR[kR]
            pinkSumR += nvR
            pinkRowsR[kR] = nvR
            out[i * 2 + 1] = ((pinkSumR / rows) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    fun generateBrownNoise(numFrames: Int): ShortArray {
        val out = ShortArray(numFrames * 2)
        for (i in 0 until numFrames) {
            val white = Random.nextDouble(-1.0, 1.0)
            brownLast = brownLast * 0.9917 + 0.0083 * white
            val s = (tanh(brownLast * 10.0) * Short.MAX_VALUE).toInt().toShort()
            out[i * 2] = s
            out[i * 2 + 1] = s
        }
        return out
    }

    fun generateGreenNoise(numFrames: Int, sampleRate: Int = 48000): ShortArray {
        val out = ShortArray(numFrames * 2)
        val freq = 500.0
        val phaseIncrement = 2 * PI * freq / sampleRate
        for (i in 0 until numFrames) {
            val white = Random.nextDouble(-1.0, 1.0)
            val mod = sin(greenPhase)
            greenPhase += phaseIncrement
            if (greenPhase > 2 * PI) greenPhase -= 2 * PI
            val s = ((white * mod) * Short.MAX_VALUE).toInt().toShort()
            out[i * 2] = s
            out[i * 2 + 1] = s
        }
        return out
    }

    fun generateBlueNoise(numFrames: Int): ShortArray {
        val out = ShortArray(numFrames * 2)
        for (i in 0 until numFrames) {
            val whiteL = Random.nextDouble(-1.0, 1.0)
            val blueL = whiteL - blueLastL
            blueLastL = whiteL
            out[i * 2] = (blueL * Short.MAX_VALUE).toInt().toShort()

            val whiteR = Random.nextDouble(-1.0, 1.0)
            val blueR = whiteR - blueLastR
            blueLastR = whiteR
            out[i * 2 + 1] = (blueR * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    fun generateVioletNoise(numFrames: Int): ShortArray {
        val out = ShortArray(numFrames * 2)
        for (i in 0 until numFrames) {
            val whiteL = Random.nextDouble(-1.0, 1.0)
            val blueL = whiteL - violetLastL
            val violetL = blueL - violetLastBlueL
            violetLastL = whiteL
            violetLastBlueL = blueL
            out[i * 2] = (violetL * Short.MAX_VALUE).toInt().toShort()

            val whiteR = Random.nextDouble(-1.0, 1.0)
            val blueR = whiteR - violetLastR
            val violetR = blueR - violetLastBlueR
            violetLastR = whiteR
            violetLastBlueR = blueR
            out[i * 2 + 1] = (violetR * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    fun getNoiseBuffer(type: NoiseType, numFrames: Int): ShortArray = when (type) {
        NoiseType.White -> generateWhiteNoise(numFrames)
        NoiseType.Pink -> generatePinkNoise(numFrames)
        NoiseType.Brown -> generateBrownNoise(numFrames)
        NoiseType.Green -> generateGreenNoise(numFrames)
        NoiseType.Blue -> generateBlueNoise(numFrames)
        NoiseType.Violet -> generateVioletNoise(numFrames)
    }
}
