package de.kaigothe.campusilmenau.ui.bibliothek

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.library.PaiaItemStatus
import de.kaigothe.campusilmenau.domain.model.library.PaiaLoanItem
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/** Der Hauptfluss „Medium verlängern“: Button, Rückfrage, Bestätigen oder Abbrechen. */
class LoanCardTest {
    @get:Rule
    val rule = createComposeRule()

    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val today = LocalDate.of(2026, 3, 10)

    private val item = PaiaLoanItem(
        item = "https://example.org/item/1",
        edition = null,
        about = "Analysis 1",
        label = "MAT 100",
        queue = null,
        renewals = 0,
        reminder = null,
        start = today.minusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC),
        end = today.plusDays(14).atStartOfDay().toInstant(ZoneOffset.UTC),
        canCancel = null,
        canRenew = true,
        storage = null,
        storageId = null,
        comment = null,
        status = PaiaItemStatus.Held,
    )

    private fun show(item: PaiaLoanItem, onRenew: () -> Unit) {
        rule.setContent {
            CampusIlmenauTheme { LoanCard(item = item, today = today, onRenew = onRenew, onOpen = null) }
        }
    }

    @Test
    fun zeigtTitelUndSignatur() {
        show(item) {}
        rule.onNodeWithText("Analysis 1").assertIsDisplayed()
        rule.onNodeWithText("MAT 100").assertIsDisplayed()
    }

    @Test
    fun verlaengernFragtErstNachUndFuehrtDannAus() {
        var renewed = 0
        show(item) { renewed++ }

        rule.onNodeWithText(text(R.string.bib_verlaengern)).performClick()
        assertEquals("Ohne Bestätigung darf nichts passieren", 0, renewed)
        rule.onNodeWithText(text(R.string.bib_verlaengern_titel)).assertIsDisplayed()

        // Der Dialog hat denselben Beschriftungstext am Bestätigen-Button; er liegt über der Karte.
        rule.onAllNodesWithText(text(R.string.bib_verlaengern)).onLast().performClick()
        assertEquals(1, renewed)
    }

    @Test
    fun abbrechenVerlaengertNicht() {
        var renewed = 0
        show(item) { renewed++ }

        rule.onNodeWithText(text(R.string.bib_verlaengern)).performClick()
        rule.onNodeWithText(text(R.string.abbrechen)).performClick()

        assertEquals(0, renewed)
        rule.onNodeWithText(text(R.string.bib_verlaengern_titel)).assertDoesNotExist()
    }

    @Test
    fun serverSperreVersteckenDenButton() {
        show(item.copy(canRenew = false)) {}
        rule.onNodeWithText(text(R.string.bib_verlaengern)).assertDoesNotExist()
    }
}
