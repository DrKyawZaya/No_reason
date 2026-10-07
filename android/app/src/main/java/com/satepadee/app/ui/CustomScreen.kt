package com.satepadee.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Mm
import com.satepadee.app.data.Target

/** The user's own recitations, the free counter, and a form to add a new recitation. */
@Composable
fun CustomScreen(model: AppModel, onCount: (Target) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    var per by rememberSaveable { mutableStateOf("108") }
    var daily by rememberSaveable { mutableStateOf("1") }
    var confirmDelete by rememberSaveable { mutableStateOf<String?>(null) }

    ScreenColumn {
        Text("ကိုယ်ပိုင် ပုတီး", style = Type.title)
        Card {
            if (model.state.recitations.isEmpty()) {
                Text("ကိုယ်ပိုင် ပုတီး မရှိသေးပါ။ အောက်တွင် ထည့်ပါ။", style = Type.small.copy(color = Palette.muted))
            }
            for (r in model.state.recitations) {
                val p = model.recitationProgress(r)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.name, style = Type.body.copy(fontWeight = FontWeight.Bold))
                        if (r.text.isNotBlank()) Text(r.text, style = Type.small.copy(color = Palette.muted))
                        Text("တစ်ပတ် ${Mm.n(r.perRound)} လုံး · ယနေ့ ${Mm.n(p.rounds)} / ${Mm.n(r.dailyRounds)} ပတ်", style = Type.tiny.copy(color = Palette.muted))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        GhostButton("စိပ်မည်") { onCount(Target.Custom(r.id)) }
                        if (confirmDelete == r.id) {
                            TextButton({ model.deleteRecitation(r.id); confirmDelete = null }) { Text("ဖျက်ရန် သေချာပါသည်", style = Type.tiny.copy(color = Palette.warn)) }
                        } else {
                            TextButton({ confirmDelete = r.id }) { Text("ဖျက်မည်", style = Type.tiny.copy(color = Palette.muted)) }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("စိပ်ပုတီး (အလွတ်)", style = Type.body.copy(fontWeight = FontWeight.Bold))
                    Text("ပန်းတိုင်မရှိ၊ ပတ်ရေသာ ရေတွက်သည် · ${Mm.n(model.state.free.rounds)} ပတ်", style = Type.small.copy(color = Palette.muted))
                }
                GhostButton("စိပ်မည်") { onCount(Target.Free) }
            }
        }

        Card {
            Text("အသစ် ထည့်မည်", style = Type.heading)
            Field("အမည်", name, "ဥပမာ - မေတ္တာပို့") { name = it }
            Field("ပုတီးတစ်လုံးလျှင် ရွတ်ဆိုမည့်စာ", text, "ဥပမာ - သဗ္ဗေ သတ္တာ အဝေရာ ဟောန္တု", lines = 2) { text = it }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field("တစ်ပတ်လျှင် လုံးရေ", per, numeric = true, modifier = Modifier.weight(1f)) { per = it.filter(Char::isDigit).take(4) }
                Field("နေ့စဉ် ပတ်ရေ", daily, numeric = true, modifier = Modifier.weight(1f)) { daily = it.filter(Char::isDigit).take(3) }
            }
            PrimaryButton("သိမ်းမည်") {
                if (name.isNotBlank()) {
                    model.addRecitation(name, text, per.toIntOrNull() ?: 108, daily.toIntOrNull() ?: 1)
                    name = ""; text = ""; per = "108"; daily = "1"
                }
            }
        }
    }
}

@Composable
private fun Field(
    label: String, value: String, hint: String = "", lines: Int = 1, numeric: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = Type.small.copy(color = Palette.muted))
        OutlinedTextField(
            value, onChange, Modifier.fillMaxWidth(), textStyle = Type.body, minLines = lines,
            placeholder = { if (hint.isNotEmpty()) Text(hint, style = Type.small.copy(color = Palette.muted)) },
            keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Palette.accent, unfocusedBorderColor = Palette.line, cursorColor = Palette.accent),
        )
    }
}
