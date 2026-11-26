package com.privatecall.browser.audio

import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceToneProcessorTest {
    @Test
    fun disabledEffectsLeaveInputUnchanged() {
        val samples = shortArrayOf(Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE)
        val original = samples.copyOf()

        VoiceToneProcessor(48_000f, "male-radio").process(samples, samples.size, enabled = false)

        assertTrue(original.contentEquals(samples))
    }

    @Test
    fun telephonePresetFiltersInputWithoutClipping() {
        val samples = ShortArray(256) { 30_000 }

        VoiceToneProcessor(48_000f, "female-telephone").process(
            samples,
            samples.size,
            enabled = true,
        )

        assertTrue(samples.any { it != 30_000.toShort() })
        assertTrue(samples.all { it.toInt() in 0..30_000 })
    }

    @Test
    fun brightPresetLimitsPeaksInsteadOfWrapping() {
        val samples = ShortArray(256) { 30_000 }

        VoiceToneProcessor(48_000f, "male-bright").process(samples, samples.size, enabled = true)

        assertTrue(samples[0] == Short.MAX_VALUE)
        assertTrue(samples.all { it.toInt() >= 0 })
    }

    @Test
    fun selectableEffectsChangeTheInputSignal() {
        listOf("effect-robot", "effect-distorted", "effect-echo").forEach { preset ->
            val samples = ShortArray(1024) { 8_000 }
            val original = samples.copyOf()

            VoiceToneProcessor(48_000f, preset).process(samples, samples.size, enabled = true)

            assertTrue("$preset should change microphone samples", !original.contentEquals(samples))
        }
    }
}
