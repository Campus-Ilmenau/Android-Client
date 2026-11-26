package de.kaigothe.campusilmenau.ui.mehr

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import de.kaigothe.campusilmenau.R

/**
 * Das Icon der App im Launcher (iOS: `AppIconWahl`). Android kennt kein „Automatisch“ im Sinne von Hell und Dunkel, das
 * Standard-Icon bringt aber ein Themed Icon mit, das ab Android 13 die Farben des Hintergrundbilds annimmt. Dunkel ist
 * ein fester Stil mit Nachtverlauf.
 *
 * Jede Wahl ist ein `activity-alias` im Manifest mit eigenem Icon; genau einer ist aktiv. [alias] ist der Klassenname
 * relativ zum Paket der App.
 */
enum class AppIconWahl(val alias: String, @StringRes val titel: Int) {
    Standard(".IconStandard", R.string.mehr_icon_standard),
    Dunkel(".IconDunkel", R.string.mehr_icon_dunkel),
}

/** Liest und setzt das Launcher-Icon über die Aliase im Manifest. */
object AppIcon {
    private fun komponente(context: Context, wahl: AppIconWahl) = ComponentName(context.packageName, context.packageName + wahl.alias)

    /**
     * Das aktive Icon. Im Manifest ist nur der Standard-Alias aktiv; [waehle] aktiviert einen anderen ausdrücklich. Ist ein
     * anderer als der Standard ausdrücklich aktiv, gilt er, sonst der Standard.
     */
    fun aktuell(context: Context): AppIconWahl {
        val pm = context.packageManager
        return AppIconWahl.entries.firstOrNull {
            it != AppIconWahl.Standard && pm.getComponentEnabledSetting(komponente(context, it)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } ?: AppIconWahl.Standard
    }

    /**
     * Schaltet den gewählten Alias ein und die anderen aus. Der neue wird zuerst aktiviert, damit die App zu keinem
     * Zeitpunkt ohne Launcher-Eintrag ist. `DONT_KILL_APP` hält die laufende App offen; der Launcher aktualisiert sein
     * Icon selbst, das kann einen Moment dauern.
     */
    fun waehle(context: Context, wahl: AppIconWahl) {
        val pm = context.packageManager
        pm.setComponentEnabledSetting(komponente(context, wahl), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        AppIconWahl.entries.filter { it != wahl }.forEach {
            pm.setComponentEnabledSetting(komponente(context, it), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }
}
