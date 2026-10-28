package com.satepadee.app.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Content
import com.satepadee.app.data.CountResult
import com.satepadee.app.data.Days
import com.satepadee.app.data.SealedWish
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Wish cards: earned once per finished stage, made once, then only shared. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class WishCardTest {
    @get:Rule val rule = createComposeRule()
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val prefs get() = context.getSharedPreferences("card", Context.MODE_PRIVATE)

    /** Day 19 of round 42: stages 1 and 2 (indices 0 and 1) are complete. */
    private fun model(): AppModel {
        prefs.edit().clear().putLong("start", Days.today() - 18).putString("done", (0..17).joinToString(","))
            .putLong("runId", 42).putString("wishes", """{"0":"မိသားစု ကျန်းမာ ချမ်းသာပါစေ"}""").putBoolean("setupDone", true).commit()
        return AppModel(prefs, Content.load(context))
    }

    private fun save(name: String, b: Bitmap) {
        File("build/screenshots").mkdirs()
        File("build/screenshots/$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun finishedStagesEarnOneCardEach() {
        val m = model()
        assertEquals(listOf(0, 1), m.earnedStages())
        assertEquals(listOf(0, 1), m.pendingCards())
    }

    @Test fun aWishIsMadeOnceAndNeverChanges() {
        val m = model()
        val first = m.sealWish(0, forSelf = false, name = "အမေ", text = "ကျန်းမာချမ်းသာပါစေ", backgroundId = "bagan")
        assertNotNull(first)
        val again = m.sealWish(0, forSelf = true, name = "", text = "something else", backgroundId = null)
        assertEquals(first, again)
        assertEquals(listOf(1), m.pendingCards())
        // Saved straight away and read back the same.
        val reloaded = AppModel(prefs, Content.load(context)).sealedWish(42, 0)
        assertEquals(first, reloaded)
    }

    @Test fun unearnedStagesAndMissingNamesCannotBeSealed() {
        val m = model()
        assertNull(m.sealWish(2, forSelf = true, name = "", text = "ကျန်းမာချမ်းသာပါစေ", backgroundId = null))
        assertNull(m.sealWish(1, forSelf = false, name = " ", text = "ကျန်းမာချမ်းသာပါစေ", backgroundId = null))
        assertNull(m.sealWish(1, forSelf = true, name = "", text = " ", backgroundId = null))
    }

    @Test fun startingAgainKeepsOldCardsAndStartsNewPlaces() {
        val m = model()
        m.sealWish(0, forSelf = true, name = "", text = "ကျန်းမာချမ်းသာပါစေ", backgroundId = null)
        m.start()
        assertTrue(m.state.runId != 42L)
        assertEquals(1, m.state.sealedWishes.size)
        assertTrue(m.pendingCards().isEmpty())
    }

    @Test fun installsFrom100UseTheirStartDayAsTheRound() {
        prefs.edit().clear().putLong("start", 20000).commit()
        assertEquals(20000L, AppModel(prefs, Content.load(context)).state.runId)
    }

    @Test fun cardsAreDrawnAt1080x1350() {
        val m = model()
        for (bg in listOf<String?>(null) + Backgrounds.all.map { it.id }) {
            val b = WishCard.render(context, CardSpec("ကျန်းမာချမ်းသာပါစေ", nameLine(false, "အမေ"), stageLine(0), bg, true, m.content.wishClosing))
            assertEquals(1080, b.width)
            assertEquals(1350, b.height)
            save("11-card-${bg ?: "plain"}", b)
        }
        val self = SealedWish(42, 1, true, "", "စိတ်ငြိမ်းချမ်းပါစေ", "buddha", Days.today())
        save("11-card-sealed-self", WishCard.render(context, self.spec(m.content.wishClosing, true)))
        val last = SealedWish(42, 8, false, "မိဘ", "ဤကုသိုလ်ကို အမျှဝေပါသည်", "shwedagon", Days.today())
        save("11-card-sealed-81days", WishCard.render(context, last.spec(m.content.wishClosing, true)))
    }

    @Test fun longWishStillFits() {
        val long = "ဤကုသိုလ်ကောင်းမှုကို မိဘ ဆရာသမား ဆွေမျိုး မိတ်ဆွေ သတ္တဝါအားလုံး အမျှရရှိကြပါစေ၊ ကျန်းမာချမ်းသာ ငြိမ်းချမ်းကြပါစေ"
        val b = WishCard.render(context, CardSpec(long, null, stageLine(8), "bagan", false, "သာဓု သာဓု သာဓု", special = true))
        save("11-card-long", b)
        assertEquals(1350, b.height)
    }

    @Test fun wishListsLoadFromContent() {
        val c = model().content
        assertTrue(c.wishesSelf.isNotEmpty())
        assertTrue(c.wishesSomeone.isNotEmpty())
        assertTrue("အမေ" in c.wishNames)
        assertEquals("သာဓု သာဓု သာဓု", c.wishClosing)
    }

    @Test fun shareOpensChooserWithThePicture() {
        val b = WishCard.render(context, CardSpec("ကျန်းမာချမ်းသာပါစေ", null, null, "shwedagon", true, ""), 540)
        WishCard.share(context, b, withLink = true)
        val chooser = shadowOf(context).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION") val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals("image/jpeg", send.type)
        @Suppress("DEPRECATION") val uri = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        assertEquals("content", uri!!.scheme)
        assertEquals(WishCard.PLAY_URL, send.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test fun stageDoneScreenOffersTheCard() {
        val m = model()
        var opened = false
        rule.setContent { SatePaDeeTheme { FinishedScreen(m, CountResult.StageDone, 8, onWishCard = { opened = true }) {} } }
        rule.onNodeWithText("ဆုတောင်းမည်").performClick()
        assertTrue(opened)
    }

    @Test fun homeShowsCardsWaitingToBeMade() {
        val m = model()
        var stage = -1
        rule.setContent { SatePaDeeTheme { HomeScreen(m, onWishCard = { stage = it }) {} } }
        rule.onNodeWithText("ဆုတောင်းကတ် ၂ ခု ရှိပါသည်").performClick()
        assertEquals(0, stage)
    }

    @Test fun someoneNeedsAName() {
        val m = model()
        rule.setContent { SatePaDeeTheme { WishCardScreen(m, 42, 1) {} } }
        rule.onNodeWithText("ဆုတောင်းမည်").performScrollTo().performClick()
        rule.onNodeWithText("အမည် ထည့်ပါ").assertExists()
        assertNull(m.sealedWish(42, 1))
    }

    @Test fun makingTheWishSealsItAndOnlySharingRemains() {
        val m = model()
        rule.setContent { SatePaDeeTheme { WishCardScreen(m, 42, 1) {} } }
        rule.onNodeWithText("ကိုယ်တိုင်").performClick()
        rule.onNodeWithText("ဆုတောင်းမည်").performScrollTo().performClick()
        rule.onNodeWithText("ဆုတောင်းမလား").assertExists()
        rule.onAllNodesWithText("ဆုတောင်းမည်")[1].performClick()
        rule.waitForIdle()
        val sealed = m.sealedWish(42, 1)!!
        assertTrue(sealed.forSelf)
        rule.onNodeWithText("မျှဝေမည်").assertExists()
        rule.onAllNodesWithText("ဆုတောင်းမည်").assertCountEquals(0)
        rule.onRoot().captureRoboImage("build/screenshots/11-wish-sealed.png")
    }

    @Config(qualifiers = "w844dp-h390dp-xxhdpi")
    @Test fun onItsSideThePreviewSitsBesideTheControls() {
        val m = model()
        rule.setContent { SatePaDeeTheme { WishCardScreen(m, 42, 1) {} } }
        val preview = rule.onNodeWithTag("cardPreview").getUnclippedBoundsInRoot()
        val root = rule.onRoot().getUnclippedBoundsInRoot()
        assertTrue("whole preview on screen", preview.bottom <= root.bottom)
        assertTrue("preview on the left", preview.right < root.right / 2)
        rule.onNodeWithText("ဘယ်သူ့အတွက် ဆုတောင်းမလဲ").assertIsDisplayed()
        rule.onRoot().captureRoboImage("build/screenshots/11-wish-landscape.png")
    }

    @Test fun previewRedrawsAfterTyping() {
        val m = model()
        rule.setContent { SatePaDeeTheme { WishCardScreen(m, 42, 1) {} } }
        val before = rule.onNodeWithTag("cardPreview").captureToImage().asAndroidBitmap()
        rule.onAllNodes(hasSetTextAction())[0].performTextInput("ညီမ")
        // The name line appears on the card once typing pauses.
        rule.waitUntil(timeoutMillis = 5_000) {
            !rule.onNodeWithTag("cardPreview").captureToImage().asAndroidBitmap().sameAs(before)
        }
    }

    @Test fun typingKeepsTheTextAndDoesNotSealEarly() {
        val m = model()
        rule.setContent { SatePaDeeTheme { WishCardScreen(m, 42, 1) {} } }
        rule.onAllNodes(hasSetTextAction())[0].performTextInput("ညီမ")
        rule.onAllNodesWithText("ညီမ")[0].assertExists()
        rule.onNodeWithText("ဆုတောင်းမည်").performScrollTo().performClick()
        rule.onNodeWithText("ဆုတောင်းမလား").assertExists()
        assertNull("not made until confirmed", m.sealedWish(42, 1))
    }

    @Test fun chartShowsTheCollection() {
        val m = model()
        m.sealWish(0, forSelf = false, name = "အမေ", text = "ကျန်းမာချမ်းသာပါစေ", backgroundId = "kyaiktiyo")
        rule.setContent { SatePaDeeTheme { ChartScreen(m) } }
        rule.onNodeWithText("ကျွန်ုပ်၏ ဆုတောင်းကတ်များ").performScrollTo()
        rule.onNodeWithText("ဆုတောင်းရန်").assertExists()
        assertFalse(m.pendingCards().contains(0))
        rule.onRoot().captureRoboImage("build/screenshots/11-collection.png")
    }
}
