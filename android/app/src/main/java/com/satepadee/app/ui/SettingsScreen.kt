package com.satepadee.app.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
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
import com.satepadee.app.data.isXiaomiFamily
import com.satepadee.app.data.openReminderHelpSettings

/** Four settings: bead, counting mode, reminder, and the ကိုးနဝင်း reference page. */
@Composable
fun SettingsScreen(model: AppModel, onAbout: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val s = model.state
    var woodOpen by remember { mutableStateOf(false) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> model.setReminder(granted) }

    fun enableReminder(on: Boolean) {
        if (on && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else model.setReminder(on)
    }

    Column(Modifier.fillMaxSize().background(Palette.bg).safeDrawingPadding()) {
        ScreenColumn {
            TopBar("ဆက်တင်", onBack)

            Card {
                Text("ပုတီး", style = Type.heading)
                Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { woodOpen = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BeadSwatch(model.wood(), Modifier.size(44.dp))
                    Column(Modifier.weight(1f)) {
                        Text(model.wood().name, style = Type.body.copy(fontWeight = FontWeight.Bold))
                        Text(if (s.birthDay != null) "မွေးနေ့ — ${model.content.birthDayName(s.birthDay)}" else "မွေးနေ့ မရွေးရသေးပါ", style = Type.small.copy(color = Palette.muted))
                    }
                    Text("ပြောင်းမည် ›", style = Type.small.copy(color = Palette.accent))
                }
            }

            Card {
                Text("ရေတွက်ပုံ", style = Type.heading)
                ModeSwitch(s.mode) { model.setMode(it) }
            }

            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("နေ့စဉ် သတိပေးမည်", style = Type.heading, modifier = Modifier.weight(1f))
                    Switch(s.reminderOn, { enableReminder(it) }, colors = SwitchDefaults.colors(checkedTrackColor = Palette.accent, checkedThumbColor = Palette.ink, uncheckedTrackColor = Palette.sunk))
                }
                if (s.reminderOn) {
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Button) {
                            TimePickerDialog(context, { _, h, m -> model.setReminder(true, h * 60 + m) }, s.reminderMinutes / 60, s.reminderMinutes % 60, false).show()
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("သတိပေးချိန်", style = Type.body, modifier = Modifier.weight(1f))
                        Text("${reminderTimeText(s.reminderMinutes)} ›", style = Type.body.copy(color = Palette.accent, fontWeight = FontWeight.Bold))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("သက်သက်လွတ်နေ့ မတိုင်မီ တစ်ရက်အလို သတိပေးမည်", style = Type.body, modifier = Modifier.weight(1f))
                        Switch(s.vegetarianReminder, { model.setReminder(true, vegetarian = it) }, colors = SwitchDefaults.colors(checkedTrackColor = Palette.accent, checkedThumbColor = Palette.ink, uncheckedTrackColor = Palette.sunk))
                    }
                    Column(
                        Modifier.fillMaxWidth().clickable(role = Role.Button) { openReminderHelpSettings(context) },
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text("သတိပေးချက် မရောက်ပါက ›", style = Type.body.copy(color = if (isXiaomiFamily) Palette.warn else Palette.accent, fontWeight = FontWeight.Bold))
                        Text(
                            if (isXiaomiFamily) "Xiaomi / Redmi ဖုန်းများတွင် \"Autostart\" ကို ဖွင့်ပေးရပါမည်။ ဤနေရာကို နှိပ်၍ စိပ်ပုတီး ကို ဖွင့်ပါ။"
                            else "ဖုန်း၏ Battery saver က ပိတ်ထားနိုင်ပါသည်။ ဤနေရာကို နှိပ်၍ ဆက်တင်တွင် ခွင့်ပြုပါ။",
                            style = Type.small.copy(color = Palette.muted),
                        )
                    }
                }
            }

            Card {
                Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onAbout), verticalAlignment = Alignment.CenterVertically) {
                    Text("ကိုးနဝင်း အကြောင်း", style = Type.heading, modifier = Modifier.weight(1f))
                    Text("›", style = Type.heading.copy(color = Palette.muted))
                }
            }
        }
    }
    if (woodOpen) WoodSheet(model) { woodOpen = false }
}

/** Reference page: summary, rules, the 9 gunas and the benefit of each stage. */
@Composable
fun AboutKozawinScreen(model: AppModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val content = model.content
    var openGuna by remember { mutableStateOf<Int?>(null) }
    var openStage by remember { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize().background(Palette.bg).safeDrawingPadding()) {
        ScreenColumn {
            TopBar(content.kozawinName, onBack)
            Text(content.kozawinSummary, style = Type.body)
            Card {
                Text("စည်းကမ်းများ", style = Type.heading)
                content.rules.forEachIndexed { i, r -> Text("${Mm.n(i + 1)}။ $r", style = Type.body) }
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
    }
}
