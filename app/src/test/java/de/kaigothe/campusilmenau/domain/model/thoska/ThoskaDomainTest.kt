package de.kaigothe.campusilmenau.domain.model.thoska

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.core.time.BerlinTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/** Testfälle zu Verlauf, Kartennamen und Formatierung der Thoska (Pendant zu den Tests der iOS-App). */
class ThoskaDomainTest {

    private val today = LocalDate.of(2026, 11, 25)
    private val now = BerlinTime.at(today, 12 * 60)

    private fun reading(daysAgo: Long, balance: Long, last: Long = 2_900, hour: Int = 9): ThoskaReading =
        ThoskaReading(BerlinTime.at(today.minusDays(daysAgo), hour * 60), balance, last)

    // MARK: Verlauf

    @Test
    fun `Ausgaben sind die Summe der Rueckgaenge ohne Aufladungen`() {
        val summary = ThoskaHistorySummary(
            listOf(reading(5, 18_200), reading(4, 15_300), reading(3, 12_500), reading(2, 26_800), reading(1, 24_000)),
            ThoskaHistoryRange.TwoWeeks, now,
        )
        // 2,9 + 2,8 + 2,8 = 8,5 €; die Aufladung auf 26,8 zählt nicht.
        assertEquals(8_500L, summary.spentMilli)
        assertEquals(listOf(false, false, false, true, false), summary.entries.map { it.isTopUp })
    }

    @Test
    fun `Rundungsrauschen ist keine Aufladung`() {
        val summary = ThoskaHistorySummary(
            listOf(reading(3, 10_000), reading(2, 10_005), reading(1, 10_011)),
            ThoskaHistoryRange.TwoWeeks, now,
        )
        // +5 Tausendstel ist Rauschen, +6 gegenüber dem Vorgänger ist eine Aufladung.
        assertEquals(listOf(false, false, true), summary.entries.map { it.isTopUp })
    }

    @Test
    fun `der Zeitraum schneidet Auslesungen ab, die Aufladung vergleicht aber mit dem Vorgaenger davor`() {
        val summary = ThoskaHistorySummary(
            listOf(reading(30, 5_000), reading(10, 25_000), reading(1, 22_000)),
            ThoskaHistoryRange.TwoWeeks, now,
        )
        assertEquals(2, summary.entries.size)
        // Die Auslesung vor 10 Tagen ist gegenüber der vor 30 Tagen eine Aufladung, obwohl diese außerhalb liegt.
        assertTrue(summary.entries.first().isTopUp)
        assertEquals(3_000L, summary.spentMilli)
    }

    @Test
    fun `Auslesungen nach jetzt zaehlen nicht und die Reihenfolge der Eingabe ist egal`() {
        val future = ThoskaReading(now.plusSeconds(3600), 1_000, 0)
        val summary = ThoskaHistorySummary(listOf(reading(1, 20_000), future, reading(3, 25_000)), ThoskaHistoryRange.TwoWeeks, now)
        assertEquals(listOf(25_000L, 20_000L), summary.entries.map { it.balanceMilli })
    }

    @Test
    fun `bei zwei Wochen gibt es einen Balken je Auslesung`() {
        val summary = ThoskaHistorySummary(listOf(reading(3, 9_000), reading(2, 30_000), reading(1, 27_000)), ThoskaHistoryRange.TwoWeeks, now)
        assertEquals(listOf(9_000L, 30_000L, 27_000L), summary.bars.map { it.balanceMilli })
        assertEquals(listOf(false, true, false), summary.bars.map { it.isTopUp })
    }

    @Test
    fun `bei drei Monaten gibt es einen Balken je Woche mit dem letzten Stand`() {
        // 25.11.2026 ist ein Mittwoch. Zwei Auslesungen in derselben Woche (Mo 23., Di 24.), eine in der Vorwoche.
        val summary = ThoskaHistorySummary(
            listOf(reading(9, 20_000), reading(2, 15_000), reading(1, 30_000)),
            ThoskaHistoryRange.ThreeMonths, now,
        )
        assertEquals(2, summary.bars.size)
        assertEquals(20_000L, summary.bars[0].balanceMilli)
        // Letzter Stand der Woche, und die Woche enthält die Aufladung.
        assertEquals(30_000L, summary.bars[1].balanceMilli)
        assertTrue(summary.bars[1].isTopUp)
        assertEquals(BerlinTime.startOfDay(LocalDate.of(2026, 11, 23)), summary.bars[1].date)
        assertEquals(listOf(0, 1), summary.bars.map { it.id })
    }

