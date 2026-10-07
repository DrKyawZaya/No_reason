package com.satepadee.app.ui

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.click
import androidx.test.core.app.ApplicationProvider
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Content
import com.satepadee.app.data.CountMode
import com.satepadee.app.data.Target
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** One swipe must count exactly one bead, even a long fast flick; one tap counts one bead. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class CountGestureTest {
    @get:Rule val rule = createComposeRule()

    private fun model(mode: CountMode): AppModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("gesture", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("mode", mode.name).commit()
        return AppModel(prefs, Content.load(context))
    }

    @Test fun longFastSwipesCountOneBeadEach() {
        val m = model(CountMode.Swipe)
        rule.setContent { SatePaDeeTheme { CounterScreen(m, Target.Free) {} } }
        val area = rule.onNodeWithTag("countArea")
        // Full-height flick, very fast: previously counted several beads.
        area.performTouchInput { swipe(Offset(centerX, top + 10f), Offset(centerX, bottom - 10f), durationMillis = 60) }
        rule.waitForIdle()
        assertEquals(1, m.state.free.beads)
        // Slow long swipe, then an upward swipe: one bead each.
        area.performTouchInput { swipe(Offset(centerX, top + 10f), Offset(centerX, bottom - 10f), durationMillis = 900) }
        area.performTouchInput { swipe(Offset(centerX, bottom - 10f), Offset(centerX, top + 10f), durationMillis = 200) }
        rule.waitForIdle()
        assertEquals(3, m.state.free.beads)
        // A tiny accidental movement does not count.
        area.performTouchInput { swipe(Offset(centerX, centerY), Offset(centerX, centerY + 8f), durationMillis = 100) }
        rule.waitForIdle()
        assertEquals(3, m.state.free.beads)
    }

    @Test fun tapCountsOneBead() {
        val m = model(CountMode.Tap)
        rule.setContent { SatePaDeeTheme { CounterScreen(m, Target.Free) {} } }
        repeat(5) { rule.onNodeWithTag("countArea").performTouchInput { click(center) } }
        rule.waitForIdle()
        assertEquals(5, m.state.free.beads)
    }
}
