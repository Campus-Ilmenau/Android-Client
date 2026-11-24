package de.kaigothe.campusilmenau.core.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CamelCaseKeysTest {

    private val json = Json

    @Test
    fun `snake_case wird zu camelCase`() {
        assertEquals("hasMore", CamelCaseKeys.toCamelCase("has_more"))
        assertEquals("totalCount", CamelCaseKeys.toCamelCase("total_count"))
        assertEquals("requestId", CamelCaseKeys.toCamelCase("request_id"))
        assertEquals("co2GramPerPortion", CamelCaseKeys.toCamelCase("co2_gram_per_portion"))
    }

    @Test
    fun `camelCase und einfache Schluessel bleiben unveraendert`() {
        assertEquals("mealGroup", CamelCaseKeys.toCamelCase("mealGroup"))
        assertEquals("data", CamelCaseKeys.toCamelCase("data"))
        assertEquals("rscId", CamelCaseKeys.toCamelCase("rscId"))
    }

    @Test
    fun `fuehrende und folgende Unterstriche bleiben stehen`() {
        assertEquals("_privateKey", CamelCaseKeys.toCamelCase("_private_key"))
        assertEquals("value_", CamelCaseKeys.toCamelCase("value_"))
        assertEquals("__", CamelCaseKeys.toCamelCase("__"))
    }

    @Test
    fun `verschachtelte Objekte und Listen werden umgewandelt, Werte nicht`() {
        val input = """{"has_more":false,"data":[{"meal_group":"Mittagessen","price":{"max_value":1.5}}],"note":"a_b"}"""
        val expected = """{"hasMore":false,"data":[{"mealGroup":"Mittagessen","price":{"maxValue":1.5}}],"note":"a_b"}"""
        assertEquals(expected, CamelCaseKeys.normalize(input, json))
    }

    @Test
    fun `Huelle in snake_case und Daten in camelCase werden gemeinsam gelesen`() {
        val input = """{"has_more":true,"total_count":3,"data":[{"mealGroup":"x"}]}"""
        val normalized = CamelCaseKeys.normalize(input, json)
        assertEquals("""{"hasMore":true,"totalCount":3,"data":[{"mealGroup":"x"}]}""", normalized)
    }

    @Test
    fun `kein JSON kommt unveraendert zurueck`() {
        assertEquals("<html>Fehler</html>", CamelCaseKeys.normalize("<html>Fehler</html>", json))
        assertEquals("", CamelCaseKeys.normalize("", json))
    }
}
