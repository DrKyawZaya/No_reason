package com.satepadee.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Days
import com.satepadee.app.data.Kozawin
import com.satepadee.app.data.Mm

/** The 81-day chart laid out like the printed card, plus stage wishes and the daily history. */
@Composable
fun ChartScreen(model: AppModel) {
    val content = model.content
    val done = model.state.done
    val today = model.dayIndex
    var picked by remember { mutableStateOf<Int?>(null) }

    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ကိုးနဝင်း ဇယား", style = Type.title, modifier = Modifier.weight(1f))
            Text("${Mm.n(done.size)} / ၈၁ ရက်", style = Type.small.copy(color = Palette.muted))
        }
        Text("ဂဏန်း = ဂုဏ်တော်နှင့် ပတ်ရေ။ အကွက်ကို နှိပ်၍ ကြည့်ပါ။", style = Type.small.copy(color = Palette.muted))

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            for (s in 0 until Kozawin.STAGES) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Mm.STAGES[s], style = Type.tiny.copy(color = Palette.muted), modifier = Modifier.width(48.dp))
                    for (p in 0 until Kozawin.DAYS_PER_STAGE) {
                        val d = Kozawin.day(s * Kozawin.DAYS_PER_STAGE + p)
                        val isDone = d.index in done
                        val missed = today != null && d.index < today && !isDone
                        val bg = when { isDone -> Palette.accent; d.vegetarian -> Palette.jadeSoft; else -> Palette.surface }
                        val border = when { d.index == today -> Palette.fg; missed -> Palette.warn; isDone -> Palette.accent; else -> Palette.line }
                        Box(
                            Modifier.weight(1f).aspectRatio(0.85f).clip(RoundedCornerShape(6.dp)).background(bg)
                                .border(if (d.index == today || missed) 2.dp else 1.dp, border, RoundedCornerShape(6.dp))
                                .clickable(role = Role.Button) { picked = d.index }
                                .semantics { contentDescription = "ရက် ${Mm.n(d.index + 1)} ${Mm.WEEKDAYS[d.weekday]} ${content.guna(d.guna).pali} ${Mm.n(d.rounds)} ပတ်" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(Mm.n(d.guna), style = Type.body.copy(fontWeight = FontWeight.Bold, color = if (isDone) Palette.ink else if (d.vegetarian) Palette.jade else Palette.fg))
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Legend(Palette.accent, Palette.accent, "ပြီး")
            Legend(Palette.jadeSoft, Palette.line, "သက်သက်လွတ်")
            Legend(Palette.surface, Palette.warn, "မှတ်တမ်းမရှိ")
            Legend(Palette.surface, Palette.fg, "ယနေ့")
        }

        picked?.let { i ->
            val d = Kozawin.day(i)
            Card(border = Palette.accent) {
                Text("ရက် ${Mm.n(i + 1)} · ${Mm.STAGES[d.stage]} အဆင့် · ${Mm.WEEKDAYS[d.weekday]}နေ့", style = Type.small.copy(color = Palette.muted))
                Text("${content.guna(d.guna).pali} · ${Mm.n(d.rounds)} ပတ်", style = Type.heading.copy(color = Palette.accent))
                Text(content.guna(d.guna).meaning.first(), style = Type.small)
                if (d.vegetarian) Pill("သက်သက်လွတ်နေ့", Palette.jadeSoft, Palette.jade)
                model.state.wishes[d.stage]?.takeIf { it.isNotBlank() }?.let { Text("${Mm.STAGES[d.stage]} အဆင့် ဆုတောင်း — $it", style = Type.small.copy(color = Palette.muted)) }
                model.state.start?.let { Text("ပြက္ခဒိန် — ${dateText(it + i)}", style = Type.tiny.copy(color = Palette.muted)) }
            }
        }

        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ဆက်တိုက် ${Mm.n(model.streakDays())} ရက်", style = Type.heading.copy(color = Palette.accent), modifier = Modifier.weight(1f))
                Text("ယနေ့ ${Mm.n(model.state.history[model.today] ?: 0)} လုံး", style = Type.small.copy(color = Palette.muted))
            }
        }
    }
}

@Composable
private fun Legend(bg: Color, border: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(bg).border(1.5.dp, border, RoundedCornerShape(3.dp)))
        Text(label, style = Type.tiny.copy(color = Palette.muted))
    }
}

private fun dateText(epochDay: Long): String {
    val c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = epochDay * 86_400_000L }
    return "${Mm.n(c.get(java.util.Calendar.DAY_OF_MONTH))}.${Mm.n(c.get(java.util.Calendar.MONTH) + 1)}.${Mm.n(c.get(java.util.Calendar.YEAR))} (${Mm.WEEKDAYS[Days.weekday(epochDay)]})"
}
