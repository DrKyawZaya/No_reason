package com.satepadee.app.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Content
import com.satepadee.app.data.CountResult
import com.satepadee.app.data.Days
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The wish card picture, sharing it, and the way in from the "သာဓု" screen. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class WishCardTest {
    @get:Rule val rule = createComposeRule()
    private val context: Application = ApplicationProvider.getApplicationContext()

    private fun model(): AppModel {
        val prefs = context.getSharedPreferences("card", Context.MODE_PRIVATE)
        prefs.edit().clear().putLong("start", Days.today() - 8).putString("done", "0,1,2,3,4,5,6,7,8")
            .putString("wishes", """{"0":"မိသားစု ကျန်းမာ ချမ်းသာပါစေ"}""").putBoolean("setupDone", true).commit()
        return AppModel(prefs, Content.load(context))
    }

    private fun save(name: String, b: Bitmap) {
        File("build/screenshots").mkdirs()
        File("build/screenshots/$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun cardIsDrawnAt1080x1350ForEveryBackground() {
        val m = model()
        for (bg in listOf<String?>(null) + Backgrounds.all.map { it.id }) {
            val spec = CardSpec("ကျန်းမာချမ်းသာပါစေ", "အမေ", stageLine(0, false), bg, true, m.content.wishClosing)
            val b = WishCard.render(context, spec)
            assertEquals(1080, b.width)
            assertEquals(1350, b.height)
            save("11-card-${bg ?: "plain"}", b)
        }
    }

    @Test fun longWishStillFits() {
        val long = "ဤကုသိုလ်ကောင်းမှုကို မိဘ ဆရာသမား ဆွေမျိုး မိတ်ဆွေ သတ္တဝါအားလုံး အမျှရရှိကြပါစေ၊ ကျန်းမာချမ်းသာ ငြိမ်းချမ်းကြပါစေ"
        val b = WishCard.render(context, CardSpec(long, "", stageLine(8, true), "bagan", false, "သာဓု သာဓု သာဓု"))
        save("11-card-long", b)
        assertEquals(1350, b.height)
    }

    @Test fun templatesLoadFromContent() {
        val m = model()
        assertTrue(m.content.wishTemplates.size >= 6)
        assertEquals("သာဓု သာဓု သာဓု", m.content.wishClosing)
    }

    @Test fun shareOpensChooserWithThePicture() {
        val b = WishCard.render(context, CardSpec("ကျန်းမာချမ်းသာပါစေ", "", null, "shwedagon", true, ""), 540)
        WishCard.share(context, b, withLink = true)
        val chooser = shadowOf(context).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION") val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals("image/jpeg", send.type)
        @Suppress("DEPRECATION") val uri = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        assertNotNull(uri)
        assertEquals("content", uri!!.scheme)
        assertEquals(WishCard.PLAY_URL, send.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test fun stageDoneScreenOffersTheCard() {
        val m = model()
        var opened = false
        rule.setContent { SatePaDeeTheme { FinishedScreen(m, CountResult.StageDone, 8, onWishCard = { opened = true }) {} } }
        rule.onNodeWithText("ဆုတောင်းကတ် ပို့မည်").performClick()
        assertTrue(opened)
    }

    @Test fun screenStartsWithTheStageWishAndChangesWithAChip() {
        val m = model()
        rule.setContent { SatePaDeeTheme { WishCardScreen(m, 0, false) {} } }
        // The stage's own wish is both the first chip and the text in the box.
        rule.onAllNodesWithText("မိသားစု ကျန်းမာ ချမ်းသာပါစေ").assertCountEquals(2)
        rule.onNodeWithText("လုပ်ငန်း အောင်မြင်ပါစေ").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("လုပ်ငန်း အောင်မြင်ပါစေ").assertCountEquals(2)
        rule.onAllNodesWithText("မိသားစု ကျန်းမာ ချမ်းသာပါစေ").assertCountEquals(1)
    }
}
