package de.kaigothe.campusilmenau.ui.bibliothek.ausweis

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import de.kaigothe.campusilmenau.domain.model.library.ausweis.AusweisZugang
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Prüfung vor dem Bibliotheksausweis: Biometrie oder Geräte-PIN. Jeder Aufruf legt einen neuen `BiometricPrompt` ohne
 * `CryptoObject` an und fragt neu ab, es gilt also keine frühere Bestätigung.
 */
object AusweisPruefung {
    suspend fun pruefen(activity: FragmentActivity, title: String): AusweisZugang {
        val authenticators = AusweisZugang.authenticators(Build.VERSION.SDK_INT)
        AusweisZugang.nachVorabpruefung(BiometricManager.from(activity).canAuthenticate(authenticators))?.let { return it }

        return suspendCancellableCoroutine { continuation ->
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resumeWith(Result.success(AusweisZugang.Erlaubt))
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (continuation.isActive) continuation.resumeWith(Result.success(AusweisZugang.nachFehler(errorCode)))
                    }
                    // Ein nicht erkannter Finger lässt die Abfrage offen: Das System zeigt den Fehlversuch selbst.
                },
            )
            // Mit erlaubter Geräte-PIN darf es keine eigene Abbrechen-Taste geben (das System zeigt eine).
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setAllowedAuthenticators(authenticators)
                .build()
            prompt.authenticate(info)
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        }
    }
}
