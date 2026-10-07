package com.satepadee.app.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.CountMode

/** Three questions on first open, one per screen: birth day (and wood), tap or swipe, reminder. */
@Composable
fun Onboarding(model: AppModel, onDone: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }
    val context = LocalContext.current
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        model.setReminder(granted); onDone()
    }

    Column(
        Modifier.fillMaxSize().background(Palette.bg).safeDrawingPadding().padding(20.dp).readableWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            repeat(3) { i ->
                Box(Modifier.size(width = 36.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(if (i <= step) Palette.accent else Palette.sunk))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(12.dp))
            when (step) {
                0 -> BirthStep(model)
                1 -> ModeStep(model)
                else -> ReminderStep(model)
            }
        }
        if (step < 2) {
            PrimaryButton("ဆက်ရန်", Modifier.height(60.dp)) { step++ }
        } else {
            PrimaryButton("သတိပေးမည်", Modifier.height(60.dp)) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else { model.setReminder(true); onDone() }
            }
            Text(
                "မလိုပါ", style = Type.body.copy(color = Palette.muted), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { model.setReminder(false); onDone() }.padding(12.dp),
            )
        }
    }
}

@Composable
private fun Question(title: String, hint: String) {
    Text(title, style = Type.title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    if (hint.isNotEmpty()) Text(hint, style = Type.body.copy(color = Palette.muted), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.BirthStep(model: AppModel) {
    val content = model.content
    val birth = model.state.birthDay
    Question("မွေးနေ့ ဘာနေ့လဲ", "နေ့နံနှင့် သင့်တော်သော ပုတီးအသားကို ရွေးပေးပါမည်")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 4) {
        for (b in content.birthDays) {
            val on = b.id == birth
            Text(
                b.name, textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
                style = Type.small.copy(color = if (on) Palette.ink else Palette.fg, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal),
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) Palette.accent else Palette.surface)
                    .border(1.dp, if (on) Palette.accent else Palette.line, RoundedCornerShape(12.dp))
                    .clickable(role = Role.RadioButton) { model.setBirthDay(b.id) }.padding(vertical = 12.dp),
            )
        }
    }
    val woods = content.woods.filter { it.day == birth }
    for (w in woods) {
        val on = w.id == model.wood().id
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.surface)
                .border(if (on) 2.dp else 1.dp, if (on) Palette.accent else Palette.line, RoundedCornerShape(16.dp))
                .clickable(role = Role.RadioButton) { model.setWood(w.id) }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BeadSwatch(w, Modifier.size(56.dp))
            Column {
                Text(w.name, style = Type.heading)
                Text("သင့်နေ့နံ ပုတီး", style = Type.small.copy(color = Palette.accent))
            }
        }
    }
    if (birth == null) Text("မရွေးလည်း ရပါသည်။ နောက်မှ ဆက်တင်တွင် ပြောင်းနိုင်သည်။", style = Type.small.copy(color = Palette.muted), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ColumnScope.ModeStep(model: AppModel) {
    Question("ဘယ်လို စိပ်မလဲ", "နောက်မှ ဆက်တင်တွင် ပြောင်းနိုင်သည်")
    for ((m, title, hint) in listOf(
        Triple(CountMode.Tap, "နှိပ်၍ စိပ်မည်", "နှိပ်တိုင်း ပုတီး တစ်လုံး"),
        Triple(CountMode.Swipe, "ပွတ်ဆွဲ၍ စိပ်မည်", "ပွတ်ဆွဲတိုင်း ပုတီး တစ်လုံး"),
    )) {
        val on = model.state.mode == m
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.surface)
                .border(if (on) 2.dp else 1.dp, if (on) Palette.accent else Palette.line, RoundedCornerShape(16.dp))
                .clickable(role = Role.RadioButton) { model.setMode(m) }.padding(18.dp),
        ) {
            Text(title, style = Type.heading.copy(color = if (on) Palette.accent else Palette.fg))
            Text(hint, style = Type.body.copy(color = Palette.muted))
        }
    }
}

@Composable
private fun ColumnScope.ReminderStep(model: AppModel) {
    val context = LocalContext.current
    val minutes = model.state.reminderMinutes
    Question("နေ့စဉ် သတိပေးမလား", "ပုတီးစိပ်ရန် အချိန်ရောက်လျှင် ဖုန်းမှ သတိပေးပါမည်")
    Text(
        reminderTimeText(minutes), textAlign = TextAlign.Center,
        style = Type.guna.copy(color = Palette.accent, fontSize = 40.sp, lineHeight = 70.sp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.surface)
            .clickable(role = Role.Button) {
                TimePickerDialog(context, { _, h, m -> model.setReminder(model.state.reminderOn, h * 60 + m) }, minutes / 60, minutes % 60, false).show()
            }.padding(vertical = 18.dp),
    )
    Text("အချိန် ပြောင်းရန် နှိပ်ပါ", style = Type.small.copy(color = Palette.muted), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

/** "နံနက် ၅:၃၀" style time, shared by onboarding and settings. */
fun reminderTimeText(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    val period = when (h) { in 0..11 -> "နံနက်"; in 12..15 -> "နေ့လယ်"; in 16..18 -> "ညနေ"; else -> "ည" }
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "$period ${com.satepadee.app.data.Mm.n(h12)}:${com.satepadee.app.data.Mm.n(m).padStart(2, '၀')}"
}
