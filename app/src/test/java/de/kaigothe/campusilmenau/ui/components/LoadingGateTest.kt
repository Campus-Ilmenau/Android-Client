package de.kaigothe.campusilmenau.ui.components

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class LoadingGateTest {

    private fun TestScope.gate(initiallyLoading: Boolean = false) =
        LoadingGate(backgroundScope, initiallyLoading)

    /** Zeit vorrücken und alle Aufgaben abarbeiten, die genau dann fällig sind. */
    private fun TestScope.advance(millis: Long) {
        advanceTimeBy(millis.milliseconds)
        runCurrent()
    }

    @Test
    fun `ohne Laden ist nichts aktiv`() = runTest {
        val gate = gate()
        assertFalse(gate.isActive.value)
        assertFalse(gate.isVisible.value)
        assertFalse(gate.isSlow.value)
    }

    @Test
    fun `Laden beginnt aktiv aber unsichtbar`() = runTest {
        val gate = gate()
        gate.setLoading(true)
        assertTrue(gate.isActive.value)
        assertFalse(gate.isVisible.value)
    }

    @Test
    fun `Startwert Laden ist sofort aktiv`() = runTest {
        assertTrue(gate(initiallyLoading = true).isActive.value)
    }

    @Test
    fun `Ziege erscheint erst nach der Verzoegerung`() = runTest {
        val gate = gate()
        gate.setLoading(true)
        advance(399)
        assertFalse(gate.isVisible.value)
        advance(1)
        assertTrue(gate.isVisible.value)
    }

    @Test
    fun `schnelle Antwort zeigt die Ziege nie`() = runTest {
        val gate = gate()
        gate.setLoading(true)
        advance(200)
        gate.setLoading(false)
        assertFalse(gate.isActive.value)
        assertFalse(gate.isVisible.value)
        advance(2_000)
        assertFalse(gate.isVisible.value)
    }

    @Test
    fun `sichtbare Ziege bleibt mindestens die Mindestdauer`() = runTest {
        val gate = gate()
        gate.setLoading(true)
        advance(400)
        gate.setLoading(false)
        // Direkt nach dem Einblenden: Die Ziege steht, der Inhalt wartet.
        assertTrue(gate.isActive.value)
        assertTrue(gate.isVisible.value)
        advance(599)
        assertTrue(gate.isVisible.value)
        advance(1)
        // Mindestdauer um: ausblenden, noch aktiv bis das Ausblenden fertig ist.
        assertFalse(gate.isVisible.value)
        assertTrue(gate.isActive.value)
        advance(199)
        assertTrue(gate.isActive.value)
        advance(1)
        assertFalse(gate.isActive.value)
    }

    @Test
    fun `Antwort nach der Mindestdauer blendet sofort aus`() = runTest {
        val gate = gate()
        gate.setLoading(true)
        advance(2_000)
        assertTrue(gate.isVisible.value)
        gate.setLoading(false)
        assertFalse(gate.isVisible.value)
        assertTrue(gate.isActive.value)
        advance(200)
        assertFalse(gate.isActive.value)
    }

    @Test
    fun `Hinweis fuer langes Laden erscheint nach acht Sekunden`() = runTest {
        val gate = gate()
        gate.setLoading(true)
        advance(7_999)
        assertFalse(gate.isSlow.value)
        advance(1)
        assertTrue(gate.isSlow.value)
    }

    @Test
    fun `nach dem Laden ist der Hinweis wieder weg`() = runTest {
        val gate = gate()
        gate.setLoading(true)
        advance(9_000)
        gate.setLoading(false)
        advance(200)
        assertFalse(gate.isSlow.value)
        assertFalse(gate.isActive.value)
    }

    @Test
    fun `erneutes Laden beginnt von vorn`() = runTest {
        val gate = gate()
        gate.setLoading(true)
        advance(1_000)
        gate.setLoading(false)
        advance(200)
        gate.setLoading(true)
        assertTrue(gate.isActive.value)
        assertFalse(gate.isVisible.value)
        advance(400)
        assertTrue(gate.isVisible.value)
    }

    @Test
    fun `doppeltes Laden setzt die Zeit nicht zurueck`() = runTest {
        val gate = gate()
        gate.setLoading(true)
        advance(300)
        gate.setLoading(true)
        advance(100)
        assertTrue(gate.isVisible.value)
    }

    @Test
    fun `Ende ohne Laden aendert nichts`() = runTest {
        val gate = gate()
        gate.setLoading(false)
        assertFalse(gate.isActive.value)
    }
}
