package de.kaigothe.campusilmenau

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import de.kaigothe.campusilmenau.ui.widget.WidgetRefresh

@HiltAndroidApp
class CampusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Die Widgets bleiben aktuell, auch wenn die App nicht läuft (alle 30 Minuten, nur mit Netz).
        WidgetRefresh.schedule(this)
    }
}
