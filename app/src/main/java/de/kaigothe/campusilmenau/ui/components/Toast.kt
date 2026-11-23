package de.kaigothe.campusilmenau.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import kotlinx.coroutines.delay

private val ToastShape = RoundedCornerShape(14.dp)

/** Standard-Anzeigedauer eines Toasts: 2,4 s (wie in der iOS-App). */
const val TOAST_DURATION_MILLIS = 2_400L

@Immutable
class ToastMessage(
    val id: Long,
    val text: String,
    val actionLabel: String?,
    val onAction: (() -> Unit)?,
)

/**
 * Zustand des Toasts. Eine neue Meldung ersetzt die aktuelle und startet die Anzeigedauer neu.
 * `show` darf von jedem Screen aufgerufen werden, der den State bekommt.
 */
@Stable
class ToastState {
    var current by mutableStateOf<ToastMessage?>(null)
        private set

    private var counter = 0L

    fun show(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        counter += 1
        current = ToastMessage(counter, text, actionLabel, onAction)
    }

    fun dismiss() {
        current = null
    }
}

@Composable
fun rememberToastState(): ToastState = remember { ToastState() }

/**
 * Zeigt den Toast unten, über dem Inhalt. In der App-Shell steht er über der Tab-Leiste.
 * Blendet nach [durationMillis] von selbst aus.
 */
@Composable
fun ToastHost(
    state: ToastState,
    modifier: Modifier = Modifier,
    durationMillis: Long = TOAST_DURATION_MILLIS,
) {
    val message = state.current
    var lastMessage by remember { mutableStateOf<ToastMessage?>(null) }
    LaunchedEffect(message?.id) {
        if (message != null) {
            lastMessage = message
            delay(durationMillis)
            state.dismiss()
        }
    }
    val display = message ?: lastMessage

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = slideInVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            if (display != null) {
                Toast(text = display.text, actionLabel = display.actionLabel, onAction = display.onAction)
            }
        }
    }
}

/** Kurze Bestätigung: `buttonPrimary`-Hintergrund, Häkchen, Semibold. Optional mit einer Aktion rechts. */
@Composable
fun Toast(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = CampusTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = ToastShape,
                ambientColor = colors.brandNight.copy(alpha = 0.3f),
                spotColor = colors.brandNight.copy(alpha = 0.3f),
            )
            .background(colors.buttonPrimary, ToastShape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = colors.onButtonPrimary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onButtonPrimary,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                ),
                color = colors.onButtonPrimary,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(role = Role.Button, onClick = onAction),
            )
        }
    }
}
