package de.kaigothe.campusilmenau.data.repository

import de.kaigothe.campusilmenau.core.network.CamelCaseKeys
import de.kaigothe.campusilmenau.core.time.ApiDates
import de.kaigothe.campusilmenau.data.remote.paia.AuthErrorDto
import de.kaigothe.campusilmenau.data.remote.paia.PaiaApi
import de.kaigothe.campusilmenau.data.remote.paia.RenewRequestDocDto
import de.kaigothe.campusilmenau.data.remote.paia.RenewRequestDto
import de.kaigothe.campusilmenau.data.security.CredentialStore
import de.kaigothe.campusilmenau.data.security.StoredCredentials
import de.kaigothe.campusilmenau.domain.model.library.PaiaError
import de.kaigothe.campusilmenau.domain.model.library.PaiaItemStatus
import de.kaigothe.campusilmenau.domain.model.library.PaiaLoanItem
import de.kaigothe.campusilmenau.domain.model.library.PaiaNotification
import de.kaigothe.campusilmenau.domain.model.library.PaiaPatron
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Was die Oberfläche von der Bibliothekssitzung sieht. */
data class PaiaState(
    val isAuthenticated: Boolean = false,
    val items: List<PaiaLoanItem> = emptyList(),
    val notifications: List<PaiaNotification> = emptyList(),
    /** Konto für den Ausweis. Nur im Speicher, wird mit dem Token verworfen. */
    val patron: PaiaPatron? = null,
    val isLoading: Boolean = false,
    /** Der letzte Fehler einer Anfrage. [PaiaError.SessionExpired] steht hier nie, dafür gibt es [sessionExpired]. */
    val error: PaiaError? = null,
    /** Die Sitzung lief ab (lokal oder per 401/403) und wurde nicht manuell beendet. Steuert Titel und Hinweis im Login-Sheet. */
    val sessionExpired: Boolean = false,
    /** Zuletzt erfolgreich verwendete Benutzernummer. Nach einem manuellen Abmelden ist sie weg. */
    val lastUsername: String? = null,
    /** Es gibt gespeicherte Zugangsdaten („Angemeldet bleiben“). */
    val remembersCredentials: Boolean = false,
)

/**
 * Die Sitzung beim PAIA-Server der UB Ilmenau und die Daten des Nutzerkontos. Ein Singleton, damit die Sitzung
 * Navigationswechsel überlebt.
 *
 * Das Token, die Patron-ID und die Ablaufzeit liegen nur im Speicher. Zugangsdaten werden nur gespeichert, wenn der Nutzer
 * „Angemeldet bleiben“ wählt ([CredentialStore], verschlüsselt); dann meldet [autoLogin] beim Start und nach einem Ablauf
 * wieder an. Ohne den Schalter gilt wie bei iOS: nach einem Neustart meldet man sich neu an.
 */
