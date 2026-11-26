package de.kaigothe.campusilmenau.domain.model.library

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationKindTest {

    @Test
    fun `die Art folgt den Schluesselwoertern im Text`() {
        assertEquals(NotificationKind.Provided, NotificationKind.of("„Regelungstechnik 1“ liegt zur Abholung bereit."))
        assertEquals(NotificationKind.Reminder, NotificationKind.of("„Einführung in die Informatik“ ist überfällig. Bitte gib das Medium zurück."))
        assertEquals(NotificationKind.Reminder, NotificationKind.of("Erste Mahnung"))
        assertEquals(NotificationKind.Reminder, NotificationKind.of("Mahngebühr offen"))
        assertEquals(NotificationKind.HoldExpired, NotificationKind.of("„Statistik kompakt“ wurde nicht abgeholt."))
        assertEquals(NotificationKind.HoldExpired, NotificationKind.of("Ihre Vormerkung ist abgelaufen."))
        assertEquals(NotificationKind.Other, NotificationKind.of("Die Bibliothek hat am Freitag geschlossen."))
    }

    @Test
    fun `Gross und Kleinschreibung ist egal`() {
        assertEquals(NotificationKind.Reminder, NotificationKind.of("ÜBERFÄLLIG"))
        assertEquals(NotificationKind.Provided, NotificationKind.of("BEREITGESTELLT"))
    }

    @Test
    fun `eindeutige Formulierungen gehen vor`() {
        // „nicht abgeholt“ ist keine Bereitstellung, obwohl „abhol…“ vorkommt.
        assertEquals(NotificationKind.HoldExpired, NotificationKind.of("Das Buch wurde nicht abgeholt und liegt nicht mehr bereit."))
        // „Vormerkung“ allein sagt nichts.
        assertEquals(NotificationKind.Other, NotificationKind.of("Neue Vormerkung eingegangen."))
    }
}
