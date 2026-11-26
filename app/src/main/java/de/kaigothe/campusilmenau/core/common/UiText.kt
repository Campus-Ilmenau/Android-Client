package de.kaigothe.campusilmenau.core.common

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import de.kaigothe.campusilmenau.R

/**
 * Ein Text aus den String-Ressourcen mit Argumenten, noch nicht aufgelöst. So kann die Logik (ohne Android-Kontext und
 * ohne Sprache) sagen, was angezeigt wird, und Tests vergleichen Ressource und Zahlen statt fertiger Sätze.
 * Argumente dürfen selbst [UiText] sein. Mit [quantity] ist [id] eine Plural-Ressource (`<plurals>`).
 */
data class UiText(val id: Int, val args: List<Any>, val quantity: Int? = null) {
    constructor(@StringRes id: Int, vararg args: Any) : this(id, args.toList(), null)

    companion object {
        /** Ein fertiger Text (Name eines Gerichts, Uhrzeit), der nicht übersetzt wird. */
        fun plain(text: String) = UiText(id = R.string.plain, args = listOf(text), quantity = null)

        /** Ein Text mit Mehrzahl: [id] ist eine `<plurals>`-Ressource, [quantity] wählt die Form. */
        fun plural(@PluralsRes id: Int, quantity: Int, vararg args: Any) = UiText(id = id, args = args.toList(), quantity = quantity)
    }

    fun resolve(context: Context): String {
        val resolved = args.map { if (it is UiText) it.resolve(context) else it }.toTypedArray()
        return if (quantity == null) {
            context.getString(id, *resolved)
        } else {
            context.resources.getQuantityString(id, quantity, *resolved)
        }
    }
}

@Composable
fun UiText.asString(): String = resolve(LocalContext.current)
