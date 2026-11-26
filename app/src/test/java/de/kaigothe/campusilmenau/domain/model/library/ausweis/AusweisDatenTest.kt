package de.kaigothe.campusilmenau.domain.model.library.ausweis

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.domain.model.library.PaiaPatron
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class AusweisDatenTest {
    private fun patron(status: Int? = null, expires: Instant? = null, note: String? = null, name: String? = "Lena Schubert") =
        PaiaPatron(name = name, expires = expires, status = status, note = note)

    @Test
    fun `status texts follow the library table`() {
        assertNull(AusweisDaten("1", "x", null).statusText)
        assertNull(AusweisDaten("1", "x", patron(status = 0)).statusText)
        assertEquals(UiText(R.string.ausweis_status_gesperrt), AusweisDaten("1", "x", patron(status = 1)).statusText)
        assertEquals(UiText(R.string.ausweis_status_abgelaufen), AusweisDaten("1", "x", patron(status = 2)).statusText)
        assertEquals(UiText(R.string.ausweis_status_gebuehren), AusweisDaten("1", "x", patron(status = 3)).statusText)
        assertEquals(UiText(R.string.ausweis_status_abgelaufen_gebuehren), AusweisDaten("1", "x", patron(status = 4)).statusText)
        assertNull(AusweisDaten("1", "x", patron(status = 9)).statusText)
    }

    @Test
    fun `fresh data wins, the session name is the fallback, blank names vanish`() {
        assertEquals("Neu", AusweisDaten.of("1", patron(name = "Neu"), patron(name = "Alt")).name)
        assertEquals("Alt", AusweisDaten.of("1", null, patron(name = "Alt")).name)
        assertNull(AusweisDaten.of("1", patron(name = "  "), null).name)
        // Ohne frischen Abruf entfallen Gültigkeit, Gruppe und Status.
        val ohneKonto = AusweisDaten.of("1", null, patron(status = 1, expires = Instant.parse("2030-01-01T00:00:00Z")))
        assertNull(ohneKonto.gueltigBis)
        assertNull(ohneKonto.statusText)
    }

    @Test
    fun `valid until is critical once the day is over or the status says so`() {
        val expires = Instant.parse("2026-03-11T23:00:00Z") // 12.03. 00:00 Berlin
        val daten = AusweisDaten("1", "x", patron(status = 0, expires = expires))
        assertEquals(LocalDate.of(2026, 3, 12), daten.gueltigBis)
        // Am Tag „Gültig bis“ gilt der Ausweis noch.
        assertFalse(daten.gueltigBisIstKritisch(LocalDate.of(2026, 3, 12)))
        assertTrue(daten.gueltigBisIstKritisch(LocalDate.of(2026, 3, 13)))
        assertTrue(AusweisDaten("1", "x", patron(status = 2)).gueltigBisIstKritisch(LocalDate.of(2020, 1, 1)))
        assertTrue(AusweisDaten("1", "x", patron(status = 4)).gueltigBisIstKritisch(LocalDate.of(2020, 1, 1)))
        assertFalse(AusweisDaten("1", "x", null).gueltigBisIstKritisch(LocalDate.of(2030, 1, 1)))
    }

    @Test
    fun `library note is trimmed and blank becomes null`() {
        assertEquals("Bitte melden", AusweisDaten("1", "x", patron(note = "  Bitte melden ")).hinweis)
        assertNull(AusweisDaten("1", "x", patron(note = "  ")).hinweis)
    }

    @Test
    fun `remaining time shows minutes and seconds and never goes negative`() {
        assertEquals("2:00", AusweisDaten.restzeit(120))
        assertEquals("1:58", AusweisDaten.restzeit(118))
        assertEquals("0:05", AusweisDaten.restzeit(5))
        assertEquals("0:00", AusweisDaten.restzeit(-3))
    }

    @Test
    fun `strong biometrics or device credential from Android 11, weak before`() {
        val strong = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val weak = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        assertEquals(strong, AusweisZugang.authenticators(30))
        assertEquals(strong, AusweisZugang.authenticators(36))
        assertEquals(weak, AusweisZugang.authenticators(29))
        assertEquals(weak, AusweisZugang.authenticators(26))
    }

    @Test
    fun `pre check lets the prompt start or explains why not`() {
        assertNull(AusweisZugang.nachVorabpruefung(BiometricManager.BIOMETRIC_SUCCESS))
        val none = AusweisZugang.nachVorabpruefung(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) as AusweisZugang.Hinweis
        assertEquals(UiText(R.string.ausweis_zugang_nicht_eingerichtet_titel), none.titel)
        val other = AusweisZugang.nachVorabpruefung(BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) as AusweisZugang.Hinweis
        assertEquals(UiText(R.string.ausweis_zugang_nicht_verfuegbar_titel), other.titel)
    }

    @Test
    fun `prompt errors map to hints, cancelling stays silent`() {
        val locked = AusweisZugang.nachFehler(BiometricPrompt.ERROR_LOCKOUT) as AusweisZugang.Hinweis
        assertEquals(UiText(R.string.ausweis_zugang_gesperrt_titel), locked.titel)
        assertTrue(AusweisZugang.nachFehler(BiometricPrompt.ERROR_LOCKOUT_PERMANENT) is AusweisZugang.Hinweis)
        assertTrue(AusweisZugang.nachFehler(BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL) is AusweisZugang.Hinweis)
        assertTrue(AusweisZugang.nachFehler(BiometricPrompt.ERROR_HW_UNAVAILABLE) is AusweisZugang.Hinweis)
        assertEquals(AusweisZugang.Abgebrochen, AusweisZugang.nachFehler(BiometricPrompt.ERROR_USER_CANCELED))
        assertEquals(AusweisZugang.Abgebrochen, AusweisZugang.nachFehler(BiometricPrompt.ERROR_NEGATIVE_BUTTON))
        assertEquals(AusweisZugang.Abgebrochen, AusweisZugang.nachFehler(BiometricPrompt.ERROR_CANCELED))
        assertEquals(AusweisZugang.Abgebrochen, AusweisZugang.nachFehler(BiometricPrompt.ERROR_TIMEOUT))
    }
}
