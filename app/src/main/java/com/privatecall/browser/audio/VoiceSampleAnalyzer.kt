package com.privatecall.browser.audio

data class VoiceSampleProfile(
    val brightnessDb: Float,
)

object VoiceSampleAnalyzer {
    fun analyzeWav(bytes: ByteArray): VoiceSampleProfile {
        require(bytes.size >= 44 && ascii(bytes, 0, 4) == "RIFF" && ascii(bytes, 8, 4) == "WAVE") {
            "Choose a PCM WAV audio sample."
        }

        var offset = 12
        var channels = 0
        var sampleRate = 0
        var bitsPerSample = 0
        var audioFormat = 0
        var dataOffset = -1
        var dataSize = 0

        while (offset + 8 <= bytes.size) {
            val chunkName = ascii(bytes, offset, 4)
            val chunkSize = uint32(bytes, offset + 4)
            val chunkStart = offset + 8
            require(chunkSize <= (bytes.size - chunkStart).toLong()) {
                "The WAV sample is incomplete."
            }

            when (chunkName) {
                "fmt " -> {
                    require(chunkSize >= 16L) { "The WAV format header is invalid." }
                    audioFormat = uint16(bytes, chunkStart)
                    channels = uint16(bytes, chunkStart + 2)
                    sampleRate = uint32(bytes, chunkStart + 4).toInt()
                    bitsPerSample = uint16(bytes, chunkStart + 14)
                }
                "data" -> {
                    dataOffset = chunkStart
                    dataSize = chunkSize.toInt()
                }
            }

            val nextOffset = chunkStart.toLong() + chunkSize + (chunkSize and 1L)
            if (nextOffset > bytes.size.toLong()) break
            offset = nextOffset.toInt()
        }

        require(audioFormat == 1 && bitsPerSample == 16 && channels in 1..2) {
            "Use an uncompressed 16-bit mono or stereo PCM WAV sample."
        }
        require(sampleRate in 8_000..192_000) { "The WAV sample rate is not supported." }
        require(dataOffset >= 0 && dataSize >= channels * 2) { "The WAV sample contains no audio." }

        val frameSize = channels * 2
        val frameCount = dataSize / frameSize
        require(frameCount >= sampleRate / 4) { "Use a voice sample at least 0.25 seconds long." }

        val accumulator = VoiceSampleToneAccumulator(sampleRate, channels)
        for (frame in 0 until frameCount) {
            var sample = 0.0
            for (channel in 0 until channels) {
                val sampleOffset = dataOffset + frame * frameSize + channel * 2
                sample += short16(bytes, sampleOffset).toDouble() / 32768.0
            }
            accumulator.addFrame(sample / channels)
        }

        return accumulator.finish()
    }

    private fun ascii(bytes: ByteArray, offset: Int, length: Int): String =
        String(bytes, offset, length, Charsets.US_ASCII)

    private fun uint16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)

    private fun uint32(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xff) or
            ((bytes[offset + 1].toLong() and 0xff) shl 8) or
            ((bytes[offset + 2].toLong() and 0xff) shl 16) or
            ((bytes[offset + 3].toLong() and 0xff) shl 24)

    private fun short16(bytes: ByteArray, offset: Int): Short =
        uint16(bytes, offset).toShort()
}

class VoiceSampleToneAccumulator(
    private val sampleRate: Int,
    channels: Int,
) {
    private val lowPassAlpha = 1.0 - kotlin.math.exp(-2.0 * Math.PI * 1_500.0 / sampleRate)
    private var lowPass = 0.0
    private var lowEnergy = 0.0
    private var highEnergy = 0.0
    private var frameCount = 0

    init {
        require(sampleRate in 8_000..192_000)
        require(channels in 1..2)
    }

    fun addFrame(monoSample: Double) {
        lowPass += lowPassAlpha * (monoSample - lowPass)
        val highPass = monoSample - lowPass
        lowEnergy += lowPass * lowPass
        highEnergy += highPass * highPass
        frameCount++
    }

    fun finish(): VoiceSampleProfile {
        require(frameCount >= sampleRate / 4) { "Use a voice sample at least 0.25 seconds long." }
        val totalEnergy = lowEnergy + highEnergy
        require(totalEnergy > 1e-8) { "The voice sample is silent." }
        val highFrequencyShare = highEnergy / totalEnergy
        val brightnessDb = ((highFrequencyShare - 0.3) * 20.0).coerceIn(-6.0, 6.0).toFloat()
        return VoiceSampleProfile(brightnessDb)
    }
}
