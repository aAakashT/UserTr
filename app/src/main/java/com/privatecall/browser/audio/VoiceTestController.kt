package com.privatecall.browser.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Process
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.log10
import kotlin.math.sqrt
import java.util.concurrent.atomic.AtomicBoolean

data class VoiceTestSettings(
    val effectsEnabled: Boolean = true,
    val noiseSuppressionEnabled: Boolean = true,
    val echoCancellationEnabled: Boolean = true,
    val automaticGainControlEnabled: Boolean = true,
    val monitorEnabled: Boolean = false,
    val preset: String = "male-natural",
)

data class VoiceTestState(
    val isRunning: Boolean = false,
    val inputLevelDb: Float = -60f,
    val monitorEnabled: Boolean = false,
    val noiseSuppressionAvailable: Boolean = false,
    val echoCancellationAvailable: Boolean = false,
    val automaticGainControlAvailable: Boolean = false,
    val error: String? = null,
)

class VoiceTestController {
    private val running = AtomicBoolean(false)
    private val _state = MutableStateFlow(VoiceTestState())
    val state: StateFlow<VoiceTestState> = _state.asStateFlow()

    @Volatile
    private var settings = VoiceTestSettings()

    @Volatile
    private var captureThread: Thread? = null

    fun start(initialSettings: VoiceTestSettings) {
        if (!running.compareAndSet(false, true)) return
        settings = initialSettings
        _state.value = VoiceTestState(isRunning = true, monitorEnabled = initialSettings.monitorEnabled)
        val thread = Thread({ captureLoop() }, "voice-test-capture")
        captureThread = thread
        thread.start()
    }

    fun updateSettings(updatedSettings: VoiceTestSettings) {
        settings = updatedSettings
        _state.value = _state.value.copy(monitorEnabled = updatedSettings.monitorEnabled)
    }

    fun stop() {
        running.set(false)
        captureThread?.interrupt()
    }

