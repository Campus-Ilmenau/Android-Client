package de.kaigothe.campusilmenau.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/** Ein Ziel von außen. Die [id] macht jede Anfrage einmalig, auch wenn derselbe Link zweimal kommt. */
data class DeepLinkRequest(val id: Long, val link: DeepLink)

/**
 * Nimmt Ziele von außen entgegen (Link, Widget, Shortcut) und hält das jüngste, bis es verarbeitet ist
 * (Pendant zu `DeepLinkEingang` und den „Wünschen“ des `AppRouter` in der iOS-App).
 * Die App-Shell wählt den Tab und quittiert die Anfrage mit [consume].
 * Kommt ein Ziel, bevor die App-Shell steht (Start-Animation läuft), bleibt es hier, bis es abgeholt wird.
 */
@Singleton
class DeepLinkRouter @Inject constructor() {
    private val counter = AtomicLong()
    private val _request = MutableStateFlow<DeepLinkRequest?>(null)

    val request: StateFlow<DeepLinkRequest?> = _request.asStateFlow()

    fun dispatch(link: DeepLink) {
        _request.value = DeepLinkRequest(counter.incrementAndGet(), link)
    }

    /** Quittiert die Anfrage mit dieser [id]. Eine neuere Anfrage bleibt stehen. */
    fun consume(id: Long) {
        _request.update { if (it?.id == id) null else it }
    }
}
