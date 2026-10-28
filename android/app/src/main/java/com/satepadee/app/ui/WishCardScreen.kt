package com.satepadee.app.ui

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Mm

/** Line shown at the top of the card's words: which stage was finished, or the whole program. */
fun stageLine(stage: Int?, programDone: Boolean): String? = when {
    programDone -> "ကိုးနဝင်း ရက် ၈၁ ရက် ပြည့်စုံပါပြီ"
    stage != null -> "ကိုးနဝင်း · ${Mm.STAGES[stage]} အဆင့် ပြီးပါပြီ"
    else -> null
}

/**
 * Make a wish card for someone after finishing a stage: pick a ready-made wish or write one,
 * add a name, choose a picture, then share it or save it. The preview is the same drawing that is shared.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WishCardScreen(model: AppModel, stage: Int?, programDone: Boolean, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val content = model.content
    val stageWish = stage?.let { model.state.wishes[it] }?.takeIf { it.isNotBlank() }
    val choices = (listOfNotNull(stageWish) + content.wishTemplates).distinct()

    var wish by rememberSaveable { mutableStateOf(stageWish ?: content.wishTemplates.firstOrNull().orEmpty()) }
    var name by rememberSaveable { mutableStateOf("") }
    var bgId by rememberSaveable { mutableStateOf<String?>(model.state.themeId ?: Backgrounds.all.first().id) }
    var footer by rememberSaveable { mutableStateOf(true) }

    val spec = CardSpec(wish.trim(), name.trim(), stageLine(stage, programDone), bgId, footer, content.wishClosing)
    val preview = remember(spec) { WishCard.render(context, spec, width = 540) }

    Column(Modifier.fillMaxSize().pageBackground().safeDrawingPadding()) {
        ScreenColumn {
            TopBar("ဆုတောင်းကတ်", onBack)

            Image(
                preview.asImageBitmap(), contentDescription = "ဆုတောင်းကတ် နမူနာ",
                modifier = Modifier.fillMaxWidth().aspectRatio(WishCard.WIDTH / WishCard.HEIGHT.toFloat())
                    .clip(RoundedCornerShape(16.dp)).border(1.dp, Palette.line, RoundedCornerShape(16.dp)).testTag("cardPreview"),
            )

            Card {
                Text("ဆုတောင်း", style = Type.heading)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in choices) {
                        val on = c == wish
                        Text(
                            c, style = Type.small.copy(color = if (on) Palette.ink else Palette.fg, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal),
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Palette.accent else Palette.sunk)
                                .clickable(role = Role.RadioButton) { wish = c }.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
                CardField("ကိုယ်တိုင် ရေးနိုင်ပါသည်", wish, lines = 2) { wish = it.take(120) }
                CardField("ဘယ်သူ့အတွက် (မထည့်လည်း ရပါသည်)", name, hint = "ဥပမာ - အမေ") { name = it.take(30) }
            }

            Card {
                Text("နောက်ခံပုံ", style = Type.heading)
                BackgroundPicker(bgId) { bgId = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("app အမည်နှင့် Play link ပြမည်", style = Type.body, modifier = Modifier.weight(1f))
                    Switch(
                        footer, { footer = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Palette.accent, checkedThumbColor = Palette.ink, uncheckedTrackColor = Palette.sunk),
                    )
                }
            }

            PrimaryButton("မျှဝေမည်", Modifier.height(60.dp)) {
                WishCard.share(context, WishCard.render(context, spec), withLink = footer)
            }
            if (Build.VERSION.SDK_INT >= 29) {
                SecondaryButton("ဓာတ်ပုံ သိမ်းမည်") {
                    val ok = WishCard.saveToGallery(context, WishCard.render(context, spec))
                    Toast.makeText(context, if (ok) "ဓာတ်ပုံ သိမ်းပြီးပါပြီ" else "မသိမ်းနိုင်ပါ", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
private fun CardField(label: String, value: String, hint: String = "", lines: Int = 1, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = Type.small.copy(color = Palette.muted))
        OutlinedTextField(
            value, onChange, Modifier.fillMaxWidth(), textStyle = Type.body, minLines = lines,
            placeholder = { if (hint.isNotEmpty()) Text(hint, style = Type.small.copy(color = Palette.muted)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Palette.accent, unfocusedBorderColor = Palette.line, cursorColor = Palette.accent,
                focusedContainerColor = Palette.bg.copy(alpha = 0.75f), unfocusedContainerColor = Palette.bg.copy(alpha = 0.75f),
            ),
        )
    }
}
