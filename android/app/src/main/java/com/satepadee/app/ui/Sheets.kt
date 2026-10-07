package com.satepadee.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.CountResult
import com.satepadee.app.data.Kozawin
import com.satepadee.app.data.Mm

/** Bead wood picker. The wood matching the user's birth day (နေ့နံ) is listed first. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WoodSheet(model: AppModel, onDismiss: () -> Unit) {
    val content = model.content
    val birth = model.state.birthDay
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Palette.surface) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("ပုတီး အမျိုးအစား", style = Type.title.copy(color = Palette.accent))
            Text("မိမိ မွေးနေ့ (နေ့နံ)", style = Type.small.copy(color = Palette.muted))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (b in content.birthDays) {
                    val on = b.id == birth
                    Text(
                        b.name, style = Type.small.copy(color = if (on) Palette.ink else Palette.fg, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal),
                        modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(if (on) Palette.accent else Palette.bg)
                            .border(1.dp, if (on) Palette.accent else Palette.line, RoundedCornerShape(99.dp))
                            .clickable(role = Role.RadioButton) { model.setBirthDay(b.id) }.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
            Text("နေ့နံအလိုက် သင့်တော်သော အသားပုတီးကို ရှေ့ဆုံးတွင် ပြထားသည်။ နှစ်သက်ရာ ရွေးနိုင်သည်။", style = Type.small.copy(color = Palette.muted))
            val selected = model.wood().id
            val woods = content.woods.sortedByDescending { it.day == birth }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 2) {
                for (w in woods) {
                    val on = w.id == selected
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Palette.bg)
                            .border(if (on) 2.dp else 1.dp, if (on) Palette.accent else Palette.line, RoundedCornerShape(12.dp))
                            .clickable(role = Role.RadioButton) { model.setWood(w.id) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BeadSwatch(w, Modifier.size(38.dp))
                        Column {
                            Text(w.name, style = Type.body.copy(fontWeight = FontWeight.Bold))
                            Text("${content.birthDayName(w.day)} နံ", style = Type.tiny.copy(color = Palette.muted))
                            if (w.day == birth) Pill("သင့်နေ့နံ", Palette.accent, Palette.ink)
                        }
                    }
                }
            }
            Text(content.woodNote, style = Type.tiny.copy(color = Palette.muted))
            PrimaryButton("ပြီးပြီ", onClick = onDismiss)
        }
    }
}

/** Shown when the day's rounds are complete; at the end of a stage it repeats the wish and the stage's benefits. */
@Composable
fun FinishedDialog(model: AppModel, result: CountResult, day: Kozawin.Day, onClose: () -> Unit) {
    val content = model.content
    AlertDialog(
        onDismissRequest = onClose, containerColor = Palette.surface,
        confirmButton = { PrimaryButton("သာဓု", Modifier.width(120.dp), onClose) },
        title = {
            Text(
                when (result) {
                    CountResult.ProgramDone -> "ကိုးနဝင်း ရက် ၈၁ ရက် ပြည့်စုံစွာ ပြီးဆုံးပါပြီ"
                    CountResult.StageDone -> "${Mm.STAGES[day.stage]} အဆင့် အောင်မြင်စွာ ပြီးဆုံးပါပြီ"
                    else -> "ယနေ့ ${Mm.n(day.rounds)} ပတ် ပြည့်ပါပြီ"
                },
                style = Type.heading.copy(color = Palette.accent),
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (result == CountResult.DayDone) {
                    Text("${Mm.STAGES[day.stage]} အဆင့်၏ ရက် ${Mm.n(day.position + 1)}/၉ ပြီးဆုံးပါပြီ။", style = Type.body)
                    val next = Kozawin.day(day.index + 1)
                    Text(
                        "မနက်ဖြန် (${Mm.WEEKDAYS[next.weekday]}) — ${content.guna(next.guna).pali} · ${Mm.n(next.rounds)} ပတ်" +
                            if (next.vegetarian) " · သက်သက်လွတ်နေ့" else "",
                        style = Type.small.copy(color = Palette.muted),
                    )
                } else {
                    Pill("${Mm.STAGES[day.stage]} အဆင့် ပြီးဆုံး", Palette.jadeSoft, Palette.jade)
                    model.state.wishes[day.stage]?.takeIf { it.isNotBlank() }?.let {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.sunk).padding(12.dp)) {
                            Text("သင်၏ ဆုတောင်း", style = Type.tiny.copy(color = Palette.muted))
                            Text(it, style = Type.body.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    Text("${Mm.STAGES[day.stage]} အဆင့် အောင်မြင်ပြီးပါက", style = Type.tiny.copy(color = Palette.muted))
                    Text(content.stageBenefits[day.stage], style = Type.body)
                    if (result == CountResult.StageDone) {
                        Text("နောက်အဆင့် — ${Mm.STAGES[day.stage + 1]} အဆင့် (${Mm.WEEKDAYS[Kozawin.day(day.index + 1).weekday]}နေ့မှ စ)", style = Type.small.copy(color = Palette.muted))
                    }
                }
            }
        },
    )
}

/** Asked once at the start of each stage; the wish is shown again when the stage is finished. */
@Composable
fun WishDialog(stage: Int, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onSave("") }, containerColor = Palette.surface,
        title = { Text("${Mm.STAGES[stage]} အဆင့်အတွက် ဆုတောင်းချက်", style = Type.heading.copy(color = Palette.accent)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("တစ်ဆင့်လျှင် အရေးအကြီးဆုံး ကိစ္စတစ်ခုကို ဆုတောင်းနိုင်သည်။ အဆင့်ပြီးဆုံးချိန်တွင် ပြန်ပြပေးပါမည်။", style = Type.small.copy(color = Palette.muted))
                OutlinedTextField(
                    text, { text = it }, Modifier.fillMaxWidth(), textStyle = Type.body, minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Palette.accent, unfocusedBorderColor = Palette.line, cursorColor = Palette.accent),
                )
            }
        },
        confirmButton = { TextButton({ onSave(text) }) { Text("သိမ်းမည်", style = Type.body.copy(color = Palette.accent, fontWeight = FontWeight.Bold)) } },
        dismissButton = { TextButton({ onSave("") }) { Text("နောက်မှ", style = Type.body.copy(color = Palette.muted)) } },
    )
}

