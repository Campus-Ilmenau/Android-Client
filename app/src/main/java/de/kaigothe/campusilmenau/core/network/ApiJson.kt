package de.kaigothe.campusilmenau.core.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/**
 * JSON-Einstellungen für die Campus-Ilmenau-API: Schlüssel in `snake_case`, unbekannte Felder werden
 * ignoriert, fehlende oder `null`-Werte bei nicht-nullbaren Feldern mit Standardwert ersetzt. Die API darf
 * wachsen, ohne die App zu brechen.
 */
object ApiJson {
    @OptIn(ExperimentalSerializationApi::class)
    fun create(): Json = Json {
        namingStrategy = JsonNamingStrategy.SnakeCase
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }
}
