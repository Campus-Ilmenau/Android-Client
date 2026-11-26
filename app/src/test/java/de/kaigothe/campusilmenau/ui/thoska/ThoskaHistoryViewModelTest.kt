package de.kaigothe.campusilmenau.ui.thoska

import de.kaigothe.campusilmenau.data.repository.ThoskaRepository
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaBalance
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaCardNaming
import de.kaigothe.campusilmenau.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ThoskaHistoryViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val now = Instant.parse("2026-11-25T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private val stored = MutableStateFlow<List<ThoskaBalance>>(emptyList())
    private val nameMap = MutableStateFlow<Map<String, String>>(emptyMap())
    private val repository = object : ThoskaRepository {
        override val balances = stored
        override val names = nameMap
        override suspend fun save(balance: ThoskaBalance) {
            stored.value = stored.value + balance
        }

        override suspend fun deleteAll() = Unit

        override suspend fun rename(cardId: String, input: String) {
            val name = ThoskaCardNaming.normalized(input)
            nameMap.value = if (name == null) nameMap.value - cardId else nameMap.value + (cardId to name)
        }
    }

    private fun reading(card: String, milli: Long, daysAgo: Long) =
        ThoskaBalance(card, milli, 2_900, now.minusSeconds(daysAgo * 86_400))

    @Test
    fun `ohne Auslesungen gibt es keine Karte`() = runTest {
        val state = ThoskaHistoryViewModel(repository, clock).state.value
        assertEquals(emptyList<Any>(), state.cards)
        assertNull(state.selectedCardId)
    }

    @Test
    fun `die zuletzt ausgelesene Karte steht vorn und ist gewaehlt`() = runTest {
        stored.value = listOf(reading("aaaa", 5_000, 10), reading("bbbb", 7_000, 5), reading("aaaa", 4_000, 1))
        val viewModel = ThoskaHistoryViewModel(repository, clock)

        val state = viewModel.state.value

        assertEquals(listOf("aaaa", "bbbb"), state.cards.map { it.id })
        assertEquals("aaaa", state.selectedCardId)
        // Nur die Auslesungen der gewählten Karte, älteste zuerst.
        assertEquals(listOf(5_000L, 4_000L), state.readings.map { it.balanceMilli })
    }

    @Test
    fun `eine andere Karte waehlen zeigt deren Auslesungen`() = runTest {
        stored.value = listOf(reading("aaaa", 5_000, 10), reading("bbbb", 7_000, 5))
        val viewModel = ThoskaHistoryViewModel(repository, clock)

        viewModel.selectCard("aaaa")
        advanceUntilIdle()
        assertEquals(listOf(5_000L), viewModel.state.value.readings.map { it.balanceMilli })

        // Eine unbekannte Karte fällt auf die zuletzt ausgelesene zurück.
        viewModel.selectCard("gibt-es-nicht")
        advanceUntilIdle()
        assertEquals("bbbb", viewModel.state.value.selectedCardId)
    }

    @Test
    fun `Namen erscheinen in den Karten und der Platzhalter-Name zaehlt als ohne Namen`() = runTest {
        stored.value = listOf(reading("04a1b2c3d4", 5_000, 1), reading("aa11bb22cc", 7_000, 2))
        nameMap.value = mapOf("04a1b2c3d4" to "Mensakarte", "aa11bb22cc" to "aa11bb22cc")
        val viewModel = ThoskaHistoryViewModel(repository, clock)

        val cards = viewModel.state.value.cards

        assertEquals("Mensakarte", cards[0].customName)
        assertNull(cards[1].customName)
        assertEquals("", cards[1].displayName)
    }

    @Test
    fun `Umbenennen wirkt sofort`() = runTest {
        stored.value = listOf(reading("aaaa", 5_000, 1))
        val viewModel = ThoskaHistoryViewModel(repository, clock)

        viewModel.rename("aaaa", " Mensa ")
        advanceUntilIdle()

        assertEquals("Mensa", viewModel.state.value.cards.single().customName)
    }
}
