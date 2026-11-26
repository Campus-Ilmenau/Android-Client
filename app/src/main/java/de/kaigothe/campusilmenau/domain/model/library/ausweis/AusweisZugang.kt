package de.kaigothe.campusilmenau.domain.model.library.ausweis

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText

/** Ergebnis der Prüfung vor dem Bibliotheksausweis. */
sealed interface AusweisZugang {
    data object Erlaubt : AusweisZugang

    /** Abgebrochen oder fehlgeschlagen: nichts anzeigen. */
    data object Abgebrochen : AusweisZugang

    /** Hinweis für „nicht eingerichtet / nicht verfügbar“ und „gesperrt“. */
    data class Hinweis(val titel: UiText, val text: UiText) : AusweisZugang

    companion object {
        /**
         * Erlaubt sind starke Biometrie **oder** die Geräte-PIN (Entscheidung 11): Auf Android haben viele Geräte nur eine PIN,
         * der Ausweis wäre dort sonst unbenutzbar. Vor Android 11 gibt es die Kombination mit starker Biometrie nicht, dort
         * gilt schwache Biometrie oder Geräte-PIN.
         */
        fun authenticators(sdk: Int): Int =
            if (sdk >= 30) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            }

        /** Ergebnis von `BiometricManager.canAuthenticate`: `null` heißt, die Abfrage darf starten. */
        fun nachVorabpruefung(code: Int): AusweisZugang? = when (code) {
            BiometricManager.BIOMETRIC_SUCCESS -> null
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Hinweis(
                UiText(R.string.ausweis_zugang_nicht_eingerichtet_titel),
                UiText(R.string.ausweis_zugang_nicht_eingerichtet_text),
            )
            else -> Hinweis(UiText(R.string.ausweis_zugang_nicht_verfuegbar_titel), UiText(R.string.ausweis_zugang_nicht_verfuegbar_text))
        }

        /** Fehler der Abfrage selbst (`onAuthenticationError`). Abbruch durch den Nutzer bleibt ohne Meldung. */
        fun nachFehler(code: Int): AusweisZugang = when (code) {
            BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> Hinweis(
                UiText(R.string.ausweis_zugang_gesperrt_titel),
                UiText(R.string.ausweis_zugang_gesperrt_text),
            )
            BiometricPrompt.ERROR_NO_BIOMETRICS, BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> Hinweis(
                UiText(R.string.ausweis_zugang_nicht_eingerichtet_titel),
                UiText(R.string.ausweis_zugang_nicht_eingerichtet_text),
            )
            BiometricPrompt.ERROR_HW_NOT_PRESENT, BiometricPrompt.ERROR_HW_UNAVAILABLE, BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED ->
                Hinweis(UiText(R.string.ausweis_zugang_nicht_verfuegbar_titel), UiText(R.string.ausweis_zugang_nicht_verfuegbar_text))
            else -> Abgebrochen
        }
    }
}
