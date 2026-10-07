package com.satepadee.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
        enableEdgeToEdge()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_TARGET)?.let { openTarget = it }
    }

    companion object {
        const val EXTRA_TARGET = "target"
    }
}
