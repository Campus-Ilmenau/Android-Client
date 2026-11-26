package de.kaigothe.campusilmenau.data.remote.paia

import de.kaigothe.campusilmenau.domain.model.library.PaiaItemStatus
import de.kaigothe.campusilmenau.domain.model.library.PaiaLoanItem
import de.kaigothe.campusilmenau.domain.model.library.PaiaNotification
import de.kaigothe.campusilmenau.domain.model.library.PaiaPatron
import de.kaigothe.campusilmenau.core.time.ApiDates
import kotlinx.serialization.Serializable

/**
 * Antwortmodelle des PAIA-Servers. Die Schlüssel stehen als camelCase (`accessToken`), die Antworten laufen wie die der
 * Campus-Ilmenau-API durch die Schlüssel-Umwandlung. Datumsangaben bleiben Text: PAIA liefert vollständiges ISO 8601 oder
 * ein reines Datum (siehe [ApiDates]).
 */
@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val tokenType: String? = null,
    val patron: String? = null,
    val scope: String? = null,
    val expiresIn: Long? = null,
)

/** Fehler des Logins: `error_description` oder `error`. */
@Serializable
data class AuthErrorDto(
    val error: String,
    val errorDescription: String? = null,
)

@Serializable
data class ItemsResponseDto(val doc: List<LoanItemDto> = emptyList())

@Serializable
data class LoanItemDto(
    val item: String? = null,
    val edition: String? = null,
    val about: String? = null,
    val label: String? = null,
    val queue: Int? = null,
    val renewals: Int? = null,
    val reminder: Int? = null,
    val starttime: String? = null,
    val endtime: String? = null,
    val cancancel: Boolean? = null,
    val canrenew: Boolean? = null,
    val storage: String? = null,
    val storageid: String? = null,
    val comment: String? = null,
    val status: Int = 0,
) {
    fun toDomain() = PaiaLoanItem(
        item = item, edition = edition, about = about, label = label, queue = queue, renewals = renewals,
        reminder = reminder, start = starttime?.let(ApiDates::parse), end = endtime?.let(ApiDates::parse),
        canCancel = cancancel, canRenew = canrenew, storage = storage, storageId = storageid, comment = comment,
        status = PaiaItemStatus.fromCode(status),
    )
}

@Serializable
data class NotificationsResponseDto(val notification: List<NotificationDto> = emptyList())

@Serializable
data class NotificationDto(
    val id: String,
    val about: String,
    val date: String,
    val item: String? = null,
    val url: String? = null,
) {
    /** `null` bei unlesbarem Datum: Eine Benachrichtigung ohne Datum lässt sich nicht einordnen. */
    fun toDomain(): PaiaNotification? =
        ApiDates.parse(date)?.let { PaiaNotification(id = id, about = about, date = it, item = item, url = url) }
}

/** Nur die Felder für den Ausweis. `email` und `address` stehen hier bewusst nicht: Sie werden gar nicht erst gelesen. */
@Serializable
data class PatronDto(
    val name: String? = null,
    val expires: String? = null,
    val status: Int? = null,
    val type: List<String>? = null,
    val note: String? = null,
) {
    fun toDomain() = PaiaPatron(name = name, expires = expires?.let(ApiDates::parse), status = status, types = type, note = note)
}

@Serializable
data class RenewRequestDto(val doc: List<RenewRequestDocDto>)

@Serializable
data class RenewRequestDocDto(val item: String)

@Serializable
data class RenewResponseDto(val doc: List<RenewResultDto> = emptyList())

@Serializable
data class RenewResultDto(
    val item: String? = null,
    val endtime: String? = null,
    val status: Int? = null,
    val error: String? = null,
)
