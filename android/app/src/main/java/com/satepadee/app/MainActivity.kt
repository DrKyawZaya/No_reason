package com.satepadee.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Content
import com.satepadee.app.data.Target
import com.satepadee.app.ui.CounterScreen
import com.satepadee.app.ui.HomeScreen
import com.satepadee.app.ui.SatePaDeeTheme

class MainActivity : ComponentActivity() {
    private val model by lazy {
        AppModel(getSharedPreferences("satepadee", Context.MODE_PRIVATE), Content.load(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SatePaDeeTheme {
                // The day can change while the app sits in the background.
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refreshToday() }
                var counting by rememberSaveable { mutableStateOf<Target?>(null) }
                val target = counting
                if (target == null) {
                    HomeScreen(model) { counting = it }
                } else {
                    BackHandler { counting = null }
                    CounterScreen(model, target) { counting = null }
                }
            }
        }
    }
}
