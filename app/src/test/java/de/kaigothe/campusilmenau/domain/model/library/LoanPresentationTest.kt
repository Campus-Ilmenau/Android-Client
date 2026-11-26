package de.kaigothe.campusilmenau.domain.model.library

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.core.time.BerlinTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/** Testfälle aus `BibliothekTests` der iOS-App: Fälligkeit, Dringlichkeit, Zeitleiste, Vormerkungen, Listen, PPN. */
class LoanPresentationTest {

    private val today = LocalDate.of(2026, 11, 26)
    private val de = Locale.GERMANY

    private fun loan(
        days: Long?,
        title: String? = "Titel",
        status: PaiaItemStatus = PaiaItemStatus.Held,
        renewals: Int? = null,
        canRenew: Boolean? = null,
    ) = PaiaLoanItem(
        item = "urn:$title", edition = null, about = title, label = "SIG", queue = null, renewals = renewals, reminder = null,
        start = null, end = days?.let { BerlinTime.at(today.plusDays(it), 12 * 60) }, canCancel = null, canRenew = canRenew,
        storage = null, storageId = null, comment = null, status = status,
    )

    private fun info(item: PaiaLoanItem) = LoanInfo(item, today, de)

    // MARK: Fälligkeit

    @Test
    fun `Tage bis zur Faelligkeit und Dringlichkeit`() {
        assertEquals(10, info(loan(10)).days)
        assertEquals(LoanUrgency.Normal, info(loan(6)).urgency)
        assertEquals(LoanUrgency.Soon, info(loan(5)).urgency)
        assertEquals(LoanUrgency.Soon, info(loan(4)).urgency)
        assertEquals(LoanUrgency.Urgent, info(loan(3)).urgency)
        assertEquals(LoanUrgency.Urgent, info(loan(0)).urgency)
        assertEquals(LoanUrgency.Urgent, info(loan(-2)).urgency)
        assertEquals(LoanUrgency.Normal, info(loan(null)).urgency)
        assertNull(info(loan(null)).days)
    }

    @Test
    fun `Text im Faelligkeits-Chip`() {
        assertEquals(UiText.plural(R.plurals.bib_ueberfaellig_seit, 2, 2), info(loan(-2)).dueText)
        assertEquals(UiText.plural(R.plurals.bib_ueberfaellig_seit, 1, 1), info(loan(-1)).dueText)
        assertEquals(UiText(R.string.bib_heute_faellig), info(loan(0)).dueText)
        assertEquals(UiText.plural(R.plurals.bib_faellig_in, 1, 1), info(loan(1)).dueText)
        assertEquals(UiText.plural(R.plurals.bib_faellig_in, 5, 5), info(loan(5)).dueText)
        assertEquals(UiText(R.string.bib_faellig_am, "10. Dez."), info(loan(14)).dueText)
        assertNull(info(loan(null)).dueText)
    }

    @Test
    fun `ueberfaellig ist nur negativ`() {
        assertTrue(info(loan(-1)).isOverdue)
        assertFalse(info(loan(0)).isOverdue)
        assertFalse(info(loan(null)).isOverdue)
    }

    @Test
    fun `der Fortschritt der Leihfrist`() {
        assertEquals(1.0, info(loan(-3)).progress, 0.0)
        // Frisch ausgeliehen: Mindestens 4 %, damit der Strich sichtbar bleibt.
        assertEquals(0.04, info(loan(21)).progress, 0.0)
        assertEquals(0.5, info(loan(10)).progress, 0.05)
        assertEquals(1.0, info(loan(0)).progress, 0.0)
        assertEquals(0.0, info(loan(null)).progress, 0.0)
    }

    @Test
    fun `Fussnote und Zeitleiste zur Verlaengerung`() {
        assertEquals(UiText(R.string.bib_noch_nicht_verlaengert), info(loan(10)).metaText)
        assertEquals(UiText(R.string.bib_verlaengert_n, 2), info(loan(10, renewals = 2)).metaText)
        assertEquals(
            UiText(R.string.bib_nicht_verlaengerbar, UiText(R.string.bib_noch_nicht_verlaengert)),
            info(loan(10, canRenew = false)).metaText,
        )
        // Nur ein ausdrückliches false sperrt.
        assertTrue(info(loan(10, canRenew = null)).canRenew)
        assertFalse(info(loan(10, canRenew = false)).canRenew)

        assertEquals(
            UiText(R.string.bib_timeline, UiText(R.string.bib_timeline_faellig_am, "10. Dez."), UiText(R.string.bib_timeline_n_mal, 1)),
            info(loan(14, renewals = 1)).timelineLabel,
        )
        assertEquals(
            UiText(R.string.bib_timeline, UiText(R.string.bib_timeline_ueberfaellig), UiText(R.string.bib_timeline_noch_nicht)),
            info(loan(-1)).timelineLabel,
        )
        assertEquals(R.string.bib_timeline_ohne, (info(loan(null)).timelineLabel.args[0] as UiText).id)
    }

    // MARK: Vormerkungen

