package de.kaigothe.campusilmenau.ui.shortcuts

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import de.kaigothe.campusilmenau.MainActivity
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.ui.navigation.DeepLink

/**
 * App-Shortcuts (langes Drücken des App-Icons). Fünf stehen statisch in `res/xml/shortcuts.xml`: Mensa heute, Heute Abend,
 * Katalog, Merkliste, Öffnungszeiten. „Thoska auslesen“ kommt nur auf Geräten mit NFC und ist deshalb dynamisch; die
 * Beschriftung wird mit der Sprache der App neu gesetzt, wenn die App startet.
 *
 * Alle Shortcuts öffnen einen Link der App ([DeepLink]), den die Shell wie jeden Link von außen behandelt.
 */
object CampusShortcuts {
    const val ThoskaId = "thoska_auslesen"

    /** Setzt den dynamischen Shortcut für Thoska; ohne NFC wird er entfernt. */
    fun publish(context: Context) {
        val hasNfc = context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
        if (!hasNfc) {
            ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(ThoskaId))
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, DeepLink.ThoskaAuslesen.url.toUri(), context, MainActivity::class.java)
        val shortcut = ShortcutInfoCompat.Builder(context, ThoskaId)
            .setShortLabel(context.getString(R.string.shortcut_thoska_kurz))
            .setLongLabel(context.getString(R.string.shortcut_thoska_lang))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_nfc))
            .setIntent(intent)
            .setRank(10)
            .build()
        // Ein Fehler (zum Beispiel zu viele Aufrufe in kurzer Zeit) ist kein Grund, den Start zu stören.
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, listOf(shortcut)) }
    }
}
