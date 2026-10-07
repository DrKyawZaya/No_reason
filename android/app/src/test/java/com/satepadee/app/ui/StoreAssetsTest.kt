package com.satepadee.app.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Content
import com.satepadee.app.data.CountResult
import com.satepadee.app.data.Days
import com.satepadee.app.data.Target
import com.satepadee.app.data.Wood
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Google Play assets: app icon, launcher icon layers, feature graphic and store screenshots,
 * written to build/store. Pixel sizes come from the mdpi qualifier (1dp = 1px).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w1100dp-h1100dp-mdpi")
class StoreAssetsTest {
    @get:Rule val rule = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun wood(): Wood = Content.load(context).woods.first { it.id == "padauk" }

    private fun model(): AppModel {
        val prefs = context.getSharedPreferences("store", Context.MODE_PRIVATE)
        prefs.edit().clear()
            .putLong("start", Days.today() - 12).putString("done", (0..11).joinToString(","))
            .putInt("progressDay", 12).putInt("progressBeads", 63).putInt("progressRounds", 2)
            .putString("wishes", """{"0":"မိသားစု ကျန်းမာ ချမ်းသာပါစေ","1":"စီးပွားရေး အဆင်ပြေပါစေ"}""")
            .putString("birthDay", "thu").putBoolean("setupDone", true).putBoolean("reminderOn", true)
            .putString(
                "recitations",
                """[{"id":"r1","name":"မေတ္တာပို့","text":"သဗ္ဗေ သတ္တာ အဝေရာ ဟောန္တု","perRound":108,"dailyRounds":3},
                   {"id":"r2","name":"ဘုရားဂုဏ်တော်","text":"ဣတိပိ သော ဘဂဝါ","perRound":108,"dailyRounds":1}]""",
            )
            .putString("history", (0..20).associate { (Days.today() - it).toString() to (300 + it * 131 % 700) }.let { org.json.JSONObject(it).toString() })
            .commit()
        return AppModel(prefs, Content.load(context))
    }

    private fun art(name: String, w: Int, h: Int, content: @Composable () -> Unit) {
        rule.setContent { SatePaDeeTheme { Box(Modifier.size(w.dp, h.dp).testTag("art")) { content() } } }
        rule.waitForIdle()
        rule.onNodeWithTag("art").captureRoboImage("build/store/$name.png")
    }

    private fun screen(name: String, content: @Composable () -> Unit) {
        rule.setContent { SatePaDeeTheme { Box(Modifier.fillMaxSize().background(Palette.bg)) { content() } } }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/store/$name.png")
    }

    @Test fun playIcon() { val w = wood(); art("icon-512", 512, 512) { IconArt(w, Modifier.fillMaxSize(), scale = 0.86f) } }
    @Test fun launcherFull() { val w = wood(); art("launcher-full-192", 192, 192) { IconArt(w, Modifier.fillMaxSize(), scale = 0.9f) } }
    // Adaptive icon layer: 108dp canvas, art kept inside the 66dp safe circle.
    // Rendered over black and over white; tools/icon_layers.py recovers the transparency from the pair.
    @Test fun launcherForegroundBlack() { val w = wood(); art("launcher-foreground-on-black", 432, 432) { IconArt(w, Modifier.fillMaxSize(), matte = Color.Black, scale = 0.66f) } }
    @Test fun launcherForegroundWhite() { val w = wood(); art("launcher-foreground-on-white", 432, 432) { IconArt(w, Modifier.fillMaxSize(), matte = Color.White, scale = 0.66f) } }

    @Test fun featureGraphic() { val w = wood(); art("feature-1024x500", 1024, 500) { FeatureArt(w) } }

