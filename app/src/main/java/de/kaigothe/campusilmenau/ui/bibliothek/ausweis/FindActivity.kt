package de.kaigothe.campusilmenau.ui.bibliothek.ausweis

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/** Die [FragmentActivity] hinter einem Compose-Kontext (er ist oft mehrfach verpackt), `null` ohne eine solche. */
tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
