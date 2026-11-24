package de.kaigothe.campusilmenau.domain.model.mensa

import java.text.NumberFormat
import java.time.LocalDate
import java.util.Currency
import java.util.Locale

/** Preisgruppe, deren Preis in der Liste und im Detail groß steht. Vorgabe ist „Studierende“. Der Schlüssel wird gespeichert. */
enum class MensaPriceGroup(val key: String) {
    Studierende("studierende"),
    Bedienstete("bedienstete"),
    Gaeste("gaeste"),
    ;

    companion object {
        fun fromKey(key: String?): MensaPriceGroup = entries.firstOrNull { it.key == key } ?: Studierende
    }
}

/** Eigenschaften, die die App aus den Flags und Symbolen der API ableitet. */
enum class MealBadge { Vegan, Vegetarian, Bio }

/** Preise eines Gerichts nach Personengruppe. */
data class MensaMealPrice(
    val student: Double?,
    val staff: Double?,
    val guest: Double?,
    val currency: String = "€",
) {
    /** „2,90 €“ in der Schreibweise der Sprache, ein fehlender Preis ist „–“. */
    fun formatted(group: MensaPriceGroup, locale: Locale = Locale.getDefault()): String = when (group) {
        MensaPriceGroup.Studierende -> format(student, currency, locale)
        MensaPriceGroup.Bedienstete -> format(staff, currency, locale)
        MensaPriceGroup.Gaeste -> format(guest, currency, locale)
    }

    companion object {
        fun format(value: Double?, currency: String, locale: Locale = Locale.getDefault()): String {
            if (value == null) return "–"
            // Die API schickt das Zeichen („€“). Ein dreistelliger Code gilt als Währungscode, alles andere als Euro.
            val code = if (currency.length == 3) currency.uppercase() else "EUR"
            val format = NumberFormat.getCurrencyInstance(locale)
            runCatching { Currency.getInstance(code) }.getOrNull()?.let { format.currency = it }
            return format.format(value)
        }
    }
}

/** Eine Komponente eines Gerichts („Reis“, „Soße“) mit ihren eigenen Zusatz-, Allergen- und Symbol-Namen. */
data class MensaMealComponent(val name: String, val codes: List<String>)

data class MensaMeal(
    /** Gilt nur für einen Tag und lässt sich nicht einzeln abfragen. */
    val id: String,
    /** Gruppe der API („Mittagessen“, „Abendessen“), leer, wenn sie fehlt. */
    val category: String,
    val name: String,
    val description: String?,
    val price: MensaMealPrice,
    val allergies: List<String>,
    val badges: Set<MealBadge>,
    val additives: List<String>,
    /** Namen der Symbole („Vegan“, „Knoblauch“, „Regional“ …). */
    val categories: List<String>,
    val co2GramPerPortion: Double?,
    val components: List<MensaMealComponent>,
    val canteenName: String,
    /** Der Tag, an dem es das Gericht gibt (der Tag der API-Zeit in UTC). */
    val date: LocalDate,
)

data class OpeningHour(
    /** `mon` … `sun`. */
    val weekday: String,
    /** `HH:mm`. */
    val opensAt: String,
    val closesAt: String,
)

data class Canteen(
    val rscId: Int?,
    val name: String,
    val hasMenu: Boolean,
    val city: String?,
    val address: String?,
    val openingHours: List<OpeningHour>,
)
