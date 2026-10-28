package com.satepadee.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.paint
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.satepadee.app.R

/** A background picture for the counter. The pictures are dark in their lower half, where the beads sit. */
data class Background(val id: String, val name: String, @DrawableRes val image: Int, @DrawableRes val thumb: Int)

object Backgrounds {
    val all = listOf(
        Background("shwedagon", "ရွှေတိဂုံ", R.drawable.theme_shwedagon, R.drawable.theme_shwedagon_thumb),
        Background("kyaiktiyo", "ကျိုက်ထီးရိုး", R.drawable.theme_kyaiktiyo, R.drawable.theme_kyaiktiyo_thumb),
        Background("bagan", "ပုဂံ", R.drawable.theme_bagan, R.drawable.theme_bagan_thumb),
        Background("buddha", "ဘုရားရှင်", R.drawable.theme_buddha, R.drawable.theme_buddha_thumb),
        Background("monastery", "တောင်ပေါ် ကျောင်း", R.drawable.theme_monastery, R.drawable.theme_monastery_thumb),
        Background("bodhi", "ဗောဓိပင်", R.drawable.theme_bodhi, R.drawable.theme_bodhi_thumb),
    )

    fun find(id: String?): Background? = all.firstOrNull { it.id == id }
}

/** The chosen background id for every page; provided once by AppShell. */
val LocalBackgroundId = staticCompositionLocalOf<String?> { null }

/** Scrim for text pages: darker than the counter's so cards and text stay easy to read. */
private val pageScrim = Brush.verticalGradient(
    0f to Palette.bg.copy(alpha = 0.55f), 0.45f to Palette.bg.copy(alpha = 0.72f), 1f to Palette.bg.copy(alpha = 0.9f),
)

/** Page background: plain teak, or the chosen picture under a dark layer. Apply before any padding. */
@Composable
fun Modifier.pageBackground(): Modifier {
    val bg = Backgrounds.find(LocalBackgroundId.current) ?: return this.background(Palette.bg)
    return this.background(Palette.bg)
        .paint(painterResource(bg.image), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
        .background(pageScrim)
}

/** The picture with a dark layer over it so numbers and beads stay easy to read. */
@Composable
fun BackgroundLayer(bg: Background, modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier) {
        Image(painterResource(bg.image), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Palette.bg.copy(alpha = 0.45f), 0.4f to Palette.bg.copy(alpha = 0.30f),
                    0.6f to Palette.bg.copy(alpha = 0.65f), 1f to Palette.bg.copy(alpha = 0.88f),
                ),
            ),
        )
    }
}

/** Row of small previews; the first is the original plain look. */
@Composable
fun BackgroundPicker(selectedId: String?, onPick: (String?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PickerItem("မူလ", selectedId == null, { onPick(null) }) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Palette.sunk, Palette.bg))))
        }
        for (b in Backgrounds.all) {
            PickerItem(b.name, selectedId == b.id, { onPick(b.id) }) {
                Image(painterResource(b.thumb), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
    }
}

@Composable
private fun PickerItem(name: String, selected: Boolean, onClick: () -> Unit, preview: @Composable () -> Unit) {
    Column(Modifier.width(84.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val shape = RoundedCornerShape(12.dp)
        Box(
            Modifier.width(84.dp).height(130.dp).clip(shape)
                .border(if (selected) 3.dp else 1.dp, if (selected) Palette.accent else Palette.line, shape)
                .clickable(role = Role.RadioButton, onClick = onClick),
        ) { preview() }
        Text(
            name, style = Type.small.copy(color = if (selected) Palette.accent else Palette.muted, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            textAlign = TextAlign.Center, maxLines = 2,
        )
    }
}
