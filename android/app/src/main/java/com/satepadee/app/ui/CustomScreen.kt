package com.satepadee.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.satepadee.app.data.AppModel

/** Short form for a new ပုတီး: name and text; bead and round numbers have defaults. */
@Composable
fun AddRecitationScreen(model: AppModel, onDone: () -> Unit) {
    BackHandler(onBack = onDone)
    var name by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    var per by rememberSaveable { mutableStateOf("108") }
    var daily by rememberSaveable { mutableStateOf("1") }
    var more by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Palette.bg).safeDrawingPadding()) {
        ScreenColumn {
            TopBar("ပုတီးအသစ်", onDone)
            Field("အမည်", name, "ဥပမာ - မေတ္တာပို့") { name = it }
            Field("ပုတီးတစ်လုံးလျှင် ရွတ်ဆိုမည့်စာ", text, "ဥပမာ - သဗ္ဗေ သတ္တာ အဝေရာ ဟောန္တု", lines = 2) { text = it }
            Text(
                if (more) "တစ်ပတ် ${per} လုံး · နေ့စဉ် ${daily} ပတ်" else "တစ်ပတ် ၁၀၈ လုံး · နေ့စဉ် ၁ ပတ်  (ပြောင်းရန်)",
                style = Type.small.copy(color = Palette.accent),
                modifier = Modifier.clickable(role = Role.Button) { more = !more },
            )
            if (more) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field("တစ်ပတ်လျှင် လုံးရေ", per, numeric = true, modifier = Modifier.weight(1f)) { per = it.filter(Char::isDigit).take(4) }
                    Field("နေ့စဉ် ပတ်ရေ", daily, numeric = true, modifier = Modifier.weight(1f)) { daily = it.filter(Char::isDigit).take(3) }
                }
            }
            PrimaryButton("သိမ်းမည်", Modifier.height(60.dp)) {
                if (name.isNotBlank()) {
                    model.addRecitation(name, text, per.toIntOrNull() ?: 108, daily.toIntOrNull() ?: 1)
                    onDone()
                }
            }
            Text("ပုတီးကို ဖျက်လိုပါက ယနေ့ စာမျက်နှာတွင် ကြာကြာ ဖိထားပါ။", style = Type.small.copy(color = Palette.muted))
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
