package com.fraccetta.scopabriscola.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.*

object SoundManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sampleRate = 44100
    @Volatile var enabled: Boolean = true

    fun playCard()            { if (enabled) scope.launch { synthesizeAndPlay(cardBuffer()) } }
    fun playScopa()           { if (enabled) scope.launch { synthesizeAndPlay(scopaBuffer()) } }
    fun playCapture()         { if (enabled) scope.launch { synthesizeAndPlay(captureBuffer()) } }
    fun playOpponentCapture() { if (enabled) scope.launch { synthesizeAndPlay(opponentCaptureBuffer()) } }
    fun playWin()             { if (enabled) scope.launch { synthesizeAndPlay(winBuffer()) } }
    fun playLose()            { if (enabled) scope.launch { synthesizeAndPlay(loseBuffer()) } }

    // ── Synthesis helpers ────────────────────────────────────────────────────

    /** Short wooden thud: low-freq sine + noise, fast exponential decay */
    private fun cardBuffer(): ShortArray {
        val samples = (sampleRate * 0.12).toInt()
        return ShortArray(samples) { i ->
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 35.0)
            val tone = sin(2 * PI * 160 * t)
            val noise = (Math.random() * 2 - 1) * 0.4
            pcm((tone * 0.6 + noise) * env)
        }
    }

    /** Coin collect: bright ascending ding, short and satisfying */
    private fun captureBuffer(): ShortArray {
        val samples = (sampleRate * 0.25).toInt()
        return ShortArray(samples) { i ->
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 12.0)
            // Two quick ascending tones
            val f1 = 520.0 + t * 400.0
            val f2 = 780.0 + t * 300.0
            val wave = sin(2 * PI * f1 * t) * 0.5 + sin(2 * PI * f2 * t) * 0.3
            pcm(wave * env)
        }
    }

    /** Dull thud: low-frequency muted hit, slightly negative */
    private fun opponentCaptureBuffer(): ShortArray {
        val samples = (sampleRate * 0.20).toInt()
        return ShortArray(samples) { i ->
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 20.0)
            val f = 180.0 - t * 60.0
            val noise = (Math.random() * 2 - 1) * 0.3
            pcm((sin(2 * PI * f * t) * 0.5 + noise) * env)
        }
    }

    /** Water splash: filtered white noise, fast attack + medium decay */
    private fun scopaBuffer(): ShortArray {
        val samples = (sampleRate * 0.4).toInt()
        var prev = 0.0
        return ShortArray(samples) { i ->
            val t = i.toDouble() / sampleRate
            val attack = min(1.0, t / 0.02)
            val decay = exp(-t * 8.0)
            val noise = Math.random() * 2 - 1
            // simple low-pass filter for "woosh" quality
            prev = prev * 0.6 + noise * 0.4
            pcm(prev * attack * decay * 1.5)
        }
    }

    /** Ship horn: 110 Hz + harmonics, vibrato, long sustain */
    private fun winBuffer(): ShortArray {
        val samples = (sampleRate * 1.8).toInt()
        return ShortArray(samples) { i ->
            val t = i.toDouble() / sampleRate
            val attack = min(1.0, t / 0.08)
            val release = if (t > 1.5) exp(-(t - 1.5) * 10.0) else 1.0
            val vibrato = 1.0 + 0.008 * sin(2 * PI * 5 * t)
            val f = 110.0 * vibrato
            val wave = sin(2 * PI * f * t) * 0.5 +
                       sin(2 * PI * f * 2 * t) * 0.3 +
                       sin(2 * PI * f * 3 * t) * 0.15 +
                       sin(2 * PI * f * 4 * t) * 0.05
            pcm(wave * attack * release)
        }
    }

    /** Sinking: descending pitch sweep 300 → 60 Hz, fade out */
    private fun loseBuffer(): ShortArray {
        val duration = 1.2
        val samples = (sampleRate * duration).toInt()
        var phase = 0.0
        return ShortArray(samples) { i ->
            val t = i.toDouble() / sampleRate
            val freq = 300.0 * exp(-t * 1.6)   // exponential descent
            val env = exp(-t * 2.0)
            phase += 2 * PI * freq / sampleRate
            pcm(sin(phase) * 0.8 * env)
        }
    }

    private fun pcm(value: Double): Short =
        (value.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()

    private fun synthesizeAndPlay(buffer: ShortArray) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(buffer, 0, buffer.size)
        track.play()
        // release after playback finishes
        Thread.sleep((buffer.size * 1000L / sampleRate) + 50)
        track.release()
    }
}
