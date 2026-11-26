package de.kaigothe.campusilmenau.ui.bibliothek

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.data.repository.PaiaRepository
import de.kaigothe.campusilmenau.data.repository.PaiaState
import de.kaigothe.campusilmenau.domain.model.library.PaiaError
import de.kaigothe.campusilmenau.domain.model.library.PaiaLoanItem
import de.kaigothe.campusilmenau.ui.components.LoadingGate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

enum class BibliothekTab { Loans, Holds }

/**
 * Der Zustand des Anmelde-Formulars. Er lebt im ViewModel und übersteht Drehen und Szenenwechsel (Autofill, Passwortmanager),
 * anders als das Formular der iOS-App, das dabei seine Eingaben verlor.
 */
data class LoginForm(
    val username: String = "",
    val password: String = "",
    /** „Angemeldet bleiben“: speichert die Zugangsdaten verschlüsselt. Voreinstellung aus. */
    val remember: Boolean = false,
    val isLoggingIn: Boolean = false,
    val error: PaiaError? = null,
) {
    val canLogin: Boolean get() = !isLoggingIn && username.isNotBlank() && password.isNotEmpty()
}

data class BibliothekUiState(
    val paia: PaiaState,
    val tab: BibliothekTab = BibliothekTab.Loans,
    val today: LocalDate,
    val loginVisible: Boolean = false,
    val notificationsVisible: Boolean = false,
    val login: LoginForm = LoginForm(),
) {
    /** Die Bibliothek hat noch keine Ausleihen und lädt zum ersten Mal: Der Bereich gehört der Ziege. */
    val isFirstLoad: Boolean get() = paia.isLoading && paia.items.isEmpty()
}

@HiltViewModel
class BibliothekViewModel @Inject constructor(
    private val repository: PaiaRepository,
    private val clock: Clock,
) : ViewModel() {

    private data class Local(
        val tab: BibliothekTab = BibliothekTab.Loans,
        val loginVisible: Boolean = false,
        val notificationsVisible: Boolean = false,
        val login: LoginForm = LoginForm(),
    )

    private val local = MutableStateFlow(Local())

    val state: StateFlow<BibliothekUiState> = combine(repository.state, local) { paia, local ->
        BibliothekUiState(
            paia = paia,
            tab = local.tab,
            today = BerlinTime.date(clock.instant()),
            loginVisible = local.loginVisible,
            notificationsVisible = local.notificationsVisible,
            login = local.login,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, BibliothekUiState(repository.state.value, today = BerlinTime.date(clock.instant())))

    /** Zeitverhalten der Ziege beim ersten Laden ohne Ausleihen. */
    val loadingGate = LoadingGate(viewModelScope)

    private var started = false

    /**
     * Beim ersten Erscheinen: mit Sitzung laden, mit gespeicherten Zugangsdaten anmelden und laden, sonst das Anmelde-Sheet
     * zeigen (wie iOS). Weitere Aufrufe tun nichts.
     */
    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            if (repository.state.value.isAuthenticated || repository.autoLogin()) {
                load()
            } else {
                openLogin()
            }
        }
    }

    fun selectTab(tab: BibliothekTab) = local.update { it.copy(tab = tab) }

    /** Ziehen zum Aktualisieren und nach dem Anmelden: Ausleihen und Mitteilungen. */
    suspend fun refresh() = load()

    fun renew(item: PaiaLoanItem) {
        viewModelScope.launch {
            try {
                repository.renew(item)
            } catch (e: CancellationException) {
                throw e
            } catch (e: PaiaError.SessionExpired) {
                onSessionExpired()
            } catch (_: PaiaError) {
                // Der Fehler steht im Zustand und wird als Meldung gezeigt.
            }
        }
    }

    fun dismissError() = repository.dismissError()

    fun logout() {
        repository.logout()
        local.update { Local(tab = it.tab) }
    }

    fun openNotifications() {
        local.update { it.copy(notificationsVisible = true) }
        viewModelScope.launch { loadNotifications() }
    }

    fun closeNotifications() = local.update { it.copy(notificationsVisible = false) }

    suspend fun refreshNotifications() = loadNotifications()

    // MARK: Anmelden

    /** Zeigt das Anmelde-Sheet. Nach einem Ablauf steht die Benutzernummer noch da, nach einem manuellen Abmelden ist das Feld leer. */
    fun openLogin() {
        val paia = repository.state.value
        local.update {
            it.copy(
                loginVisible = true,
                login = LoginForm(
                    username = if (paia.sessionExpired) paia.lastUsername.orEmpty() else it.login.username,
                    password = "",
                    remember = paia.remembersCredentials || it.login.remember,
                ),
            )
        }
    }

    fun cancelLogin() = local.update { it.copy(loginVisible = false, login = it.login.copy(password = "", error = null)) }

    // Der Fehlerkasten verschwindet, sobald man weitertippt.
    fun setUsername(value: String) = local.update { it.copy(login = it.login.copy(username = value, error = null)) }

    fun setPassword(value: String) = local.update { it.copy(login = it.login.copy(password = value, error = null)) }

    fun setRemember(value: Boolean) = local.update { it.copy(login = it.login.copy(remember = value)) }

    fun login() {
        val form = local.value.login
        if (!form.canLogin) return
        local.update { it.copy(login = it.login.copy(isLoggingIn = true, error = null)) }
        viewModelScope.launch {
            try {
                repository.login(form.username.trim(), form.password, form.remember)
                // Das Passwort bleibt nicht im Speicher des Formulars.
                local.update { it.copy(loginVisible = false, login = LoginForm(username = form.username.trim(), remember = form.remember)) }
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: PaiaError) {
                local.update { it.copy(login = it.login.copy(isLoggingIn = false, error = e)) }
            }
        }
    }

    // MARK: Laden

    private suspend fun load() {
        loadingGate.setLoading(true)
        try {
            repository.fetchItems()
            // Mitteilungen sind Beiwerk: Ihr Fehler stört die Ausleihen nicht.
            loadNotifications()
        } catch (e: CancellationException) {
            throw e
        } catch (_: PaiaError.SessionExpired) {
            onSessionExpired()
        } catch (_: PaiaError) {
            // Der Fehler steht im Zustand.
        } finally {
            loadingGate.setLoading(false)
        }
    }

    private suspend fun loadNotifications() {
        try {
            repository.fetchNotifications()
        } catch (e: CancellationException) {
            throw e
        } catch (_: PaiaError.SessionExpired) {
            onSessionExpired()
        } catch (_: PaiaError) {
            // still
        }
    }

    /** Mit gespeicherten Zugangsdaten still wieder anmelden, sonst das Anmelde-Sheet zeigen. */
    private suspend fun onSessionExpired() {
        if (repository.autoLogin()) {
            try {
                repository.fetchItems()
            } catch (e: CancellationException) {
                throw e
            } catch (_: PaiaError) {
                // Der Fehler steht im Zustand.
            }
        } else {
            openLogin()
        }
    }
}
