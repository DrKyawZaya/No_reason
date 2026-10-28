package com.satepadee.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Content
import com.satepadee.app.data.Reminders
import com.satepadee.app.ui.AppShell
import com.satepadee.app.ui.SatePaDeeTheme

class MainActivity : ComponentActivity() {
    /** Counter to open, set when the app is launched from a reminder notification. */
    private var openTarget by mutableStateOf<String?>(null)

    private val model by lazy {
        AppModel(Reminders.prefs(this), Content.load(this)).also { m ->
            m.onRemindersChanged = { Reminders.reschedule(this, it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars; every screen pads itself with the system insets.
        // Done here rather than with enableEdgeToEdge(), which still calls the bar-colour APIs that
        // Android 15 deprecated. The bars are made transparent in the theme instead.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            // The app is always dark, so the bar icons stay light.
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        Reminders.reschedule(this, model.state)
        openTarget = intent?.getStringExtra(EXTRA_TARGET)
        setContent {
            SatePaDeeTheme {
                // The day can change while the app sits in the background.
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refreshToday() }
                AppShell(model, openTarget) { openTarget = null }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        model.saveNow()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_TARGET)?.let { openTarget = it }
    }

    companion object {
        const val EXTRA_TARGET = "target"
    }
}
