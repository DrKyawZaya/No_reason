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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Mm
import com.satepadee.app.data.SealedWish

/**
 * The wish card earned by finishing a stage. Made once (for oneself or for someone), then sealed:
 * after that it can only be viewed, shared and saved, never edited.
 */
@Composable
fun WishCardScreen(model: AppModel, runId: Long, stage: Int, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val sealed = model.sealedWish(runId, stage)
    when {
        sealed != null -> SealedCard(model, sealed, onBack)
        runId == model.state.runId && stage in model.earnedStages() -> MakeCard(model, stage, onBack)
        else -> LaunchedEffect(Unit) { onBack() }
    }
}

private const val TITLE = "ဆုတောင်းကတ်"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MakeCard(model: AppModel, stage: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val content = model.content
    val stageWish = model.state.wishes[stage]?.takeIf { it.isNotBlank() }

    var forSelf by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var wish by rememberSaveable { mutableStateOf(stageWish ?: content.wishesSomeone.firstOrNull().orEmpty()) }
    var bgId by rememberSaveable { mutableStateOf<String?>(model.state.themeId ?: Backgrounds.all.first().id) }
    var problem by rememberSaveable { mutableStateOf<String?>(null) }
    var confirm by rememberSaveable { mutableStateOf(false) }

    val choices = (listOfNotNull(stageWish) + if (forSelf) content.wishesSelf else content.wishesSomeone).distinct()
    val spec = CardSpec(
        wish.trim(), nameLine(forSelf, name), stageLine(stage), bgId, footer = true, closing = content.wishClosing,
        special = stage == com.satepadee.app.data.Kozawin.STAGES - 1,
    )
    val preview = remember(spec) { WishCard.render(context, spec, width = 540) }

    Column(Modifier.fillMaxSize().pageBackground().safeDrawingPadding()) {
        ScreenColumn {
            TopBar(TITLE, onBack)
            CardPreview(preview)

            Card {
                Text("ဘယ်သူ့အတွက် ဆုတောင်းမလဲ", style = Type.heading)
                Choice(listOf(false to "တစ်ယောက်ယောက်", true to "ကိုယ်တိုင်"), forSelf) { forSelf = it; problem = null }
                if (!forSelf) {
                    Chips(content.wishNames, name) { name = it; problem = null }
                    CardField("အမည်", name, hint = "ဥပမာ - အမေ") { name = it.take(30); problem = null }
                }
            }

            Card {
                Text("ဆုတောင်း", style = Type.heading)
                Chips(choices, wish) { wish = it; problem = null }
                CardField("ကိုယ်တိုင် ရေးနိုင်ပါသည်", wish, lines = 2) { wish = it.take(120); problem = null }
            }

            Card {
                Text("နောက်ခံပုံ", style = Type.heading)
                BackgroundPicker(bgId) { bgId = it }
            }

            problem?.let { Text(it, style = Type.body.copy(color = Palette.warn), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            Text(
                "ဆုတောင်းပြီးပါက ပြင်၍ မရတော့ပါ။", style = Type.small.copy(color = Palette.muted),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton("ဆုတောင်းမည်", Modifier.height(60.dp)) {
                problem = when {
                    wish.isBlank() -> "ဆုတောင်းစာ ထည့်ပါ"
                    !forSelf && name.isBlank() -> "အမည် ထည့်ပါ"
                    else -> null
                }
                if (problem == null) confirm = true
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false }, containerColor = Palette.surface,
            title = { Text("ဆုတောင်းမလား", style = Type.heading) },
            text = { Text("ဆုတောင်းပြီးပါက ပြင်၍ မရတော့ပါ။ မျှဝေခြင်း၊ ဓာတ်ပုံသိမ်းခြင်းသာ ပြုလုပ်နိုင်ပါမည်။", style = Type.body.copy(color = Palette.muted)) },
            confirmButton = {
                TextButton({ confirm = false; model.sealWish(stage, forSelf, name, wish, bgId) }) {
                    Text("ဆုတောင်းမည်", style = Type.body.copy(color = Palette.accent, fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = { TextButton({ confirm = false }) { Text("ပြန်ပြင်မည်", style = Type.body.copy(color = Palette.muted)) } },
        )
    }
}

@Composable
private fun SealedCard(model: AppModel, wish: SealedWish, onBack: () -> Unit) {
    val context = LocalContext.current
    var footer by rememberSaveable { mutableStateOf(true) }
    val spec = wish.spec(model.content.wishClosing, footer)
    val preview = remember(spec) { WishCard.render(context, spec, width = 540) }

    Column(Modifier.fillMaxSize().pageBackground().safeDrawingPadding()) {
        ScreenColumn {
            TopBar(TITLE, onBack)
            CardPreview(preview)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Pill("ဆုတောင်းပြီး · ${dateText(wish.madeDay)}", Palette.jadeSoft, Palette.jade)
            }
            Text(
                "ဤဆုတောင်းကို ပြင်၍ မရပါ။ မျှဝေနိုင်ပါသည်။", style = Type.small.copy(color = Palette.muted),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Card {
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
private fun CardPreview(bitmap: android.graphics.Bitmap) {
    Image(
        bitmap.asImageBitmap(), contentDescription = "ဆုတောင်းကတ်",
        modifier = Modifier.fillMaxWidth().aspectRatio(WishCard.WIDTH / WishCard.HEIGHT.toFloat())
            .clip(RoundedCornerShape(16.dp)).border(1.dp, Palette.line, RoundedCornerShape(16.dp)).testTag("cardPreview"),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(items: List<String>, selected: String, onPick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (c in items) {
            val on = c == selected
            Text(
                c, style = Type.small.copy(color = if (on) Palette.ink else Palette.fg, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal),
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Palette.accent else Palette.sunk)
                    .clickable(role = Role.RadioButton) { onPick(c) }.padding(horizontal = 14.dp, vertical = 6.dp),
            )
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