    @Config(qualifiers = "w360dp-h720dp-xxhdpi")
    @Test fun phoneHome() { val m = model(); screen("phone-1-home") { AppShell(m) } }
    @Config(qualifiers = "w360dp-h720dp-xxhdpi")
    @Test fun phoneCounter() { val m = model(); screen("phone-2-counter") { CounterScreen(m, Target.Kozawin) {} } }
    @Config(qualifiers = "w360dp-h720dp-xxhdpi")
    @Test fun phoneOnboarding() { val m = model(); screen("phone-3-birthday") { Onboarding(m) {} } }
    @Config(qualifiers = "w360dp-h720dp-xxhdpi")
    @Test fun phoneFinished() { val m = model(); screen("phone-4-stage-done") { FinishedScreen(m, CountResult.StageDone, 8) {} } }
    @Config(qualifiers = "w360dp-h720dp-xxhdpi")
    @Test fun phoneChart() { val m = model(); screen("phone-5-chart") { ChartScreen(m) } }
    @Config(qualifiers = "w360dp-h720dp-xxhdpi")
    @Test fun phoneSettings() { val m = model(); screen("phone-6-settings") { SettingsScreen(m, onAbout = {}) {} } }
    @Config(qualifiers = "w360dp-h720dp-xxhdpi")
    @Test fun phoneAdd() { val m = model(); screen("phone-7-add") { RecitationForm(m, editId = "r1") {} } }

    @Config(qualifiers = "w800dp-h1280dp-xhdpi")
    @Test fun tabletHome() { val m = model(); screen("tablet-1-home") { AppShell(m) } }
    @Config(qualifiers = "w800dp-h1280dp-xhdpi")
    @Test fun tabletCounter() { val m = model(); screen("tablet-2-counter") { CounterScreen(m, Target.Kozawin) {} } }
}

/** A closed ring of beads with the larger guru bead and tassel at the bottom. */
private fun DrawScope.beadRing(wood: Wood, center: Offset, s: Float, beads: Int = 12) {
    val ring = s * 0.30f
    val r = s * 0.071f
    val cy = center.y
    drawCircle(wood.lo, ring, Offset(center.x, cy), style = Stroke(s * 0.012f))
    for (i in 1 until beads) {
        val th = PI / 2 + i * 2 * PI / beads
        val p = Offset(center.x + ring * cos(th).toFloat(), cy + ring * sin(th).toFloat())
        val axis = Offset(-sin(th).toFloat(), cos(th).toFloat())
        drawBead(wood, p, r, lit = true, now = false, alpha = 1f, glow = false, seed = i * 13 + 5, axis = axis, string = wood.lo, stringWidth = s * 0.012f)
    }
    val g = s * 0.075f
    val guru = Offset(center.x, cy + ring)
    drawCircle(Brush.radialGradient(listOf(Palette.accent.copy(alpha = 0.35f), Color.Transparent), guru, g * 2.2f), g * 2.2f, guru)
    tassel(Offset(guru.x, guru.y + g * 0.8f), g)
    drawBead(wood, guru, g, lit = true, now = false, alpha = 1f, glow = false, seed = 1, axis = Offset(1f, 0f), string = wood.lo, stringWidth = s * 0.012f)
}

private fun DrawScope.tassel(top: Offset, g: Float) {
    for (dx in listOf(-0.42f, -0.21f, 0f, 0.21f, 0.42f)) {
        drawLine(Palette.accent, top, Offset(top.x + dx * g, top.y + g * 1.25f), strokeWidth = g * 0.16f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    drawCircle(Color(0xFFFFD27A), g * 0.2f, Offset(top.x, top.y + g * 0.12f))
}

@Composable
private fun IconArt(wood: Wood, modifier: Modifier, scale: Float, matte: Color? = null) {
    Canvas(modifier) {
        if (matte != null) drawRect(matte)
        else drawRect(Brush.radialGradient(listOf(Color(0xFF3D2815), Palette.bg), center, size.minDimension * 0.75f))
        beadRing(wood, center, size.minDimension * scale)
    }
}

@Composable
private fun FeatureArt(wood: Wood) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Color(0xFF3D2815), Palette.bg), Offset(760f, 230f), 700f),
        ),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.padding(start = 72.dp).width(520.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("စိပ်ပုတီး", style = Type.title.copy(fontSize = 76.sp, lineHeight = 130.sp, color = Palette.accent))
                Text("ကိုးနဝင်း နှင့် နေ့စဉ် ပုတီးစိပ်ရန်", style = Type.heading.copy(fontSize = 30.sp, lineHeight = 52.sp))
                Text("နေ့နံသင့် သစ်သားပုတီး · သတိပေးချက်", style = Type.body.copy(fontSize = 22.sp, lineHeight = 40.sp, color = Palette.muted))
            }
            Canvas(Modifier.fillMaxHeight().width(432.dp)) { beadRing(wood, Offset(size.width * 0.5f, size.height * 0.52f), size.height * 0.88f, beads = 14) }
        }
    }
}
