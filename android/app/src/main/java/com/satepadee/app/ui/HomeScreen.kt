package com.satepadee.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Days
import com.satepadee.app.data.Kozawin
import com.satepadee.app.data.Mm
import com.satepadee.app.data.Target

@Composable
fun HomeScreen(model: AppModel, onCount: (Target) -> Unit) {
    val content = model.content
    val index = model.dayIndex
    val today = model.todayDay()
    var missedOpen by remember(model.today) { mutableStateOf(model.missedDays().isNotEmpty()) }
    var rulesOpen by remember { mutableStateOf(false) }
    var woodOpen by remember { mutableStateOf(false) }
    var wishFor by remember { mutableStateOf<Int?>(null) }

    Column(
        Modifier.fillMaxSize().background(Palette.bg).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("ယနေ့", style = Type.title, modifier = Modifier.weight(1f))
            Text("${Mm.WEEKDAYS[Days.weekday(model.today)]}နေ့", style = Type.small.copy(color = Palette.muted))
        }

        val missed = model.missedDays()
        if (missed.isNotEmpty()) {
            Card(border = Palette.warn) {
                Pill("ရက်ကျော် ${Mm.n(missed.size)} ရက်", Palette.warnSoft, Palette.warn)
                Text("ရက် ${missed.joinToString("၊ ") { Mm.n(it + 1) }} ကို အက်ပ်တွင် မှတ်တမ်းမရှိပါ။", style = Type.small)
                SecondaryButton("ဆုံးဖြတ်မည်") { missedOpen = true }
            }
        }

        when {
            index == null -> Card(border = Palette.accent) {
                Text(content.kozawinName, style = Type.guna.copy(color = Palette.accent))
                Text(content.kozawinSummary, style = Type.small.copy(color = Palette.muted))
                val start = Days.nextMondayOrToday(model.today)
                Text(
                    if (start == model.today) "ယနေ့ တနင်္လာနေ့ဖြစ်၍ ယနေ့ပင် စတင်နိုင်ပါသည်။"
                    else "တနင်္လာနေ့မှ စတင်ရပါမည်။ ရက် ${Mm.n(start - model.today)} ရက်အကြာ တနင်္လာနေ့တွင် စတင်ပါမည်။",
                    style = Type.body,
                )
                PrimaryButton("ကိုးနဝင်း စတင်မည်") { model.start() }
            }
            index < 0 -> Card(border = Palette.accent) {
                Text(content.kozawinName, style = Type.guna.copy(color = Palette.accent))
                Text("ရက် ${Mm.n(-index)} ရက်အကြာ တနင်္လာနေ့တွင် စတင်ပါမည်။", style = Type.body)
                Text("ပထမနေ့ — ${content.guna(Kozawin.day(0).guna).pali} · ${Mm.n(Kozawin.day(0).rounds)} ပတ်", style = Type.small.copy(color = Palette.muted))
            }
            today == null -> Card(border = Palette.accent) {
                Text("ကိုးနဝင်း ရက် ၈၁ ရက် ပြည့်စုံစွာ ပြီးဆုံးပါပြီ", style = Type.heading.copy(color = Palette.accent))
                Text("သာဓု သာဓု သာဓု", style = Type.body)
                SecondaryButton("နောက်တစ်ကြိမ် စတင်မည်") { model.start() }
            }
            else -> TodayCard(model, today) {
                if (model.state.wishes[today.stage] == null) wishFor = today.stage else onCount(Target.Kozawin)
            }
        }

        if (today != null && today.index + 1 < Kozawin.TOTAL_DAYS && Kozawin.day(today.index + 1).vegetarian) {
            Card {
                Pill("သတိပေးချက်", Palette.jadeSoft, Palette.jade)
                Text("မနက်ဖြန် (${Mm.WEEKDAYS[Kozawin.day(today.index + 1).weekday]}) သည် သက်သက်လွတ် စားရမည့်နေ့ ဖြစ်ပါသည်။", style = Type.body)
            }
        }

        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("စိပ်ပုတီး (အလွတ်)", style = Type.body.copy(fontWeight = FontWeight.Bold))
                    Text("ပန်းတိုင်မရှိ · ${Mm.n(model.state.free.rounds)} ပတ်", style = Type.small.copy(color = Palette.muted))
                }
                GhostButton("စိပ်မည်") { onCount(Target.Free) }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { woodOpen = true }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BeadSwatch(model.wood(), Modifier.height(28.dp).fillMaxWidth(0.1f))
                Text("ပုတီး — ${model.wood().name}", style = Type.small, modifier = Modifier.weight(1f))
                Text("ပြောင်းမည်", style = Type.small.copy(color = Palette.accent))
            }
        }

        Card {
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { rulesOpen = !rulesOpen }, verticalAlignment = Alignment.CenterVertically) {
                Text("ကိုးနဝင်း စည်းကမ်းများ", style = Type.heading, modifier = Modifier.weight(1f))
                Text(if (rulesOpen) "ပိတ်မည်" else "ကြည့်မည်", style = Type.small.copy(color = Palette.accent))
            }
            if (rulesOpen) content.rules.forEachIndexed { i, r -> Text("${Mm.n(i + 1)}။ $r", style = Type.small) }
        }
    }

    if (missedOpen) MissedDialog(model) { missedOpen = false }
    if (woodOpen) WoodSheet(model) { woodOpen = false }
    wishFor?.let { stage -> WishDialog(stage) { model.setWish(stage, it); wishFor = null; onCount(Target.Kozawin) } }
}

