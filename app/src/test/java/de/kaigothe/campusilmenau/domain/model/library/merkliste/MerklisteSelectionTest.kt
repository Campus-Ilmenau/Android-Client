package de.kaigothe.campusilmenau.domain.model.library.merkliste

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MerklisteSelectionTest {
    private val ids = listOf("a", "b", "c")

    @Test
    fun `down and up move one step`() {
        assertEquals("b", MerklisteSelection.move(ids, "a", 1))
        assertEquals("a", MerklisteSelection.move(ids, "b", -1))
    }

    @Test
    fun `selection stops at the edges`() {
        assertEquals("c", MerklisteSelection.move(ids, "c", 1))
        assertEquals("a", MerklisteSelection.move(ids, "a", -1))
    }

    @Test
    fun `without a selection down takes the first and up the last`() {
        assertEquals("a", MerklisteSelection.move(ids, null, 1))
        assertEquals("c", MerklisteSelection.move(ids, null, -1))
        // Ein Titel, der nicht mehr in der Liste steht (zum Beispiel entfernt), gilt als keine Auswahl.
        assertEquals("a", MerklisteSelection.move(ids, "x", 1))
    }

    @Test
    fun `an empty list does not consume the key`() {
        assertNull(MerklisteSelection.move(emptyList(), null, 1))
    }
}
