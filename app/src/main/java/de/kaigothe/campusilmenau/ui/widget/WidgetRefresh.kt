package de.kaigothe.campusilmenau.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import java.util.concurrent.TimeUnit

/**
 * Hält die Widgets aktuell: alle 30 Minuten werden beide neu gezeichnet (sie laden dabei den Speiseplan und die
 * Veranstaltungen). Das Widget-Framework selbst aktualisiert nicht (`updatePeriodMillis` ist 0), weil sein kleinster
 * Abstand 30 Minuten ist und es keine Bedingungen kennt.
 */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        WidgetRefresh.updateAll(applicationContext)
        return Result.success()
    }
}

object WidgetRefresh {
    private const val WorkName = "widget-refresh"

    /** Zeichnet alle platzierten Widgets neu. Ohne platzierte Widgets tut es nichts. */
    suspend fun updateAll(context: Context) {
        MensaWidget().updateAll(context)
        AbendWidget().updateAll(context)
    }

    /** Legt die regelmäßige Aktualisierung an. Eine bestehende bleibt, ein erneuter Aufruf ändert nichts. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WorkName, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
