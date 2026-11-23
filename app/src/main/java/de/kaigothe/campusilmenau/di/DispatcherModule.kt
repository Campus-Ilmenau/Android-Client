package de.kaigothe.campusilmenau.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.kaigothe.campusilmenau.core.common.DefaultDispatcher
import de.kaigothe.campusilmenau.core.common.IoDispatcher
import de.kaigothe.campusilmenau.core.time.BerlinTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /** Die Uhr der App. Tests ersetzen sie durch eine feste Uhr (`Clock.fixed`). */
    @Provides
    fun provideClock(): Clock = Clock.system(BerlinTime.zone)
}
