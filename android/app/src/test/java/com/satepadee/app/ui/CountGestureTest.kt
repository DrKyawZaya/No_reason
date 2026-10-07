package com.satepadee.app.ui

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
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

    @Test fun freeCounterResetsAfterConfirm() {
        val m = model(CountMode.Tap)
        rule.setContent { SatePaDeeTheme { CounterScreen(m, Target.Free) {} } }
        repeat(3) { rule.onNodeWithTag("countArea").performTouchInput { click(center) } }
        rule.onNodeWithText("ပြန်စမည်").performClick()
        rule.onNodeWithText("မစပါ").performClick()
        assertEquals(3, m.state.free.beads)
        rule.onNodeWithText("ပြန်စမည်").performClick()
        rule.onAllNodesWithText("ပြန်စမည်")[1].performClick()
        rule.waitForIdle()
        assertEquals(0, m.state.free.beads)
        assertEquals(0, m.state.free.rounds)
    }
}

/** Add, edit and delete a ပုတီး through the form, as the user would. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class RecitationFormTest {
    @get:Rule val rule = createComposeRule()

    private fun model(): AppModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("form", Context.MODE_PRIVATE)
        prefs.edit().clear().putBoolean("setupDone", true).commit()
        return AppModel(prefs, Content.load(context))
    }

    @Test fun addEditDelete() {
        val m = model()
        m.addRecitation("မေတ္တာပို့", "သဗ္ဗေ သတ္တာ", 108, 3)
        val id = m.state.recitations.single().id
        rule.setContent { SatePaDeeTheme { AppShell(m) } }

        rule.onNodeWithText("ပြင်").performClick()
        rule.onNodeWithText("ပုတီး ပြင်ရန်").assertExists()
        rule.onNodeWithText("မေတ္တာပို့").performTextReplacement("မေတ္တာ")
        rule.onNodeWithText("3").performTextReplacement("5")
        rule.onNodeWithText("သိမ်းမည်").performClick()
        rule.waitForIdle()
        assertEquals("မေတ္တာ", m.recitation(id)!!.name)
        assertEquals(5, m.recitation(id)!!.dailyRounds)

        rule.onNodeWithText("ပြင်").performClick()
        rule.onNodeWithText("ဤပုတီးကို ဖျက်မည်").performScrollTo().performClick()
        rule.onNodeWithText("ဖျက်မည်").performClick()
        rule.waitForIdle()
        assertEquals(0, m.state.recitations.size)
    }
}
