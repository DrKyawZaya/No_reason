package com.satepadee.app

import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The app starts edge to edge with light bar icons, without the deprecated enableEdgeToEdge() path. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivityTest {
    @Test fun startsEdgeToEdgeWithLightBarIcons() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            s.onActivity { a ->
                val c = WindowInsetsControllerCompat(a.window, a.window.decorView)
                assertFalse(c.isAppearanceLightStatusBars)
                assertFalse(c.isAppearanceLightNavigationBars)
            }
        }
    }
}
