package com.example.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Handles local audio chime (ding-dong), haptic vibration, and Indonesian Text-to-Speech
 * announcements when a student's queue ticket is called to a counter.
 */
class AlertAudioEngine(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var ttsReady = false
    private var textToSpeech: TextToSpeech? = null

    init {
        try {
            textToSpeech = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val indonesian = Locale("id", "ID")
                    val result = textToSpeech?.setLanguage(indonesian)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech?.language = Locale.getDefault()
                    }
                    textToSpeech?.setSpeechRate(0.95f)
                    textToSpeech?.setPitch(1.02f)
                    ttsReady = true
                }
            }
        } catch (e: Exception) {
            Log.w("AlertAudioEngine", "TTS initialization skipped: ${e.message}")
        }
    }

    fun triggerCallAlert(
        queueNumber: String,
        studentName: String,
        counterName: String
    ) {
        vibrateCallPattern()
        scope.launch {
            playDingDongChime()
            delay(180)
            speakAnnouncement(queueNumber, studentName, counterName)
        }
    }

    fun speakAnnouncement(
        queueNumber: String,
        studentName: String,
        counterName: String
    ) {
        val spokenNumber = formatSpokenQueueNumber(queueNumber)
        val announcement = "Perhatian. Nomor antrean $spokenNumber, atas nama $studentName, silakan menuju $counterName."
        if (ttsReady) {
            try {
                textToSpeech?.speak(
                    announcement,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ult_call_${System.currentTimeMillis()}"
                )
            } catch (e: Exception) {
                Log.w("AlertAudioEngine", "TTS speak error: ${e.message}")
            }
        }
    }

    private fun formatSpokenQueueNumber(queueNumber: String): String {
        // Converts "A-001" into "A, nol nol satu" for clear Indonesian pronunciation
        val parts = queueNumber.split("-")
        if (parts.size != 2) return queueNumber
        val prefix = parts[0]
        val digits = parts[1].map { ch ->
            when (ch) {
                '0' -> "nol"
                '1' -> "satu"
                '2' -> "dua"
                '3' -> "tiga"
                '4' -> "empat"
                '5' -> "lima"
                '6' -> "enam"
                '7' -> "tujuh"
                '8' -> "delapan"
                '9' -> "sembilan"
                else -> ch.toString()
            }
        }.joinToString(" ")
        return "$prefix, $digits"
    }

    private fun vibrateCallPattern() {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator == null || !vibrator.hasVibrator()) return
            val pattern = longArrayOf(0, 260, 140, 260, 140, 380)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w("AlertAudioEngine", "Vibration unavailable: ${e.message}")
        }
    }

    private fun playDingDongChime() {
        try {
            val sampleRate = 22050
            val dingSamples = synthesizeBellTone(659.25, 0.34, sampleRate) // E5
            val dongSamples = synthesizeBellTone(523.25, 0.52, sampleRate) // C5
            val totalSamples = ShortArray(dingSamples.size + dongSamples.size)
            dingSamples.copyInto(totalSamples, 0)
            dongSamples.copyInto(totalSamples, dingSamples.size)

            val bufferBytes = totalSamples.size * 2
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(totalSamples, 0, totalSamples.size)
            audioTrack.play()
            Thread.sleep(900)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            Log.w("AlertAudioEngine", "Chime playback skipped: ${e.message}")
        }
    }

    private fun synthesizeBellTone(freqHz: Double, durationSec: Double, sampleRate: Int): ShortArray {
        val count = (durationSec * sampleRate).toInt()
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val attack = (t / 0.015).coerceAtMost(1.0)
            val decay = exp(-4.8 * t)
            val fundamental = sin(2.0 * PI * freqHz * t)
            val overtone = 0.25 * sin(2.0 * PI * freqHz * 2.0 * t)
            val value = (fundamental + overtone) * attack * decay * 0.55
            samples[i] = (value * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            ttsReady = false
        } catch (_: Exception) {
        }
    }
}
