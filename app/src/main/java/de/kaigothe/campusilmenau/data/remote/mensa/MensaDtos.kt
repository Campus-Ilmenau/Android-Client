package de.kaigothe.campusilmenau.data.remote.mensa

import de.kaigothe.campusilmenau.core.time.ApiInstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Antwortmodelle der Mensa-API, aufgebaut nach echten Antworten der Dev-API (Stand 24.11.2026, siehe
 * `docs/migration/api/samples/`). Sie sind bewusst tolerant: Bis auf Titel und Mensa-Name hat jedes Feld einen
 * Standardwert, damit eine kleine Änderung der API den Speiseplan nicht unlesbar macht.
 * Die Schlüssel stehen hier als camelCase, die Antworten werden vorher umgewandelt (`CamelCaseKeys`).
 */
@Serializable
data class MensaListDto<T>(
    val data: List<T> = emptyList(),
    val hasMore: Boolean = false,
    val totalCount: Int = 0,
)

@Serializable
data class MealDto(
    val id: String,
    val canteen: CanteenInfoDto,
    @Serializable(with = ApiInstantSerializer::class) val date: Instant,
    val mealGroup: String? = null,
    val title: String,
    val additiveCodes: List<CodeDto> = emptyList(),
    val allergenCodes: List<CodeDto> = emptyList(),
    val symbolCodes: List<CodeDto> = emptyList(),
    val flags: FlagsDto = FlagsDto(),
    val co2GramPerPortion: Double? = null,
    val price: PriceDto? = null,
    val components: List<ComponentDto> = emptyList(),
)

/** Die Mensa, zu der ein Gericht gehört (nur der Name wird gebraucht). */
@Serializable
data class CanteenInfoDto(val name: String)

@Serializable
data class CodeDto(
    val code: String = "",
    val name: String? = null,
)

@Serializable
data class FlagsDto(
    val vegetarian: Boolean = false,
    val vegan: Boolean = false,
)

@Serializable
data class PriceDto(
    val student: Double? = null,
    val staff: Double? = null,
    val guest: Double? = null,
    val currency: String = "€",
)

@Serializable
data class ComponentDto(
    val name: String,
    val codes: List<CodeDto> = emptyList(),
)

@Serializable
data class CanteenDto(
    val rscId: Int? = null,
    val name: String,
    val hasMenu: Boolean = false,
    val city: String? = null,
    val address: String? = null,
    val openingHours: List<OpeningHourDto> = emptyList(),
)

@Serializable
data class OpeningHourDto(
    val weekday: String,
    val opensAt: String,
    val closesAt: String,
)
