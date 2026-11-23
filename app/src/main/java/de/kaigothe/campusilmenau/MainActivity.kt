package de.kaigothe.campusilmenau

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
import de.kaigothe.campusilmenau.ui.PlatzhalterScreen
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                PlatzhalterScreen()
            }
        }
    }
}
