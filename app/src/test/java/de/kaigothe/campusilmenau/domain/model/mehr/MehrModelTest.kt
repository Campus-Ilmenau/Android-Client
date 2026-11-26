package de.kaigothe.campusilmenau.domain.model.mehr

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.domain.model.SpracheWahl
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaBalance
import de.kaigothe.campusilmenau.ui.mehr.MehrBereich
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class MehrModelTest {
    @Test
    fun `language choice maps tags, regions and unknown values`() {
        assertEquals(SpracheWahl.Automatisch, SpracheWahl.ofTag(null))
        assertEquals(SpracheWahl.Deutsch, SpracheWahl.ofTag("de"))
        assertEquals(SpracheWahl.Deutsch, SpracheWahl.ofTag("de-AT"))
        assertEquals(SpracheWahl.Englisch, SpracheWahl.ofTag("EN_us"))
        assertEquals(SpracheWahl.Automatisch, SpracheWahl.ofTag("fr"))
    }

    @Test
    fun `automatic follows English devices, everything else gets German`() {
        assertEquals(SpracheWahl.Englisch, SpracheWahl.fuerGeraet("en"))
        assertEquals(SpracheWahl.Deutsch, SpracheWahl.fuerGeraet("de"))
        assertEquals(SpracheWahl.Deutsch, SpracheWahl.fuerGeraet("fr"))
        assertEquals(SpracheWahl.Deutsch, SpracheWahl.fuerGeraet(null))
    }

    @Test
    fun `link areas are read case insensitively and unknown ones are ignored`() {
        assertEquals(MehrBereich.Konto, MehrBereich.ofLink("konto"))
        assertEquals(MehrBereich.Ueber, MehrBereich.ofLink("UEBER"))
        assertNull(MehrBereich.ofLink("einstellungen"))
        assertNull(MehrBereich.ofLink(null))
    }

    private fun balance(card: String, minute: Long, milli: Long) =
        ThoskaBalance(card, milli, 0, Instant.parse("2026-03-01T10:00:00Z").plusSeconds(minute * 60))

    @Test
    fun `stored card history is grouped per card, newest card first`() {
        val karten = ThoskaKarteUebersicht.of(
            listOf(balance("A1B2C3D4", 1, 5_000), balance("ZZZZ", 5, 9_000), balance("A1B2C3D4", 10, 7_500)),
            names = mapOf("A1B2C3D4" to " Lena "),
        )
        assertEquals(listOf("A1B2C3D4", "ZZZZ"), karten.map { it.cardId })
        val erste = karten.first()
        assertEquals(2, erste.anzahl)
        assertEquals(7_500, erste.letzterStandMilli)
        assertEquals(UiText.plain("Lena"), erste.name)
        assertEquals(UiText(R.string.thoska_karte_fallback, "ZZZZ"), karten.last().name)
        assertEquals(Instant.parse("2026-03-01T10:01:00Z"), erste.erste)
    }
}
