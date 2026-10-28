package com.satepadee.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.Kozawin
import com.satepadee.app.data.Mm
import com.satepadee.app.data.SealedWish

/**
 * "ကျွန်ုပ်၏ ဆုတောင်းကတ်များ": nine places for the current round (made, waiting, or not reached yet),
 * then the cards from earlier rounds.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WishCollection(model: AppModel, onOpen: (runId: Long, stage: Int) -> Unit) {
    val run = model.state.runId
    val earlier = model.state.sealedWishes.filter { it.runId != run }
    if ((run == null || model.state.start == null) && earlier.isEmpty()) return
    val earned = model.earnedStages()

    Card {
        Text("ကျွန်ုပ်၏ ဆုတောင်းကတ်များ", style = Type.heading)
        Text("အဆင့်တစ်ခု ပြီးတိုင်း ဆုတောင်းကတ် တစ်ခု ရပါသည်။", style = Type.small.copy(color = Palette.muted))
        if (run != null && model.state.start != null) {
            for (row in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0 until 3) {
                        val stage = row * 3 + col
                        val sealed = model.sealedWish(run, stage)
                        Box(Modifier.weight(1f)) {
                            when {
                                sealed != null -> Thumb(model, sealed) { onOpen(run, stage) }
                                stage in earned -> Slot(stage, "ဆုတောင်းရန်", waiting = true) { onOpen(run, stage) }
                                else -> Slot(stage, "မရောက်သေး", waiting = false, onClick = null)
                            }
                        }
                    }
                }
            }
        }
        if (earlier.isNotEmpty()) {
            Text("ယခင် အကြိမ်များ", style = Type.small.copy(color = Palette.muted, fontWeight = FontWeight.Bold))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (w in earlier.sortedWith(compareBy({ it.runId }, { it.stage }))) {
                    Box(Modifier.width(96.dp)) { Thumb(model, w) { onOpen(w.runId, w.stage) } }
                }
            }
        }
    }
}

@Composable
private fun Thumb(model: AppModel, wish: SealedWish, onClick: () -> Unit) {
    val context = LocalContext.current
    val bmp = remember(wish) { WishCard.render(context, wish.spec(model.content.wishClosing, footer = false), width = 216).asImageBitmap() }
    Image(
        bmp, contentDescription = "${Mm.STAGES[wish.stage]} အဆင့် ဆုတောင်းကတ်",
        modifier = Modifier.fillMaxSize().aspectRatio(WishCard.WIDTH / WishCard.HEIGHT.toFloat()).clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
private fun Slot(stage: Int, label: String, waiting: Boolean, onClick: (() -> Unit)?) {
    val color = if (waiting) Palette.accent else Palette.line
    Column(
        Modifier.fillMaxSize().aspectRatio(WishCard.WIDTH / WishCard.HEIGHT.toFloat()).clip(RoundedCornerShape(10.dp))
            .background(if (waiting) Palette.accentSoft.copy(alpha = 0.5f) else Palette.sunk.copy(alpha = 0.6f))
            .drawBehind {
                drawRoundRect(
                    color, cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(width = (if (waiting) 2 else 1).dp.toPx(), pathEffect = if (waiting) PathEffect.dashPathEffect(floatArrayOf(12f, 8f)) else null),
                )
            }
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics { contentDescription = "${Mm.STAGES[stage]} အဆင့် $label" },
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(Mm.STAGES[stage], style = Type.small.copy(color = if (waiting) Palette.accent else Palette.muted, fontWeight = FontWeight.Bold))
        Text(label, style = Type.tiny.copy(color = if (waiting) Palette.accent else Palette.muted), textAlign = TextAlign.Center)
        if (stage == Kozawin.STAGES - 1) Text("ပြည့်စုံ", style = Type.tiny.copy(color = Palette.muted))
    }
}
