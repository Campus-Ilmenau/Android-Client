package de.kaigothe.campusilmenau.domain.model.library.catalog

import de.kaigothe.campusilmenau.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySearchQueryTest {
    private fun fields(q: LibrarySearchQuery) = q.validate().map { it.field }

    @Test fun emptyQueryNeedsASearchTerm() {
        assertEquals(listOf(LibraryQueryField.Text), fields(LibrarySearchQuery()))
        assertEquals(listOf(LibraryQueryField.Text), fields(LibrarySearchQuery(text = "   ")))
        assertTrue(LibrarySearchQuery(text = "java").validate().isEmpty())
        assertTrue(LibrarySearchQuery(author = "Kofler").validate().isEmpty())
    }

    @Test fun localAndContentOnlyCountTheText() {
        assertFalse(LibrarySearchQuery(title = "x", scope = LibrarySearchScope.Local).hasSearchTerm)
        assertFalse(LibrarySearchQuery(author = "x", scope = LibrarySearchScope.Content).hasSearchTerm)
        assertTrue(LibrarySearchQuery(title = "x", scope = LibrarySearchScope.Catalog).hasSearchTerm)
    }

    @Test fun textLengthIsCappedAtTwoHundred() {
        assertTrue(LibrarySearchQuery(text = "a".repeat(200)).validate().isEmpty())
        val issues = LibrarySearchQuery(text = "a".repeat(201)).validate()
        assertEquals(R.string.bib_suche_fehler_laenge, issues.single().message.id)
    }

    @Test fun yearsMustHaveFourDigitsAndNoLeadingZero() {
        assertEquals(listOf(LibraryQueryField.YearFrom), fields(LibrarySearchQuery(text = "a", yearFrom = "99")))
        assertEquals(listOf(LibraryQueryField.YearTo), fields(LibrarySearchQuery(text = "a", yearTo = "0999")))
        assertEquals(listOf(LibraryQueryField.YearFrom), fields(LibrarySearchQuery(text = "a", yearFrom = "٢٠٢٠")))
        assertTrue(LibrarySearchQuery(text = "a", yearFrom = "2000").validate().isEmpty())
    }

    @Test fun yearRangeOrderAndSpan() {
        assertEquals(listOf(LibraryQueryField.YearFrom), fields(LibrarySearchQuery(text = "a", yearFrom = "2020", yearTo = "2010")))
        assertTrue(LibrarySearchQuery(text = "a", yearFrom = "1900", yearTo = "2050").validate().isEmpty())
        assertEquals(listOf(LibraryQueryField.YearTo), fields(LibrarySearchQuery(text = "a", yearFrom = "1900", yearTo = "2051")))
    }

    @Test fun pageMustBePositive() {
        assertEquals(listOf(LibraryQueryField.Text), fields(LibrarySearchQuery(text = "a", page = 0)))
    }

    @Test fun paramsSkipEmptyFieldsAndAlwaysSendPaging() {
        val params = LibrarySearchQuery(text = " java ", title = "", author = "Kofler", mediaType = LibraryMediaFilter.Ebook, yearFrom = "2000").params()
        assertEquals(
            mapOf("q" to "java", "author" to "Kofler", "type" to "ebook", "year_from" to "2000", "page" to "1", "per_page" to "20"),
            params,
        )
    }

    @Test fun localScopeDropsFieldParameters() {
        val params = LibrarySearchQuery(text = "java", title = "t", author = "a", scope = LibrarySearchScope.Local).params()
        assertEquals(mapOf("q" to "java", "in" to "local", "page" to "1", "per_page" to "20"), params)
    }

    @Test fun fieldSearchDetection() {
        assertFalse(LibrarySearchQuery(text = "java").isFieldSearch)
        assertTrue(LibrarySearchQuery(title = "java").isFieldSearch)
        assertTrue(LibrarySearchQuery(text = "java", yearFrom = "2000").isFieldSearch)
        assertFalse(LibrarySearchQuery(text = "java", title = "x", scope = LibrarySearchScope.Local).isFieldSearch)
    }

    @Test fun continuationGoesToCatalogPageOne() {
        val next = LibrarySearchQuery(text = "java", page = 3).continuedInCatalog()
        assertEquals(LibrarySearchScope.Catalog, next.scope)
        assertEquals(1, next.page)
        assertEquals(2, next.nextPage().page)
    }
}
