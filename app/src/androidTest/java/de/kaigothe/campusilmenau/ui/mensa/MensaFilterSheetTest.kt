package de.kaigothe.campusilmenau.ui.mensa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.mensa.MensaDiet
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFilter
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Der Hauptfluss „Mensa filtern“: Ernährung wählen, Allergen ausschließen, zurücksetzen. */
class MensaFilterSheetTest {
    @get:Rule
    val rule = createComposeRule()

    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun show(initial: MensaFilter, onChange: (MensaFilter) -> Unit) {
        rule.setContent {
            var filter by remember { mutableStateOf(initial) }
            CampusIlmenauTheme {
                MensaFilterSheet(
                    filter = filter,
                    allergens = listOf("Gluten", "Milch"),
                    onFilterChange = {
                        filter = it
                        onChange(it)
                    },
                    onDismiss = {},
                )
            }
        }
    }

    @Test
    fun zuruecksetzenIstOhneFilterGesperrt() {
        show(MensaFilter()) {}
        rule.onNodeWithText(text(R.string.filter_zuruecksetzen_kurz)).assertIsNotEnabled()
    }

    @Test
    fun ernaehrungWaehlenMeldetDenNeuenFilter() {
        var latest: MensaFilter? = null
        show(MensaFilter()) { latest = it }

        rule.onNodeWithText(text(R.string.diaet_vegan)).performClick()

        assertEquals(MensaDiet.Vegan, latest?.diet)
    }

    @Test
    fun zuruecksetzenStelltDenGrundzustandHer() {
        var latest: MensaFilter? = null
        show(MensaFilter(diet = MensaDiet.Vegetarisch, onlyBio = true)) { latest = it }

        rule.onNodeWithText(text(R.string.filter_zuruecksetzen_kurz)).assertIsEnabled().performClick()

        assertEquals(MensaFilter(), latest)
    }
}
