package com.satepadee.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Target

private enum class Tab(val label: String) { Home("ယနေ့"), Chart("ဇယား"), Custom("ကိုယ်ပိုင်"), About("အကြောင်း") }

/** Four tabs, as in the Figma design; the counter opens full screen over them. */
@Composable
fun AppShell(model: AppModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var counting by rememberSaveable { mutableStateOf<String?>(null) }
    val target = counting?.let(Target::fromKey)
    val count: (Target) -> Unit = { counting = it.key }

    if (target != null) {
        BackHandler { counting = null }
        CounterScreen(model, target) { counting = null }
        return
    }
    BackHandler(enabled = tab != Tab.Home) { tab = Tab.Home }

    Column(Modifier.fillMaxSize().background(Palette.bg).statusBarsPadding()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.Home -> HomeScreen(model, count)
                Tab.Chart -> ChartScreen(model)
                Tab.Custom -> CustomScreen(model, count)
                Tab.About -> SettingsScreen(model)
            }
        }
        HorizontalDivider(color = Palette.line)
        Row(Modifier.fillMaxWidth().background(Palette.surface).navigationBarsPadding().padding(top = 6.dp, bottom = 8.dp)) {
            for (t in Tab.entries) {
                val on = t == tab
                val color = if (on) Palette.accent else Palette.muted
                Column(
                    Modifier.weight(1f).clickable(role = Role.Tab) { tab = t }.padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TabIcon(t, color)
                    Text(t.label, style = Type.tiny.copy(color = color, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal))
                }
            }
        }
    }
}

@Composable
private fun TabIcon(tab: Tab, color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        when (tab) {
            Tab.Home -> { drawCircle(color, w * 0.36f, style = stroke); drawCircle(color, w * 0.13f, style = stroke) }
            Tab.Chart -> {
                drawRoundRect(color, Offset(w * 0.15f, w * 0.15f), Size(w * 0.7f, w * 0.7f), CornerRadius(w * 0.08f), style = stroke)
                for (f in listOf(0.38f, 0.62f)) {
                    drawLine(color, Offset(w * 0.15f, w * f), Offset(w * 0.85f, w * f), w * 0.08f)
                    drawLine(color, Offset(w * f, w * 0.15f), Offset(w * f, w * 0.85f), w * 0.08f)
                }
            }
            Tab.Custom -> {
                drawLine(color, Offset(w * 0.5f, w * 0.18f), Offset(w * 0.5f, w * 0.82f), w * 0.08f, StrokeCap.Round)
                drawLine(color, Offset(w * 0.18f, w * 0.5f), Offset(w * 0.82f, w * 0.5f), w * 0.08f, StrokeCap.Round)
            }
            Tab.About -> {
                drawCircle(color, w * 0.38f, style = stroke)
                drawLine(color, Offset(w * 0.5f, w * 0.45f), Offset(w * 0.5f, w * 0.68f), w * 0.08f, StrokeCap.Round)
                drawCircle(color, w * 0.05f, Offset(w * 0.5f, w * 0.32f))
            }
        }
    }
}
