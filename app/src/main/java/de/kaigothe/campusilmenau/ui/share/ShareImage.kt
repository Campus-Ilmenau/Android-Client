package de.kaigothe.campusilmenau.ui.share

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.withFrameNanos
import androidx.core.content.FileProvider
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.ui.share.card.ShareCardSpec
import de.kaigothe.campusilmenau.ui.share.card.ShareCardStyle
import de.kaigothe.campusilmenau.ui.share.card.ShareCardTheme
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Das Bild einer Karte zum Teilen: Dateiname, Stil des Rahmens und die Karte selbst. */
class ShareImageSpec(val fileName: String, val style: ShareCardStyle, val card: @Composable () -> Unit)

/**
 * Rendert Karten als Bild. Die Karte entsteht in einer `ComposeView`, die außerhalb des sichtbaren Bereichs am Fenster
 * hängt: Sie braucht ein Fenster für ihre Zusammensetzung, soll aber nie zu sehen sein. Die Breite ist fest 1080 px
 * (360 dp bei Dichte 3), die Höhe folgt dem Inhalt.
 */
object ShareImageRenderer {
    private const val MaxFrames = 30
    private const val StableFrames = 3

    /** Rendert [card] im Stil [style]. Wirft, wenn die Karte nicht entsteht. */
    suspend fun render(activity: Activity, style: ShareCardStyle, card: @Composable () -> Unit): Bitmap {
        val widthPx = (ShareCardSpec.WidthDp * ShareCardSpec.Scale).roundToInt()
        val decor = activity.window.decorView as ViewGroup
        val composeView = ComposeView(activity).apply { setContent { ShareCardTheme(style, card) } }
        val host = OffscreenHost(activity, widthPx).apply {
            addView(composeView, FrameLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT))
            // Weit links außerhalb des Fensters, für Bedienungshilfen unsichtbar.
            translationX = -widthPx * 2f
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
        decor.addView(host, FrameLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT))
        try {
            // Warten, bis die Zusammensetzung steht und die Höhe sich nicht mehr ändert (die Eintrittskarte misst ihren
            // oberen Teil erst im zweiten Durchgang).
            var last = -1
            var stable = 0
            var frames = 0
            while (frames < MaxFrames && stable < StableFrames) {
                withFrameNanos { }
                frames++
                val height = host.height
                if (height > 0 && height == last) stable++ else stable = 0
                last = height
            }
            check(host.height > 0) { "Die Karte hat keine Höhe." }
            return Bitmap.createBitmap(widthPx, host.height, Bitmap.Config.ARGB_8888).also { host.draw(Canvas(it)) }
        } finally {
            decor.removeView(host)
        }
    }

    /** Hält das Bild so breit wie die Karte und so hoch wie ihren Inhalt, ohne vom Fenster beschnitten zu werden. */
    private class OffscreenHost(context: Context, private val widthPx: Int) : FrameLayout(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val child = getChildAt(0)
            if (child == null) {
                setMeasuredDimension(widthPx, 0)
                return
            }
            child.measure(
                MeasureSpec.makeMeasureSpec(widthPx, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
            setMeasuredDimension(widthPx, child.measuredHeight)
        }
    }
}

/** Schreibt das Bild als PNG in den Zwischenspeicher und gibt die Adresse für den Share-Intent zurück. */
suspend fun Context.writeSharePng(bitmap: Bitmap, fileName: String): Uri = withContext(Dispatchers.IO) {
    val folder = File(cacheDir, "share").apply { mkdirs() }
    val file = File(folder, fileName)
    // Gleicher Name, gleiche Datei: Ein neueres Bild ersetzt das ältere.
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    FileProvider.getUriForFile(this@writeSharePng, "$packageName.fileprovider", file)
}

/**
 * Öffnet das Teilen-Blatt mit dem Bild. Es geht **ohne** Zusatztext raus (die Marke steht im Bild); ohne Betreff,
 * weil Nachrichten-Apps ihn sonst als eigene Nachricht zusätzlich senden.
 */
fun Context.shareImage(uri: Uri) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    // Die Leseerlaubnis gilt über den ClipData-Eintrag auch für die App, die das Teilen-Blatt wählt.
    send.clipData = ClipData.newRawUri("", uri)
    val chooser = Intent.createChooser(send, getString(R.string.share_chooser))
    if (findActivityContext() == null) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(chooser)
}