@Singleton
class PaiaRepository @Inject constructor(
    private val api: PaiaApi,
    private val json: Json,
    private val credentials: CredentialStore,
    private val clock: Clock,
) {
    private val _state = MutableStateFlow(PaiaState(remembersCredentials = credentials.load() != null))
    val state: StateFlow<PaiaState> = _state.asStateFlow()

    @Volatile
    private var accessToken: String? = null

    @Volatile
    private var patronId: String? = null

    @Volatile
    private var tokenExpiry: Instant? = null

    /**
     * Meldet per Resource-Owner-Password-Grant an. Mit [remember] werden die Zugangsdaten verschlüsselt gespeichert, ohne
     * sie wird ein früheres Speichern widerrufen.
     */
    suspend fun login(username: String, password: String, remember: Boolean) {
        _state.update { it.copy(isLoading = true, error = null) }
        try {
            val response = try {
                api.login("password", username, password, PaiaApi.SCOPE)
            } catch (e: HttpException) {
                throw loginError(e)
            } catch (e: SerializationException) {
                throw PaiaError.InvalidResponse(e)
            } catch (e: IOException) {
                throw PaiaError.Network(e)
            }
            accessToken = response.accessToken
            patronId = response.patron
            tokenExpiry = response.expiresIn?.let { clock.instant().plusSeconds(it) }
            if (remember) credentials.save(StoredCredentials(username, password)) else credentials.clear()
            _state.update {
                it.copy(isAuthenticated = true, sessionExpired = false, lastUsername = username, remembersCredentials = remember)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: PaiaError) {
            _state.update { it.copy(error = e) }
            throw e
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Meldet mit den gespeicherten Zugangsdaten an, falls es welche gibt und man nicht angemeldet ist. Gibt `true` zurück,
     * wenn danach eine Sitzung besteht. Lehnt der Server die Daten ab, werden sie gelöscht (das Passwort stimmt nicht mehr).
     */
    suspend fun autoLogin(): Boolean {
        if (_state.value.isAuthenticated) return true
        val stored = credentials.load() ?: return false
        return try {
            login(stored.username, stored.password, remember = true)
            true
        } catch (e: PaiaError.AuthenticationFailed) {
            credentials.clear()
            _state.update { it.copy(remembersCredentials = false) }
            false
        } catch (_: PaiaError) {
            // Kein Netz o. Ä.: Die Daten bleiben für den nächsten Versuch.
            false
        }
    }

    /** Lädt die ausgeliehenen, vorgemerkten und bestellten Medien. */
    suspend fun fetchItems() {
        _state.update { it.copy(isLoading = true, error = null) }
        try {
            val response = authorized { patron, auth -> api.items(patron, auth) }
            _state.update { it.copy(items = response.doc.map { dto -> dto.toDomain() }) }
        } catch (e: PaiaError) {
            recordError(e)
            throw e
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Lädt die Benachrichtigungen. Der Endpunkt ist laut Spezifikation optional: Ein Server ohne ihn antwortet mit 404, das
     * ist kein Fehler, den man anzeigen will (er würde die Fehlermeldung der Ausleihen überschreiben).
     */
    suspend fun fetchNotifications() {
        _state.update { it.copy(isLoading = true) }
        try {
            val response = authorized { patron, auth -> api.notifications(patron, auth) }
            _state.update { it.copy(notifications = response.notification.mapNotNull { dto -> dto.toDomain() }) }
        } catch (e: PaiaError.Http) {
            if (e.status == 404) _state.update { it.copy(notifications = emptyList()) } else recordAndThrow(e)
        } catch (e: PaiaError) {
            recordAndThrow(e)
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Lädt das Nutzerkonto für den Ausweis. Fehler setzen bewusst keinen `error`, weil der Ausweis auch ohne diese Angaben
     * funktioniert. Wurde währenddessen abgemeldet, dürfen die Daten nicht zurückkehren.
     */
    suspend fun fetchPatron(): PaiaPatron {
        val loaded = authorized { patron, auth -> api.patron(patron, auth) }.toDomain()
        if (!_state.value.isAuthenticated) throw PaiaError.NotAuthenticated()
        _state.update { it.copy(patron = loaded) }
        return loaded
    }

    /** Verlängert die Leihfrist eines Mediums (`core/{patron}/renew`). */
    suspend fun renew(item: PaiaLoanItem) {
        val uri = item.item ?: throw PaiaError.MissingItemIdentifier()
        try {
            val response = authorized { patron, auth -> api.renew(patron, auth, RenewRequestDto(listOf(RenewRequestDocDto(uri)))) }
            val result = response.doc.firstOrNull { it.item == uri } ?: response.doc.firstOrNull() ?: throw PaiaError.InvalidResponse()
            result.error?.let { throw PaiaError.RenewalFailed(it) }
            _state.update { state ->
                state.copy(
                    items = state.items.map {
                        if (it.item == uri) it.renewed(result.endtime?.let(ApiDates::parse), result.status?.let(PaiaItemStatus::fromCode)) else it
                    },
                )
            }
        } catch (e: PaiaError) {
            recordError(e)
            throw e
        }
    }

    /**
     * Meldet manuell ab: Token, Daten, gespeicherte Zugangsdaten und die vorbelegte Benutzernummer sind weg. Der Aufrufer
     * löscht damit auch das, was der Nutzer mit „Angemeldet bleiben“ ablegen ließ.
     */
    fun logout() {
        clearSession()
        credentials.clear()
        _state.value = PaiaState()
    }

    /** Der Nutzer hat die Fehlermeldung geschlossen. */
    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    // MARK: Intern

    /** Beendet die Sitzung wie [logout], behält aber Benutzernummer und gespeicherte Zugangsdaten und merkt sich den Ablauf. */
    private fun expireSession() {
        clearSession()
        _state.update { PaiaState(sessionExpired = true, lastUsername = it.lastUsername, remembersCredentials = it.remembersCredentials) }
    }

    private fun clearSession() {
        accessToken = null
        patronId = null
        tokenExpiry = null
    }

    /** Führt einen Aufruf mit Token aus. 401/403 und ein lokal abgelaufenes Token beenden die Sitzung. */
    private suspend fun <T> authorized(block: suspend (patron: String, authorization: String) -> T): T {
        tokenExpiry?.let {
            if (!clock.instant().isBefore(it)) {
                expireSession()
                throw PaiaError.SessionExpired()
            }
        }
        val token = accessToken
        val patron = patronId
        if (!_state.value.isAuthenticated || token == null || patron == null) throw PaiaError.NotAuthenticated()
        return try {
            block(patron, "Bearer $token")
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                expireSession()
                throw PaiaError.SessionExpired()
            }
            throw PaiaError.Http(e.code())
        } catch (e: SerializationException) {
            throw PaiaError.InvalidResponse(e)
        } catch (e: IOException) {
            throw PaiaError.Network(e)
        }
    }

    /** Ein Ablauf ist keine normale Fehlermeldung: Das Login-Sheet übernimmt. */
    private fun recordError(error: PaiaError) {
        if (error !is PaiaError.SessionExpired) _state.update { it.copy(error = error) }
    }

    private fun recordAndThrow(error: PaiaError): Nothing {
        recordError(error)
        throw error
    }

    private fun loginError(e: HttpException): PaiaError {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val detail = body?.takeIf { it.isNotBlank() }?.let {
            runCatching { json.decodeFromString<AuthErrorDto>(CamelCaseKeys.normalize(it, json)) }.getOrNull()
        }
        return if (detail != null) {
            PaiaError.AuthenticationFailed(detail.errorDescription ?: detail.error)
        } else {
            PaiaError.Http(e.code())
        }
    }
}
