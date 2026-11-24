package de.kaigothe.campusilmenau.core.network

import kotlinx.serialization.json.Json

/**
 * JSON-Einstellungen für die Campus-Ilmenau-API. Die Modelle heißen durchgehend camelCase, die Schlüssel der
 * Antworten werden vorher umgewandelt (siehe [CamelCaseKeys]). Unbekannte Felder werden ignoriert, fehlende oder
 * `null`-Werte bei nicht-nullbaren Feldern mit dem Standardwert ersetzt: Die API darf wachsen, ohne die App zu brechen.
 */
object ApiJson {
    fun create(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }
}
