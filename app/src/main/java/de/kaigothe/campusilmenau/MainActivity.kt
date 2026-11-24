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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.kaigothe.campusilmenau.domain.model.Darstellung
import de.kaigothe.campusilmenau.ui.launch.LaunchOverlay
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
        // Der Splash des Systems zeigt den ersten Frame der Start-Animation. Er bleibt, bis die Einstellungen
        // gelesen sind (sonst blitzt bei „Dunkel“ kurz das helle Theme auf) und wird dann ohne eigene
        // Ausblendung entfernt: Die Compose-Animation zeichnet denselben Frame und läuft weiter.
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }
        splashScreen.setOnExitAnimationListener { it.remove() }

        super.onCreate(savedInstanceState)
        // Nach einem Konfigurationswechsel kommt derselbe Intent erneut: Er darf kein zweites Mal öffnen.
        if (savedInstanceState == null) handleDeepLink(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val darstellung = (uiState as? MainUiState.Ready)?.darstellung ?: Darstellung.System
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

            // Die Animation läuft nur beim Kaltstart. Nach Drehen oder Wiederherstellen bleibt sie aus.
            var launchFinished by rememberSaveable { mutableStateOf(false) }

            CampusIlmenauTheme(darkTheme = darkTheme) {
                LaunchOverlay(playing = !launchFinished, onFinished = { launchFinished = true }) {
                    AppShell(launchFinished = launchFinished)
                }
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
