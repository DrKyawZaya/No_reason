package com.satepadee.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Content
import com.satepadee.app.data.Reminders
import com.satepadee.app.ui.AppShell
import com.satepadee.app.ui.SatePaDeeTheme

class MainActivity : ComponentActivity() {
    private val model by lazy {
        AppModel(Reminders.prefs(this), Content.load(this)).also { m ->
            m.onRemindersChanged = { Reminders.reschedule(this, it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Reminders.reschedule(this, model.state)
        setContent {
            SatePaDeeTheme {
                // The day can change while the app sits in the background.
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refreshToday() }
                AppShell(model)
            }
        }
    }
}
