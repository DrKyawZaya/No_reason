package com.satepadee.app.ui

import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.LruCache
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.satepadee.app.data.Kozawin
import com.satepadee.app.data.Mm
import com.satepadee.app.data.SealedWish
import java.io.File

/** What goes on a wish card. Null lines are left out. */
data class CardSpec(
    val wish: String,
    /** "အမေ အတွက်"; null on a card for oneself. */
    val nameLine: String?,
    val stageLine: String?,
    val backgroundId: String?,
    val footer: Boolean,
    val closing: String,
    /** "ဆုတောင်းပြီး · date" once the wish is made. */
    val madeLine: String? = null,
    /** The card for finishing all 81 days gets a gold border. */
    val special: Boolean = false,
)

/** Top line of a card: which stage was finished, or the whole program for the last stage. */
fun stageLine(stage: Int): String =
    if (stage == Kozawin.STAGES - 1) "ကိုးနဝင်း ပြည့်စုံ · ရက် ၈၁" else "ကိုးနဝင်း · ${Mm.STAGES[stage]} အဆင့် ပြီးပါပြီ"

fun nameLine(forSelf: Boolean, name: String): String? = if (forSelf) null else name.trim().takeIf { it.isNotEmpty() }?.let { "$it အတွက်" }

/** The card exactly as it was made. */
fun SealedWish.spec(closing: String, footer: Boolean) = CardSpec(
    wish = text, nameLine = nameLine(forSelf, name), stageLine = stageLine(stage), backgroundId = backgroundId,
    footer = footer, closing = closing, madeLine = "ဆုတောင်းပြီး · ${dateText(madeDay)}", special = stage == Kozawin.STAGES - 1,
)

/**
 * The shareable wish card: drawn as a picture so the Myanmar text looks right on every phone,
 * including ones that still use Zawgyi. Shared through the system share menu; nothing is uploaded.
 */
object WishCard {
    const val WIDTH = 1080
    const val HEIGHT = 1350
    const val PLAY_URL = "https://play.google.com/store/apps/details?id=com.satepadee.app"
    private const val CREAM = 0xFFFFF3DC.toInt()

    /** Draws the card at [width] pixels wide (4:5). The preview uses a smaller width than the shared picture. */
    fun render(context: Context, spec: CardSpec, width: Int = WIDTH): Bitmap {
        val w = width
        val h = width * HEIGHT / WIDTH
        val s = w / WIDTH.toFloat()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = Palette.bg.toArgb()

        val picture = Backgrounds.find(spec.backgroundId)
        if (picture != null) {
            // The pictures are 9:16 with the pagoda or statue near the top; keep the top part.
            val src = picture(context, picture.image, if (w < WIDTH / 2) 2 else 1)
            val cropH = (src.width * h / w.toFloat()).toInt().coerceAtMost(src.height)
            c.drawBitmap(src, Rect(0, 0, src.width, cropH), Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Palette.sunk.toArgb(), bg, Shader.TileMode.CLAMP)
            })
        }
        // Darker towards the bottom, where the words sit.
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(withAlpha(bg, 30), withAlpha(bg, 120), withAlpha(bg, 230)), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP,
            )
        })

        val pad = 72 * s
        val textWidth = (w - 2 * pad).toInt()
        val footerTop = (if (spec.footer) h - 90 * s else h - 40 * s) - (if (spec.special) 24 * s else 0f)

        // The wish gets smaller when it is long, so everything still fits.
        var wishSize = 68f
        var blocks: List<StaticLayout>
        val gap = 26 * s
        while (true) {
            blocks = listOfNotNull(
                spec.stageLine?.let { layout(it, 34 * s, Palette.accent.toArgb(), false, textWidth, s) },
                layout(spec.wish.ifBlank { " " }, wishSize * s, CREAM, true, textWidth, s),
                spec.nameLine?.let { layout(it, 38 * s, Palette.fg.toArgb(), false, textWidth, s) },
                spec.closing.takeIf { it.isNotBlank() }?.let { layout(it, 34 * s, Palette.accent.toArgb(), false, textWidth, s) },
                spec.madeLine?.let { layout(it, 26 * s, Palette.muted.toArgb(), false, textWidth, s) },
            )
            val total = blocks.sumOf { it.height } + gap * (blocks.size - 1)
            if (total <= footerTop - h * 0.48f || wishSize <= 40f) break
            wishSize -= 6f
        }
        val total = blocks.sumOf { it.height } + gap * (blocks.size - 1)
        val regionTop = h * 0.48f
        var y = (regionTop + (footerTop - regionTop - total) / 2).coerceAtLeast(h * 0.30f)
        for (b in blocks) {
            c.save(); c.translate(pad, y); b.draw(c); c.restore()
            y += b.height + gap
        }

        if (spec.special) {
            // Gold double frame for the 81-day card.
            val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Palette.accent.toArgb() }
            gold.strokeWidth = 6 * s
            c.drawRoundRect(26 * s, 26 * s, w - 26 * s, h - 26 * s, 28 * s, 28 * s, gold)
            gold.strokeWidth = 2 * s
            gold.alpha = 160
            c.drawRoundRect(42 * s, 42 * s, w - 42 * s, h - 42 * s, 20 * s, 20 * s, gold)
        }

        if (spec.footer) {
            val f = layout("စိပ်ပုတီး · Google Play", 28 * s, Palette.muted.toArgb(), false, textWidth, s)
            c.save(); c.translate(pad, h - (if (spec.special) 60 else 40) * s - f.height); f.draw(c); c.restore()
        }
        return bmp
    }

    private fun layout(text: String, size: Float, color: Int, bold: Boolean, width: Int, s: Float): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setShadowLayer(10 * s, 0f, 2 * s, 0xCC000000.toInt())
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(true)
            .build()
    }

    // Decoding a background takes tens of milliseconds, too slow to repeat for every letter typed.
    // Two full-size pictures plus the small ones fit in 16 MB.
    private val pictures = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    private fun picture(context: Context, res: Int, sampleSize: Int): Bitmap {
        val key = "$res/$sampleSize"
        return pictures.get(key) ?: BitmapFactory.decodeResource(context.resources, res, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            .also { pictures.put(key, it) }
    }

    private fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha shl 24)

    /** Opens the phone's share menu with the card picture (and the Play link when [withLink]). */
    fun share(context: Context, card: Bitmap, withLink: Boolean) {
        val dir = File(context.cacheDir, "cards").apply { mkdirs() }
        val file = File(dir, "wish.jpg")
        file.outputStream().use { card.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).setType("image/jpeg")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri("", uri)
        if (withLink) send.putExtra(Intent.EXTRA_TEXT, PLAY_URL)
        val chooser = Intent.createChooser(send, "ဆုတောင်းကတ် မျှဝေမည်")
        if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /** Saves the card into Pictures/SatePaDee. Android 10 and later need no permission for this. */
    @RequiresApi(29)
    fun saveToGallery(context: Context, card: Bitmap): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "satepadee-wish-${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SatePaDee")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return runCatching {
            resolver.openOutputStream(uri)!!.use { card.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        }.getOrElse {
            resolver.delete(uri, null, null)
            false
        }
    }
}