    private fun captureLoop() {
        var recorder: AudioRecord? = null
        var track: AudioTrack? = null
        var noiseSuppressor: NoiseSuppressor? = null
        var echoCanceler: AcousticEchoCanceler? = null
        var gainControl: AutomaticGainControl? = null
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val channelMask = AudioFormat.CHANNEL_IN_MONO
            val encoding = AudioFormat.ENCODING_PCM_16BIT
            val sampleRate = listOf(48_000, 44_100, 16_000).firstOrNull { rate ->
                AudioRecord.getMinBufferSize(rate, channelMask, encoding) > 0 &&
                    AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, encoding) > 0
            } ?: throw IllegalStateException("This device does not support microphone audio testing.")
            val minimumRecordBuffer = AudioRecord.getMinBufferSize(sampleRate, channelMask, encoding)
            if (minimumRecordBuffer <= 0) {
                throw IllegalStateException("Microphone capture is unavailable on this device.")
            }
            val samples = ShortArray(maxOf(minimumRecordBuffer / 2, sampleRate / 100))
            val audioRecorder = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelMask)
                        .setEncoding(encoding)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(minimumRecordBuffer, samples.size * 2))
                .build()
            recorder = audioRecorder
            check(audioRecorder.state == AudioRecord.STATE_INITIALIZED) {
                "The microphone could not be initialized."
            }

            val minimumTrackBuffer = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                encoding,
            )
            if (minimumTrackBuffer <= 0) {
                throw IllegalStateException("Audio preview is unavailable on this device.")
            }
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(encoding)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(minimumTrackBuffer, samples.size * 2))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track = audioTrack
            check(audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                "The audio preview could not be initialized."
            }

            val sessionId = audioRecorder.audioSessionId
            noiseSuppressor = if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(sessionId) else null
            echoCanceler = if (AcousticEchoCanceler.isAvailable()) {
                AcousticEchoCanceler.create(sessionId)
            } else {
                null
            }
            gainControl = if (AutomaticGainControl.isAvailable()) {
                AutomaticGainControl.create(sessionId)
            } else {
                null
            }
            _state.value = _state.value.copy(
                noiseSuppressionAvailable = noiseSuppressor != null,
                echoCancellationAvailable = echoCanceler != null,
                automaticGainControlAvailable = gainControl != null,
                error = null,
            )

            audioRecorder.startRecording()
            var isPlaying = false
            var activePreset = ""
            var toneProcessor = VoiceToneProcessor(sampleRate.toFloat(), settings.preset)
            var appliedNoiseSuppression: Boolean? = null
            var appliedEchoCancellation: Boolean? = null
            var appliedGainControl: Boolean? = null
            var lastMeterUpdate = 0L
            while (running.get()) {
                val currentSettings = settings
                if (appliedNoiseSuppression != currentSettings.noiseSuppressionEnabled) {
                    noiseSuppressor?.enabled = currentSettings.noiseSuppressionEnabled
                    appliedNoiseSuppression = currentSettings.noiseSuppressionEnabled
                }
                if (appliedEchoCancellation != currentSettings.echoCancellationEnabled) {
                    echoCanceler?.enabled = currentSettings.echoCancellationEnabled
                    appliedEchoCancellation = currentSettings.echoCancellationEnabled
                }
                if (appliedGainControl != currentSettings.automaticGainControlEnabled) {
                    gainControl?.enabled = currentSettings.automaticGainControlEnabled
                    appliedGainControl = currentSettings.automaticGainControlEnabled
                }

                if (activePreset != currentSettings.preset) {
                    activePreset = currentSettings.preset
                    toneProcessor = VoiceToneProcessor(sampleRate.toFloat(), activePreset)
                }

                if (currentSettings.monitorEnabled != isPlaying) {
                    if (currentSettings.monitorEnabled) {
                        audioTrack.play()
                    } else {
                        audioTrack.pause()
                        audioTrack.flush()
                    }
                    isPlaying = currentSettings.monitorEnabled
                }

                val readCount = audioRecorder.read(
                    samples,
                    0,
                    samples.size,
                    AudioRecord.READ_NON_BLOCKING,
                )
                if (readCount < 0) {
                    throw IllegalStateException("Microphone capture stopped unexpectedly (error $readCount).")
                }
                if (readCount == 0) {
                    Thread.sleep(5)
                    continue
                }

                var sumSquares = 0.0
                for (index in 0 until readCount) {
                    val sample = samples[index].toDouble() / Short.MAX_VALUE
                    sumSquares += sample * sample
                }
                val rms = sqrt(sumSquares / readCount).toFloat()
                val levelDb = if (rms > 0f) (20f * log10(rms)).coerceIn(-60f, 0f) else -60f
                toneProcessor.process(samples, readCount, currentSettings.effectsEnabled)

                if (isPlaying) {
                    var writtenCount = 0
                    while (writtenCount < readCount && running.get()) {
                        val written = audioTrack.write(
                            samples,
                            writtenCount,
                            readCount - writtenCount,
                            AudioTrack.WRITE_BLOCKING,
                        )
                        if (written < 0) {
                            throw IllegalStateException(
                                "Audio preview stopped unexpectedly (error $written).",
                            )
                        }
                        if (written == 0) {
                            Thread.sleep(5)
                            continue
                        }
                        writtenCount += written
                    }
                }

                val now = System.currentTimeMillis()
                if (now - lastMeterUpdate >= 80L) {
                    _state.value = _state.value.copy(inputLevelDb = levelDb)
                    lastMeterUpdate = now
                }
            }
        } catch (error: SecurityException) {
            _state.value = _state.value.copy(error = "Microphone permission was denied.")
        } catch (error: IllegalArgumentException) {
            _state.value = _state.value.copy(error = error.message ?: "Audio is not supported on this device.")
        } catch (error: IllegalStateException) {
            _state.value = _state.value.copy(error = error.message ?: "Audio processing failed.")
        } catch (error: InterruptedException) {
            if (running.get()) {
                _state.value = _state.value.copy(error = "Microphone monitoring was interrupted.")
            }
            Thread.currentThread().interrupt()
        } finally {
            running.set(false)
            noiseSuppressor?.release()
            echoCanceler?.release()
            gainControl?.release()
            recorder?.release()
            track?.release()
            _state.value = _state.value.copy(isRunning = false, inputLevelDb = -60f)
            captureThread = null
        }
    }
}
