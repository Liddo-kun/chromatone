package com.fuseforge.chromatone.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object PcmDecoder {
    private const val CACHE_VERSION = 2
    private const val TARGET_SAMPLE_RATE = 48000

    fun decode(context: Context, key: String, rawResId: Int): ShortArray {
        return readCache(context, key) ?: decodeAndCache(context, key, rawResId)
    }

    private fun readCache(context: Context, key: String): ShortArray? {
        val file = cacheFile(context, key)
        if (!file.exists()) return null
        val bytes = file.readBytes()
        val shortArray = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray)
        return shortArray
    }

    private fun decodeAndCache(context: Context, key: String, rawResId: Int): ShortArray {
        val result = decodeFromCodec(context, rawResId)
        val dir = cacheDir(context)
        dir.mkdirs()
        val bytes = ByteArray(result.size * 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(result)
        cacheFile(context, key).writeBytes(bytes)
        return result
    }

    private fun cacheDir(context: Context): File {
        return File(context.filesDir, "pcm_v$CACHE_VERSION")
    }

    private fun cacheFile(context: Context, key: String): File {
        return File(cacheDir(context), "$key.pcm")
    }

    private fun decodeFromCodec(context: Context, rawResId: Int): ShortArray {
        val afd = context.resources.openRawResourceFd(rawResId)
        val extractor = MediaExtractor()
        extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        afd.close()

        var audioTrackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                audioTrackIndex = i
                break
            }
        }
        extractor.selectTrack(audioTrackIndex)

        val format = extractor.getTrackFormat(audioTrackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val chunks = mutableListOf<ShortArray>()
        var totalSamples = 0
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false

        while (true) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(0)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
            if (outputIndex >= 0) {
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    codec.releaseOutputBuffer(outputIndex, false)
                    break
                }
                val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                outputBuffer.position(bufferInfo.offset)
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                val shortBuffer = outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                val samples = ShortArray(shortBuffer.remaining())
                shortBuffer.get(samples)

                val stereoSamples = if (channels == 1) {
                    ShortArray(samples.size * 2).also { out ->
                        for (i in samples.indices) {
                            out[i * 2] = samples[i]
                            out[i * 2 + 1] = samples[i]
                        }
                    }
                } else {
                    samples
                }
                chunks.add(stereoSamples)
                totalSamples += stereoSamples.size

                codec.releaseOutputBuffer(outputIndex, false)
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        val result = ShortArray(totalSamples)
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(result, offset)
            offset += chunk.size
        }

        if (sampleRate != TARGET_SAMPLE_RATE) {
            return resample(result, sampleRate, TARGET_SAMPLE_RATE)
        }
        return result
    }

    private fun resample(input: ShortArray, fromRate: Int, toRate: Int): ShortArray {
        val ratio = fromRate.toDouble() / toRate.toDouble()
        val inputFrames = input.size / 2
        val outputFrames = (inputFrames / ratio).toInt()
        val output = ShortArray(outputFrames * 2)
        for (i in 0 until outputFrames) {
            val srcPos = i * ratio
            val srcFrame = srcPos.toInt()
            val frac = srcPos - srcFrame
            val nextFrame = if (srcFrame + 1 < inputFrames) srcFrame + 1 else srcFrame
            val s0L = input[srcFrame * 2].toInt()
            val s0R = input[srcFrame * 2 + 1].toInt()
            val s1L = input[nextFrame * 2].toInt()
            val s1R = input[nextFrame * 2 + 1].toInt()
            output[i * 2]     = (s0L + (s1L - s0L) * frac).toInt().toShort()
            output[i * 2 + 1] = (s0R + (s1R - s0R) * frac).toInt().toShort()
        }
        return output
    }
}
