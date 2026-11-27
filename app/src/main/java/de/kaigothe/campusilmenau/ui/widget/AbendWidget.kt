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
 * Das Widget „Heute Abend“: die Veranstaltungen von heute Abend, bis zu drei, mit Uhrzeit und Ort. Ein Tipp öffnet die
 * Liste von heute Abend, ein Tipp auf eine Veranstaltung ihre Seite.
 */
class AbendWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(Small, Large))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val source = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java).widgetData()
        val data = source.content().toAbendWidgetData()
        provideContent { AbendWidgetContent(data) }
    }

    companion object {
        val Small = DpSize(180.dp, 110.dp)
        val Large = DpSize(250.dp, 250.dp)
    }
}

class AbendWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AbendWidget()
}

@Composable
private fun AbendWidgetContent(data: AbendWidgetData) {
    val context = LocalContext.current
    val size = LocalSize.current
    // Klein: eine Veranstaltung, groß bis zu drei.
    val rows = data.rows.take(if (size.height < 150.dp) 1 else 3)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.surface)
            .cornerRadius(20.dp),
    ) {
        WidgetHeader(
            kicker = context.getString(R.string.widget_abend_kicker),
            title = context.getString(R.string.widget_abend_titel),
            subtitle = null,
            modifier = GlanceModifier.clickable(openLink(context, data.url)),
        )
        if (rows.isEmpty()) {
            Text(
                text = context.getString(R.string.widget_abend_leer),
                style = TextStyle(color = WidgetColors.textSecondary, fontSize = 12.sp),
                modifier = GlanceModifier.fillMaxWidth().padding(14.dp).clickable(openLink(context, data.url)),
            )
        } else {
            rows.forEach { row ->
                val place = row.place?.let { " · $it" }.orEmpty()
                val line = if (row.time.isEmpty()) row.title + place else "${row.time}  ${row.title}$place"
                WidgetRowView(line, null, modifier = GlanceModifier.clickable(openLink(context, row.url)))
            }
        }
    }
}
