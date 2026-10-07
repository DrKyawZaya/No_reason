package com.satepadee.app.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Content
import com.satepadee.app.data.RoundSound
import com.satepadee.app.data.RoundVibration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoundSignalTest {
    @Test fun choiceIsSavedAndDefaultsToLongVibrationWithoutSound() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("signal", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val m = AppModel(prefs, Content.load(context))
        assertEquals(RoundVibration.Long, m.state.roundVibration)
        assertEquals(RoundSound.Off, m.state.roundSound)
        m.setRoundSignal(vibration = RoundVibration.Off)
        m.setRoundSignal(sound = RoundSound.Bell)
        val again = AppModel(prefs, Content.load(context))
        assertEquals(RoundVibration.Off, again.state.roundVibration)
        assertEquals(RoundSound.Bell, again.state.roundSound)
    }

    @Test fun soundsStartLoudAndFadeToSilence() {
        for (sound in listOf(RoundSound.Bell, RoundSound.Gong)) {
            val pcm = Chime.samples(sound)
            assertTrue(pcm.size > Chime.RATE * 2)
            val head = pcm.take(Chime.RATE / 5).maxOf { abs(it.toInt()) }
            val tail = pcm.takeLast(200).maxOf { abs(it.toInt()) }
            assertTrue("$sound head $head", head > 20000)
            assertTrue("$sound tail $tail", tail < 300)
        }
        assertEquals(0, Chime.samples(RoundSound.Off).size)
    }
}