    @Test
    fun `ohne Auslesungen ist alles leer`() {
        val summary = ThoskaHistorySummary(emptyList(), ThoskaHistoryRange.ThreeMonths, now)
        assertTrue(summary.entries.isEmpty())
        assertTrue(summary.bars.isEmpty())
        assertEquals(0L, summary.spentMilli)
    }

    @Test
    fun `die Zeitraeume beginnen 14 Tage bzw 3 Monate vor jetzt`() {
        assertEquals(BerlinTime.at(today.minusDays(14), 12 * 60), ThoskaHistoryRange.TwoWeeks.startDate(now))
        assertEquals(BerlinTime.at(LocalDate.of(2026, 8, 25), 12 * 60), ThoskaHistoryRange.ThreeMonths.startDate(now))
    }

    // MARK: Kartennamen

    @Test
    fun `ein leerer Name oder die Karten-ID gelten als ohne Namen`() {
        val id = "04a1b2c3d4"
        assertNull(ThoskaCardNaming.customName(id, emptyMap()))
        assertNull(ThoskaCardNaming.customName(id, mapOf(id to "   ")))
        assertNull(ThoskaCardNaming.customName(id, mapOf(id to id)))
        assertEquals("Mensakarte", ThoskaCardNaming.customName(id, mapOf(id to "  Mensakarte ")))
        assertNull(ThoskaCardNaming.customName(id, mapOf("andere" to "Name")))
    }

    @Test
    fun `der Ersatzname nimmt die letzten vier Zeichen gross`() {
        assertEquals(UiText(R.string.thoska_karte_fallback, "C3D4"), ThoskaCardNaming.fallbackName("04a1b2c3d4"))
        assertEquals(UiText(R.string.thoska_karte_fallback, "AB"), ThoskaCardNaming.fallbackName("ab"))
    }

    @Test
    fun `Umbenennen trimmt und eine leere Eingabe setzt zurueck`() {
        assertEquals("Mensa", ThoskaCardNaming.normalized("  Mensa "))
        assertNull(ThoskaCardNaming.normalized("   "))
        assertNull(ThoskaCardNaming.normalized(""))
    }

    // MARK: Formatierung

    @Test
    fun `Betraege in Euro`() {
        val de = Locale.GERMANY
        assertEquals("12,40 €", ThoskaFormatting.euro(12_400, de))
        assertEquals("0,00 €", ThoskaFormatting.euro(0, de))
        assertEquals("− 2,90 €", ThoskaFormatting.minusEuro(2_900, de))
        assertEquals("+ 20,00 €", ThoskaFormatting.plusEuro(20_000, de))
        assertEquals(UiText(R.string.thoska_euro_sprechen, "12,40"), ThoskaFormatting.spokenEuro(12_400, de))
    }

    @Test
    fun `Zeitpunkt der letzten Auslesung`() {
        val de = Locale.GERMANY
        assertEquals(UiText(R.string.thoska_gerade_eben), ThoskaFormatting.lastRead(now.minusSeconds(30), now, de))
        assertEquals(UiText(R.string.thoska_zeit_heute, "08:42"), ThoskaFormatting.lastRead(BerlinTime.at(today, 8 * 60 + 42), now, de))
        assertEquals(UiText(R.string.thoska_zeit_gestern, "08:42"), ThoskaFormatting.lastRead(BerlinTime.at(today.minusDays(1), 8 * 60 + 42), now, de))
        val older = ThoskaFormatting.lastRead(BerlinTime.at(LocalDate.of(2026, 11, 2), 8 * 60 + 42), now, de)
        assertEquals(R.string.thoska_zeit_datum, older.id)
        assertEquals("08:42", older.args[1])
        assertEquals("Mo., 02.11.", older.args[0])
        // Auch eine Auslesung kurz in der Zukunft gilt als „gerade eben“.
        assertEquals(UiText(R.string.thoska_gerade_eben), ThoskaFormatting.lastRead(now.plusSeconds(20), now, de))
    }
}
