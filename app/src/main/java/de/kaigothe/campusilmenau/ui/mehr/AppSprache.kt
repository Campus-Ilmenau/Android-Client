package de.kaigothe.campusilmenau.ui.mehr

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import de.kaigothe.campusilmenau.domain.model.SpracheWahl
import java.util.Locale

/**
 * Die Sprache der App unabhängig vom System. Ab Android 13 setzt das System sie (`LocaleManager`, erscheint auch in den
 * App-Einstellungen des Geräts); davor merkt sich die App die Wahl selbst und setzt sie beim Start der Activity
 * ([wrap]). Eine Änderung gilt sofort, die Activity startet dazu neu.
 */
object AppSprache {
    private const val PREFS = "sprache"
    private const val KEY = "wahl"

    /** Die gewählte Sprache; [SpracheWahl.Automatisch], wenn nichts gewählt ist. */
    fun aktuell(context: Context): SpracheWahl =
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (locales.isEmpty) SpracheWahl.Automatisch else SpracheWahl.ofTag(locales[0].language)
        } else {
            SpracheWahl.ofTag(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))
        }

    /** Setzt die Sprache und startet [activity] neu, damit alle Texte wechseln. */
    fun waehle(activity: Activity, wahl: SpracheWahl) {
        if (Build.VERSION.SDK_INT >= 33) {
            // Das System startet die Activity selbst neu.
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                wahl.tag?.let(LocaleList::forLanguageTags) ?: LocaleList.getEmptyLocaleList()
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, wahl.tag).apply()
            activity.recreate()
        }
    }

    /** Vor Android 13: legt die gewählte Sprache über den Kontext der Activity. Ohne Wahl bleibt er, wie er ist. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val tag = SpracheWahl.ofTag(base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)).tag ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        return base.createConfigurationContext(configuration)
    }

    /** Die Sprache, in der die App gerade erscheint (für „Automatisch · Deutsch“). */
    fun erscheintAls(context: Context): SpracheWahl = SpracheWahl.fuerGeraet(context.resources.configuration.locales[0].language)
}
