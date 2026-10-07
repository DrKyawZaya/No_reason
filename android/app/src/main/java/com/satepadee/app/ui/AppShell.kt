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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.CountResult
import com.satepadee.app.data.Target

private enum class Tab(val label: String) { Home("ယနေ့"), Chart("ဇယား") }

/**
 * Two tabs (ယနေ့, ဇယား). Settings, the ကိုးနဝင်း page, the add form, the counter and the
 * "သာဓု" screen open full screen over them. First open shows the three setup questions.
 */
@Composable
fun AppShell(model: AppModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var page by rememberSaveable { mutableStateOf<String?>(null) }      // "settings", "about", "add", "edit:<id>"
    var counting by rememberSaveable { mutableStateOf<String?>(null) }  // Target.key
    var finished by rememberSaveable { mutableStateOf<String?>(null) }  // "Result:dayIndex"

    if (!model.state.setupDone) { Onboarding(model) { model.completeSetup() }; return }

    finished?.let { f ->
        val (r, d) = f.split(':')
        FinishedScreen(model, CountResult.valueOf(r), d.toIntOrNull()) { finished = null }
        return
    }
    counting?.let { key ->
        BackHandler { counting = null }
        CounterScreen(model, Target.fromKey(key), onFinished = { r, d -> counting = null; finished = "${r.name}:${d ?: ""}" }) { counting = null }
        return
    }
    when (page) {
        "settings" -> { SettingsScreen(model, onAbout = { page = "about" }) { page = null }; return }
        "about" -> { AboutKozawinScreen(model) { page = "settings" }; return }
        "add" -> { RecitationForm(model) { page = null }; return }
    }
    page?.takeIf { it.startsWith("edit:") }?.let { p -> RecitationForm(model, editId = p.removePrefix("edit:")) { page = null }; return }
    BackHandler(enabled = tab != Tab.Home) { tab = Tab.Home }

    Column(Modifier.fillMaxSize().background(Palette.bg).statusBarsPadding()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.Home -> HomeScreen(model, onSettings = { page = "settings" }, onAdd = { page = "add" }, onEdit = { page = "edit:$it" }) { counting = it.key }
                Tab.Chart -> ChartScreen(model)
            }
        }
        HorizontalDivider(color = Palette.line)
        Row(Modifier.fillMaxWidth().background(Palette.surface).navigationBarsPadding().padding(top = 8.dp, bottom = 10.dp)) {
            for (t in Tab.entries) {
                val on = t == tab
                val color = if (on) Palette.accent else Palette.muted
                Column(
                    Modifier.weight(1f).clickable(role = Role.Tab) { tab = t }.padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TabIcon(t, color)
                    Text(t.label, style = Type.small.copy(color = color, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal))
                }
            }
        }
    }
}

@Composable
private fun TabIcon(tab: Tab, color: Color) {
    Canvas(Modifier.size(24.dp)) {
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
        }
    }
}
