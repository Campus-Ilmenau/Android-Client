package de.kaigothe.campusilmenau.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.SheetShape
import kotlinx.coroutines.launch

/**
 * Sheet von unten, immer in voller Höhe (kein halbes Einrasten): Griff, „Abbrechen“ links oben, Inhalt auf `surface`.
 * Ecken 14 oben. Der Inhalt bekommt die Navigationsleiste als Abstand, damit nichts darunter verschwindet.
 *
 * [startAction] ersetzt „Abbrechen“ links, [endAction] steht rechts (zum Beispiel „Zurücksetzen“ und „Fertig“ bei einem
 * Sheet, dessen Änderungen sofort gelten). Beide bekommen die Funktion zum Schließen mit Animation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
    endAction: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismissRequest()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (startAction != null) {
                    startAction(dismiss)
                } else {
                    TextButton(onClick = dismiss) {
                        Text(text = stringResource(R.string.abbrechen), color = CampusTheme.colors.accent)
                    }
                }
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (endAction != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    endAction(dismiss)
                }
            }
            content()
        }
    }
}
