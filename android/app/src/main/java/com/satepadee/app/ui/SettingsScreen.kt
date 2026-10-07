package com.satepadee.app.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Mm

/** အကြောင်း tab: counting settings, reminders, and the ကိုးနဝင်း reference text. */
@Composable
fun SettingsScreen(model: AppModel) {
    val context = LocalContext.current
    val content = model.content
    val s = model.state
    var woodOpen by remember { mutableStateOf(false) }
    var openGuna by remember { mutableStateOf<Int?>(null) }
    var openStage by remember { mutableStateOf<Int?>(null) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        model.setReminder(granted)
    }

    fun enableReminder(on: Boolean) {
        if (on && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else model.setReminder(on)
    }

    ScreenColumn {
        Text("အကြောင်း", style = Type.title)

        Card {
            Text("ပုတီး ဆက်တင်", style = Type.heading)
            Text("ရေတွက်ပုံ", style = Type.small.copy(color = Palette.muted))
            ModeSwitch(s.mode) { model.setMode(it) }
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { woodOpen = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BeadSwatch(model.wood(), Modifier.size(34.dp))
                Column(Modifier.weight(1f)) {
                    Text(model.wood().name, style = Type.body.copy(fontWeight = FontWeight.Bold))
                    Text(if (s.birthDay != null) "မွေးနေ့ — ${content.birthDayName(s.birthDay)}" else "မွေးနေ့ (နေ့နံ) မရွေးရသေးပါ", style = Type.tiny.copy(color = Palette.muted))
                }
                Text("ပြောင်းမည်", style = Type.small.copy(color = Palette.accent))
            }
        }

        Card {
            Text("သတိပေးချက်", style = Type.heading)
            ToggleRow("နေ့စဉ် သတိပေးမည်", s.reminderOn) { enableReminder(it) }
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button, enabled = s.reminderOn) {
                    TimePickerDialog(context, { _, h, m -> model.setReminder(true, h * 60 + m) }, s.reminderMinutes / 60, s.reminderMinutes % 60, false).show()
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("သတိပေးချိန်", style = Type.body.copy(color = if (s.reminderOn) Palette.fg else Palette.muted), modifier = Modifier.weight(1f))
                Text(timeText(s.reminderMinutes), style = Type.body.copy(color = if (s.reminderOn) Palette.accent else Palette.muted, fontWeight = FontWeight.Bold))
            }
            ToggleRow("သက်သက်လွတ်နေ့ မတိုင်မီ တစ်ရက်အလို သတိပေးမည်", s.vegetarianReminder, enabled = s.reminderOn) {
                model.setReminder(s.reminderOn, vegetarian = it)
            }
        }

        Card {
            Text(content.kozawinName, style = Type.heading)
            Text(content.kozawinSummary, style = Type.small)
            content.rules.forEachIndexed { i, r -> Text("${Mm.n(i + 1)}။ $r", style = Type.small) }
        }

        Card {
            Text("ဂုဏ်တော် ၉ ပါး", style = Type.heading)
            for (g in content.gunas) {
                Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { openGuna = if (openGuna == g.number) null else g.number }) {
                    Text("(${Mm.n(g.number)}) ${g.pali}", style = Type.body.copy(fontWeight = FontWeight.Bold))
                    if (openGuna == g.number) {
                        g.meaning.forEach { Text(it, style = Type.small) }
                        Text("စိပ်ရမည့် ပတ်ရေ — ${Mm.n(g.number)} ပတ်", style = Type.tiny.copy(color = Palette.muted))
                    }
                }
            }
        }

        Card {
            Text("အဆင့်အလိုက် အကျိုးများ", style = Type.heading)
            content.stageBenefits.forEachIndexed { i, b ->
                Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { openStage = if (openStage == i) null else i }) {
                    Text("${Mm.STAGES[i]} အဆင့် အောင်မြင်ပြီးပါက", style = Type.body.copy(fontWeight = FontWeight.Bold))
                    if (openStage == i) Text(b, style = Type.small)
                }
            }
        }
    }

    if (woodOpen) WoodSheet(model) { woodOpen = false }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Type.body.copy(color = if (enabled) Palette.fg else Palette.muted), modifier = Modifier.weight(1f))
        Switch(
            checked, onChange, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = Palette.accent, checkedThumbColor = Palette.ink, uncheckedTrackColor = Palette.sunk),
        )
    }
}

private fun timeText(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    val period = when (h) { in 0..11 -> "နံနက်"; in 12..15 -> "နေ့လယ်"; in 16..18 -> "ညနေ"; else -> "ည" }
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "$period ${Mm.n(h12)}:${Mm.n(m).padStart(2, '၀')}"
}
