package com.privatecall.browser.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.IOException
import java.nio.ByteOrder

class VoiceSampleDecoder(context: Context) {
    private val appContext = context.applicationContext

    fun analyze(uri: Uri): VoiceSampleProfile {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var decoderStarted = false
        try {
            extractor.setDataSource(appContext, uri, null)
            val audioTrackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: throw IOException("The selected file has no audio track.")
            val sourceFormat = extractor.getTrackFormat(audioTrackIndex)
            val mime = sourceFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IOException("The selected audio format is not supported.")
            val durationUs = if (sourceFormat.containsKey(MediaFormat.KEY_DURATION)) {
                sourceFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }
            require(durationUs <= MAX_DURATION_US || durationUs == 0L) {
                "Choose a voice sample no longer than 30 seconds."
            }
            val sourceSampleRate = sourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val sourceChannels = sourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            validateAudioFormat(sourceSampleRate, sourceChannels)

            extractor.selectTrack(audioTrackIndex)
            val audioDecoder = MediaCodec.createDecoderByType(mime)
            decoder = audioDecoder
            audioDecoder.configure(sourceFormat, null, null, 0)
            audioDecoder.start()
            decoderStarted = true

            var sampleRate = sourceSampleRate
            var channels = sourceChannels
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
            var toneAccumulator = VoiceSampleToneAccumulator(sampleRate, channels)
            var decodedFrames = 0L
            var inputEnded = false
            var outputEnded = false
            var lastProgressNanos = System.nanoTime()
            val bufferInfo = MediaCodec.BufferInfo()

            while (!outputEnded) {
                var madeProgress = false
                if (!inputEnded) {
                    val inputIndex = audioDecoder.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = audioDecoder.getInputBuffer(inputIndex)
                            ?: throw IOException("The audio decoder input buffer is unavailable.")
                        inputBuffer.clear()
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            audioDecoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputEnded = true
                        } else {
                            audioDecoder.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                extractor.sampleFlags,
                            )
                            extractor.advance()
                        }
                        madeProgress = true
                    }
                }

                when (val outputIndex = audioDecoder.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT_US)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> madeProgress = true
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = audioDecoder.outputFormat
                        val newSampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        val newChannels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        val newPcmEncoding = if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        } else {
                            AudioFormat.ENCODING_PCM_16BIT
                        }
                        validateAudioFormat(newSampleRate, newChannels)
                        require(
                            newPcmEncoding == AudioFormat.ENCODING_PCM_16BIT ||
                                newPcmEncoding == AudioFormat.ENCODING_PCM_FLOAT,
                        ) { "The decoded audio format is not supported." }
                        if (newSampleRate != sampleRate || newChannels != channels) {
                            require(decodedFrames == 0L) {
                                "The audio sample changed format while decoding."
                            }
                            sampleRate = newSampleRate
                            channels = newChannels
                            toneAccumulator = VoiceSampleToneAccumulator(sampleRate, channels)
                        }
                        pcmEncoding = newPcmEncoding
                        madeProgress = true
                    }
                    else -> if (outputIndex >= 0) {
                        val outputBuffer = audioDecoder.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            val frames = accumulatePcm(
                                outputBuffer,
                                pcmEncoding,
                                channels,
                                toneAccumulator,
                            )
                            decodedFrames += frames
                            require(decodedFrames <= sampleRate.toLong() * MAX_DURATION_SECONDS) {
                                "Choose a voice sample no longer than 30 seconds."
                            }
                        }
                        outputEnded =
                            bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        audioDecoder.releaseOutputBuffer(outputIndex, false)
                        madeProgress = true
                    }
                }
                if (madeProgress) {
                    lastProgressNanos = System.nanoTime()
                } else if (System.nanoTime() - lastProgressNanos > DECODER_STALL_TIMEOUT_NANOS) {
                    throw IOException("The selected audio could not be decoded.")
                }
            }

            return toneAccumulator.finish()
        } finally {
            if (decoderStarted) {
                try {
                    decoder?.stop()
                } catch (_: IllegalStateException) {
                    // The decoder may already have stopped after a failed decode.
                }
            }
            decoder?.release()
            extractor.release()
        }
    }

    private fun accumulatePcm(
        buffer: java.nio.ByteBuffer,
        pcmEncoding: Int,
        channels: Int,
        accumulator: VoiceSampleToneAccumulator,
    ): Int {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val bytesPerSample = if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
        val frameSize = channels * bytesPerSample
        var frames = 0
        while (buffer.remaining() >= frameSize) {
            var monoSample = 0.0
            repeat(channels) {
                monoSample += if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                    buffer.float.toDouble().coerceIn(-1.0, 1.0)
                } else {
                    buffer.short.toDouble() / 32768.0
                }
            }
            accumulator.addFrame(monoSample / channels)
            frames++
        }
        return frames
    }

    private fun validateAudioFormat(sampleRate: Int, channels: Int) {
        require(sampleRate in 8_000..192_000 && channels in 1..2) {
            "The selected audio sample format is not supported."
        }
    }

    private companion object {
        const val DEQUEUE_TIMEOUT_US = 10_000L
        const val DECODER_STALL_TIMEOUT_NANOS = 5_000_000_000L
        const val MAX_DURATION_SECONDS = 30
        const val MAX_DURATION_US = 30_000_000L
    }
}
