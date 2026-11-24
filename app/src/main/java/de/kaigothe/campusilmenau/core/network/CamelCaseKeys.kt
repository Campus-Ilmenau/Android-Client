package de.kaigothe.campusilmenau.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Die Campus-Ilmenau-API mischt die Schreibweisen: Die Listen-Hülle kommt in `snake_case` (`has_more`, `total_count`),
 * die Daten selbst in `camelCase` (`mealGroup`, `openingHours`). Die iOS-App liest beides mit
 * `keyDecodingStrategy = .convertFromSnakeCase`, das camelCase unverändert lässt. Hier passiert dasselbe:
 * Alle Schlüssel werden vor dem Dekodieren in camelCase umgewandelt, die Modelle heißen durchgehend camelCase.
 */
object CamelCaseKeys {

    /** `has_more` → `hasMore`. Schlüssel ohne Unterstrich bleiben unverändert, führende und folgende Unterstriche auch. */
    fun toCamelCase(key: String): String {
        if (!key.contains('_')) return key
        val leading = key.takeWhile { it == '_' }
        val trailing = key.takeLastWhile { it == '_' }
        if (leading.length == key.length) return key
        val core = key.substring(leading.length, key.length - trailing.length)
        val parts = core.split('_').filter { it.isNotEmpty() }
        if (parts.isEmpty()) return key
        val joined = parts.first() + parts.drop(1).joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
        return leading + joined + trailing
    }

    fun normalize(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.entries.associate { (key, value) -> toCamelCase(key) to normalize(value) })
        is JsonArray -> JsonArray(element.map(::normalize))
        else -> element
    }

    /**
     * Der Text mit camelCase-Schlüsseln. Nur Objekte und Listen werden umgewandelt. Alles andere (leer, HTML einer
     * Fehlerseite, ein einzelner Wert) kommt unverändert zurück: kotlinx liest unquotierten Text als Wert und würde
     * ihn sonst in Anführungszeichen setzen.
     */
    fun normalize(text: String, json: Json): String {
        if (text.isBlank()) return text
        return try {
            val element = json.parseToJsonElement(text)
            if (element is JsonObject || element is JsonArray) {
                json.encodeToString(JsonElement.serializer(), normalize(element))
            } else {
                text
            }
        } catch (_: Exception) {
            text
        }
    }
}

/** Wandelt JSON-Antworten der API in camelCase-Schlüssel um (siehe [CamelCaseKeys]). Andere Antworten bleiben, wie sie sind. */
class CamelCaseKeysInterceptor(private val json: Json) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body
        val type = body.contentType()
        if (type == null || !type.subtype.contains("json", ignoreCase = true)) return response
        val normalized = CamelCaseKeys.normalize(body.string(), json)
        return response.newBuilder().body(normalized.toResponseBody(type)).build()
    }
}
