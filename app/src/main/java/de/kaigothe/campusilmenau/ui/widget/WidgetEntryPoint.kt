package de.kaigothe.campusilmenau.ui.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.kaigothe.campusilmenau.data.repository.WidgetDataSource

/** Zugang der Widgets zu den Daten der App. Ein Widget ist kein Hilt-Einstiegspunkt, deshalb der direkte Weg. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetData(): WidgetDataSource
}
