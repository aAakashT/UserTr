package com.privatecall.browser.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class VoiceSampleAnalyzerTest {
    @Test
    fun brighterReferenceProducesBrighterToneProfile() {
        val sampleRate = 16_000
        val darkerProfile = VoiceSampleAnalyzer.analyzeWav(
            pcmWav(sampleRate = sampleRate, seconds = 1, frequencyHz = 220),
        )
        val brighterProfile = VoiceSampleAnalyzer.analyzeWav(
            pcmWav(sampleRate = sampleRate, seconds = 1, frequencyHz = 5_000),
        )

        assertTrue(darkerProfile.brightnessDb in -6f..6f)
        assertTrue(brighterProfile.brightnessDb in -6f..6f)
        assertTrue(brighterProfile.brightnessDb > darkerProfile.brightnessDb)
    }

    @Test
    fun rejectsNonWavAudio() {
        try {
            VoiceSampleAnalyzer.analyzeWav(byteArrayOf(1, 2, 3))
        } catch (error: IllegalArgumentException) {
            assertEquals("Choose a PCM WAV audio sample.", error.message)
            return
        }
        throw AssertionError("Expected unsupported input to be rejected")
    }

    private fun pcmWav(sampleRate: Int, seconds: Int, frequencyHz: Int): ByteArray {
        val sampleCount = sampleRate * seconds
        val dataSize = sampleCount * 2
        val output = ByteArrayOutputStream()
        output.write("RIFF".toByteArray(Charsets.US_ASCII))
        writeInt(output, 36 + dataSize)
        output.write("WAVEfmt ".toByteArray(Charsets.US_ASCII))
        writeInt(output, 16)
        writeShort(output, 1)
        writeShort(output, 1)
        writeInt(output, sampleRate)
        writeInt(output, sampleRate * 2)
        writeShort(output, 2)
        writeShort(output, 16)
        output.write("data".toByteArray(Charsets.US_ASCII))
        writeInt(output, dataSize)
        repeat(sampleCount) { index ->
            val value = (10_000 * kotlin.math.sin(
                2.0 * Math.PI * frequencyHz * index / sampleRate,
            )).toInt()
            writeShort(output, value)
        }
        return output.toByteArray()
    }

    private fun writeInt(output: ByteArrayOutputStream, value: Int) {
        repeat(4) { shift -> output.write((value ushr (shift * 8)) and 0xff) }
    }

    private fun writeShort(output: ByteArrayOutputStream, value: Int) {
        output.write(value and 0xff)
        output.write((value ushr 8) and 0xff)
    }
}
