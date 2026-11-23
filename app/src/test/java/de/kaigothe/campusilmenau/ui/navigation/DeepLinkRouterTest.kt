package de.kaigothe.campusilmenau.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkRouterTest {

    @Test
    fun `ohne Ziel ist nichts zu tun`() {
        assertNull(DeepLinkRouter().request.value)
    }

    @Test
    fun `dasselbe Ziel zweimal ergibt zwei verschiedene Anfragen`() {
        val router = DeepLinkRouter()
        router.dispatch(DeepLink.Events)
        val first = router.request.value
        router.dispatch(DeepLink.Events)
        val second = router.request.value

        assertEquals(DeepLink.Events, second?.link)
        assertNotEquals(first?.id, second?.id)
    }

    @Test
    fun `quittieren raeumt die Anfrage weg`() {
        val router = DeepLinkRouter()
        router.dispatch(DeepLink.Merkliste)
        router.consume(router.request.value!!.id)
        assertNull(router.request.value)
    }

    @Test
    fun `eine neuere Anfrage bleibt stehen, wenn die alte quittiert wird`() {
        val router = DeepLinkRouter()
        router.dispatch(DeepLink.Events)
        val oldId = router.request.value!!.id
        router.dispatch(DeepLink.MensaZeiten)

        router.consume(oldId)

        assertEquals(DeepLink.MensaZeiten, router.request.value?.link)
    }
}
