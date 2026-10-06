package com.satepadee.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.satepadee.app.data.CountMode

@Composable
fun Card(modifier: Modifier = Modifier, border: Color = Palette.line, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.surface)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun Pill(text: String, bg: Color, fg: Color) {
    Text(
        text, style = Type.small.copy(fontWeight = FontWeight.Bold, color = fg),
        modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(bg).padding(horizontal = 10.dp, vertical = 1.dp),
    )
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick, modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Palette.accent, contentColor = Palette.ink),
    ) { Text(text, style = Type.body.copy(fontWeight = FontWeight.Bold, color = Palette.ink)) }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick, modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Palette.line), contentPadding = PaddingValues(horizontal = 16.dp),
    ) { Text(text, style = Type.body, textAlign = TextAlign.Center) }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit) {
    Text(
        text, style = Type.body,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Palette.sunk)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().size(height = 8.dp, width = 0.dp)) {
        val h = size.height
        drawLine(Palette.sunk, Offset(h / 2, h / 2), Offset(size.width - h / 2, h / 2), h, StrokeCap.Round)
        if (fraction > 0f) drawLine(Palette.accent, Offset(h / 2, h / 2), Offset(h / 2 + (size.width - h) * fraction.coerceIn(0f, 1f), h / 2), h, StrokeCap.Round)
    }
}

/** Tap or swipe; the user's choice of how a bead is counted. */
@Composable
fun ModeSwitch(mode: CountMode, onChange: (CountMode) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.sunk).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for ((m, label) in listOf(CountMode.Tap to "နှိပ်၍ စိပ်မည်", CountMode.Swipe to "ပွတ်ဆွဲ၍ စိပ်မည်")) {
            val on = m == mode
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (on) Palette.surface else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onChange(m) }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = Type.body.copy(color = if (on) Palette.fg else Palette.muted, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)) }
        }
    }
}

@Composable
fun BackIcon(modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val w = size.width
        drawLine(Palette.fg, Offset(w * 0.62f, w * 0.2f), Offset(w * 0.32f, w * 0.5f), w * 0.09f, StrokeCap.Round)
        drawLine(Palette.fg, Offset(w * 0.32f, w * 0.5f), Offset(w * 0.62f, w * 0.8f), w * 0.09f, StrokeCap.Round)
    }
}
