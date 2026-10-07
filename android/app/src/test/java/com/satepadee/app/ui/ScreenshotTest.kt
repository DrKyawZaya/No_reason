package com.satepadee.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Content
import com.satepadee.app.data.Days
import com.satepadee.app.data.Target
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders each screen to build/screenshots so layouts can be checked without a device. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()

    /** Day 9 of ကိုးနဝင်း (stage 1, last day), 5 of 8 rounds done, as in the design. */
    private fun model(): AppModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("shot", Context.MODE_PRIVATE)
        prefs.edit().clear()
            .putLong("start", Days.today() - 8).putString("done", "0,1,2,3,4,5,6,7")
            .putInt("progressDay", 8).putInt("progressBeads", 40).putInt("progressRounds", 5)
            .putString("wishes", """{"0":"မိသားစု ကျန်းမာ ချမ်းသာပါစေ"}""")
            .putString("birthDay", "thu").putBoolean("setupDone", true).putBoolean("reminderOn", true)
            .putString("recitations", """[{"id":"r1","name":"မေတ္တာပို့","text":"သဗ္ဗေ သတ္တာ အဝေရာ ဟောန္တု","perRound":108,"dailyRounds":3}]""")
            .putString("history", (0..13).associate { (Days.today() - it).toString() to (it * 97 % 900) }.let { org.json.JSONObject(it).toString() })
            .commit()
        return AppModel(prefs, Content.load(context))
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.setContent { SatePaDeeTheme { content() } }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    @Test fun home() { val m = model(); shot("1-home") { HomeScreen(m) {} } }
    @Test fun counterTap() { val m = model(); shot("2-counter") { CounterScreen(m, Target.Kozawin) {} } }
    @Test fun chart() { val m = model(); shot("3-chart") { ChartScreen(m) } }
    @Test fun custom() { val m = model(); shot("4-add") { RecitationForm(m) {} } }
    @Test fun edit() { val m = model(); shot("4-edit") { RecitationForm(m, editId = "r1") {} } }
    @Test fun settings() { val m = model(); shot("5-settings") { SettingsScreen(m, onAbout = {}) {} } }
    @Test fun about() { val m = model(); shot("8-about") { AboutKozawinScreen(m) {} } }
    @Test fun onboarding() { val m = model(); shot("0-onboarding") { Onboarding(m) {} } }
    @Test fun finishedDay() { val m = model(); shot("9-finished-day") { FinishedScreen(m, com.satepadee.app.data.CountResult.DayDone, 7) {} } }
    @Test fun finishedStage() { val m = model(); shot("9-finished-stage") { FinishedScreen(m, com.satepadee.app.data.CountResult.StageDone, 8) {} } }
    @Config(qualifiers = "w800dp-h1280dp-xhdpi")
    @Test fun tabletShell() { val m = model(); shot("6-shell-tablet") { AppShell(m) } }
    @Test fun woods() {
        val m = model()
        shot("7-woods") {
            androidx.compose.foundation.layout.FlowRow(
                androidx.compose.ui.Modifier.background(Palette.bg).padding(12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            ) {
                for (w in m.content.woods) {
                    androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        BeadSwatch(w, androidx.compose.ui.Modifier.size(110.dp))
                        androidx.compose.material3.Text(w.name, style = Type.small)
                    }
                }
            }
        }
    }
    @Test fun shell() { val m = model(); shot("6-shell") { AppShell(m) } }
}
