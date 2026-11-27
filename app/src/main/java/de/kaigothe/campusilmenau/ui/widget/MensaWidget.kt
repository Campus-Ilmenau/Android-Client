package de.kaigothe.campusilmenau.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.android.EntryPointAccessors
import de.kaigothe.campusilmenau.R

/**
 * Das Widget „Mensa heute“: die Jetzt-Karte der Startseite auf dem Homescreen, mit der Mensa, dem Filter und der Preisgruppe
 * aus den Einstellungen der App. Klein zeigt Mensa und das erste Gericht, mittel und groß bis zu drei. Ein Tipp öffnet den
 * Speiseplan, ein Tipp auf ein Gericht dessen Seite.
 */
class MensaWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(Small, Medium, Large))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val source = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java).widgetData()
        val data = source.content().toMensaWidgetData { it.resolve(context) }
        provideContent { MensaWidgetContent(data) }
    }

    companion object {
        val Small = DpSize(110.dp, 110.dp)
        val Medium = DpSize(250.dp, 110.dp)
        val Large = DpSize(250.dp, 250.dp)

        /** Wie viele Gerichte bei dieser Höhe Platz haben. */
        fun rowsFor(height: androidx.compose.ui.unit.Dp): Int = when {
            height < 150.dp -> 1
            else -> 3
        }
    }
}

class MensaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MensaWidget()
}

@Composable
private fun MensaWidgetContent(data: MensaWidgetData) {
    val context = LocalContext.current
    val size = LocalSize.current
    val rows = data.rows.take(MensaWidget.rowsFor(size.height))
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.surface)
            .cornerRadius(20.dp),
    ) {
        WidgetHeader(
            kicker = data.kicker,
            title = data.title,
            // Die Unterzeile braucht Höhe; im kleinen Widget entfällt sie.
            subtitle = data.subtitle.takeIf { size.height >= 150.dp },
            modifier = GlanceModifier.clickable(openLink(context, data.url)),
        )
        if (rows.isEmpty()) {
            Text(
                text = context.getString(R.string.widget_keine_daten),
                style = TextStyle(color = WidgetColors.textSecondary, fontSize = 12.sp),
                modifier = GlanceModifier.fillMaxWidth().padding(14.dp).clickable(openLink(context, data.url)),
            )
        } else {
            rows.forEach { row ->
                WidgetRowView(row.text, row.trailing, modifier = GlanceModifier.clickable(openLink(context, row.url)))
            }
        }
    }
}
