package com.satepadee.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Warm teak and saffron, matching the Figma design. Dark only: the app is used at dawn and night. */
object Palette {
    val bg = Color(0xFF16110C)
    val surface = Color(0xFF211912)
    val sunk = Color(0xFF2B2118)
    val fg = Color(0xFFF2E5D1)
    val muted = Color(0xFFB29C80)
    val line = Color(0xFF3B2E22)
    val accent = Color(0xFFEBA443)
    val accentSoft = Color(0xFF4A3317)
    val ink = Color(0xFF1A120A)
    val jade = Color(0xFF6CC08B)
    val jadeSoft = Color(0xFF1F3A29)
    val warn = Color(0xFFE88A6F)
    val warnSoft = Color(0xFF43231A)
}

// Myanmar script stacks marks above and below the line; generous line height avoids clipping.
private fun mm(size: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontSize = size.sp, lineHeight = (size * 1.75).sp, fontWeight = weight, color = Palette.fg)

object Type {
    val title = mm(26, FontWeight.Bold)
    val guna = mm(30, FontWeight.Bold)
    val heading = mm(17, FontWeight.Bold)
    val body = mm(15)
    val small = mm(13)
    val tiny = mm(12)
    val count = mm(56, FontWeight.Bold)
}

@Composable
fun SatePaDeeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.accent, onPrimary = Palette.ink,
            background = Palette.bg, onBackground = Palette.fg,
            surface = Palette.surface, onSurface = Palette.fg,
            surfaceVariant = Palette.sunk, onSurfaceVariant = Palette.muted,
            outline = Palette.line, error = Palette.warn,
        ),
        typography = Typography(
            bodyLarge = Type.body, bodyMedium = Type.body, bodySmall = Type.small,
            titleLarge = Type.title, titleMedium = Type.heading, labelLarge = mm(15, FontWeight.SemiBold),
        ),
        content = content,
    )
}
