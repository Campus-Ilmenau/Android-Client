package de.kaigothe.campusilmenau.ui.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Zeiten der Ladeanzeige (Design-Übergabe, Abschnitt „Ladeanimation“). */
data class LoadingTiming(
    /** Erst danach erscheint die Ziege. Schnelle Antworten zeigen gar nichts. */
    val delay: Duration = 400.milliseconds,
    /** So lange bleibt die Ziege mindestens stehen, wenn sie einmal da war. */
    val minimumVisible: Duration = 600.milliseconds,
    /** Ein- und Ausblenden der Deckkraft. */
    val fade: Duration = 200.milliseconds,
    /** Ab hier kommt die zweite Zeile „Das dauert länger als sonst.“. */
    val slowAfter: Duration = 8.seconds,
) {
    companion object {
        val Standard = LoadingTiming()
    }
}

/**
 * Zeitverhalten der Ziege beim ersten Laden (Pendant zu `LoadingGate` der iOS-App). Die Ansicht meldet nur, ob geladen
 * wird ([setLoading]). Der Gate sagt, ob der Ladebereich den Inhalt ersetzt ([isActive]), ob die Ziege sichtbar ist
 * ([isVisible]) und ob der Hinweis für langes Laden erscheint ([isSlow]).
 *
 * Ablauf: 0 s Laden beginnt ([isActive], noch unsichtbar) → 0,4 s einblenden → mindestens 0,6 s stehen lassen →
 * 0,2 s ausblenden → [isActive] endet und der Inhalt kommt. Ab 8 s [isSlow].
 * Kommt die Antwort vor 0,4 s, wird die Ziege nie sichtbar und der Inhalt sofort gezeigt.
 *
 * Die Zeit läuft über `delay` im [scope]; Tests steuern sie mit virtueller Zeit.
 */
class LoadingGate(
    private val scope: CoroutineScope,
    initiallyLoading: Boolean = false,
    val timing: LoadingTiming = LoadingTiming.Standard,
) {
    private val _isActive = MutableStateFlow(initiallyLoading)
    private val _isVisible = MutableStateFlow(false)
    private val _isSlow = MutableStateFlow(false)

    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()
    val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()
    val isSlow: StateFlow<Boolean> = _isSlow.asStateFlow()

    private var isLoading = false

    /** Die Antwort kam, die Ziege steht aber noch ihre Mindestdauer. */
    private var finishPending = false
    private var minimumElapsed = false

    // Verzögerung und Hinweis, Mindestdauer und Ausblenden laufen als getrennte Aufgaben.
    private var loadingJob: Job? = null
    private var minimumJob: Job? = null
    private var fadeJob: Job? = null

    fun setLoading(loading: Boolean) {
        if (loading) {
            if (isLoading) return
            isLoading = true
            cancelAll()
            _isActive.value = true
            _isVisible.value = false
            _isSlow.value = false
            finishPending = false
            minimumElapsed = false
            loadingJob = scope.launch { runLoading() }
        } else {
            if (!isLoading) return
            isLoading = false
            loadingJob?.cancel()
            when {
                // Die Ziege war nie zu sehen: Der Inhalt kommt sofort.
                !_isVisible.value -> {
                    cancelAll()
                    deactivate()
                }

                minimumElapsed -> fadeOut()
                // Die Mindestdauer läuft schon und blendet danach aus.
                else -> finishPending = true
            }
        }
    }

    private fun cancelAll() {
        loadingJob?.cancel()
        minimumJob?.cancel()
        fadeJob?.cancel()
    }

    /** Einblenden nach der Verzögerung. Ab dann läuft die Mindestdauer, später kommt der Hinweis. */
    private suspend fun runLoading() {
        delay(timing.delay)
        _isVisible.value = true
        minimumJob = scope.launch { runMinimum() }
        delay(timing.slowAfter - timing.delay)
        _isSlow.value = true
    }

    private suspend fun runMinimum() {
        delay(timing.minimumVisible)
        minimumElapsed = true
        if (finishPending) fadeOut()
    }

    private fun fadeOut() {
        finishPending = false
        _isVisible.value = false
        fadeJob = scope.launch {
            delay(timing.fade)
            deactivate()
        }
    }

    private fun deactivate() {
        _isActive.value = false
        _isVisible.value = false
        _isSlow.value = false
    }
}
