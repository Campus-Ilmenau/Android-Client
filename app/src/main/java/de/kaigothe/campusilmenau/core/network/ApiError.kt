package de.kaigothe.campusilmenau.core.network

import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * Fehler einer Schnittstelle. Die Typen tragen Daten, keine Anzeigetexte: Die Oberfläche wählt den Text
 * aus den String-Ressourcen. Ein abgebrochener Aufruf (Coroutine-Abbruch) ist **kein** Fehler und wird
 * weitergereicht.
 */
sealed class ApiError(message: String?, cause: Throwable? = null) : Exception(message, cause) {

    /** Kein Netz, Zeitüberschreitung oder Verbindungsabbruch. Ein erneuter Versuch ist sinnvoll. */
    class Network(cause: Throwable) : ApiError(cause.message, cause)

    /**
     * Der Server antwortete mit einem Fehlerstatus, meist mit Fehlerhülle `{ "error": { … } }`.
     * @property code Fehlercode der API (zum Beispiel `invalid_parameter_format`, `cover_pending`).
     * @property retryAfter Wartezeit laut `Retry-After`, falls vorhanden.
     */
    class Http(
        val status: Int,
        val code: String? = null,
        val serverMessage: String? = null,
        val param: String? = null,
        val requestId: String? = null,
        val retryAfter: Duration? = null,
    ) : ApiError(serverMessage ?: "HTTP $status") {
        val isNotFound: Boolean get() = status == 404

        /** 503: Der Dienst ist gerade nicht erreichbar. Ein erneuter Versuch ist sinnvoll. */
        val isUnavailable: Boolean get() = status == 503
    }

    /** Die Antwort ließ sich nicht lesen (unerwartetes Format). */
    class Decoding(cause: Throwable) : ApiError(cause.message, cause)

    /** Ein erneuter Versuch ist sinnvoll (kein Netz oder Dienst nicht erreichbar). */
    val isRetryable: Boolean
        get() = this is Network || (this is Http && isUnavailable)
}

/** Fehlerhülle der Campus-Ilmenau-API: `{ "error": { "type", "code", "message", "param", "request_id" } }`. */
@Serializable
internal data class ApiErrorEnvelope(val error: ApiErrorBody)

@Serializable
internal data class ApiErrorBody(
    val type: String? = null,
    val code: String? = null,
    val message: String? = null,
    val param: String? = null,
    val requestId: String? = null,
)
