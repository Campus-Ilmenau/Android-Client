package de.kaigothe.campusilmenau.domain.model.library.catalog

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.data.repository.LibraryCoverLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class LibraryTitlePresentationTest {
    private val date: (Instant) -> String = { "12.03.2026" }
    private val media = LibraryMedia(id = "med_1", title = "T")

    private fun item(
        status: LibraryAvailabilityStatus,
        due: Instant? = null,
        queue: Int? = null,
        reserve: String? = null,
        place: String? = "Lehrbuchsammlung",
    ) = LibraryItem(
        label = "ZQ 5000",
        location = place?.let { LibraryLocation(null, null, it, "https://plan") },
        status = status,
        dueDate = due,
        reserveUrl = reserve,
        queue = queue,
    )

    private fun detail(status: LibraryAvailabilityStatus, vararg items: LibraryItem, media: LibraryMedia = this.media) =
        LibraryMediaDetail(media, items.toList(), status, null, null)

    @Test
    fun `available with several copies counts those on the shelf`() {
        val block = detail(
            LibraryAvailabilityStatus.Available,
            item(LibraryAvailabilityStatus.Available), item(LibraryAvailabilityStatus.OnLoan),
        ).statusBlock(date)
        assertEquals(LibraryStatusBlock.Tone.Green, block.tone)
        assertEquals(UiText(R.string.bib_titel_status_im_regal, 1, 2), block.subtitle)
    }

    @Test
    fun `available without copies points to the catalogue`() {
        assertEquals(UiText(R.string.bib_titel_status_keine_exemplare), detail(LibraryAvailabilityStatus.Available).statusBlock(date).subtitle)
    }

    @Test
    fun `on loan shows the earliest return and the queue`() {
        val due = Instant.parse("2026-03-12T10:00:00Z")
        val block = detail(
            LibraryAvailabilityStatus.OnLoan,
            item(LibraryAvailabilityStatus.OnLoan, due = due, queue = 2), item(LibraryAvailabilityStatus.OnLoan, queue = 1),
        ).statusBlock(date)
        assertEquals(UiText(R.string.bib_titel_alle_ausgeliehen), block.title)
        assertEquals(
            UiText(R.string.bib_kat_verbinder, UiText(R.string.bib_titel_fruehestens, "12.03.2026"), UiText.plural(R.plurals.bib_titel_vormerkungen, 3, 3)),
            block.subtitle,
        )
        assertEquals(LibraryStatusBlock.Tone.Neutral, block.tone)
    }

    @Test
    fun `reference only is blue and online is green`() {
        assertEquals(LibraryStatusBlock.Tone.Blue, detail(LibraryAvailabilityStatus.ReferenceOnly).statusBlock(date).tone)
        assertEquals(LibraryStatusBlock.Tone.Green, detail(LibraryAvailabilityStatus.Online).statusBlock(date).tone)
        assertEquals(UiText(R.string.bib_titel_status_unbekannt), detail(LibraryAvailabilityStatus.Unknown).statusBlock(date).title)
    }

    @Test
    fun `primary action needs a reserve link or an online link`() {
        val reserving = detail(LibraryAvailabilityStatus.OnLoan, item(LibraryAvailabilityStatus.OnLoan), item(LibraryAvailabilityStatus.OnLoan, reserve = "https://r"))
        assertEquals(LibraryPrimaryAction.Reserve("https://r"), reserving.primaryAction)
        assertNull(detail(LibraryAvailabilityStatus.OnLoan, item(LibraryAvailabilityStatus.OnLoan)).primaryAction)
        assertEquals(
            LibraryPrimaryAction.ReadOnline("https://o"),
            detail(LibraryAvailabilityStatus.Online, media = media.copy(onlineUrl = "https://o")).primaryAction,
        )
        assertNull(detail(LibraryAvailabilityStatus.Online).primaryAction)
        assertNull(detail(LibraryAvailabilityStatus.Available, item(LibraryAvailabilityStatus.Available, reserve = "https://r")).primaryAction)
    }

    @Test
    fun `item row builds the extra line by status`() {
        val due = Instant.parse("2026-03-12T10:00:00Z")
        val loan = item(LibraryAvailabilityStatus.OnLoan, due = due, queue = 1).row(date)
        assertEquals(UiText(R.string.bib_kat_verbinder, UiText(R.string.bib_titel_zurueck_bis, "12.03.2026"), UiText.plural(R.plurals.bib_titel_vormerkungen, 1, 1)), loan.extra)
        assertEquals(UiText(R.string.bib_titel_nur_lesesaal), item(LibraryAvailabilityStatus.ReferenceOnly).row(date).extra)
        assertNull(item(LibraryAvailabilityStatus.Available).row(date).extra)
        assertEquals(UiText(R.string.bib_titel_exemplar), item(LibraryAvailabilityStatus.Available, place = null).row(date).place)
        assertEquals("ZQ 5000", loan.signature)
    }

    @Test
    fun `facts keep only fields with data and at most two isbns`() {
        val facts = media.copy(isbns = listOf("1", " ", "2", "3"), publisher = " ", rvk = listOf("ZQ", "ZR"), subjects = listOf("A", ""), abstract = "x").facts("Deutsch")
        assertEquals("1, 2", facts.isbn)
        assertNull(facts.publisher)
        assertEquals("ZQ, ZR", facts.rvk)
        assertEquals(listOf("A"), facts.subjects)
        assertEquals("Deutsch", facts.language)
        assertTrue(media.facts(null).isEmpty)
    }

    @Test
    fun `edition line joins non empty parts`() {
        assertEquals("16. Auflage · 2022", media.copy(edition = "16. Auflage", year = "2022", publisher = " ").editionLine)
    }

    @Test
    fun `table of contents text is shortened`() {
        val short = LibraryTableOfContents(text = " kurz ", entries = emptyList(), sourceUrl = null)
        assertEquals("kurz", short.shortText)
        val long = LibraryTableOfContents(text = "a".repeat(TOC_TEXT_LIMIT + 50), entries = emptyList(), sourceUrl = null)
        assertEquals(TOC_TEXT_LIMIT + 2, long.shortText!!.length)
        assertNull(LibraryTableOfContents(text = " ", entries = emptyList(), sourceUrl = null).shortText)
    }

    @Test
    fun `language names handle MARC codes`() {
        assertEquals("Deutsch", LibraryLanguage.name("ger", Locale.GERMAN, "mehr", "unbekannt"))
        assertEquals("English", LibraryLanguage.name("eng", Locale.ENGLISH, "mehr", "unbekannt"))
        assertEquals("French", LibraryLanguage.name("fre", Locale.ENGLISH, "mehr", "unbekannt"))
        assertEquals("mehr", LibraryLanguage.name("mul", Locale.GERMAN, "mehr", "unbekannt"))
        assertEquals("unbekannt", LibraryLanguage.name("und", Locale.GERMAN, "mehr", "unbekannt"))
        assertEquals("XXX", LibraryLanguage.name("xxx", Locale.GERMAN, "mehr", "unbekannt"))
    }

    @Test
    fun `cover retry waits as told but at most thirty seconds`() {
        assertEquals(5.seconds, LibraryCoverLoader.waitTime(5.seconds))
        assertEquals(30.seconds, LibraryCoverLoader.waitTime(120.seconds))
        assertEquals(30.seconds, LibraryCoverLoader.waitTime(null))
    }
}