/** Missed days: the user may have recited without the app, so we ask instead of resetting. */
@Composable
fun MissedDialog(model: AppModel, onDismiss: () -> Unit) {
    val missed = model.missedDays()
    if (missed.isEmpty()) return
    val stage = missed.first() / Kozawin.DAYS_PER_STAGE
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Palette.surface,
        title = { Text("ရက် ${missed.joinToString("၊ ") { Mm.n(it + 1) }} ကို မှတ်တမ်း မရှိပါ", style = Type.heading.copy(color = Palette.accent)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("ရက်ကျော်နေပါသည်", Palette.warnSoft, Palette.warn)
                Text("ကိုးနဝင်းကို ရက်မပျက်မကွက် ဝင်ရပါမည်။ အက်ပ်မသုံးဘဲ စိပ်ခဲ့ပါက \"စိပ်ပြီးပါပြီ\" ကို ရွေးပါ။", style = Type.small.copy(color = Palette.muted))
                PrimaryButton("အက်ပ်မသုံးဘဲ စိပ်ပြီးပါပြီ") { model.markMissedDone(); onDismiss() }
                SecondaryButton("${Mm.STAGES[stage]} အဆင့်ကို အစမှ ပြန်စမည်") { model.restartStage(); onDismiss() }
                SecondaryButton("ရက် ၁ မှ ပြန်စမည် (တနင်္လာနေ့)") { model.restartProgram(); onDismiss() }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("နောက်မှ ဆုံးဖြတ်မည်", style = Type.body.copy(color = Palette.accent)) } },
    )
}

@Composable
fun GoalDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose, containerColor = Palette.surface,
        title = { Text("ယနေ့ ပန်းတိုင် ပြည့်ပါပြီ", style = Type.heading.copy(color = Palette.accent)) },
        text = { Text("သာဓု သာဓု သာဓု", style = Type.body) },
        confirmButton = { PrimaryButton("သာဓု", Modifier.width(120.dp), onClose) },
    )
}
