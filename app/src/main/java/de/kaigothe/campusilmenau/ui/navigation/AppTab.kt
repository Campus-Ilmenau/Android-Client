package de.kaigothe.campusilmenau.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import de.kaigothe.campusilmenau.R

/**
 * Die fünf Tabs der App. „Mehr“ ist kein Tab, sondern der Personen-Button oben rechts auf „Heute“.
 * Die Reihenfolge ist die der Tab-Leiste.
 */
enum class AppTab(
    @param:StringRes val title: Int,
    val icon: ImageVector,
) {
    Heute(R.string.tab_heute, Icons.Outlined.WbSunny),
    Mensa(R.string.tab_mensa, Icons.Outlined.Restaurant),
    Thoska(R.string.tab_thoska, Icons.Outlined.Nfc),
    Bibliothek(R.string.tab_bibliothek, Icons.AutoMirrored.Outlined.MenuBook),
    Events(R.string.tab_events, Icons.Outlined.CalendarMonth),
    ;

    companion object {
        /** Die Tabs, die dieses Gerät zeigt: ohne NFC-Hardware kein Thoska. */
        fun visible(hasNfc: Boolean): List<AppTab> = entries.filter { it != Thoska || hasNfc }
    }
}
