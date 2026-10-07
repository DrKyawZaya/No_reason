package com.satepadee.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Days
import com.satepadee.app.data.Kozawin
import com.satepadee.app.data.Mm
import com.satepadee.app.data.Target

/** Today's guna and one big button; other ပုတီး as simple rows underneath. */
@Composable
fun HomeScreen(model: AppModel, onSettings: () -> Unit = {}, onAdd: () -> Unit = {}, onEdit: (String) -> Unit = {}, onCount: (Target) -> Unit) {
    val content = model.content
    val index = model.dayIndex
    val today = model.todayDay()
    var missedOpen by remember(model.today) { mutableStateOf(model.missedDays().isNotEmpty()) }
    var wishFor by remember { mutableStateOf<Int?>(null) }

    ScreenColumn {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ယနေ့", style = Type.title)
                Text("${Mm.WEEKDAYS[Days.weekday(model.today)]}နေ့", style = Type.small.copy(color = Palette.muted))
            }
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Palette.surface).border(1.dp, Palette.line, RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onSettings).semantics { contentDescription = "ဆက်တင်" },
                contentAlignment = Alignment.Center,
            ) { GearIcon() }
        }

        val missed = model.missedDays()
        if (missed.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.warnSoft)
                    .clickable(role = Role.Button) { missedOpen = true }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ရက်ကျော် ${Mm.n(missed.size)} ရက် ရှိပါသည်", style = Type.body.copy(color = Palette.warn), modifier = Modifier.weight(1f))
                Text("ကြည့်မည် ›", style = Type.small.copy(color = Palette.warn))
            }
        }

        when {
            index == null -> Card(border = Palette.accent) {
                Text(content.kozawinName, style = Type.guna.copy(color = Palette.accent), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(content.kozawinSummary, style = Type.body.copy(color = Palette.muted))
                val start = Days.nextMondayOrToday(model.today)
                Text(
                    if (start == model.today) "ယနေ့ တနင်္လာနေ့ဖြစ်၍ ယနေ့ပင် စတင်နိုင်ပါသည်။"
                    else "တနင်္လာနေ့မှ စတင်ရပါမည်။ ရက် ${Mm.n(start - model.today)} ရက်အကြာ တနင်္လာနေ့တွင် စတင်ပါမည်။",
                    style = Type.body,
                )
                PrimaryButton("ကိုးနဝင်း စတင်မည်", Modifier.height(60.dp)) { model.start() }
            }
            index < 0 -> Card(border = Palette.accent) {
                Text(content.kozawinName, style = Type.guna.copy(color = Palette.accent), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text("ရက် ${Mm.n(-index)} ရက်အကြာ တနင်္လာနေ့တွင် စတင်ပါမည်။", style = Type.body, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text("ပထမနေ့ — ${content.guna(Kozawin.day(0).guna).pali} · ${Mm.n(Kozawin.day(0).rounds)} ပတ်", style = Type.small.copy(color = Palette.muted), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            today == null -> Card(border = Palette.accent) {
                Text("ကိုးနဝင်း ရက် ၈၁ ရက် ပြည့်စုံစွာ ပြီးဆုံးပါပြီ", style = Type.heading.copy(color = Palette.accent), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                SecondaryButton("နောက်တစ်ကြိမ် စတင်မည်") { model.start() }
            }
            else -> TodayPanel(model, today) {
                if (model.state.wishes[today.stage] == null) wishFor = today.stage else onCount(Target.Kozawin)
            }
        }

        Card {
            for (r in model.state.recitations) {
                val p = model.recitationProgress(r)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button) { onCount(Target.Custom(r.id)) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(r.name, style = Type.body.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                        Text("${Mm.n(p.rounds)} / ${Mm.n(r.dailyRounds)} ပတ်  ›", style = Type.small.copy(color = Palette.muted))
                    }
                    Text(
                        "ပြင်", style = Type.small.copy(color = Palette.accent, fontWeight = FontWeight.Bold),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Palette.sunk)
                            .clickable(role = Role.Button) { onEdit(r.id) }.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button) { onCount(Target.Free) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("စိပ်ပုတီး (အလွတ်)", style = Type.body.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                Text("${Mm.n(model.state.free.rounds)} ပတ်  ›", style = Type.small.copy(color = Palette.muted))
            }
            Text(
                "+ ပုတီးအသစ်", style = Type.body.copy(color = Palette.accent, fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onAdd).padding(vertical = 8.dp),
            )
        }
    }

    if (missedOpen) MissedDialog(model) { missedOpen = false }
    wishFor?.let { stage -> WishDialog(stage) { model.setWish(stage, it); wishFor = null; onCount(Target.Kozawin) } }
}

@Composable
private fun TodayPanel(model: AppModel, day: Kozawin.Day, onStart: () -> Unit) {
    val p = model.todayProgress()
    val done = day.index in model.state.done
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ကိုးနဝင်း · ရက် ${Mm.n(day.index + 1)} / ၈၁", style = Type.body.copy(color = Palette.muted))
        Text(model.content.guna(day.guna).pali, style = Type.guna.copy(color = Palette.accent, fontSize = 40.sp, lineHeight = 70.sp), textAlign = TextAlign.Center)
        Text(if (done) "ယနေ့ ပြီးပါပြီ" else "${Mm.n(p.rounds)} / ${Mm.n(day.rounds)} ပတ်", style = Type.heading)
        if (day.vegetarian) Pill("ယနေ့ သက်သက်လွတ်နေ့", Palette.jadeSoft, Palette.jade)
        ProgressBar(if (done) 1f else (p.rounds * Kozawin.BEADS_PER_ROUND + p.beads) / (day.rounds * Kozawin.BEADS_PER_ROUND).toFloat())
        PrimaryButton(if (done) "ထပ်မံ စိပ်မည်" else "စိပ်မည်", Modifier.height(68.dp), onClick = onStart)
        if (day.index + 1 < Kozawin.TOTAL_DAYS && Kozawin.day(day.index + 1).vegetarian) {
            Text("မနက်ဖြန် သက်သက်လွတ်နေ့", style = Type.small.copy(color = Palette.jade))
        }
    }
}
