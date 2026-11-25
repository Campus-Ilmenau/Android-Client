package de.kaigothe.campusilmenau.core.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import de.kaigothe.campusilmenau.R

/**
 * Ein Text aus den String-Ressourcen mit Argumenten, noch nicht aufgelöst. So kann die Logik (ohne Android-Kontext und
 * ohne Sprache) sagen, was angezeigt wird, und Tests vergleichen Ressource und Zahlen statt fertiger Sätze.
 * Argumente dürfen selbst [UiText] sein.
 */
data class UiText(@StringRes val id: Int, val args: List<Any>) {
    constructor(@StringRes id: Int, vararg args: Any) : this(id, args.toList())

    companion object {
        /** Ein fertiger Text (Name eines Gerichts, Uhrzeit), der nicht übersetzt wird. */
        fun plain(text: String) = UiText(R.string.plain, listOf(text))
    }

    fun resolve(context: Context): String =
        context.getString(id, *args.map { if (it is UiText) it.resolve(context) else it }.toTypedArray())
}

@Composable
fun UiText.asString(): String = resolve(LocalContext.current)
