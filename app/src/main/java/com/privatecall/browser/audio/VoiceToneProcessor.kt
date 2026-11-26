package com.privatecall.browser.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

internal class VoiceToneProcessor(
    sampleRate: Float,
    preset: String,
) {
    private enum class Effect {
        NONE,
        ROBOT,
        DISTORTION,
        ECHO,
    }

    private val effect = when {
        preset.contains("robot") -> Effect.ROBOT
        preset.contains("distorted") -> Effect.DISTORTION
        preset.contains("echo") -> Effect.ECHO
        else -> Effect.NONE
    }
    private val filters = when {
        preset.contains("deep") -> listOf(Filter(sampleRate, 1_150f, highPass = false))
        preset.contains("bright") || preset.contains("crisp") ->
            listOf(Filter(sampleRate, 180f, highPass = true))
        preset.contains("radio") || preset.contains("telephone") ->
            listOf(
                Filter(sampleRate, 300f, highPass = true),
                Filter(sampleRate, 3_400f, highPass = false),
            )
        preset.contains("warm") -> listOf(Filter(sampleRate, 2_400f, highPass = false))
        preset.contains("cinematic") ->
            listOf(
                Filter(sampleRate, 80f, highPass = true),
                Filter(sampleRate, 7_000f, highPass = false),
            )
        preset.contains("podcast") -> listOf(Filter(sampleRate, 100f, highPass = true))
        preset.contains("bass") -> listOf(Filter(sampleRate, 5_200f, highPass = false))
        else -> emptyList()
    }

    private val outputGain = when {
        preset.contains("deep") -> 0.95f
        preset.contains("bright") -> 1.12f
        preset.contains("radio") -> 0.85f
        preset.contains("bass") -> 1.08f
        preset.contains("telephone") -> 0.80f
        else -> 1f
    }

    private val echoBuffer = if (effect == Effect.ECHO) {
        FloatArray((sampleRate * 0.24f).toInt().coerceAtLeast(1))
    } else {
        FloatArray(0)
    }
    private val modulationPhaseIncrement = 2.0 * PI * 35.0 / sampleRate.toDouble()
    private var echoIndex = 0
    private var modulationPhase = 0.0

    fun process(samples: ShortArray, length: Int, enabled: Boolean) {
        if (!enabled) return
        for (index in 0 until length) {
            val input = samples[index] / 32768f
            var output = input
            filters.forEach { output = it.process(output) }
            output = when (effect) {
                Effect.ROBOT -> {
                    modulationPhase += modulationPhaseIncrement
                    if (modulationPhase >= 2.0 * PI) modulationPhase -= 2.0 * PI
                    output * sin(modulationPhase).toFloat()
                }
                Effect.DISTORTION -> {
                    (tanh((output * 4f).toDouble()) / tanh(4.0)).toFloat()
                }
                Effect.ECHO -> {
                    val delayed = echoBuffer[echoIndex]
                    echoBuffer[echoIndex] = (output + delayed * 0.28f).coerceIn(-1f, 1f)
                    echoIndex = (echoIndex + 1) % echoBuffer.size
                    output * 0.78f + delayed * 0.42f
                }
                Effect.NONE -> output
            }
            output *= outputGain
            samples[index] = (output.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        }
    }

    private class Filter(
        sampleRate: Float,
        cutoffHz: Float,
        private val highPass: Boolean,
    ) {
        private val alpha = exp((-2.0 * PI * cutoffHz / sampleRate).toFloat())
        private var previousInput = 0f
        private var previousOutput = 0f

        fun process(input: Float): Float {
            val output = if (highPass) {
                alpha * (previousOutput + input - previousInput)
            } else {
                (1f - alpha) * input + alpha * previousOutput
            }
            previousInput = input
            previousOutput = output
            return output
        }
    }
}
