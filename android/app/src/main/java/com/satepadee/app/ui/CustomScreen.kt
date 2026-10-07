package com.satepadee.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.satepadee.app.data.AppModel

/**
 * Add a new ပုတီး, or edit one when [editId] is given (with a delete button).
 * All four fields are shown; beads per round and rounds per day start at 108 and 1.
 */
@Composable
fun RecitationForm(model: AppModel, editId: String? = null, onDone: () -> Unit) {
    BackHandler(onBack = onDone)
    val existing = editId?.let { model.recitation(it) }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var text by rememberSaveable { mutableStateOf(existing?.text ?: "") }
    var per by rememberSaveable { mutableStateOf((existing?.perRound ?: 108).toString()) }
    var daily by rememberSaveable { mutableStateOf((existing?.dailyRounds ?: 1).toString()) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var nameMissing by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Palette.bg).safeDrawingPadding()) {
        ScreenColumn {
            TopBar(if (existing != null) "ပုတီး ပြင်ရန်" else "ပုတီးအသစ်", onDone)
            Field("အမည်", name, "ဥပမာ - မေတ္တာပို့") { name = it; nameMissing = false }
            if (nameMissing) Text("အမည် ထည့်ပါ", style = Type.small.copy(color = Palette.warn))
            Field("ပုတီးတစ်လုံးလျှင် ရွတ်ဆိုမည့်စာ", text, "ဥပမာ - သဗ္ဗေ သတ္တာ အဝေရာ ဟောန္တု", lines = 2) { text = it }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field("တစ်ပတ်လျှင် လုံးရေ", per, numeric = true, modifier = Modifier.weight(1f)) { per = it.filter(Char::isDigit).take(4) }
                Field("နေ့စဉ် ပတ်ရေ", daily, numeric = true, modifier = Modifier.weight(1f)) { daily = it.filter(Char::isDigit).take(3) }
            }
            PrimaryButton("သိမ်းမည်", Modifier.height(60.dp)) {
                if (name.isBlank()) { nameMissing = true; return@PrimaryButton }
                val p = per.toIntOrNull() ?: 108
                val d = daily.toIntOrNull() ?: 1
                if (existing != null) model.updateRecitation(existing.id, name, text, p, d) else model.addRecitation(name, text, p, d)
                onDone()
            }
            if (existing != null) {
                SecondaryButton("ဤပုတီးကို ဖျက်မည်") { confirmDelete = true }
            }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false }, containerColor = Palette.surface,
            title = { Text("\"${existing.name}\" ကို ဖျက်မလား", style = Type.heading) },
            text = { Text("ယနေ့ စိပ်ထားသော ပတ်ရေလည်း ပျက်ပါမည်။", style = Type.body.copy(color = Palette.muted)) },
            confirmButton = { TextButton({ model.deleteRecitation(existing.id); confirmDelete = false; onDone() }) { Text("ဖျက်မည်", style = Type.body.copy(color = Palette.warn)) } },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("မဖျက်ပါ", style = Type.body.copy(color = Palette.muted)) } },
        )
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
