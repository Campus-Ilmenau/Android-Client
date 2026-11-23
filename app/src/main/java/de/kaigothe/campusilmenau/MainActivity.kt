package de.kaigothe.campusilmenau

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.kaigothe.campusilmenau.domain.model.Darstellung
import de.kaigothe.campusilmenau.ui.navigation.AppShell
import de.kaigothe.campusilmenau.ui.navigation.DeepLink
import de.kaigothe.campusilmenau.ui.navigation.DeepLinkRouter
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var deepLinkRouter: DeepLinkRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Nach einem Konfigurationswechsel kommt derselbe Intent erneut: Er darf kein zweites Mal öffnen.
        if (savedInstanceState == null) handleDeepLink(intent)

        setContent {
            val darstellung by viewModel.darstellung.collectAsStateWithLifecycle()
            val darkTheme = when (darstellung) {
                Darstellung.System -> isSystemInDarkTheme()
                Darstellung.Hell -> false
                Darstellung.Dunkel -> true
            }

            // Die Systemleisten folgen der gewählten Darstellung, nicht nur dem System.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
                onDispose {}
            }

            CampusIlmenauTheme(darkTheme = darkTheme) {
                AppShell()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    /** Ein Link `campusilmenau://…` (Widget, Shortcut, Browser) geht an den Router. Fremde Links werden ignoriert. */
    private fun handleDeepLink(intent: Intent?) {
        val link = intent?.data?.toString()?.let(DeepLink::parse) ?: return
        deepLinkRouter.dispatch(link)
    }
}
