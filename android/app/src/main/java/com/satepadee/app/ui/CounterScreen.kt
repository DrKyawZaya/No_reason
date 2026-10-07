package com.satepadee.app.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satepadee.app.data.AppModel
import com.satepadee.app.data.CountMode
import com.satepadee.app.data.CountResult
import com.satepadee.app.data.Kozawin
import com.satepadee.app.data.Mm
import com.satepadee.app.data.Target
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun CounterScreen(
    model: AppModel,
    target: Target,
    onFinished: (CountResult, Int?) -> Unit = { _, _ -> },
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = remember { Haptics(context) }
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }

    val day = if (target == Target.Kozawin) model.todayDay() else null
    val progress = model.progressFor(target)
    val per = model.beadsPerRound(target)
    val goal = model.targetRounds(target)
    val wood = model.wood()
    val mode = model.state.mode
    var showMeaning by remember { mutableStateOf(false) }

    val phase = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val total = progress.rounds * per + progress.beads

    fun countOne() {
        val dayIndex = day?.index
        val r = model.count(target)
        if (r == CountResult.Bead) haptics.tick() else haptics.round()
        if (r == CountResult.DayDone || r == CountResult.StageDone || r == CountResult.ProgramDone || r == CountResult.GoalDone) {
            onFinished(r, dayIndex)
        }
    }

    Column(
        Modifier.fillMaxSize().background(Palette.bg).safeDrawingPadding().padding(16.dp).readableWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Palette.surface).border(1.dp, Palette.line, RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = "နောက်သို့" },
                contentAlignment = Alignment.Center,
            ) { BackIcon() }
            Spacer(Modifier.weight(1f))
            Text(
                if (goal > 0) "${Mm.n(progress.rounds)} / ${Mm.n(goal)} ပတ်" else "${Mm.n(progress.rounds)} ပတ်",
                style = Type.heading.copy(color = Palette.muted),
            )
        }

        // Tap the title to show or hide its meaning; the counting area gets the rest of the screen.
        val custom = (target as? Target.Custom)?.let { model.recitation(it.id) }
        val (title, meaning) = when {
            day != null -> model.content.guna(day.guna).let { it.pali to it.meaning.first() }
            custom != null -> custom.text.ifBlank { custom.name } to (if (custom.text.isNotBlank()) custom.name else "")
            else -> "စိပ်ပုတီး (အလွတ်)" to ""
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .clickable(enabled = meaning.isNotEmpty(), role = Role.Button) { showMeaning = !showMeaning },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = Type.guna.copy(color = Palette.accent), textAlign = TextAlign.Center)
            if (showMeaning) Text(meaning, style = Type.small.copy(color = Palette.muted), textAlign = TextAlign.Center)
        }

        // Counting area: the whole box is the target for taps or swipes.
        Row(
            Modifier.weight(1f).fillMaxWidth().testTag("countArea").clip(RoundedCornerShape(24.dp))
                .background(Brush.radialGradient(listOf(Palette.surface, Palette.bg)))
                .border(1.dp, Palette.line, RoundedCornerShape(24.dp))
                .semantics {
                    contentDescription = if (mode == CountMode.Tap) "နှိပ်၍ ရေတွက်ပါ" else "ပွတ်ဆွဲ၍ ရေတွက်ပါ"
                    onClick { countOne(); true }
                }
                .pointerInput(mode, target) {
                    if (mode == CountMode.Tap) {
                        detectTapGestures(onTap = {
                            countOne()
                            scope.launch { phase.snapTo((phase.value - 1f).coerceAtLeast(-3f)); phase.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 320f)) }
                        })
                    } else {
                        // One swipe = exactly one bead, however long or fast, up or down.
                        var travel = 0f
                        var pulled = 0f
                        detectVerticalDragGestures(
                            onDragStart = { travel = 0f; pulled = 0f },
                            onDragEnd = {
                                val counted = pulled >= 0.35f
                                scope.launch {
                                    if (counted) { countOne(); phase.snapTo(phase.value - 1f) }
                                    phase.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 320f))
                                }
                            },
                            onDragCancel = { scope.launch { phase.animateTo(0f) } },
                            onVerticalDrag = { change, dy ->
                                change.consume()
                                val sp = strandSpacing(size.height.toFloat(), 76.dp.toPx())
                                travel += dy
                                pulled = (abs(travel) / sp).coerceAtMost(1f)
                                scope.launch { phase.snapTo(pulled) }
                            },
                        )
                    }
                },
        ) {
            BeadStrand(wood, total, { phase.value }, per, Modifier.weight(0.62f).fillMaxHeight())
            Column(Modifier.weight(0.38f).fillMaxHeight(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Mm.n(progress.beads), style = Type.count.copy(color = Palette.accent, fontSize = 64.sp, lineHeight = 100.sp))
                Text("/ ${Mm.n(per)}", style = Type.body.copy(color = Palette.muted))
                Spacer(Modifier.size(6.dp))
                ProgressBar(progress.beads / per.toFloat(), Modifier.width(80.dp))
            }
        }

    }
}

/** Short tick per bead, a stronger pattern at the end of a round. */
class Haptics(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun tick() = vibrate(longArrayOf(0, 15))
    fun round() = vibrate(longArrayOf(0, 60, 60, 200))

    private fun vibrate(pattern: LongArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        else @Suppress("DEPRECATION") v.vibrate(pattern, -1)
    }
}
