package de.kaigothe.campusilmenau.core.common

import javax.inject.Qualifier

/** Für Netzwerk, Datenbank und Dateien. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Für Rechenarbeit. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