    @Test
    fun `Status-Chip einer Vormerkung`() {
        assertEquals(UiText(R.string.bib_status_abholbereit), HoldInfo(loan(2, status = PaiaItemStatus.Provided)).statusText)
        assertEquals(HoldTone.Green, HoldInfo(loan(2, status = PaiaItemStatus.Provided)).tone)
        assertEquals(HoldTone.Blue, HoldInfo(loan(2, status = PaiaItemStatus.Ordered)).tone)
        assertEquals(HoldTone.Neutral, HoldInfo(loan(2, status = PaiaItemStatus.Reserved)).tone)
        assertEquals(HoldTone.DangerSoft, HoldInfo(loan(2, status = PaiaItemStatus.Rejected)).tone)
        assertEquals(UiText(R.string.bib_status_kein), HoldInfo(loan(2, status = PaiaItemStatus.None)).statusText)
    }

    @Test
    fun `Zusatzzeile einer Vormerkung aus Kommentar, Ort, Warteschlange und Abholfrist`() {
        val provided = loan(3, status = PaiaItemStatus.Provided).copy(comment = " Regal 4 ", storage = "Theke", queue = 2)
        assertEquals(
            listOf(
                UiText.plain("Regal 4"),
                UiText(R.string.bib_abholort, "Theke"),
                UiText(R.string.bib_warteschlange, 2),
                UiText(R.string.bib_abholbar_bis, "29. Nov."),
            ),
            HoldInfo(provided, de).detail,
        )
        // Nicht bereitgestellt: „Standort“, keine Abholfrist; Warteschlange 0 entfällt.
        val reserved = loan(3, status = PaiaItemStatus.Reserved).copy(storage = "Magazin", queue = 0)
        assertEquals(listOf(UiText(R.string.bib_standort, "Magazin")), HoldInfo(reserved, de).detail)
        assertTrue(HoldInfo(loan(3, status = PaiaItemStatus.Reserved)).detail.isEmpty())
    }

    // MARK: Listen

    @Test
    fun `Ausleihen nach Faelligkeit, Ueberfaelliges zuerst, ohne Datum zuletzt`() {
        val lists = LoanLists(
            listOf(
                loan(null, "Z ohne"), loan(10, "B"), loan(-3, "überfällig"), loan(10, "A"),
                loan(2, "Vormerkung", status = PaiaItemStatus.Reserved),
            ),
        )
        assertEquals(listOf("überfällig", "A", "B", "Z ohne"), lists.loans.map { it.displayTitle })
        assertEquals(listOf("Vormerkung"), lists.holds.map { it.displayTitle })
    }

    @Test
    fun `die Zusammenfassung zaehlt ausgeliehen, ueberfaellig und bald faellig`() {
        assertEquals(listOf(UiText(R.string.bib_nichts_ausgeliehen)), LoanLists(emptyList()).summary(today))
        val lists = LoanLists(listOf(loan(-1, "a"), loan(0, "b"), loan(5, "c"), loan(6, "d")))
        assertEquals(
            listOf(UiText(R.string.bib_n_ausgeliehen, 4), UiText(R.string.bib_n_ueberfaellig, 1), UiText(R.string.bib_n_bald_faellig, 2)),
            lists.summary(today),
        )
        assertEquals(listOf(UiText(R.string.bib_n_ausgeliehen, 1)), LoanLists(listOf(loan(30))).summary(today))
    }

    // MARK: Medium

    @Test
    fun `Titel und Signatur`() {
        val withTitle = loan(1)
        assertEquals("Titel", withTitle.displayTitle)
        assertEquals("SIG", withTitle.signature)
        val onlyLabel = withTitle.copy(about = null)
        assertEquals("SIG", onlyLabel.displayTitle)
        assertNull(onlyLabel.signature)
        assertNull(onlyLabel.copy(label = null).displayTitle)
    }

    @Test
    fun `die PPN steht in der Ausgabe oder im Exemplar`() {
        val base = loan(1).copy(item = null)
        assertEquals("1009895648", base.copy(edition = "http://uri.gbv.de/document/opac-de-ilm1:ppn:1009895648").catalogPpn)
        assertEquals("123456789", base.copy(item = "https://x.example/ppn/123456789").catalogPpn)
        assertEquals("12345678X", base.copy(edition = "…?PPN=12345678x").catalogPpn)
        // Die Ausgabe geht vor dem Exemplar.
        assertEquals("111111111", base.copy(edition = "a:ppn:111111111", item = "b:ppn:222222222").catalogPpn)
        assertNull(base.copy(edition = "keine nummer", item = "ppn:12").catalogPpn)
        assertNull(base.catalogPpn)
    }

    @Test
    fun `die Kennung nimmt URI, dann about, dann Signatur`() {
        assertEquals("urn:Titel", loan(1).id)
        assertEquals("Titel", loan(1).copy(item = null).id)
        assertTrue(loan(1).copy(item = null, about = null).id.startsWith("SIG|"))
    }

    @Test
    fun `eine Verlaengerung zaehlt hoch und uebernimmt Datum und Status`() {
        val renewed = loan(2, renewals = null).renewed(BerlinTime.at(today.plusDays(23), 0), null)
        assertEquals(1, renewed.renewals)
        assertEquals(BerlinTime.at(today.plusDays(23), 0), renewed.end)
        assertEquals(PaiaItemStatus.Held, renewed.status)
        // Ohne neue Angaben bleiben die alten.
        val same = loan(2).renewed(null, null)
        assertEquals(loan(2).end, same.end)
    }
}