@Composable
private fun TodayCard(model: AppModel, day: Kozawin.Day, onStart: () -> Unit) {
    val content = model.content
    val p = model.todayProgress()
    val done = day.index in model.state.done
    val guna = content.guna(day.guna)
    Card(border = Palette.accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ကိုးနဝင်း · ${Mm.STAGES[day.stage]} အဆင့် · ရက် ${Mm.n(day.index + 1)}/၈၁", style = Type.small.copy(color = Palette.muted), modifier = Modifier.weight(1f))
            if (day.vegetarian) Pill("သက်သက်လွတ်နေ့", Palette.jadeSoft, Palette.jade)
        }
        Column {
            Text(guna.pali, style = Type.guna.copy(color = Palette.accent))
            Text(guna.meaning.first(), style = Type.small.copy(color = Palette.muted))
        }
        Row {
            Text(if (done) "ယနေ့ ပြီးပါပြီ" else "${Mm.n(p.rounds)} / ${Mm.n(day.rounds)} ပတ်", style = Type.small, modifier = Modifier.weight(1f))
            Text("${Mm.n(day.rounds * Kozawin.BEADS_PER_ROUND)} လုံး", style = Type.small.copy(color = Palette.muted))
        }
        ProgressBar(if (done) 1f else (p.rounds * Kozawin.BEADS_PER_ROUND + p.beads) / (day.rounds * Kozawin.BEADS_PER_ROUND).toFloat())
        PrimaryButton(if (done) "ထပ်မံ စိပ်မည်" else if (p.beads + p.rounds > 0) "ဆက်စိပ်မည်" else "စိပ်မည်", onClick = onStart)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until Kozawin.DAYS_PER_STAGE) {
                val d = day.stage * Kozawin.DAYS_PER_STAGE + i
                val color = when {
                    d in model.state.done -> Palette.accent
                    d == day.index -> Palette.accentSoft
                    else -> Palette.sunk
                }
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color).let {
                    if (d == day.index) it.border(1.dp, Palette.accent, RoundedCornerShape(3.dp)) else it
                })
            }
        }
        model.state.wishes[day.stage]?.takeIf { it.isNotBlank() }?.let {
            Text("ဤအဆင့် ဆုတောင်း — $it", style = Type.small, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Palette.sunk).padding(10.dp))
        }
    }
}
