package de.kaigothe.campusilmenau.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Erzwingt „Bewegung reduzieren“ (Previews, Tests). `null`: das System entscheidet.
 */
val LocalForceReducedMotion = compositionLocalOf<Boolean?> { null }

/**
 * Ist „Bewegung reduzieren“ aktiv? Android kennt keinen eigenen Schalter wie iOS, die Entsprechung ist die
 * Animationsdauer-Skalierung 0 (Entwickleroptionen und Bedienungshilfen „Animationen entfernen“).
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val forced = LocalForceReducedMotion.current
    val context = LocalContext.current
    return forced ?: remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

@Composable
fun WithReducedMotion(reduced: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalForceReducedMotion provides reduced, content = content)
}
