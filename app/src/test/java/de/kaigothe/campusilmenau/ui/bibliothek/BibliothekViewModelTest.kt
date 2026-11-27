package de.kaigothe.campusilmenau.ui.bibliothek

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.data.repository.PaiaRepository
import de.kaigothe.campusilmenau.data.repository.PaiaState
import de.kaigothe.campusilmenau.domain.model.library.PaiaError
import de.kaigothe.campusilmenau.domain.model.library.PaiaPatron
import de.kaigothe.campusilmenau.domain.model.library.ausweis.AusweisZugang
import de.kaigothe.campusilmenau.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Testfälle aus `BibliotheksausweisTests` und `BibliothekTests` der iOS-App für den Ablauf: Start, Ausweis, Anmelden. */
@OptIn(ExperimentalCoroutinesApi::class)
class BibliothekViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainRule = MainDispatcherRule(dispatcher)

    private val paiaState = MutableStateFlow(PaiaState())
    private val repository = mockk<PaiaRepository>(relaxed = true) { every { state } returns paiaState }
    private val clock = Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC)

    private fun viewModel() = BibliothekViewModel(repository, clock)

    private fun signedIn(username: String = "0001234567") {
        paiaState.value = PaiaState(isAuthenticated = true, lastUsername = username)
    }

    // --- Start

    @Test
    fun `without a session and without stored credentials the login sheet opens`() = runTest(dispatcher) {
        coEvery { repository.autoLogin() } returns false
        val vm = viewModel()
        vm.start()
        advanceUntilIdle()
        assertTrue(vm.state.value.loginVisible)
        coVerify(exactly = 0) { repository.fetchItems() }
    }

    @Test
    fun `stored credentials sign in silently and load the loans`() = runTest(dispatcher) {
        coEvery { repository.autoLogin() } returns true
        val vm = viewModel()
        vm.start()
        advanceUntilIdle()
        assertFalse(vm.state.value.loginVisible)
        coVerify(exactly = 1) { repository.fetchItems() }
    }

    @Test
    fun `an open session loads without signing in again`() = runTest(dispatcher) {
        signedIn()
        val vm = viewModel()
        vm.start()
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.autoLogin() }
        coVerify(exactly = 1) { repository.fetchItems() }
    }

    @Test
    fun `starting twice does nothing the second time`() = runTest(dispatcher) {
        signedIn()
        val vm = viewModel()
        vm.start()
        vm.start()
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.fetchItems() }
    }

    @Test
    fun `an expired session during loading signs in again or opens the sheet`() = runTest(dispatcher) {
        signedIn()
        coEvery { repository.fetchItems() } throws PaiaError.SessionExpired()
        coEvery { repository.autoLogin() } returns false
        val vm = viewModel()
        vm.start()
        advanceUntilIdle()
        assertTrue(vm.state.value.loginVisible)
    }

    // --- Ausweis

    @Test
    fun `the card opens after the check with fresh data`() = runTest(dispatcher) {
        signedIn()
        coEvery { repository.fetchPatron() } returns PaiaPatron(name = "Muster, Max", status = 0)
        val vm = viewModel()
        vm.zeigeAusweis { AusweisZugang.Erlaubt }
        advanceUntilIdle()
        val card = vm.state.value.ausweis
        assertNotNull(card)
        assertEquals("0001234567", card!!.benutzernummer)
        assertEquals("Muster, Max", card.name)
        assertFalse(vm.state.value.ausweisWirdGeprueft)
    }

    @Test
    fun `a hint after the check shows the hint and no card`() = runTest(dispatcher) {
        signedIn()
        val hinweis = AusweisZugang.Hinweis(UiText(R.string.ausweis_ok), UiText(R.string.ausweis_ok))
        val vm = viewModel()
        vm.zeigeAusweis { hinweis }
        advanceUntilIdle()
        assertEquals(hinweis, vm.state.value.zugangHinweis)
        assertNull(vm.state.value.ausweis)
        coVerify(exactly = 0) { repository.fetchPatron() }

        vm.schliesseZugangHinweis()
        advanceUntilIdle()
        assertNull(vm.state.value.zugangHinweis)
    }

    @Test
    fun `cancelling the check stays silent`() = runTest(dispatcher) {
        signedIn()
        val vm = viewModel()
        vm.zeigeAusweis { AusweisZugang.Abgebrochen }
        advanceUntilIdle()
        assertNull(vm.state.value.ausweis)
        assertNull(vm.state.value.zugangHinweis)
        assertFalse(vm.state.value.ausweisWirdGeprueft)
        coVerify(exactly = 0) { repository.fetchPatron() }
    }

    @Test
    fun `a failed fetch still shows the card with name and number from the session`() = runTest(dispatcher) {
        signedIn()
        coEvery { repository.fetchPatron() } throws PaiaError.Http(503)
        val vm = viewModel()
        vm.zeigeAusweis { AusweisZugang.Erlaubt }
        advanceUntilIdle()
        val card = vm.state.value.ausweis
        assertNotNull(card)
        assertEquals("0001234567", card!!.benutzernummer)
        assertNull(card.gueltigBis)
    }

    @Test
    fun `a session that ends while the card loads opens the login instead`() = runTest(dispatcher) {
        signedIn()
        coEvery { repository.fetchPatron() } coAnswers {
            paiaState.value = PaiaState(isAuthenticated = false, sessionExpired = true, lastUsername = "0001234567")
            throw PaiaError.SessionExpired()
        }
        coEvery { repository.autoLogin() } returns false
        val vm = viewModel()
        vm.zeigeAusweis { AusweisZugang.Erlaubt }
        advanceUntilIdle()
        assertNull(vm.state.value.ausweis)
        assertTrue(vm.state.value.loginVisible)
        assertEquals("0001234567", vm.state.value.login.username)
    }

    @Test
    fun `the check runs for every opening and never twice at once`() = runTest(dispatcher) {
        signedIn()
        coEvery { repository.fetchPatron() } returns PaiaPatron(status = 0)
        val vm = viewModel()
        var checks = 0
        val check: suspend () -> AusweisZugang = {
            checks++
            AusweisZugang.Erlaubt
        }
        vm.zeigeAusweis(check)
        vm.zeigeAusweis(check)
        advanceUntilIdle()
        assertEquals(1, checks)
        // Solange der Ausweis offen ist, startet nichts Neues.
        vm.zeigeAusweis(check)
        advanceUntilIdle()
        assertEquals(1, checks)

        vm.schliesseAusweis()
        vm.zeigeAusweis(check)
        advanceUntilIdle()
        assertEquals(2, checks)
    }

    // --- Anmelden

    @Test
    fun `login needs a number and a password and is locked while it runs`() {
        assertFalse(LoginForm().canLogin)
        assertFalse(LoginForm(username = "  ", password = "x").canLogin)
        assertFalse(LoginForm(username = "0001234567", password = "").canLogin)
        assertTrue(LoginForm(username = "0001234567", password = "x").canLogin)
        assertFalse(LoginForm(username = "0001234567", password = "x", isLoggingIn = true).canLogin)
    }

    @Test
    fun `a successful login closes the sheet, forgets the password and loads`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.openLogin()
        vm.setUsername(" 0001234567 ")
        vm.setPassword("geheim")
        vm.setRemember(true)
        vm.login()
        advanceUntilIdle()
        coVerify { repository.login("0001234567", "geheim", true) }
        assertFalse(vm.state.value.loginVisible)
        assertEquals("", vm.state.value.login.password)
        assertEquals("0001234567", vm.state.value.login.username)
        coVerify(exactly = 1) { repository.fetchItems() }
    }

    @Test
    fun `a refused login shows the error and keeps the sheet, typing clears it`() = runTest(dispatcher) {
        coEvery { repository.login(any(), any(), any()) } throws PaiaError.AuthenticationFailed("Falsches Passwort")
        val vm = viewModel()
        vm.openLogin()
        vm.setUsername("0001234567")
        vm.setPassword("falsch")
        vm.login()
        advanceUntilIdle()
        assertTrue(vm.state.value.loginVisible)
        assertTrue(vm.state.value.login.error is PaiaError.AuthenticationFailed)
        assertFalse(vm.state.value.login.isLoggingIn)

        vm.setPassword("richtig")
        advanceUntilIdle()
        assertNull(vm.state.value.login.error)
    }

    @Test
    fun `login does nothing while the form is incomplete`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.openLogin()
        vm.setUsername("0001234567")
        vm.login()
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.login(any(), any(), any()) }
    }

    @Test
    fun `cancelling the login forgets the password`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.openLogin()
        vm.setPassword("geheim")
        vm.cancelLogin()
        advanceUntilIdle()
        assertFalse(vm.state.value.loginVisible)
        assertEquals("", vm.state.value.login.password)
    }

    @Test
    fun `after an expiry the number stays in the field, after logging out it is empty`() = runTest(dispatcher) {
        paiaState.value = PaiaState(sessionExpired = true, lastUsername = "0001234567")
        val vm = viewModel()
        vm.openLogin()
        advanceUntilIdle()
        assertEquals("0001234567", vm.state.value.login.username)

        paiaState.value = PaiaState(lastUsername = null)
        val fresh = viewModel()
        fresh.openLogin()
        advanceUntilIdle()
        assertEquals("", fresh.state.value.login.username)
    }

    @Test
    fun `logging out ends the session and resets what the screen holds but keeps the tab`() = runTest(dispatcher) {
        signedIn()
        val vm = viewModel()
        vm.selectTab(BibliothekTab.Holds)
        vm.openNotifications()
        advanceUntilIdle()
        assertTrue(vm.state.value.notificationsVisible)
        assertEquals(BibliothekTab.Holds, vm.state.value.tab)

        vm.logout()
        advanceUntilIdle()
        verify { repository.logout() }
        assertFalse(vm.state.value.notificationsVisible)
        assertEquals(BibliothekTab.Holds, vm.state.value.tab)
    }

    @Test
    fun `the tab switches between loans and holds`() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(BibliothekTab.Loans, vm.state.value.tab)
        vm.selectTab(BibliothekTab.Holds)
        advanceUntilIdle()
        assertEquals(BibliothekTab.Holds, vm.state.value.tab)
    }
}
