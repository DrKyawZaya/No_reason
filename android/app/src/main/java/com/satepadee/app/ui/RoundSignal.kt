package com.satepadee.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.satepadee.app.data.RoundSound
import com.satepadee.app.data.RoundVibration
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/** Short tick per bead; the end of a round uses the pattern chosen in settings. */
class Haptics(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun tick() = vibrate(longArrayOf(0, 15))
    fun round(kind: RoundVibration) = when (kind) {
        RoundVibration.Short -> vibrate(longArrayOf(0, 60, 60, 200))
        RoundVibration.Long -> vibrate(longArrayOf(0, 500, 120, 600))
        RoundVibration.Off -> Unit
    }

    private fun vibrate(pattern: LongArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        else @Suppress("DEPRECATION") v.vibrate(pattern, -1)
    }
}

/**
 * Plays a struck temple bell (ခေါင်းလောင်း) or pagoda gong (ကြေးစည်) at the end of a round.
 * The sound is synthesised once from a few decaying partials, so the app ships no audio files.
 * It uses the media volume, so the user controls it with the volume buttons.
 */
class Chime {
    private val tracks = mutableMapOf<RoundSound, AudioTrack>()

    fun play(sound: RoundSound) {
        if (sound == RoundSound.Off) return
        Thread {
            synchronized(this) {
                runCatching {
                    val t = tracks.getOrPut(sound) { build(sound) }
                    if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
                    t.reloadStaticData()
                    t.play()
                }
            }
        }.start()
    }

    fun release() = synchronized(this) {
        tracks.values.forEach { runCatching { it.release() } }
        tracks.clear()
    }

    private fun build(sound: RoundSound): AudioTrack {
        val pcm = samples(sound)
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        return track
    }

    companion object {
        const val RATE = 22050

        /** (frequency Hz, amplitude, decay seconds) for each partial. */
        private fun partials(sound: RoundSound): List<Triple<Double, Double, Double>> = when (sound) {
            // Small bronze bell: bright strike, minor-third partial, long hum underneath.
            RoundSound.Bell -> listOf(
                Triple(330.0, 0.35, 2.2), Triple(660.0, 1.0, 1.6), Triple(792.0, 0.45, 1.1),
                Triple(990.0, 0.35, 0.9), Triple(1320.0, 0.3, 0.6), Triple(1716.0, 0.18, 0.35), Triple(2310.0, 0.12, 0.2),
            )
            // Gong: low, warm, with two close partials that beat slowly.
            RoundSound.Gong -> listOf(
                Triple(196.0, 1.0, 3.2), Triple(198.5, 0.6, 3.0), Triple(292.0, 0.45, 2.2),
                Triple(392.0, 0.35, 1.6), Triple(523.0, 0.22, 1.0), Triple(784.0, 0.12, 0.5),
            )
            RoundSound.Off -> emptyList()
        }

        fun samples(sound: RoundSound): ShortArray {
            val parts = partials(sound)
            if (parts.isEmpty()) return ShortArray(0)
            val seconds = parts.maxOf { it.third } * 3.5
            val n = (seconds * RATE).toInt()
            val out = DoubleArray(n)
            for (i in 0 until n) {
                val t = i.toDouble() / RATE
                val attack = minOf(1.0, t / 0.004) * minOf(1.0, (n - i).toDouble() / (n * 0.2))
                var v = 0.0
                for ((f, a, d) in parts) v += a * exp(-t / d) * sin(2 * PI * f * t)
                out[i] = v * attack
            }
            val peak = out.maxOf { abs(it) }.coerceAtLeast(1e-9)
            return ShortArray(n) { (out[it] / peak * 0.8 * Short.MAX_VALUE).toInt().toShort() }
        }
    }
}
