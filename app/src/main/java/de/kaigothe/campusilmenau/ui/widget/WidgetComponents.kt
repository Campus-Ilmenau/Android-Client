package de.kaigothe.campusilmenau.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as FixedColor
import de.kaigothe.campusilmenau.MainActivity
import androidx.compose.runtime.Composable
import androidx.glance.layout.fillMaxWidth
import androidx.glance.text.TextAlign
import de.kaigothe.campusilmenau.ui.theme.CampusColors
import de.kaigothe.campusilmenau.ui.theme.DarkCampusColors
import de.kaigothe.campusilmenau.ui.theme.LightCampusColors

/**
 * Bausteine der Widgets. Die Farben kommen aus denselben Paletten wie die App (hell und dunkel), das Widget wechselt mit
 * dem Modus des Geräts. Grün und Blau sind Flächen, Text steht nie darauf in Grün.
 */
internal object WidgetColors {
    private fun pair(pick: (CampusColors) -> Color) = ColorProvider(day = pick(LightCampusColors), night = pick(DarkCampusColors))

    val surface = pair { it.surface }
    val separator = pair { it.separator }
    val textPrimary = pair { it.textPrimary }
    val textSecondary = pair { it.textSecondary }
    val accent = pair { it.accent }

    /** Der Kopf mit der Mensa: Blau mit weißer Schrift, in beiden Modi gleich. */
    val header = FixedColor(LightCampusColors.blue)
    val onHeader = FixedColor(LightCampusColors.onBlue)
}

/** Ein Tipp öffnet den Link in der App (`campusilmenau://…`, die Shell leitet ihn wie jeden Link von außen). */
internal fun openLink(context: Context, url: String): Action =
    actionStartActivity(Intent(Intent.ACTION_VIEW, url.toUri(), context, MainActivity::class.java))

/** Kopf des Widgets: Blau, oben kleine Zeile, darunter der Titel, optional eine Unterzeile. */
@Composable
internal fun WidgetHeader(kicker: String?, title: String, subtitle: String?, modifier: GlanceModifier = GlanceModifier) {
    Column(modifier = modifier.fillMaxWidth().background(WidgetColors.header).padding(horizontal = 14.dp, vertical = 10.dp)) {
        if (kicker != null) {
            Text(
                text = kicker.uppercase(),
                style = TextStyle(color = WidgetColors.onHeader, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }
        Text(
            text = title,
            style = TextStyle(color = WidgetColors.onHeader, fontSize = 17.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        if (subtitle != null) {
            Text(text = subtitle, style = TextStyle(color = WidgetColors.onHeader, fontSize = 12.sp), maxLines = 1)
        }
    }
}

/** Eine Zeile: links Text, rechts Preis oder Zeit. */
@Composable
internal fun WidgetRowView(text: String, trailing: String?, modifier: GlanceModifier = GlanceModifier) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = TextStyle(color = WidgetColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            maxLines = 2,
            modifier = GlanceModifier.defaultWeight(),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = TextStyle(color = WidgetColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
                maxLines = 1,
                modifier = GlanceModifier.padding(start = 8.dp),
            )
        }
    }
}
