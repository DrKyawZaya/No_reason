package com.satepadee.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.CountResult
import com.satepadee.app.data.Kozawin
import com.satepadee.app.data.Mm

/** One calm screen after the day's rounds (or a stage, or all 81 days) are complete. */
@Composable
fun FinishedScreen(model: AppModel, result: CountResult, dayIndex: Int?, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val content = model.content
    val day = dayIndex?.let(Kozawin::day)
    Column(
        Modifier.fillMaxSize().background(Palette.bg).safeDrawingPadding().padding(20.dp).readableWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Text("သာဓု", style = Type.guna.copy(color = Palette.accent, fontSize = 52.sp, lineHeight = 90.sp))
            when {
                result == CountResult.GoalDone || day == null -> Text("ယနေ့ ပန်းတိုင် ပြည့်ပါပြီ", style = Type.heading, textAlign = TextAlign.Center)
                result == CountResult.DayDone -> {
                    Text("ယနေ့ ${Mm.n(day.rounds)} ပတ် ပြည့်ပါပြီ", style = Type.heading, textAlign = TextAlign.Center)
                    val next = Kozawin.day(day.index + 1)
                    Card {
                        Text("မနက်ဖြန် · ${Mm.WEEKDAYS[next.weekday]}", style = Type.small.copy(color = Palette.muted))
                        Text("${content.guna(next.guna).pali} · ${Mm.n(next.rounds)} ပတ်", style = Type.heading)
                        if (next.vegetarian) Pill("သက်သက်လွတ်နေ့", Palette.jadeSoft, Palette.jade)
                    }
                }
                else -> {
                    val all = result == CountResult.ProgramDone
                    Pill("${Mm.STAGES[day.stage]} အဆင့် ပြီးဆုံး", Palette.jadeSoft, Palette.jade)
                    Text(
                        if (all) "ကိုးနဝင်း ရက် ၈၁ ရက် ပြည့်စုံစွာ ပြီးဆုံးပါပြီ" else "${Mm.STAGES[day.stage]} အဆင့် အောင်မြင်စွာ ပြီးဆုံးပါပြီ",
                        style = Type.heading, textAlign = TextAlign.Center,
                    )
                    model.state.wishes[day.stage]?.takeIf { it.isNotBlank() }?.let {
                        Card {
                            Text("သင်၏ ဆုတောင်း", style = Type.small.copy(color = Palette.muted))
                            Text(it, style = Type.body.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    Card {
                        Text("${Mm.STAGES[day.stage]} အဆင့် အောင်မြင်ပြီးပါက", style = Type.small.copy(color = Palette.muted))
                        Text(content.stageBenefits[day.stage], style = Type.body)
                    }
                    if (!all) {
                        Text(
                            "နောက်အဆင့် — ${Mm.STAGES[day.stage + 1]} အဆင့် (${Mm.WEEKDAYS[Kozawin.day(day.index + 1).weekday]}နေ့မှ စ)",
                            style = Type.small.copy(color = Palette.muted), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        PrimaryButton("ပြီးပြီ", Modifier.height(60.dp), onClick = onClose)
    }
}
