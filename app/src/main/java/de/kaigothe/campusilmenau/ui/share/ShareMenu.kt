package de.kaigothe.campusilmenau.ui.share

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.share.ShareContent
import de.kaigothe.campusilmenau.ui.bibliothek.ausweis.findFragmentActivity
import de.kaigothe.campusilmenau.ui.navigation.LocalToastState
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Teilen-Taste mit Menü: Kopfzeile (was geteilt wird), „Als Bild teilen“ und „Als Text teilen“. Beide führen zum
 * Teilen-Blatt des Systems. [anchor] zeichnet die Taste und bekommt die Aktion, die das Menü öffnet. Das Bild entsteht erst, wenn
 * jemand es wählt (rund eine Zehntelsekunde), nicht vorab.
 */
@Composable
fun ShareMenu(
    text: ShareContent,
    image: ShareImageSpec?,
    anchor: @Composable (onClick: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val toast = LocalToastState.current
    val scope = rememberCoroutineScope()
    val colors = CampusTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val failed = stringResource(R.string.share_bild_fehler)

    Box(modifier = modifier) {
        anchor { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                text = text.title,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
            if (image != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share_als_bild)) },
                    leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                    onClick = {
                        expanded = false
                        val activity = context.findFragmentActivity()
                        scope.launch {
                            try {
                                if (activity == null) error("Keine Activity")
                                val bitmap = ShareImageRenderer.render(activity, image.style, image.card)
                                context.shareImage(context.writeSharePng(bitmap, image.fileName))
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                toast.show(failed)
                            }
                        }
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.share_als_text)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.TextSnippet, contentDescription = null) },
                onClick = {
                    expanded = false
                    context.shareText(text)
                },
            )
        }
    }
}
