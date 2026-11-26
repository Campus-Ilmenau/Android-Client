package de.kaigothe.campusilmenau.domain.model.library.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryPersonNameTest {
    @Test
    fun `swaps surname and first name`() {
        assertEquals("Anna Zacher", LibraryPersonName.display("Zacher, Anna"))
    }

    @Test
    fun `leaves names without a comma or with an empty part untouched`() {
        assertEquals("Wikipedia", LibraryPersonName.display(" Wikipedia "))
        assertEquals("Zacher,", LibraryPersonName.display("Zacher,"))
    }

    @Test
    fun `lists at most three names then adds the marker`() {
        val authors = listOf("A, a", "B, b", "C, c", "D, d")
        assertEquals("a A, b B, c C u. a.", LibraryPersonName.list(authors, "u. a."))
        assertEquals("a A, b B", LibraryPersonName.list(authors.take(2), "u. a."))
    }

    @Test
    fun `meta line skips empty parts`() {
        val media = LibraryMedia(id = "med_1", title = "T", authors = listOf("Zacher, Anna"), year = "2020")
        assertEquals("Anna Zacher · 2020 · Buch", media.metaLine("Buch", "u. a."))
        assertEquals("Buch", LibraryMedia(id = "med_2", title = "T").metaLine("Buch", "u. a."))
    }

    @Test
    fun `subtitle is trimmed`() {
        assertEquals("Eins", LibraryMedia(id = "m", title = "T", subtitle = "  Eins ").trimmedSubtitle)
        assertNull(LibraryMedia(id = "m", title = "T", subtitle = "  ").trimmedSubtitle)
    }
}
