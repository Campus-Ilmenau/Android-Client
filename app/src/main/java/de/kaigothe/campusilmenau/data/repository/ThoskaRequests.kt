package de.kaigothe.campusilmenau.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Der Wunsch aus dem Link `campusilmenau://thoska/auslesen`: Das Auslesen soll sofort starten. Die Ansicht übernimmt ihn,
 * sobald sie sichtbar ist (der Lesemodus braucht eine Activity im Vordergrund), bei einem Kaltstart wartet er bis dahin.
 */
@Singleton
class ThoskaRequests @Inject constructor() {
    private val _scanRequested = MutableStateFlow(false)
    val scanRequested: StateFlow<Boolean> = _scanRequested.asStateFlow()

    fun requestScan() {
        _scanRequested.value = true
    }

    fun consume() {
        _scanRequested.value = false
    }
}
