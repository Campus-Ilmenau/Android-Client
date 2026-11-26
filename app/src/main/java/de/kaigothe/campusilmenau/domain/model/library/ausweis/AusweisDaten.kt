package de.kaigothe.campusilmenau.domain.model.library.ausweis

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.library.PaiaPatron
import java.time.LocalDate

/**
 * Alles, was der Bibliotheksausweis anzeigt. Entsteht beim Öffnen und lebt nur so lange wie die Ansicht: Nichts davon wird
 * gespeichert. `email` und `address` des Kontos werden nie gelesen.
 */
data class AusweisDaten(
    val benutzernummer: String,
    /** Name aus dem Abruf, sonst aus dem letzten Abruf der Sitzung. */
    val name: String?,
    /** Das Konto aus dem frischen Abruf; `null`, wenn der Abruf fehlschlug (dann entfallen „Gültig bis“, Gruppe und Status). */
    val konto: PaiaPatron?,
) {
    /** 0 = nichts. 1 bis 4 gemäß PAIA. */
    val status: Int get() = konto?.status ?: 0

    /** Nutzergruppe als Chip. Solange die URIs der UB unbekannt sind, entfällt der Chip. */
    val nutzergruppe: UiText? get() = konto?.userGroup

    /** Hinweis der Bibliothek (`note`), leer wird zu `null`. */
    val hinweis: String? get() = konto?.note?.trim()?.takeIf { it.isNotEmpty() }

    /** Text für den Status-Kasten; bei Status 0 oder unbekannt `null`. */
    val statusText: UiText?
        get() = when (status) {
            1 -> UiText(R.string.ausweis_status_gesperrt)
            2 -> UiText(R.string.ausweis_status_abgelaufen)
            3 -> UiText(R.string.ausweis_status_gebuehren)
            4 -> UiText(R.string.ausweis_status_abgelaufen_gebuehren)
            else -> null
        }

    /** Das Datum „Gültig bis“ als Berliner Kalendertag, oder `null` (dann entfällt die Zeile). */
    val gueltigBis: LocalDate? get() = konto?.expires?.let(BerlinTime::date)

    /** Datum in `danger`: Es liegt vor heute (am Tag „Gültig bis“ gilt der Ausweis noch) oder der Status ist 2 bzw. 4. */
    fun gueltigBisIstKritisch(heute: LocalDate): Boolean {
        if (status == 2 || status == 4) return true
        return gueltigBis?.isBefore(heute) == true
    }

    companion object {
        /** `frisch` ist das Ergebnis des Abrufs beim Öffnen (`null` bei Fehler), `letzter` der Stand der Sitzung. */
        fun of(benutzernummer: String, frisch: PaiaPatron?, letzter: PaiaPatron?) = AusweisDaten(
            benutzernummer = benutzernummer,
            name = (frisch?.name ?: letzter?.name)?.trim()?.takeIf { it.isNotEmpty() },
            konto = frisch,
        )

        /** Restzeit als m:ss, zum Beispiel „1:58“. */
        fun restzeit(sekunden: Int): String {
            val s = maxOf(0, sekunden)
            return "${s / 60}:" + "%02d".format(s % 60)
        }

        /** Sekunden bis zum automatischen Schließen. */
        const val DAUER_SEKUNDEN = 120
    }
}
