package com.example

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * A centralized SoundManager that uses synthesized audio via AudioTrack.
 * This provides premium, zero-latency sound effects without needing external asset files.
 */
object SoundManager {
    private const val SAMPLE_RATE = 44100
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun playMoveSound() {
        ioScope.launch {
            // Soft wood-like tap: low frequency, very quick decay
            playSound(frequency = 300.0, durationMs = 80, envelopeType = Envelope.SHARP_DECAY, volume = 0.6)
        }
    }

    fun playCaptureSound() {
        ioScope.launch {
            // More complex, slightly harsher sound for capture
            playSound(
                frequency = 250.0,
                durationMs = 120,
                envelopeType = Envelope.SHARP_DECAY,
                volume = 0.8,
                secondaryFrequency = 400.0
            )
        }
    }

    fun playCheckSound() {
        ioScope.launch {
            // High-pitched, sustained alert ping
            playSound(
                frequency = 600.0,
                durationMs = 400,
                envelopeType = Envelope.SLOW_DECAY,
                volume = 0.7,
                secondaryFrequency = 880.0
            )
        }
    }

    private enum class Envelope {
        SHARP_DECAY,
        SLOW_DECAY
    }

    private fun playSound(
        frequency: Double,
        durationMs: Int,
        envelopeType: Envelope,
        volume: Double = 0.5,
        secondaryFrequency: Double? = null
    ) {
        val numSamples = (durationMs * SAMPLE_RATE) / 1000
        val sample = ShortArray(numSamples)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(numSamples * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            
            // Envelope calculation
            val progress = i.toDouble() / numSamples
            val envelope = when (envelopeType) {
                Envelope.SHARP_DECAY -> Math.pow(1.0 - progress, 4.0) // Drops off very quickly
                Envelope.SLOW_DECAY -> 1.0 - progress // Linear fade out
            }

            var wave = sin(2.0 * PI * frequency * t)
            
            // Add secondary frequency for richer sound (like a chord or slight dissonance)
            if (secondaryFrequency != null) {
                wave = (wave + sin(2.0 * PI * secondaryFrequency * t)) / 2.0
            }

            // Smooth the start to avoid popping
            val attack = if (i < 200) i / 200.0 else 1.0
            
            val value = wave * envelope * attack * volume * Short.MAX_VALUE
            sample[i] = value.toInt().toShort()
        }

        audioTrack.write(sample, 0, numSamples)
        audioTrack.play()

        // Release the track after it finishes playing
        Thread.sleep(durationMs.toLong() + 50)
        audioTrack.release()
    }
}
