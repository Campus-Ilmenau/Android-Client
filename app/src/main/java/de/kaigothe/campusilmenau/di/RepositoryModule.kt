package de.kaigothe.campusilmenau.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.kaigothe.campusilmenau.data.remote.mensa.MensaApi
import de.kaigothe.campusilmenau.data.nfc.NfcAvailability
import de.kaigothe.campusilmenau.data.nfc.NfcThoskaReader
import de.kaigothe.campusilmenau.data.nfc.SystemNfcAvailability
import de.kaigothe.campusilmenau.data.nfc.ThoskaReader
import de.kaigothe.campusilmenau.data.remote.events.EventsApi
import de.kaigothe.campusilmenau.data.repository.RoomThoskaRepository
import de.kaigothe.campusilmenau.data.repository.ThoskaRepository
import de.kaigothe.campusilmenau.data.repository.EventsRepository
import de.kaigothe.campusilmenau.data.repository.NetworkEventsRepository
import de.kaigothe.campusilmenau.data.repository.RepositoryHeuteEventsSource
import de.kaigothe.campusilmenau.data.repository.EmptyNoticesSource
import de.kaigothe.campusilmenau.data.repository.HeuteEventsSource
import de.kaigothe.campusilmenau.data.repository.MensaRepository
import de.kaigothe.campusilmenau.data.repository.NoticesSource
import de.kaigothe.campusilmenau.data.repository.NetworkMensaRepository
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindMensaRepository(impl: NetworkMensaRepository): MensaRepository

    @Binds
    abstract fun bindHeuteEventsSource(impl: RepositoryHeuteEventsSource): HeuteEventsSource

    @Binds
    abstract fun bindNoticesSource(impl: EmptyNoticesSource): NoticesSource

    @Binds
    abstract fun bindEventsRepository(impl: NetworkEventsRepository): EventsRepository

    @Binds
    abstract fun bindThoskaRepository(impl: RoomThoskaRepository): ThoskaRepository

    @Binds
    abstract fun bindThoskaReader(impl: NfcThoskaReader): ThoskaReader

    @Binds
    abstract fun bindNfcAvailability(impl: SystemNfcAvailability): NfcAvailability

    companion object {
        @Provides
        @Singleton
        fun provideMensaApi(retrofit: Retrofit): MensaApi = retrofit.create()

        @Provides
        @Singleton
        fun provideEventsApi(retrofit: Retrofit): EventsApi = retrofit.create()
    }
}
